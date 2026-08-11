package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.FloatModules;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.LivingUpdateEvent;
import com.github.kaguya.events.PlayerUpdateEvent;
import com.github.kaguya.events.RightClickMouseEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.BlockUtil;
import com.github.kaguya.util.ItemUtil;
import com.github.kaguya.util.PlayerUtil;
import com.github.kaguya.util.TeamUtil;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.PercentProperty;
import com.github.kaguya.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;
    private int count;
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() == 1);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty killauraOnly = new BooleanProperty("killaura-only", false, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT", "GRIM"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() == 1);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT", "GRIM"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() == 1);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        if (killauraOnly.getValue()) {
            KillAura ka = (KillAura) Kaguya.moduleManager.modules.get(KillAura.class);
            if (ka == null || !ka.isEnabled() || ka.getTarget() == null) return false;
        }
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    private boolean isGrimMode() {
        return (this.swordMode.getValue() == 2 && ItemUtil.isHoldingSword())
                || (this.foodMode.getValue() == 3 && ItemUtil.isEating())
                || (this.bowMode.getValue() == 3 && ItemUtil.isUsingBow());
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        count++;
        if (ItemUtil.isHoldingSword()) {
            if (swordMode.getValue() == 2) return count % 2 == 0 ? 100 : 20;
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            if (foodMode.getValue() == 3) return count % 2 == 0 ? 100 : 20;
            return this.foodMotion.getValue();
        } else if (ItemUtil.isUsingBow()) {
            if (bowMode.getValue() == 3) return count % 2 == 0 ? 100 : 20;
            return this.bowMotion.getValue();
        }
        return 100;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Kaguya.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Kaguya.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                }
            }
            if (this.isFloatMode() && !Kaguya.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }
}
