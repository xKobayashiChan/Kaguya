package com.github.kaguya.mixins;

import com.github.kaguya.Kaguya;
import com.github.kaguya.module.modules.Xray;
import net.minecraft.block.BlockLeaves;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {BlockLeaves.class}, priority = 9999)
public abstract class MixinBlockLeaves {
    @Inject(
            method = {"getBlockLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void getBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfoReturnable) {
        if (Kaguya.moduleManager != null) {
            if (Kaguya.moduleManager.modules.get(Xray.class).isEnabled()) {
                callbackInfoReturnable.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
            }
        }
    }
}
