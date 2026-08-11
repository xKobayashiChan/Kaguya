package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.Render3DEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.property.properties.ModeProperty;
import com.github.kaguya.util.PacketUtil;
import com.github.kaguya.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.*;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty legit        = new BooleanProperty("legit",          false);
    public final BooleanProperty releaseOnHit = new BooleanProperty("release-on-hit", true,  () -> this.legit.getValue());
    public final IntProperty delay            = new IntProperty(    "delay",           400,   0, 1000);
    public final FloatProperty hitRange       = new FloatProperty(  "range",           3.0f,  3.0f, 10.0f);
    public final BooleanProperty onlyIfNeeded = new BooleanProperty("only-if-needed", true);
    public final BooleanProperty esp          = new BooleanProperty("esp",             true);
    public final ModeProperty espMode         = new ModeProperty(   "esp-mode",        0, new String[]{"Hitbox", "None"});

    private final Queue<Packet<?>> incomingPackets = new LinkedList<>();
    private final Queue<Packet<?>> outgoingPackets = new LinkedList<>();
    private final Map<Integer, Vec3> realPositions = new HashMap<>();
    private final TimerUtil timer = new TimerUtil();

    private EntityLivingBase target;
    private Vec3 lastRealPos;

    public BackTrack() {
        super("BackTrack", false);
    }

    @Override
    public void onEnabled() {
        incomingPackets.clear();
        outgoingPackets.clear();
        realPositions.clear();
        lastRealPos = null;
        timer.reset();
    }

    @Override
    public void onDisabled() {
        releaseAll();
        incomingPackets.clear();
        outgoingPackets.clear();
        realPositions.clear();
        lastRealPos = null;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        Scaffold scaffold = (Scaffold) Kaguya.moduleManager.modules.get(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            releaseAll();
            incomingPackets.clear();
            outgoingPackets.clear();
            return;
        }

        if (event.getType() == EventType.RECEIVE) {
            handleIncoming(event);
        } else if (event.getType() == EventType.SEND) {
            handleOutgoing(event);
        }
    }

    private void handleIncoming(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            Entity e = p.getEntity(mc.theWorld);
            if (e != null) {
                int id = e.getEntityId();
                Vec3 pos = realPositions.getOrDefault(id, new Vec3(0, 0, 0));
                realPositions.put(id, pos.addVector(
                        p.func_149062_c() / 32.0,
                        p.func_149061_d() / 32.0,
                        p.func_149064_e() / 32.0
                ));
            }
        }

        if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            realPositions.put(p.getEntityId(),
                    new Vec3(p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0));
        }

        if (shouldQueue()) {
            if (blockIncoming(packet)) {
                incomingPackets.add(packet);
                event.setCancelled(true);
            }
        } else {
            releaseIncoming();
        }
    }

    private void handleOutgoing(PacketEvent event) {
        if (!legit.getValue()) return;

        if (shouldQueue()) {
            if (blockOutgoing(event.getPacket())) {
                outgoingPackets.add(event.getPacket());
                event.setCancelled(true);
            }
        } else {
            releaseOutgoing();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null) return;

        EntityLivingBase newTarget = getClosestEntity();
        if (newTarget != target) {
            releaseAll();
            lastRealPos = null;
        }
        target = newTarget;
        if (target == null) return;

        Vec3 real = realPositions.get(target.getEntityId());
        if (real == null) return;

        double distReal    = mc.thePlayer.getDistance(real.xCoord, real.yCoord, real.zCoord);
        double distCurrent = mc.thePlayer.getDistanceToEntity(target);

        if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime && mc.thePlayer.maxHurtTime > 0)
            releaseAll();

        if (distReal > hitRange.getValue() || timer.hasTimeElapsed(delay.getValue()))
            releaseAll();

        if (onlyIfNeeded.getValue()) {
            if (distCurrent <= distReal) releaseAll();
            if (lastRealPos != null) {
                double lastDist = mc.thePlayer.getDistance(
                        lastRealPos.xCoord, lastRealPos.yCoord, lastRealPos.zCoord);
                if (distReal < lastDist) releaseAll();
            }
        }

        if (legit.getValue() && releaseOnHit.getValue() && target.hurtTime == 1)
            releaseAll();

        lastRealPos = real;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!esp.getValue() || espMode.getValue() != 0 || target == null) return;

        Vec3 real = realPositions.get(target.getEntityId());
        if (real == null) return;

        double x = real.xCoord - mc.getRenderManager().viewerPosX;
        double y = real.yCoord - mc.getRenderManager().viewerPosY;
        double z = real.zCoord - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB box = new AxisAlignedBB(
                x - target.width / 2, y, z - target.width / 2,
                x + target.width / 2, y + target.height, z + target.width / 2
        );

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        RenderGlobal.drawOutlinedBoundingBox(box, 255, 0, 0, 153);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private boolean shouldQueue() {
        if (target == null) return false;
        Vec3 real = realPositions.get(target.getEntityId());
        if (real == null) return false;

        double distReal    = mc.thePlayer.getDistance(real.xCoord, real.yCoord, real.zCoord);
        double distCurrent = mc.thePlayer.getDistanceToEntity(target);

        if (!onlyIfNeeded.getValue()) {
            return distReal + 0.15 < distCurrent && !timer.hasTimeElapsed(delay.getValue());
        }
        return distReal < distCurrent;
    }

    @SuppressWarnings("unchecked")
    private void releaseIncoming() {
        if (mc.getNetHandler() == null) return;
        while (!incomingPackets.isEmpty()) {
            try {
                ((Packet<INetHandlerPlayClient>) incomingPackets.poll()).processPacket(mc.getNetHandler());
            } catch (Exception ignored) {}
        }
        timer.reset();
    }

    private void releaseOutgoing() {
        while (!outgoingPackets.isEmpty())
            PacketUtil.sendPacketNoEvent(outgoingPackets.poll());
        timer.reset();
    }

    private void releaseAll() {
        releaseIncoming();
        releaseOutgoing();
    }

    private boolean blockIncoming(Packet<?> p) {
        if (!onlyIfNeeded.getValue()) {
            if (p instanceof S12PacketEntityVelocity || p instanceof S27PacketExplosion) return false;
            return p instanceof S14PacketEntity
                    || p instanceof S18PacketEntityTeleport
                    || p instanceof S19PacketEntityHeadLook
                    || p instanceof S0FPacketSpawnMob;
        }
        return p instanceof S12PacketEntityVelocity
                || p instanceof S27PacketExplosion
                || p instanceof S14PacketEntity
                || p instanceof S18PacketEntityTeleport
                || p instanceof S19PacketEntityHeadLook
                || p instanceof S0FPacketSpawnMob;
    }

    private boolean blockOutgoing(Packet<?> p) {
        return p instanceof C03PacketPlayer
                || p instanceof C02PacketUseEntity
                || p instanceof C0APacketAnimation
                || p instanceof C0BPacketEntityAction
                || p instanceof C08PacketPlayerBlockPlacement
                || p instanceof C07PacketPlayerDigging
                || p instanceof C09PacketHeldItemChange
                || p instanceof C00PacketKeepAlive;
    }

    private EntityLivingBase getClosestEntity() {
        EntityLivingBase closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                double dist = mc.thePlayer.getDistanceToEntity(entity);
                if (dist < closestDist && dist <= hitRange.getValue()) {
                    closestDist = dist;
                    closest = (EntityLivingBase) entity;
                }
            }
        }
        return closest;
    }
}
