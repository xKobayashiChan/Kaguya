package com.example.lexiyaddons.mixins;

import com.example.lexiyaddons.Myau;
import com.example.lexiyaddons.module.modules.AntiObfuscate;
import com.example.lexiyaddons.module.modules.NickHider;
import com.example.lexiyaddons.util.LangFallback;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(value = {FontRenderer.class}, priority = 9999)
public abstract class MixinFontRenderer {
    @ModifyVariable(
            method = {"renderString"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String renderString(String string) {
        string = translateIfKey(string);
        if (Myau.moduleManager == null) {
            return string;
        } else {
            AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
            if (antiObfuscate.isEnabled()) {
                string = antiObfuscate.stripObfuscated(string);
            }
            NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }

    @ModifyVariable(
            method = {"getStringWidth"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String getStringWidth(String string) {
        string = translateIfKey(string);
        if (Myau.moduleManager == null) {
            return string;
        } else {
            AntiObfuscate antiObfuscate = (AntiObfuscate) Myau.moduleManager.modules.get(AntiObfuscate.class);
            if (antiObfuscate.isEnabled()) {
                string = antiObfuscate.stripObfuscated(string);
            }
            NickHider nickHider = (NickHider) Myau.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }

    @Redirect(
            method = {"getStringWidth"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;charAt(I)C",
                    ordinal = 1
            )
    )
    private char getStringWidth(String string, int index) {
        char charAt = string.charAt(index);
        return charAt != '0'
                && charAt != '1'
                && charAt != '2'
                && charAt != '3'
                && charAt != '4'
                && charAt != '5'
                && charAt != '6'
                && charAt != '7'
                && charAt != '8'
                && charAt != '9'
                && charAt != 'a'
                && charAt != 'A'
                && charAt != 'b'
                && charAt != 'B'
                && charAt != 'c'
                && charAt != 'C'
                && charAt != 'd'
                && charAt != 'D'
                && charAt != 'e'
                && charAt != 'E'
                && charAt != 'f'
                && charAt != 'F'
                ? charAt
                : 'r';
    }

    /**
     * Last-resort translation at the FontRenderer level.
     * Catches ANY string that looks like a translation key (word.word pattern)
     * and translates it using the en_US.lang fallback map.
     */
    private String translateIfKey(String text) {
        if (text == null) {
            return null;
        }

        if (LangFallback.looksLikeTranslationKey(text)) {
            String fallback = LangFallback.translate(text);
            if (fallback != null) {
                return fallback;
            }
        }
        return text;
    }
}
