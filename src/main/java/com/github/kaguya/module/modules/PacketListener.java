package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.util.ChatUtil;

public class PacketListener extends Module {
    public final BooleanProperty logSend    = new BooleanProperty("send",    true);
    public final BooleanProperty logReceive = new BooleanProperty("receive", true);

    public PacketListener() {
        super("PacketListener", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;

        if (event.getType() == EventType.SEND && this.logSend.getValue()) {
            ChatUtil.sendFormatted("&7[&aSEND&7] &f" + event.getPacket().getClass().getSimpleName());
        } else if (event.getType() == EventType.RECEIVE && this.logReceive.getValue()) {
            ChatUtil.sendFormatted("&7[&bRECV&7] &f" + event.getPacket().getClass().getSimpleName());
        }
    }
}
