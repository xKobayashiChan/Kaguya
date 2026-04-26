package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.LoadWorldEvent;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.util.ItemUtil;
import com.github.kaguya.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KnockbackDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty airDelay    = new IntProperty("air-delay",    90,  0, 1000);
    public final IntProperty groundDelay = new IntProperty("ground-delay",  0,  0, 1000);
    public final IntProperty chance      = new IntProperty("chance",       100, 0,  100);
    public final BooleanProperty realtimeDamage  = new BooleanProperty("realtime-damage",  true);
    public final BooleanProperty requireTarget   = new BooleanProperty("require-target",   false);
    public final BooleanProperty onlySwords      = new BooleanProperty("only-swords",      false);

    private final Queue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    private boolean blink = false;

    public KnockbackDelay() {
        super("KnockbackDelay", false);
    }

    @Override
    public void onDisabled() {
        reset();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{airDelay.getValue() + " / " + groundDelay.getValue()};
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return;

        if (mc.currentScreen != null) { reset(); return; }
        if (!shouldActivate())         { reset(); return; }

        int delay = mc.thePlayer.onGround ? groundDelay.getValue() : airDelay.getValue();
        if (!packets.isEmpty()) handle(delay);

        if (mc.thePlayer.hurtTime > 0) {
            blink = true;
        } else if (packets.isEmpty()) {
            blink = false;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        reset();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20 || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S07PacketRespawn)        return;
        if (packet instanceof S03PacketTimeUpdate)      return;
        if (packet instanceof S06PacketUpdateHealth)    return;
        if (packet instanceof S13PacketDestroyEntities) return;
        if (packet instanceof S02PacketChat)            return;
        if (packet instanceof S25PacketBlockBreakAnim)  return;
        if (packet instanceof S2FPacketSetSlot)         return;

        if (packet instanceof S2BPacketChangeGameState) {
            int state = ((S2BPacketChangeGameState) packet).getGameState();
            if (state == 1 || state == 2 || state == 7 || state == 8) return;
        }

        if (realtimeDamage.getValue() && packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus status = (S19PacketEntityStatus) packet;
            if (status.getOpCode() == 2 && status.getEntity(mc.theWorld) == mc.thePlayer) return;
        }

        if (blink) {
            event.setCancelled(true);
            packets.add(new TimedPacket(packet, System.currentTimeMillis()));
        }
    }

    private boolean shouldActivate() {
        if ((int) RandomUtil.nextFloat(0, 100) > chance.getValue()) return false;
        if (requireTarget.getValue() && getTarget() == null)  return false;
        if (onlySwords.getValue() && !ItemUtil.isHoldingSword()) return false;
        return true;
    }

    private void reset() {
        if (!blink) return;
        blink = false;
        flush();
    }

    private void handle(int delay) {
        while (!packets.isEmpty()) {
            TimedPacket w = packets.peek();
            if (w != null && System.currentTimeMillis() - w.time >= delay) {
                packets.poll();
                processSilent(w.packet);
            } else {
                break;
            }
        }
    }

    private void flush() {
        TimedPacket w;
        while ((w = packets.poll()) != null) processSilent(w.packet);
    }

    @SuppressWarnings("unchecked")
    private void processSilent(Packet<?> packet) {
        try {
            if (mc.getNetHandler() != null)
                ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
        } catch (Exception ignored) {}
    }

    private net.minecraft.entity.Entity getTarget() {
        KillAura ka = (KillAura) Kaguya.moduleManager.modules.get(KillAura.class);
        if (ka != null && ka.isEnabled() && ka.getTarget() != null) return ka.getTarget();
        if (mc.pointedEntity != null) return mc.pointedEntity;
        return null;
    }

    private static class TimedPacket {
        final Packet<?> packet;
        final long time;
        TimedPacket(Packet<?> packet, long time) { this.packet = packet; this.time = time; }
    }
}
