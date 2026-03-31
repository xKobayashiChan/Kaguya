package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.module.Category;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * フルブライト — ガンマ値を最大にして暗い場所でも見えるようにする
 */
public class FullBright extends Module {
    private float previousGamma = 1.0f;

    public FullBright() {
        super("FullBright", "画面を明るくする", Category.RENDER, Keyboard.KEY_NONE);
    }

    @Override
    public void onEnable() {
        previousGamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
        Minecraft.getMinecraft().gameSettings.gammaSetting = 100.0f;
    }

    @Override
    public void onDisable() {
        Minecraft.getMinecraft().gameSettings.gammaSetting = previousGamma;
    }
}

