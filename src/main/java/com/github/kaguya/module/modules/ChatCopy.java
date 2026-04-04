package com.github.kaguya.module.modules;

import com.github.kaguya.command.commands.ChatCopyCommand;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.module.Module;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

public class ChatCopy extends Module {
    public ChatCopy() {
        super("ChatCopy", true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.RECEIVE) return;
        if (!(event.getPacket() instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.getPacket();
        if (packet.getType() == 2) return; // actionbar は無視

        IChatComponent original = packet.getChatComponent();
        String plainText = original.getUnformattedText();
        if (plainText.isEmpty()) return;

        int id = ChatCopyCommand.store(plainText);

        // [C] ボタンを作成
        ChatComponentText copyButton = new ChatComponentText(" \u00a7f[C]");
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".cc " + id));
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("\u00a7bClick to copy")));
        copyButton.setChatStyle(style);

        // 元のメッセージ + [C]
        ChatComponentText result = new ChatComponentText("");
        result.appendSibling(original);
        result.appendSibling(copyButton);

        // パケットのコンポーネントを差し替え
        try {
            java.lang.reflect.Field chatField = S02PacketChat.class.getDeclaredField("chatComponent");
            chatField.setAccessible(true);
            chatField.set(packet, result);
        } catch (Exception e) {
            // フィールド名が違う場合のフォールバック
            try {
                for (java.lang.reflect.Field f : S02PacketChat.class.getDeclaredFields()) {
                    if (IChatComponent.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(packet, result);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}

