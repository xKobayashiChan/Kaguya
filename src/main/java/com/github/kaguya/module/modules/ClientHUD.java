package com.github.kaguya.module.modules;

import com.github.kaguya.auth.AuthManager;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
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
        String userName = AuthManager.isAuthenticated() ? AuthManager.getCurrentUserId() : "User";
        String buildPrefix = "Build - " + BUILD_DATE + " - ";
        String buildText = buildPrefix + userName;
        ScaledResolution resolution = new ScaledResolution(mc);

        mc.fontRendererObj.drawStringWithShadow(brandText, 4.0f, 4.0f, 0xFFFFFFFF);
        float buildX = resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(buildText) - 4.0f;
        float buildY = resolution.getScaledHeight() - mc.fontRendererObj.FONT_HEIGHT - 4.0f;
        mc.fontRendererObj.drawStringWithShadow(buildPrefix, buildX, buildY, 0xFFFFFFFF);
        float userX = buildX + mc.fontRendererObj.getStringWidth(buildPrefix);
        mc.fontRendererObj.drawStringWithShadow(userName, userX, buildY, 0xFFAA00AA);
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

