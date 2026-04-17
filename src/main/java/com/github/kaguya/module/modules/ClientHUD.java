package com.github.kaguya.module.modules;

import com.github.kaguya.auth.AuthManager;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String BUILD_DATE = loadBuildDate();

    public final BooleanProperty showClientName = new BooleanProperty("client-name", true);
    public final BooleanProperty showUser = new BooleanProperty("user", true);
    public String clientDisplayName = "Kaguya";

    public ClientHUD() {
        super("ClientHUD", true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.fontRendererObj == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        String userName = AuthManager.isAuthenticated() ? AuthManager.getCurrentUserId() : "User";
        ScaledResolution resolution = new ScaledResolution(mc);

        // 左上: クライアント名
        if (this.showClientName.getValue()) {
            mc.fontRendererObj.drawStringWithShadow(this.clientDisplayName, 4.0f, 4.0f, 0xFFFFFFFF);
        }

        // 右下: Build - <日付> - <ユーザーID>
        if (this.showUser.getValue()) {
            String partBuild = "Build - ";
            String partSep = " - ";
            String fullText = partBuild + BUILD_DATE + partSep + userName;
            float buildY = resolution.getScaledHeight() - mc.fontRendererObj.FONT_HEIGHT - 4.0f;
            float x = resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(fullText) - 4.0f;

            mc.fontRendererObj.drawStringWithShadow(partBuild, x, buildY, 0xFFAAAAAA);
            x += mc.fontRendererObj.getStringWidth(partBuild);

            mc.fontRendererObj.drawStringWithShadow(BUILD_DATE, x, buildY, 0xFFFFFFFF);
            x += mc.fontRendererObj.getStringWidth(BUILD_DATE);

            mc.fontRendererObj.drawStringWithShadow(partSep, x, buildY, 0xFFAAAAAA);
            x += mc.fontRendererObj.getStringWidth(partSep);

            mc.fontRendererObj.drawStringWithShadow(userName, x, buildY, 0xFF55FF55);
        }
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

