package com.github.kaguya.mixins;

import com.example.lexiyaddons.util.LangFallback;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {StatCollector.class}, priority = 9999)
public abstract class MixinStatCollector {

    @Inject(method = {"translateToLocal"}, at = @At("RETURN"), cancellable = true)
    private static void translateToLocal(String key, CallbackInfoReturnable<String> cir) {
        String translated = cir.getReturnValue();
        if (!translated.equals(key)) {
            return;
        }

        String fallback = LangFallback.translate(key);
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = {"translateToLocalFormatted"}, at = @At("RETURN"), cancellable = true)
    private static void translateToLocalFormatted(String key, Object[] args, CallbackInfoReturnable<String> cir) {
        String translated = cir.getReturnValue();
        if (!translated.equals(key)) {
            return;
        }

        String fallback = LangFallback.translateFormatted(key, args);
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }
}
