package com.github.kaguya;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = KaguyaClient.MODID, name = KaguyaClient.NAME, version = KaguyaClient.VERSION)
public class KaguyaClient {
    public static final String MODID = "Kaguya";
    public static final String NAME = "Kaguya Client";
    public static final String VERSION = "v1.2.0";

    @Mod.Instance(MODID)
    private static KaguyaClient instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        System.out.println("[Kaguya Client] Initialized!");
    }

    public static KaguyaClient getInstance() {
        return instance;
    }
}
