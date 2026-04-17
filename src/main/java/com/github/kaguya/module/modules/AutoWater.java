package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.mixins.IAccessorPlayerControllerMP;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class AutoWater extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int prevSlot = -1;
    private boolean wantToPlace = false;
    private boolean wantToCollect = false;
    private boolean collecting = false;
    private int collectDelay = 0;

    public AutoWater() {
        super("AutoWater", false);
    }

    @Override
    public void onDisabled() {
        prevSlot = -1;
        wantToPlace = false;
        wantToCollect = false;
        collecting = false;
        collectDelay = 0;
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.water_bucket) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.bucket) {
                return i;
            }
        }
        return -1;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (event.getType() == EventType.PRE) {
            if (!collecting && prevSlot == -1 && mc.thePlayer.isBurning()) {
                int slot = findWaterBucketSlot();
                if (slot != -1) {
                    prevSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = slot;
                    ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                    wantToPlace = true;
                    event.setRotation(mc.thePlayer.rotationYaw, 89.9F, 50);
                }
            } else if (collecting) {
                if (collectDelay > 0) {
                    collectDelay--;
                } else {
                    int slot = findEmptyBucketSlot();
                    if (slot != -1) {
                        prevSlot = mc.thePlayer.inventory.currentItem;
                        mc.thePlayer.inventory.currentItem = slot;
                        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                        wantToCollect = true;
                        event.setRotation(mc.thePlayer.rotationYaw, 89.9F, 50);
                    } else {
                        collecting = false;
                    }
                }
            }
        } else if (event.getType() == EventType.POST) {
            if (wantToPlace) {
                wantToPlace = false;
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
                mc.thePlayer.inventory.currentItem = prevSlot;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                prevSlot = -1;
                collecting = true;
                collectDelay = 2;
            } else if (wantToCollect) {
                wantToCollect = false;
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
                mc.thePlayer.inventory.currentItem = prevSlot;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                prevSlot = -1;
                collecting = false;
            }
        }
    }
}