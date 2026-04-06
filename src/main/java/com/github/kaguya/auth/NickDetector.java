package com.github.kaguya.auth;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

import java.util.UUID;

/**
 * Hypixelのnickを半自動で検知する。
 *
 * 仕組み:
 *   - ゲーム開始時、自分の本名UUIDがタブリストから消える
 *   - nick有りの場合、本名UUIDはそのまま戻ってこない
 *   - 一定Tick後にUUIDが戻っていなければnick中と判断 → ユーザーに通知
 *   - ロビーに戻ると本名UUIDが追加される → 自動クリア
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

        if (action != S38PacketPlayerListItem.Action.ADD_PLAYER
                && action != S38PacketPlayerListItem.Action.REMOVE_PLAYER) return;

        for (S38PacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
            if (data.getProfile() == null) continue;
            if (!localUUID.equals(data.getProfile().getId())) continue;

            if (action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                // 本名UUIDがタブリストから消えた → カウントダウン開始
                countdown = NICK_DETECT_TICKS;

            } else {
                // 本名UUIDが戻ってきた → nickなし or ロビーに戻った
                countdown = -1;
                if (AuthManager.getCurrentNick() != null) {
                    AuthManager.clearNick();
                    ChatUtil.sendFormatted(Kaguya.clientName + "&eNick cleared automatically.&r");
                }
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
            // UUIDが戻ってこなかった = nick中
            ChatUtil.sendFormatted(
                Kaguya.clientName + "&6Nick mode detected!&r "
                + ".nick set <&b名前&r> で登録してください。&r"
            );
        }
    }

    private UUID getLocalUUID() {
        if (mc.getSession() == null) return null;
        com.mojang.authlib.GameProfile profile = mc.getSession().getProfile();
        return (profile != null) ? profile.getId() : null;
    }
}
