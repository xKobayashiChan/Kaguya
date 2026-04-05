package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.module.Module;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class Denick extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final long RATE_LIMIT_BACKOFF_MS = 10_000L;
    private static final long NEGATIVE_CACHE_TTL_MS = 300_000L;

    private final ConcurrentHashMap<UUID, String> realNameCache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingRequests = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> retryAfter = new ConcurrentHashMap<>();
    private ExecutorService executor;

    public Denick() {
        super("Denick", false);
    }

    /**
     * キャッシュから本名を取得する。未取得の場合はnullを返す。
     */
    public String getRealName(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return realNameCache.get(uuid);
    }

    @Override
    public void onEnabled() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Denick-Lookup");
            t.setDaemon(true);
            return t;
        });
        realNameCache.clear();
        pendingRequests.clear();
        retryAfter.clear();
    }

    @Override
    public void onDisabled() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        realNameCache.clear();
        pendingRequests.clear();
        retryAfter.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || !this.isEnabled()) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        long now = System.currentTimeMillis();
        retryAfter.entrySet().removeIf(entry -> now >= entry.getValue());
        Collection<NetworkPlayerInfo> playerInfoList = mc.getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : playerInfoList) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;
            if (realNameCache.containsKey(uuid) || pendingRequests.contains(uuid)) continue;

            Long retryTime = retryAfter.get(uuid);
            if (retryTime != null && now < retryTime) continue;

            ExecutorService localExecutor = executor;
            if (localExecutor == null) continue;
            pendingRequests.add(uuid);
            localExecutor.submit(() -> fetchRealName(uuid));
        }
    }

    private void fetchRealName(UUID uuid) {
        HttpURLConnection conn = null;
        try {
            String uuidStr = uuid.toString().replace("-", "");
            conn = (HttpURLConnection) new URL(API_URL + uuidStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    String realName = json.get("name").getAsString();
                    realNameCache.put(uuid, realName);
                }
            } else if (responseCode == 429) {
                // レート制限：バックオフ期間中は再投入しない
                retryAfter.put(uuid, System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MS);
            } else {
                // 200/429以外（404/500等）：一定時間ネガティブキャッシュ
                retryAfter.put(uuid, System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS);
            }
        } catch (Exception ignored) {
            // 例外時もネガティブキャッシュを入れてリクエスト嵐を防ぐ
            retryAfter.put(uuid, System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            pendingRequests.remove(uuid);
        }
    }
}

