package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.LexiyAddons;
import com.example.lexiyaddons.module.Category;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * ClickGUI を開くためのモジュール — 右SHIFTキーで開閉
 */
public class ClickGuiModule extends Module {

    public ClickGuiModule() {
        super("ClickGUI", "設定画面を開く", Category.MISC, Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        Minecraft.getMinecraft().displayGuiScreen(LexiyAddons.getInstance().getClickGui());
    }

    @Override
    public void onDisable() {
        // GUI のクローズは GuiScreen のライフサイクルで処理される
    }
}

