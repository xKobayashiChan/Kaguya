package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.util.MoveUtil;
import com.github.kaguya.util.RotationUtil;
import com.github.kaguya.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

import java.io.IOException;

public class TimerRangev999 extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty minRange        = new FloatProperty("min-range",          6.0F, 3.0F, 6.0F);
    public final FloatProperty maxRange        = new FloatProperty("max-range",          6.0F, 3.0F, 6.0F);
    public final FloatProperty maxTimer        = new FloatProperty("timer",              2.0F, 1.0F, 10.0F);
    public final FloatProperty slowTimer       = new FloatProperty("slow-timer",         0.0F, 0.0F, 1.0F);
    public final FloatProperty chargeMultiplier= new FloatProperty("charge-multiplier",  1.0F, 0.1F, 1.0F);
    public final IntProperty   delay           = new IntProperty(  "delay",            200,    0,  3000);
    public final BooleanProperty instantTimer  = new BooleanProperty("instant-teleport", true);
    public final BooleanProperty preLoad       = new BooleanProperty("pre-load",         false);
    public final BooleanProperty legitPreload  = new BooleanProperty("legit-preload",    false);
    public final BooleanProperty notInCombo    = new BooleanProperty("not-in-combo",     true);
    public final BooleanProperty onlyForward   = new BooleanProperty("only-forward",     true);
    public final BooleanProperty onlyOnGround  = new BooleanProperty("only-on-ground",   false);
    public final BooleanProperty noFluid       = new BooleanProperty("no-fluid",         true);

    private KillAura killAura;
    private EntityLivingBase target;
    private double balance, lastBalance, smartMaxBalance;
    private boolean fast;
    private final TimerUtil delayTimer  = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();
    private float currentTimer = 1.0F;
    public static float serverYaw;
    public static float serverPitch;

    public TimerRangev999() {
        super("TimerRangev999", false);
    }

    @Override
    public void onEnabled() {
        killAura = (KillAura) Kaguya.moduleManager.modules.get(KillAura.class);
        balance = lastBalance = smartMaxBalance = 0;
        fast = false;
    }

    @Override
    public void onDisabled() {
        setTimer(1.0F);
        fast = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        Scaffold scaffold = (Scaffold) Kaguya.moduleManager.modules.get(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            reset();
            return;
        }

        if (killAura == null || !killAura.isEnabled()) {
            reset();
            return;
        }

        target = killAura.getTarget();

        if (preLoad.getValue()) {
            if (fast) {
                balance += chargeMultiplier.getValue();
            } else {
                balance++;
            }
        } else if (fast) {
            balance += chargeMultiplier.getValue();
        } else {
            balance++;
        }

        if (!fast) {
            if (balance > lastBalance) {
                setTimer(slowTimer.getValue());
            } else {
                setTimer(1.0F);
            }
        }

        if (target == null || outOfRange()) {
            reset();
            return;
        }

        if (fast) {
            if (instantTimer.getValue()) {
                boolean shouldStop = false;
                while (!shouldStop) {
                    if (shouldStop() || balance >= lastBalance + smartMaxBalance) {
                        shouldStop = true;
                        setTimer(1.0F);
                        fast = false;
                        delayTimer.reset();
                        break;
                    }
                    try {
                        mc.runTick();
                    } catch (IOException ignored) {
                    }
                    if (attackTimer.hasTimeElapsed(350)) {
                        killAura.onUpdate(new UpdateEvent(EventType.PRE,
                                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
                                mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch));
                        attackTimer.reset();
                    }
                    balance += chargeMultiplier.getValue();
                }
                return;
            }
            setTimer(maxTimer.getValue());
            return;
        }

        if (!delayTimer.hasTimeElapsed(delay.getValue()))
            return;

        if (!shouldStop() && isHurtTime()) {
            setSmartBalance();
            lastBalance = balance;
            fast = true;
        } else {
            setTimer(slowTimer.getValue());
        }
    }

    private void setSmartBalance() {
        if (target == null) {
            smartMaxBalance = 0;
            return;
        }
        double distance = mc.thePlayer.getDistanceToEntity(target);
        double playerBPS = Math.max(MoveUtil.getBaseMoveSpeed() / 1.2, MoveUtil.getSpeed());
        double finalDistance = distance - 3.0;

        if (!preLoad.getValue()) {
            smartMaxBalance = Math.ceil(finalDistance / playerBPS);
            return;
        }

        double motionX = Math.abs(target.lastTickPosX - target.posX);
        double motionZ = Math.abs(target.lastTickPosZ - target.posZ);
        double targetBPS = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (targetBPS > 0.1) {
            targetBPS = MoveUtil.getBaseMoveSpeed();
        }
        smartMaxBalance = Math.round(finalDistance / (playerBPS + targetBPS));
    }

    private boolean shouldStop() {
        if (legitPreload.getValue() && preLoad.getValue() && !fast && balance >= lastBalance)
            return true;
        if (target == null) return true;

        double predictX = mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * 2.0;
        double predictZ = mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * 2.0;
        float dx = (float)(predictX - target.posX);
        float dy = (float)(mc.thePlayer.posY - target.posY);
        float dz = (float)(predictZ - target.posZ);
        double predictedDist = MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);

        if (onlyOnGround.getValue() && !mc.thePlayer.onGround) return true;

        if (mc.thePlayer.getDistanceToEntity(target) <= minRange.getValue()) {
            if (preLoad.getValue()) {
                if (!fast) return true;
            } else if (!fast && currentTimer != slowTimer.getValue()) {
                return true;
            }
        }

        if (isTargetVisible()) return true;
        if (!isHurtTime()) return true;
        if (outOfRange()) return true;

        if (onlyForward.getValue() &&
                (MoveUtil.getSpeed() <= 0.12
                        || !mc.gameSettings.keyBindForward.isKeyDown()
                        || predictedDist > mc.thePlayer.getDistanceToEntity(target) + 0.12))
            return true;

        if (noFluid.getValue() && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()))
            return true;

        double lastDist = mc.thePlayer.getDistance(
                target.lastTickPosX, target.lastTickPosY, target.lastTickPosZ);
        double nowDist = mc.thePlayer.getDistanceToEntity(target);

        if (nowDist > lastDist) {
            if (preLoad.getValue() && !fast) return false;
            return notInCombo.getValue();
        }

        return false;
    }

    private boolean isTargetVisible() {
        if (target == null) return false;
        MovingObjectPosition mop = RotationUtil.rayTrace(
                target.getEntityBoundingBox(), serverYaw, serverPitch, 3.0);
        if (mop != null) return true;
        return mc.objectMouseOver != null && mc.objectMouseOver.entityHit == target;
    }

    private boolean isHurtTime() {
        if (!preLoad.getValue())
            return mc.thePlayer.hurtTime <= 1;

        double distance = mc.thePlayer.getDistanceToEntity(target);
        double playerBPS = Math.max(MoveUtil.getBaseMoveSpeed() / 1.1, MoveUtil.getSpeed());
        double motionX = Math.abs(target.lastTickPosX - target.posX);
        double motionZ = Math.abs(target.lastTickPosZ - target.posZ);
        double targetBPS = Math.max(MoveUtil.getBaseMoveSpeed() / 1.1,
                Math.sqrt(motionX * motionX + motionZ * motionZ));
        double finalDistance = distance - 3.0;
        double hurtTime = finalDistance / (playerBPS + targetBPS * 1.1);
        return mc.thePlayer.hurtTime <= hurtTime;
    }

    private boolean outOfRange() {
        return mc.thePlayer.getDistanceToEntity(target) > maxRange.getValue();
    }

    private void reset() {
        setTimer(1.0F);
        fast = false;
    }

    private void setTimer(float speed) {
        currentTimer = speed;
        try {
            java.lang.reflect.Field timerField = Minecraft.class.getDeclaredField("timer");
            timerField.setAccessible(true);
            Object timer = timerField.get(mc);
            java.lang.reflect.Field speedField = timer.getClass().getDeclaredField("timerSpeed");
            speedField.setAccessible(true);
            speedField.setFloat(timer, speed);
        } catch (Exception ignored) {
        }
    }
}
