package com.github.kaguya.module.modules;

import com.github.kaguya.Myau;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.SafeWalkEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ItemUtil;
import com.github.kaguya.util.MoveUtil;
import com.github.kaguya.util.PlayerUtil;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty motion = new FloatProperty("motion", 1.0F, 0.5F, 1.0F);
    public final FloatProperty speedMotion = new FloatProperty("speed-motion", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty air = new BooleanProperty("air", false);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty pitCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", false);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);

    private boolean canSafeWalk() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if (this.blocksOnly.getValue() && !ItemUtil.isHoldingBlock()) {
            return false;
        } else {
            return (!this.requirePress.getValue() || mc.gameSettings.keyBindUseItem.isKeyDown()) && (mc.thePlayer.onGround && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)
                    || this.air.getValue() && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -2.0));
        }
    }

    public SafeWalk() {
        super("SafeWalk", false);
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled()) {
            if (this.canSafeWalk()) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && this.canSafeWalk()) {
                if (MoveUtil.getSpeedLevel() <= 0) {
                    if (this.motion.getValue() != 1.0F) {
                        MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.motion.getValue());
                    }
                } else if (this.speedMotion.getValue() != 1.0F) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.speedMotion.getValue());
                }
            }
        }
    }
}
