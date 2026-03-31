package com.example.lexiyaddons;

import com.example.lexiyaddons.gui.ClickGui;
import com.example.lexiyaddons.gui.HudRenderer;
import com.example.lexiyaddons.module.Module;
import com.example.lexiyaddons.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = LexiyAddons.MODID, name = LexiyAddons.NAME, version = LexiyAddons.VERSION)
public class LexiyAddons {
    public static final String MODID = "lexiyaddons";
    public static final String NAME = "LexiyAddons";
    public static final String VERSION = "1.0";

    @Mod.Instance(MODID)
    private static LexiyAddons instance;
    private ModuleManager moduleManager;
    private ClickGui clickGui;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this; // フォールバック
        moduleManager = new ModuleManager();
        moduleManager.init();
        clickGui = new ClickGui();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        System.out.println("[LexiyAddons] Initialized!");
    }

    /**
     * 毎ティック、有効なモジュールの onTick() を呼ぶ
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;

        for (Module module : moduleManager.getModules()) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    /**
     * キー入力でモジュールをトグル
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) {
            int keyCode = Keyboard.getEventKey();
            for (Module module : moduleManager.getModules()) {
                if (module.getKeyBind() != Keyboard.KEY_NONE && module.getKeyBind() == keyCode) {
                    module.toggle();
                }
            }
        }
    }

    public static LexiyAddons getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ClickGui getClickGui() {
        return clickGui;
    }
}

