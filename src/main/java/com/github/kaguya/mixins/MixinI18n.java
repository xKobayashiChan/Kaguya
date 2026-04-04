package com.github.kaguya.mixins;

import com.github.kaguya.util.LangFallback;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {I18n.class}, priority = 9999)
public abstract class MixinI18n {

    @Inject(method = {"format"}, at = @At("RETURN"), cancellable = true)
    private static void format(String key, Object[] args, CallbackInfoReturnable<String> cir) {
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
