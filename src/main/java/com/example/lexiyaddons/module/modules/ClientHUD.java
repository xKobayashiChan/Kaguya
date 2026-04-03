package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.event.EventTarget;
import com.example.lexiyaddons.events.Render2DEvent;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String BUILD_DATE = loadBuildDate();

    public ClientHUD() {
        super("ClientHUD", true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.fontRendererObj == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        String brandText = "Kaguya Client";
        String buildText = "Build - " + BUILD_DATE + " - User";
        ScaledResolution resolution = new ScaledResolution(mc);

        mc.fontRendererObj.drawStringWithShadow(brandText, 4.0f, 4.0f, 0xFFFFFFFF);
        float buildX = resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(buildText) - 4.0f;
        float buildY = resolution.getScaledHeight() - mc.fontRendererObj.FONT_HEIGHT - 4.0f;
        mc.fontRendererObj.drawStringWithShadow(buildText, buildX, buildY, 0xFFFFFFFF);
    }

    private static String loadBuildDate() {
        Properties properties = new Properties();
        try (InputStream stream = ClientHUD.class.getResourceAsStream("/lexiyaddons.properties")) {
            if (stream == null) {
                return "dev";
            }
            properties.load(stream);
            String buildDate = properties.getProperty("build.date", "").trim();
            if (!buildDate.isEmpty() && !buildDate.contains("${")) {
                return buildDate;
            }
        } catch (IOException ignored) {
            return "dev";
        }
        return "dev";
    }
}

