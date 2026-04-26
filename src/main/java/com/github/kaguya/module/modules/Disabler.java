package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.AttackEvent;
import com.github.kaguya.events.LoadWorldEvent;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.mixins.IAccessorC03PacketPlayer;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.*;
import com.github.kaguya.util.LatePacket;
import com.github.kaguya.util.MoveUtil;
import com.github.kaguya.util.PacketUtil;
import com.github.kaguya.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class Disabler extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty lifeboat        = new BooleanProperty("lifeboat",         false);
    public final BooleanProperty startSprint      = new BooleanProperty("start-sprint",     true);
    public final BooleanProperty grimPlace        = new BooleanProperty("grim-place",       false);
    public final BooleanProperty vulcanScaffold   = new BooleanProperty("vulcan-scaffold",  false);
    public final IntProperty vulcanPacketTick     = new IntProperty(    "vulcan-packet-tick",15, 1, 20);
    public final BooleanProperty verusFly         = new BooleanProperty("verus-fly",        false);
    public final BooleanProperty verusCombat      = new BooleanProperty("verus-combat",     false);
    public final BooleanProperty onlyCombat       = new BooleanProperty("only-combat",      true);
    public final BooleanProperty intaveFly        = new BooleanProperty("intave-fly",       false);
    public final BooleanProperty moveDisabler     = new BooleanProperty("move-disabler",    false);
    public final BooleanProperty noRotationDisabler = new BooleanProperty("no-rotation-disabler", false);
    public final ModeProperty modifyMode          = new ModeProperty("modify-mode", 0, new String[]{"ConvertNull", "Spoof", "Zero", "SpoofZero", "Negative", "OffsetYaw"});
    public final FloatProperty offsetAmount       = new FloatProperty("offset-amount", 6.0F, -180.0F, 180.0F);
    public final BooleanProperty basicDisabler    = new BooleanProperty("basic-disabler",   false);
    public final BooleanProperty cancelC00        = new BooleanProperty("cancel-c00",       true);
    public final BooleanProperty cancelC0F        = new BooleanProperty("cancel-c0f",       true);
    public final BooleanProperty cancelC0A        = new BooleanProperty("cancel-c0a",       true);
    public final BooleanProperty cancelC0B        = new BooleanProperty("cancel-c0b",       true);
    public final BooleanProperty cancelC07        = new BooleanProperty("cancel-c07",       true);
    public final BooleanProperty cancelC13        = new BooleanProperty("cancel-c13",       true);
    public final BooleanProperty cancelC03        = new BooleanProperty("cancel-c03",       true);
    public final BooleanProperty c03NoMove        = new BooleanProperty("c03-no-move",      true);
    public final BooleanProperty watchdogMotion   = new BooleanProperty("watchdog-motion",  false);
    public final BooleanProperty watchdogInventory= new BooleanProperty("watchdog-inventory",false);
    public final BooleanProperty spigotSpam       = new BooleanProperty("spigot-spam",      false);
    public final TextProperty message             = new TextProperty(   "message",           "/skill");
    public final BooleanProperty chatDebug        = new BooleanProperty("chat-debug",        false);
    public final BooleanProperty betaVerus        = new BooleanProperty("verus-beta",        false);
    public final IntProperty betaVerusBufferSize  = new IntProperty(    "buffer-size",       300, 0, 1000);
    public final IntProperty betaVerusRepeatTimes = new IntProperty(    "repeat-times",      1, 1, 5);
    public final IntProperty betaVerusRepeatTimesFighting = new IntProperty("repeat-times-fighting", 1, 1, 5);
    public final IntProperty betaVerusFlagDelay   = new IntProperty(    "flag-delay",        40, 35, 60);
    public final BooleanProperty matrixDisabler   = new BooleanProperty("matrix-disabler",  false);
    public final BooleanProperty matrixTA         = new BooleanProperty("matrix-ta",         true);
    public final BooleanProperty matrixTA188      = new BooleanProperty("matrix-ta-188",     false);
    public final FloatProperty matrixTAPacket     = new FloatProperty(  "matrix-ta-packet",  1.0F, 1.0F, 5.0F);
    public final ModeProperty matrixAB            = new ModeProperty(   "matrix-ab",         0, new String[]{"Off", "BlockHit", "Shield"});
    public final ModeProperty matrixT             = new ModeProperty(   "matrix-t",          0, new String[]{"Off", "Pingspoof", "FunnyValue", "OldCancel"});
    public final BooleanProperty matrixReach      = new BooleanProperty("matrix-reach",      false);
    public final BooleanProperty matrixAllDir     = new BooleanProperty("matrix-alldir",     false);
    public final BooleanProperty verusExperimental= new BooleanProperty("verus-experimental",false);
    public final BooleanProperty verusExpVoidTP   = new BooleanProperty("exp-void-tp",       false);
    public final IntProperty verusExpVoidTPDelay  = new IntProperty(    "exp-void-tp-delay", 1000, 0, 30000);

    private boolean shouldDelay = false;
    private final LinkedBlockingQueue<Packet<INetHandlerPlayClient>> packets = new LinkedBlockingQueue<>();
    private int flags = 0;
    private boolean execute = false;
    private boolean jump = false;
    private boolean c16 = false;
    private boolean c0d = false;
    private boolean transaction = false;
    public boolean isOnCombat = false;
    private boolean betaVerus2Stat = false;
    private boolean betaVerusModified = false;
    private final ArrayDeque<Packet<INetHandlerPlayClient>> betaVerusPacketBuffer = new ArrayDeque<>();
    private final TimerUtil betaVerusLagTimer = new TimerUtil();
    private final TimerUtil lastC00timer = new TimerUtil();
    private final TimerUtil lastSAPtimer = new TimerUtil();
    private final TimerUtil lastC03timer = new TimerUtil();
    private final TimerUtil lastFlagtimer = new TimerUtil();
    private boolean wasBlockHit = false;
    private int matrixIndex1 = 0;
    private double lastSpeed2d = 0.0;
    private int flagSkip = 0;
    private boolean predictNextC0F = false;
    private boolean shouldDelayMatrix = false;
    private int lastPongId = 0;
    private int c0fCount = 0;
    private long randomLong = 0L;
    public int savedAbusePacket = 0;
    private final ArrayDeque<Packet<?>> c00s = new ArrayDeque<>();
    private final ArrayDeque<LatePacket> c0fs = new ArrayDeque<>();
    private long lastVoidTP = 0L;
    private int cancelNext = 0;

    public Disabler() {
        super("Disabler", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        Packet<?> packet = event.getPacket();

        if (this.matrixDisabler.getValue()) handleMatrixPacket(event, packet);

        if (this.lifeboat.getValue() && packet instanceof C0FPacketConfirmTransaction) {
            event.setCancelled(true);
            debugMessage("Cancelled Lifeboat Transaction");
        }

        if (this.basicDisabler.getValue()) {
            if (packet instanceof C00PacketKeepAlive && cancelC00.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C0FPacketConfirmTransaction && cancelC0F.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C0APacketAnimation && cancelC0A.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C0BPacketEntityAction && cancelC0B.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C07PacketPlayerDigging && cancelC07.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C13PacketPlayerAbilities && cancelC13.getValue()) {
                event.setCancelled(true);
            } else if (packet instanceof C03PacketPlayer && cancelC03.getValue()) {
                C03PacketPlayer c03 = (C03PacketPlayer) packet;
                if (!(c03 instanceof C03PacketPlayer.C04PacketPlayerPosition)
                        && !(c03 instanceof C03PacketPlayer.C05PacketPlayerLook)
                        && !(c03 instanceof C03PacketPlayer.C06PacketPlayerPosLook)) {
                    if (c03NoMove.getValue() && isMoving()) return;
                    event.setCancelled(true);
                }
            }
        }

        if (this.noRotationDisabler.getValue() && packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            switch (modifyMode.getValue()) {
                case 0:
                    if (c03 instanceof C03PacketPlayer.C04PacketPlayerPosition
                            || c03 instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
                        PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, c03.isOnGround()));
                    } else {
                        PacketUtil.sendPacket(new C03PacketPlayer(c03.isOnGround()));
                    }
                    event.setCancelled(true);
                    break;
                case 3:
                    PacketUtil.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(
                            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                            0.0F, 0.0F, c03.isOnGround()));
                    event.setCancelled(true);
                    break;
            }
        }

        if (this.watchdogMotion.getValue()) {
            if (packet instanceof S07PacketRespawn) {
                flags = 0; execute = false; jump = true;
            } else if (packet instanceof S08PacketPlayerPosLook && ++flags >= 20) {
                execute = false; flags = 0;
            }
        }

        if (this.watchdogInventory.getValue()) {
            if (packet instanceof C16PacketClientStatus) {
                if (c16) event.setCancelled(true);
                c16 = true;
            }
            if (packet instanceof C0DPacketCloseWindow) {
                if (c0d) event.setCancelled(true);
                c0d = true;
            }
        }

        if (this.grimPlace.getValue() && packet instanceof C08PacketPlayerBlockPlacement) {
            event.setCancelled(true);
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }

        if (this.intaveFly.getValue()) {
            if (packet instanceof S08PacketPlayerPosLook && mc.thePlayer.capabilities.isFlying) {
                shouldDelay = true;
            }
            if (packet instanceof S32PacketConfirmTransaction && shouldDelay) {
                event.setCancelled(true);
                packets.add((Packet<INetHandlerPlayClient>) packet);
            }
        }

        if (this.verusCombat.getValue()) {
            if (mc.thePlayer.ticksExisted <= 20) { isOnCombat = false; return; }
            if (onlyCombat.getValue() && !isOnCombat) return;
            if (packet instanceof S32PacketConfirmTransaction) {
                event.setCancelled(true);
                PacketUtil.sendPacket(new C0FPacketConfirmTransaction(
                        transaction ? 1 : -1, (short)(transaction ? -1 : 1), transaction));
                transaction = !transaction;
            }
            isOnCombat = false;
        }

        if (this.betaVerus.getValue()) {
            if (!(packet instanceof C0FPacketConfirmTransaction)) {
                if (packet instanceof C03PacketPlayer) {
                    if (mc.thePlayer.ticksExisted % betaVerusFlagDelay.getValue() == 0
                            && mc.thePlayer.ticksExisted > betaVerusFlagDelay.getValue() + 1
                            && !betaVerusModified) {
                        betaVerusModified = true;
                        event.setCancelled(true);
                    }
                } else if (packet instanceof S08PacketPlayerPosLook) {
                    S08PacketPlayerPosLook s08 = (S08PacketPlayerPosLook) packet;
                    double dx = s08.getX() - mc.thePlayer.posX;
                    double dy = s08.getY() - mc.thePlayer.posY;
                    double dz = s08.getZ() - mc.thePlayer.posZ;
                    if (Math.sqrt(dx*dx + dy*dy + dz*dz) <= 8.0) {
                        event.setCancelled(true);
                        PacketUtil.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(
                                s08.getX(), s08.getY(), s08.getZ(), s08.getYaw(), s08.getPitch(), true));
                    }
                }
            } else {
                betaVerusPacketBuffer.add((Packet<INetHandlerPlayClient>) packet);
                event.setCancelled(true);
                if (betaVerusPacketBuffer.size() > betaVerusBufferSize.getValue()) {
                    if (!betaVerus2Stat) betaVerus2Stat = true;
                    Packet<INetHandlerPlayClient> p = betaVerusPacketBuffer.poll();
                    int times = isOnCombat ? betaVerusRepeatTimesFighting.getValue() : betaVerusRepeatTimes.getValue();
                    for (int i = 0; i < times; i++) PacketUtil.sendPacketNoEvent(p);
                }
            }
            if (mc.thePlayer.ticksExisted <= 7) {
                betaVerusLagTimer.reset();
                betaVerusPacketBuffer.clear();
            }
        }

        if (this.verusExperimental.getValue() && verusExpVoidTP.getValue()) {
            if (packet instanceof C03PacketPlayer) {
                if (mc.thePlayer.ticksExisted > 20 && mc.thePlayer.posY > -64.0
                        && lastVoidTP + verusExpVoidTPDelay.getValue() < System.currentTimeMillis()) {
                    lastVoidTP = System.currentTimeMillis();
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer.posX, -48.0, mc.thePlayer.posZ, true));
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround));
                    cancelNext = 2;
                    event.setCancelled(true);
                }
            } else if (packet instanceof S08PacketPlayerPosLook && cancelNext > 0) {
                --cancelNext;
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (watchdogMotion.getValue() && jump) mc.thePlayer.jump();

        if (watchdogInventory.getValue()) { c16 = false; c0d = false; }

        if (verusFly.getValue() && !isOnCombat && !mc.thePlayer.isDead) {
            BlockPos pos = mc.thePlayer.getPosition().add(0, mc.thePlayer.posY > 0 ? -255 : 255, 0);
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(
                    pos, 256, new ItemStack(Items.apple), 0.0F,
                    0.5F + (float)(Math.random() * 0.44), 0.0F));
        }

        if (vulcanScaffold.getValue() && !mc.thePlayer.isInWater() && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isDead && !mc.thePlayer.isRiding() && isMoving()
                && mc.thePlayer.ticksExisted % vulcanPacketTick.getValue() == 0) {
            PacketUtil.sendPacket(new C0BPacketEntityAction(mc.thePlayer, Action.START_SNEAKING));
            PacketUtil.sendPacket(new C0BPacketEntityAction(mc.thePlayer, Action.STOP_SNEAKING));
        }

        if (betaVerus.getValue()) {
            betaVerusModified = false;
            if (betaVerusLagTimer.hasTimeElapsed(490L)) {
                betaVerusLagTimer.reset();
                if (!betaVerusPacketBuffer.isEmpty()) {
                    Packet<INetHandlerPlayClient> p = betaVerusPacketBuffer.poll();
                    int times = isOnCombat ? betaVerusRepeatTimesFighting.getValue() : betaVerusRepeatTimes.getValue();
                    for (int i = 0; i < times; i++) PacketUtil.sendPacketNoEvent(p);
                }
            }
        }

        if (matrixDisabler.getValue() && matrixTA.getValue()) {
            double currentSpeed = MoveUtil.getSpeed();
            if (currentSpeed > 0.001) {
                double diff = Math.abs(currentSpeed - lastSpeed2d);
                if (!mc.thePlayer.onGround && diff < 1e-4) {
                    MoveUtil.setSpeed(currentSpeed * (0.99999999999 - Math.random() * 1e-7), MoveUtil.getMoveYaw());
                }
            }
            lastSpeed2d = currentSpeed;

            if (lastSAPtimer.hasTimeElapsed(50L) && savedAbusePacket <= 0) {
                ++savedAbusePacket;
                lastSAPtimer.reset();
            }

            if (lastC03timer.hasTimeElapsed(4000L) && MoveUtil.getSpeed() < 0.001) {
                lastC03timer.reset();
                shouldDelayMatrix = (double)randomLong * 1.3 < (double)c0fCount;
                randomLong = 0; c0fCount = 0;
                if (!c00s.isEmpty()) {
                    lastC00timer.reset();
                    while (!c00s.isEmpty()) PacketUtil.sendPacketNoEvent(c00s.pollFirst());
                }
            }

            if (lastFlagtimer.hasTimeElapsed(400L)) { flagSkip = 0; lastFlagtimer.reset(); }
            if (savedAbusePacket < -2) savedAbusePacket = -2;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) isOnCombat = true;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (this.isEnabled()) {
            isOnCombat = false;
            if (betaVerus.getValue()) {
                betaVerus2Stat = false;
                betaVerusPacketBuffer.clear();
                betaVerusLagTimer.reset();
            }
        }
    }

    @Override
    public void onDisabled() {
        flags = 0; execute = false; jump = false; transaction = false;
        isOnCombat = false; c16 = false; c0d = false; shouldDelay = false;
        betaVerus2Stat = false; betaVerusModified = false; cancelNext = 0;
        lastVoidTP = 0L;
        packets.clear(); betaVerusPacketBuffer.clear(); c00s.clear(); c0fs.clear();
        matrixIndex1 = 0; lastSpeed2d = 0.0; flagSkip = 0;
        predictNextC0F = false; shouldDelayMatrix = false;
        lastPongId = 0; c0fCount = 0; randomLong = 0L; savedAbusePacket = 0;
    }

    @Override
    public String[] getSuffix() {
        int active = 0;
        if (basicDisabler.getValue()) active++;
        if (verusCombat.getValue()) active++;
        if (watchdogMotion.getValue()) active++;
        if (betaVerus.getValue()) active++;
        return new String[]{String.valueOf(active)};
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    private void handleMatrixPacket(PacketEvent event, Packet<?> packet) {
        if (packet instanceof C0FPacketConfirmTransaction && matrixT.getValue() == 1) {
            if (!event.isCancelled()) {
                long firstTime = !c0fs.isEmpty() ? c0fs.getLast().getRequiredMs() : 100L;
                c0fs.add(new LatePacket(packet, System.currentTimeMillis() + 35000L));
                event.setCancelled(true);
                long secTime = !c0fs.isEmpty() ? c0fs.getLast().getRequiredMs() : 200L;
                if (secTime - firstTime >= 20L) c0fs.pollLast();
            }
            while (!c0fs.isEmpty() && c0fs.peekFirst().getRequiredMs() <= System.currentTimeMillis()) {
                PacketUtil.sendPacketNoEvent(c0fs.pollFirst().getPacket());
            }
            ++c0fCount;
        }

        if (packet instanceof C00PacketKeepAlive && matrixT.getValue() == 1) {
            if (c00s.isEmpty()) lastC00timer.reset();
            c00s.add(packet);
            event.setCancelled(true);
        }

        if (packet instanceof C03PacketPlayer && matrixT.getValue() == 1) {
            --savedAbusePacket;
            lastC03timer.reset();
            if (savedAbusePacket < 5 && !c0fs.isEmpty()) {
                long firstTime = c0fs.getFirst().getRequiredMs();
                PacketUtil.sendPacketNoEvent(c0fs.pollFirst().getPacket());
                if (!c0fs.isEmpty() && c0fs.getFirst().getRequiredMs() - firstTime < 20L) {
                    PacketUtil.sendPacketNoEvent(c0fs.pollFirst().getPacket());
                }
            }
            if (lastC00timer.hasTimeElapsed(10000L)) {
                shouldDelayMatrix = (double)randomLong * 1.3 < (double)c0fCount;
                randomLong = 0; c0fCount = 0;
                if (!c00s.isEmpty()) {
                    lastC00timer.reset();
                    while (!c00s.isEmpty()) PacketUtil.sendPacketNoEvent(c00s.pollFirst());
                }
            }
            ++randomLong;
        }

        if (packet instanceof S08PacketPlayerPosLook && event.getType() == EventType.RECEIVE) {
            if (flagSkip > 0) { --flagSkip; event.setCancelled(true); }
            lastFlagtimer.reset();
        }

        if (packet instanceof S07PacketRespawn && event.getType() == EventType.RECEIVE) {
            savedAbusePacket = 0; flagSkip = 0; lastSAPtimer.reset();
        }
    }

    private void debugMessage(String msg) {
        if (chatDebug.getValue()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bDisabler§7] §f" + msg));
        }
    }
}
