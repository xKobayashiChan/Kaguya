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
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class Denick extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final ConcurrentHashMap<UUID, String> realNameCache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingRequests = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Denick-Lookup");
        t.setDaemon(true);
        return t;
    });

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
    public void onDisabled() {
        realNameCache.clear();
        pendingRequests.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || !this.isEnabled()) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        Collection<NetworkPlayerInfo> playerInfoList = mc.getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : playerInfoList) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;
            if (realNameCache.containsKey(uuid) || pendingRequests.contains(uuid)) continue;

            pendingRequests.add(uuid);
            executor.submit(() -> fetchRealName(uuid));
        }
    }

    private void fetchRealName(UUID uuid) {
        try {
            String uuidStr = uuid.toString().replace("-", "");
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + uuidStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    String realName = json.get("name").getAsString();
                    realNameCache.put(uuid, realName);
                }
            } else if (responseCode == 429) {
                // レート制限に達した場合、少し待ってからリトライできるようにする
                pendingRequests.remove(uuid);
                Thread.sleep(10000);
            }
        } catch (Exception ignored) {
        } finally {
            pendingRequests.remove(uuid);
        }
    }
}

