package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class FlagDetector extends Module {
    public FlagDetector() {
        super("FlagDetector", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            ChatUtil.sendFormatted("&7[&cFlagDetector&7] &fServer flag detected (Lagback)!");
        }
    }
}
