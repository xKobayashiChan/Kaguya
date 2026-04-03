package com.example.lexiyaddons;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = LexiyAddons.MODID, name = LexiyAddons.NAME, version = LexiyAddons.VERSION)
public class LexiyAddons {
    public static final String MODID = "lexiyaddons";
    public static final String NAME = "Kaguya Client";
    public static final String VERSION = "v1.1.0";

    @Mod.Instance(MODID)
    private static LexiyAddons instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        System.out.println("[Kaguya Client] Initialized!");
    }

    public static LexiyAddons getInstance() {
        return instance;
    }
}
