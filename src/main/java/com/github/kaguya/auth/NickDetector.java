package com.github.kaguya.auth;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

import java.util.Collection;
import java.util.UUID;

/**
 * サーバーのnickを自動検知する。
 *
 * 仕組み（2パターン対応）:
 *
 * [パターンA] UUIDが消えるタイプ (Hypixel等):
 *   - ゲーム開始時、自分の本名UUIDがタブリストから消える
 *   - nick有りの場合、本名UUIDはそのまま戻ってこない
 *   - 一定Tick後にUUIDが戻っていなければnick中と判断
 *   - ロビーに戻ると本名UUIDが追加される → 自動クリア
 *   - カウントダウン完了時にタブリストを走査してnick名の自動取得を試みる
 *
 * [パターンB] DisplayNameが変わるタイプ:
 *   - UUIDはタブリストに残り、UPDATE_DISPLAY_NAME でnick名に変わる
 *   - パケット受信時に即座にnick名を自動取得・登録する
 */
public class NickDetector {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 本名UUIDが消えてからこのTick数以内に戻ってこなければnick判定
    // ゲーム開始のロード時間を考慮して余裕を持たせる（200tick = 10秒）
    private static final int NICK_DETECT_TICKS = 200;

    // カウントダウン（-1 = 待機中でない）
    private int countdown = -1;

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (!(event.getPacket() instanceof S38PacketPlayerListItem)) return;

        UUID localUUID = getLocalUUID();
        if (localUUID == null) return;

        S38PacketPlayerListItem packet = (S38PacketPlayerListItem) event.getPacket();
        S38PacketPlayerListItem.Action action = packet.getAction();

        for (S38PacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
            if (data.getProfile() == null) continue;
            if (!localUUID.equals(data.getProfile().getId())) continue;

            if (action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                // [パターンA] 本名UUIDがタブリストから消えた → カウントダウン開始
                countdown = NICK_DETECT_TICKS;

            } else if (action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                // 本名UUIDが戻ってきた → nickなし or ロビーに戻った
                countdown = -1;
                if (AuthManager.getCurrentNick() != null) {
                    AuthManager.clearNick();
                    ChatUtil.sendFormatted(Kaguya.clientName + "&eNick cleared automatically.&r");
                }

            } else if (action == S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME) {
                // [パターンB] UUIDはそのまま、表示名だけ変わった
                handleDisplayNameUpdate(data);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (countdown < 0) return;

        countdown--;

        if (countdown == 0) {
            countdown = -1;
            // [パターンA] UUIDが戻ってこなかった = nick中
            // まずタブリスト走査で自動取得を試みる
            if (!tryAutoDetectNickFromTabList()) {
                ChatUtil.sendFormatted(
                    Kaguya.clientName + "&6Nick mode detected!&r "
                    + ".nick set <&b名前&r> で登録してください。&r"
                );
            }
        }
    }

    /**
     * [パターンB] UPDATE_DISPLAY_NAME パケット受信時の処理。
     * 表示名が本名と異なれば nick として自動登録する。
     */
    private void handleDisplayNameUpdate(S38PacketPlayerListItem.AddPlayerData data) {
        if (data.getDisplayName() == null) {
            // 表示名がnullにリセット = nick解除
            if (AuthManager.getCurrentNick() != null) {
                AuthManager.clearNick();
                ChatUtil.sendFormatted(Kaguya.clientName + "&eNick cleared automatically.&r");
            }
            return;
        }

        String displayName = data.getDisplayName().getUnformattedText();
        String realName = getRealName();
        if (realName == null || displayName.equals(realName)) return;

        // 表示名が本名と違う = nick中
        if (!displayName.equals(AuthManager.getCurrentNick())) {
            AuthManager.setNick(displayName);
            ChatUtil.sendFormatted(
                Kaguya.clientName + "&6Nick auto-detected: &b" + displayName + "&r (registered automatically)"
            );
        }
    }

    /**
     * [パターンA用] タブリストを走査して自分のUUID エントリの表示名を取得し、
     * nick として自動登録を試みる。成功した場合 true を返す。
     *
     * PacketEvent は処理前に発火するが、onTick 時点ではパケット処理済みのため
     * UUID エントリはすでに消えている可能性がある。
     * その場合は mc.thePlayer のエンティティ名を fallback として使用する。
     */
    private boolean tryAutoDetectNickFromTabList() {
        if (mc.getNetHandler() == null) return false;

        UUID localUUID = getLocalUUID();
        String realName = getRealName();
        if (localUUID == null || realName == null) return false;

        // UUIDでタブエントリを検索
        Collection<NetworkPlayerInfo> playerList = mc.getNetHandler().getPlayerInfoMap();
        if (playerList != null) {
            for (NetworkPlayerInfo info : playerList) {
                if (info == null || info.getGameProfile() == null) continue;
                if (!localUUID.equals(info.getGameProfile().getId())) continue;

                if (info.getDisplayName() != null) {
                    String displayName = info.getDisplayName().getUnformattedText();
                    if (!displayName.isEmpty() && !displayName.equals(realName)) {
                        AuthManager.setNick(displayName);
                        ChatUtil.sendFormatted(
                            Kaguya.clientName + "&6Nick auto-detected: &b" + displayName + "&r (registered automatically)"
                        );
                        return true;
                    }
                }
                // UUID エントリはあるが displayName が本名と同じ = nick なし
                return false;
            }
        }

        // UUID エントリが消えている場合、エンティティ名を fallback として確認
        if (mc.thePlayer != null) {
            String entityName = mc.thePlayer.getName();
            if (entityName != null && !entityName.isEmpty() && !entityName.equals(realName)) {
                AuthManager.setNick(entityName);
                ChatUtil.sendFormatted(
                    Kaguya.clientName + "&6Nick auto-detected: &b" + entityName + "&r (registered automatically)"
                );
                return true;
            }
        }

        return false;
    }

    private UUID getLocalUUID() {
        if (mc.getSession() == null) return null;
        com.mojang.authlib.GameProfile profile = mc.getSession().getProfile();
        return (profile != null) ? profile.getId() : null;
    }

    private String getRealName() {
        if (mc.getSession() == null) return null;
        return mc.getSession().getUsername();
    }
}
