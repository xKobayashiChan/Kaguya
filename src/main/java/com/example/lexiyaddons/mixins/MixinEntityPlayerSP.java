package com.example.lexiyaddons.mixins;

import com.example.lexiyaddons.command.CommandHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    /**
     * チャット送信時に "@" で始まるメッセージをコマンドとして処理し、
     * サーバーへの送信をキャンセルする
     */
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (CommandHandler.handleCommand(message)) {
            ci.cancel();
        }
    }
}

