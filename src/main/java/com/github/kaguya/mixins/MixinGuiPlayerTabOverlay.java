package com.github.kaguya.mixins;

import com.github.kaguya.Kaguya;
import com.github.kaguya.auth.AuthManager;
import com.github.kaguya.enums.ChatColors;
import com.github.kaguya.module.modules.Denick;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiPlayerTabOverlay.class}, priority = 9999)
public abstract class MixinGuiPlayerTabOverlay {

    @Inject(
            method = {"getPlayerName"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void onGetPlayerName(NetworkPlayerInfo networkPlayerInfo, CallbackInfoReturnable<String> cir) {
        if (Kaguya.friendManager != null) {
            String name = networkPlayerInfo.getGameProfile().getName();
            String original = cir.getReturnValue();

            // フレンド表示
            if (Kaguya.friendManager.isFriend(name)) {
                String prefix = ChatColors.formatColor("&b[FRIEND] &r");
                original = prefix + original;
            }

            // KaguyaユーザーならIDを紫色で表示（自分含む全員）
            if (AuthManager.isAuthenticated()) {
                String kaguyaId = AuthManager.getUserIdByMcName(name);
                if (kaguyaId != null) {
                    original = original + ChatColors.formatColor(" &5- " + kaguyaId);
                }
            }

            cir.setReturnValue(original);
        }

        if (Kaguya.moduleManager != null) {
            Denick denick = (Denick) Kaguya.moduleManager.modules.get(Denick.class);
            if (denick != null && denick.isEnabled()) {
                UUID uuid = networkPlayerInfo.getGameProfile().getId();
                String realName = denick.getRealName(uuid);
                if (realName != null) {
                    String currentName = networkPlayerInfo.getGameProfile().getName();
                    if (!realName.equalsIgnoreCase(currentName)) {
                        String original = cir.getReturnValue();
                        cir.setReturnValue(original + " §7(" + realName + ")");
                    }
                }
            }
        }
    }
}

