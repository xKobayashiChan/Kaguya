package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.BlinkModules;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.MoveInputEvent;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Displace extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DISPLACE_WINDOW_TICKS = 10;

    public final FloatProperty yawOffset   = new FloatProperty("yaw-offset",  90.0F, 0.0F, 180.0F);
    public final FloatProperty delay       = new FloatProperty("delay",         0.0F, 0.0F, 500.0F);
    public final ModeProperty direction    = new ModeProperty( "direction",     0, new String[]{"Left", "Right"});
    public final BooleanProperty findVoid  = new BooleanProperty("find-void",  false);
    public final BooleanProperty blink     = new BooleanProperty("blink",      false);
    public final BooleanProperty hasKnockback = new BooleanProperty("has-knockback", false);

    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean releaseBlinkNextGameTick = false;
    private int tickCounter = 0;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    public Displace() {
        super("Displace", false);
    }

    @Override
    public void onEnabled() {
        displaceThisTick = false; active = false; hasKB = false;
        compensateNextTick = false; wasDisplacingLastTick = false;
        releaseBlinkNextGameTick = false; tickCounter = 0;
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    @Override
    public void onDisabled() {
        active = false; compensateNextTick = false;
        wasDisplacingLastTick = false; releaseBlinkNextGameTick = false;
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{(int) Math.round(delay.getValue()) + "ms"};
    }

    private static int msToTicks(double ms) {
        return ms <= 0 ? 0 : (int) Math.ceil(ms / 50.0);
    }

    private boolean anyMovementKey() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private boolean tryFindVoidDirection(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return false;
        dx /= dist; dz /= dist;
        double rightX = -dz, rightZ = dx;
        double eyeY = target.posY + target.getEyeHeight();
        int leftVoid = 0, rightVoid = 0;
        for (int i = 1; i <= 12; i++) {
            double off = i * 0.5;
            if (mc.theWorld.rayTraceBlocks(new Vec3(target.posX + rightX*off, eyeY, target.posZ + rightZ*off),
                    new Vec3(target.posX + rightX*off, eyeY - 10, target.posZ + rightZ*off)) == null) rightVoid++;
            if (mc.theWorld.rayTraceBlocks(new Vec3(target.posX - rightX*off, eyeY, target.posZ - rightZ*off),
                    new Vec3(target.posX - rightX*off, eyeY - 10, target.posZ - rightZ*off)) == null) leftVoid++;
        }
        if (leftVoid == 0 && rightVoid == 0) return false;
        if (leftVoid != rightVoid) displaceLeft = leftVoid > rightVoid;
        return true;
    }

    private void pruneTargetDelayStates() {
        if (mc.theWorld == null) { targetWindowStartTicks.clear(); return; }
        Iterator<Map.Entry<Integer, Integer>> it = targetWindowStartTicks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            Entity entity = mc.theWorld.getEntityByID(e.getKey());
            if (!(entity instanceof EntityPlayer) || entity.isDead || ((EntityPlayer) entity).deathTime != 0)
                it.remove();
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) return true;
        int id = target.getEntityId();
        Integer windowStart = targetWindowStartTicks.get(id);
        if (windowStart == null || currentTick - windowStart >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks.put(id, currentTick);
            return true;
        }
        int delayTicks = msToTicks(delay.getValue());
        if (delayTicks <= 0) return true;
        return currentTick - windowStart >= delayTicks;
    }

    private void releaseBlink() {
        Kaguya.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (releaseBlinkNextGameTick) {
            releaseBlink();
            releaseBlinkNextGameTick = false;
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled()) { compensateNextTick = false; return; }
        if (!active) { compensateNextTick = false; return; }

        if (compensateNextTick && !displaceThisTick) {
            compensateNextTick = false;
            mc.thePlayer.movementInput.moveStrafe = displaceLeft ? -1 : 1;
            return;
        }

        if (!displaceThisTick || hasKB) return;
        if (!anyMovementKey()) return;

        mc.thePlayer.movementInput.moveForward = 1;
        compensateNextTick = true;
    }

    @EventTarget(Priority.HIGH)
    public void onSendPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.POST) return;
        if (!blink.getValue() || !active || !displaceThisTick || releaseBlinkNextGameTick) return;
        if (!(event.getPacket() instanceof C03PacketPlayer)) return;
        if (Kaguya.blinkManager.getBlinkingModule() == BlinkModules.DISPLACE) return;

        Kaguya.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
        releaseBlinkNextGameTick = true;
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            active = false; compensateNextTick = false; wasDisplacingLastTick = false; return;
        }

        tickCounter++;
        pruneTargetDelayStates();

        if (hasKnockback.getValue() && EnchantmentHelper.getKnockbackModifier(mc.thePlayer) == 0) {
            active = false; displaceThisTick = false; compensateNextTick = false;
            wasDisplacingLastTick = false; return;
        }

        boolean attacking = Mouse.isButtonDown(0) || isKillAuraActive();
        EntityPlayer target = attacking ? findClosestTarget(9.0) : null;
        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        active = target != null && (hasKBEnchant || anyMovementKey());

        if (!active) {
            displaceThisTick = false; compensateNextTick = false;
            wasDisplacingLastTick = false; return;
        }

        if (!findVoid.getValue() || !tryFindVoidDirection(target))
            displaceLeft = direction.getValue() == 0;

        hasKB = hasKBEnchant;
        displaceThisTick = !displaceThisTick;

        if (displaceThisTick && !shouldDisplaceInCurrentWindow(target, tickCounter)) {
            displaceThisTick = false; compensateNextTick = false;
            wasDisplacingLastTick = false; return;
        }

        if (!displaceThisTick && wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) KeyBinding.onTick(key);
        }

        wasDisplacingLastTick = displaceThisTick;
        if (!displaceThisTick) return;

        float baseYaw = mc.thePlayer.rotationYaw + (displaceLeft ? -yawOffset.getValue() : yawOffset.getValue());
        event.setRotation(baseYaw, mc.thePlayer.rotationPitch, 1);
        event.setPervRotation(baseYaw, 1);
    }

    private boolean isKillAuraActive() {
        KillAura ka = (KillAura) Kaguya.moduleManager.modules.get(KillAura.class);
        return ka != null && ka.isEnabled() && ka.getTarget() != null;
    }

    private EntityPlayer findClosestTarget(double maxRange) {
        EntityPlayer closest = null;
        double closestDist = maxRange;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;
            if (entity.isDead || ((EntityPlayer) entity).deathTime != 0) continue;
            double dist = mc.thePlayer.getDistanceToEntity(entity);
            if (dist < closestDist) { closest = (EntityPlayer) entity; closestDist = dist; }
        }
        return closest;
    }
}
