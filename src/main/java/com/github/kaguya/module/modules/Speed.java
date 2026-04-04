package com.github.kaguya.module.modules;

import com.github.kaguya.Myau;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.LivingUpdateEvent;
import com.github.kaguya.events.StrafeEvent;
import com.github.kaguya.mixins.IAccessorEntity;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.MoveUtil;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class Speed extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty multiplier = new FloatProperty("multiplier", 1.0F, 0.0F, 10.0F);
    public final FloatProperty friction = new FloatProperty("friction", 1.0F, 0.0F, 10.0F);
    public final PercentProperty strafe = new PercentProperty("strafe", 0);

    private boolean canBoost() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return !scaffold.isEnabled() && MoveUtil.isForwardPressed()
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava()
                && !((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    public Speed() {
        super("Speed", false);
    }

    @EventTarget(Priority.LOW)
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && this.canBoost()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42F;
                MoveUtil.setSpeed(
                        MoveUtil.getJumpMotion() * (double) this.multiplier.getValue().floatValue(),
                        MoveUtil.getMoveYaw()
                );
            } else {
                if (this.friction.getValue() != 1.0F) {
                    event.setFriction(event.getFriction() * this.friction.getValue());
                }
                if (this.strafe.getValue() > 0) {
                    double speed = MoveUtil.getSpeed();
                    MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getDirectionYaw());
                    MoveUtil.addSpeed(
                            speed * (double) ((float) this.strafe.getValue().intValue() / 100.0F), MoveUtil.getMoveYaw()
                    );
                    MoveUtil.setSpeed(speed);
                }
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.canBoost()) {
            mc.thePlayer.movementInput.jump = false;
        }
    }
}
