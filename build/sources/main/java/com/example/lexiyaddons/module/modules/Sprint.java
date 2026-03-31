package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.module.Category;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * 自動スプリント — 前に移動しているとき自動でダッシュする
 */
public class Sprint extends Module {

    public Sprint() {
        super("Sprint", "自動でスプリントする", Category.MOVEMENT, Keyboard.KEY_NONE);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSneaking()) {
            mc.thePlayer.setSprinting(true);
        }
    }
}

