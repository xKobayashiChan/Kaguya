package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.KeyEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MCF extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCF() {
        super("MCF", false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (this.isEnabled() && event.getKey() == -98) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                String hitName = mc.objectMouseOver.entityHit.getName();
                if (!Kaguya.friendManager.isFriend(hitName)) {
                    Kaguya.friendManager.add(hitName);
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Kaguya.clientName, hitName));
                } else {
                    Kaguya.friendManager.remove(hitName);
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Kaguya.clientName, hitName));
                }
            }
        }
    }
}
