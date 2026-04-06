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

        String userName = AuthManager.isAuthenticated() ? AuthManager.getCurrentUserId() : "User";
        ScaledResolution resolution = new ScaledResolution(mc);

        // 左上: クライアント名
        mc.fontRendererObj.drawStringWithShadow("Kaguya Client", 4.0f, 4.0f, 0xFFFFFFFF);

        // 右下: Build - <日付> - <ユーザーID>
        String partBuild = "Build - ";
        String partSep = " - ";
        String fullText = partBuild + BUILD_DATE + partSep + userName;
        float buildY = resolution.getScaledHeight() - mc.fontRendererObj.FONT_HEIGHT - 4.0f;
        float x = resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(fullText) - 4.0f;

        // "Build - " 薄いグレー
        mc.fontRendererObj.drawStringWithShadow(partBuild, x, buildY, 0xFFAAAAAA);
        x += mc.fontRendererObj.getStringWidth(partBuild);

        // ビルド日 真っ白
        mc.fontRendererObj.drawStringWithShadow(BUILD_DATE, x, buildY, 0xFFFFFFFF);
        x += mc.fontRendererObj.getStringWidth(BUILD_DATE);

        // " - " 薄いグレー
        mc.fontRendererObj.drawStringWithShadow(partSep, x, buildY, 0xFFAAAAAA);
        x += mc.fontRendererObj.getStringWidth(partSep);

        // ユーザーID 黄緑色
        mc.fontRendererObj.drawStringWithShadow(userName, x, buildY, 0xFF55FF55);
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

