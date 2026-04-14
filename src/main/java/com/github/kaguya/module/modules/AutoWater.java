package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.LeftClickMouseEvent;
import com.github.kaguya.events.RightClickMouseEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.mixins.IAccessorPlayerControllerMP;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;

public class AutoWater extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int prevSlot = -1;

    public AutoWater() {
        super("AutoWater", false);
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

    public boolean isSwitching() {
        return this.prevSlot != -1;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            this.prevSlot = -1;
            return;
        }

        switch (event.getType()) {
            case PRE:
                if (this.prevSlot == -1 && mc.thePlayer.isBurning()) {
                    int slot = findWaterBucketSlot();
                    if (slot != -1) {
                        this.prevSlot = mc.thePlayer.inventory.currentItem;
                        mc.thePlayer.inventory.currentItem = slot;
                        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
                        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(
                                below, 1, mc.thePlayer.inventory.getCurrentItem(), 0.5f, 1.0f, 0.5f));
                    }
                }
                break;
            case POST:
                if (this.prevSlot != -1) {
                    mc.thePlayer.inventory.currentItem = this.prevSlot;
                    ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                    this.prevSlot = -1;
                }
                break;
        }
    }

    // スイッチ中に誤クリックしないようガード
    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isSwitching()) {
            event.setCancelled(true);
        }
    }
}