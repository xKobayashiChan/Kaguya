package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.LoadWorldEvent;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerLag extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty lagMs = new IntProperty("lag-ms", 1000, 500, 30000);

    private final ConcurrentLinkedQueue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private int currentLatency = 0;

    public ServerLag() {
        super("ServerLag", false);
    }

    @Override
    public void onEnabled() {
        currentLatency = lagMs.getValue();
    }

    @Override
    public void onDisabled() {
        releaseAll();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.RECEIVE || event.isCancelled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (currentLatency == 0) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S19PacketEntityStatus
                || packet instanceof S02PacketChat
                || packet instanceof S0BPacketAnimation
                || packet instanceof S06PacketUpdateHealth) return;

        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) {
            releaseAll();
            return;
        }

        @SuppressWarnings("unchecked")
        Packet<INetHandlerPlayClient> playPacket = (Packet<INetHandlerPlayClient>) packet;
        packetQueue.add(new TimedPacket(playPacket, System.currentTimeMillis()));
        event.setCancelled(true);

        while (!packetQueue.isEmpty()) {
            TimedPacket first = packetQueue.peek();
            if (first != null && System.currentTimeMillis() - first.time >= currentLatency) {
                packetQueue.poll();
                try {
                    first.packet.processPacket(mc.getNetHandler());
                } catch (Exception ignored) {}
                if (packetQueue.isEmpty()) currentLatency = 0;
            } else {
                break;
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        releaseAll();
    }

    private void releaseAll() {
        for (TimedPacket tp : packetQueue) {
            try {
                tp.packet.processPacket(mc.getNetHandler());
            } catch (Exception ignored) {}
        }
        packetQueue.clear();
        currentLatency = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{lagMs.getValue() + "ms"};
    }

    private static class TimedPacket {
        final Packet<INetHandlerPlayClient> packet;
        final long time;

        TimedPacket(Packet<INetHandlerPlayClient> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
