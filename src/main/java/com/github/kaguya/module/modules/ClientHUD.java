package com.github.kaguya.module.modules;

import com.github.kaguya.auth.AuthManager;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.ModeProperty;
import com.github.kaguya.property.properties.PercentProperty;
import com.github.kaguya.mixins.IAccessorMinecraft;
import com.github.kaguya.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class ClientHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String BUILD_DATE = loadBuildDate();

    public final BooleanProperty showClientName = new BooleanProperty("client-name", true);
    public final BooleanProperty showUser = new BooleanProperty("user", true);
    public final PercentProperty userBackground = new PercentProperty("user-background", 40, () -> this.showUser.getValue());
    public final ModeProperty infoMode = new ModeProperty(
            "info-mode", 0, new String[]{"FPS", "PING", "TIME", "NONE"}
    );
    public String clientDisplayName = "Kaguya";

    private static final int COLOR_GRAY  = 0xFFAAAAAA;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

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

        // 左上: クライアント名 + info
        if (this.showClientName.getValue()) {
            float x = 4.0f;
            mc.fontRendererObj.drawStringWithShadow(this.clientDisplayName, x, 4.0f, COLOR_WHITE);
            x += mc.fontRendererObj.getStringWidth(this.clientDisplayName);

            int mode = this.infoMode.getValue();
            if (mode != 3) { // NONE以外
                String infoValue = getInfoValue(mode);
                String bracket1 = " [";
                String bracket2 = "]";

                mc.fontRendererObj.drawStringWithShadow(bracket1, x, 4.0f, COLOR_GRAY);
                x += mc.fontRendererObj.getStringWidth(bracket1);

                mc.fontRendererObj.drawStringWithShadow(infoValue, x, 4.0f, COLOR_WHITE);
                x += mc.fontRendererObj.getStringWidth(infoValue);

                mc.fontRendererObj.drawStringWithShadow(bracket2, x, 4.0f, COLOR_GRAY);
            }
        }

        // 右下: Development/Release - [日付 -] <ユーザーID>
        if (this.showUser.getValue()) {
            boolean isDev = "dev".equals(BUILD_DATE);
            String partLabel = isDev ? "Development" : "Release";
            String partSep   = " - ";
            String displayDate = isDev ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()) : BUILD_DATE;
            String fullText  = partLabel + partSep + displayDate + partSep + userName;
            float buildY = resolution.getScaledHeight() - mc.fontRendererObj.FONT_HEIGHT - 4.0f;
            float x = resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(fullText) - 4.0f;

            int bgOpacity = this.userBackground.getValue();
            if (bgOpacity > 0) {
                int bgColor = new java.awt.Color(0f, 0f, 0f, bgOpacity / 100.0f).getRGB();
                RenderUtil.enableRenderState();
                RenderUtil.drawRect(
                        x - 5,
                        buildY - 2,
                        x + mc.fontRendererObj.getStringWidth(fullText) + 5,
                        buildY + mc.fontRendererObj.FONT_HEIGHT + 2,
                        bgColor
                );
                RenderUtil.disableRenderState();
            }

            HideClientText.clientHUDRendering = true;
            mc.fontRendererObj.drawStringWithShadow(partLabel, x, buildY, 0xFF55FF55);
            x += mc.fontRendererObj.getStringWidth(partLabel);

            mc.fontRendererObj.drawStringWithShadow(partSep, x, buildY, 0xFFAAAAAA);
            x += mc.fontRendererObj.getStringWidth(partSep);

            mc.fontRendererObj.drawStringWithShadow(displayDate, x, buildY, 0xFFFFFFFF);
            x += mc.fontRendererObj.getStringWidth(BUILD_DATE);

            mc.fontRendererObj.drawStringWithShadow(partSep, x, buildY, 0xFFAAAAAA);
            x += mc.fontRendererObj.getStringWidth(partSep);

            mc.fontRendererObj.drawStringWithShadow(userName, x, buildY, 0xFFFFFFFF);
            HideClientText.clientHUDRendering = false;
        }
    }

    private String getInfoValue(int mode) {
        switch (mode) {
            case 0: { // FPS
                int fps = ((IAccessorMinecraft)(Object)mc).getDebugFPS();
                double ms = fps > 0 ? 1000.0 / fps : 0;
                return fps + " fps of " + String.format("%.1f", ms) + "ms";
            }
            case 1: { // PING
                if (mc.thePlayer != null && mc.getNetHandler() != null) {
                    NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                    if (info != null) {
                        return info.getResponseTime() + "ms";
                    }
                }
                return "---ms";
            }
            case 2: { // TIME
                return TIME_FORMAT.format(new Date());
            }
            default:
                return "";
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

