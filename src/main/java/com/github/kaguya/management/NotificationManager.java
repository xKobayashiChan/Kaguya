package com.github.kaguya.management;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.ChatColors;
import com.github.kaguya.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final List<Notification> notifications = new ArrayList<>();

    private static final long DISPLAY_DURATION = 2000L;
    private static final long FADE_IN_DURATION = 200L;
    private static final long FADE_OUT_DURATION = 400L;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int MARGIN = 3;
    private static final int BAR_WIDTH = 2;
    private static final int LINE_SPACING = 2;
    private static final int MAX_NOTIFICATIONS = 5;

    public static void show(String message) {
        notifications.add(new Notification(Kaguya.clientName.trim(), message, System.currentTimeMillis()));
    }

    public static void show(String title, String message) {
        notifications.add(new Notification(title, message, System.currentTimeMillis()));
    }

    public static void render() {
        if (mc.thePlayer == null || mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        long now = System.currentTimeMillis();
        long totalDuration = FADE_IN_DURATION + DISPLAY_DURATION + FADE_OUT_DURATION;

        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            if (now - it.next().timestamp > totalDuration) {
                it.remove();
            }
        }

        if (notifications.isEmpty()) return;

        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        int boxHeight = fontHeight * 2 + LINE_SPACING + PADDING_Y * 2;

        float yOffset = 0;
        int displayed = 0;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            if (displayed >= MAX_NOTIFICATIONS) break;

            Notification notif = notifications.get(i);
            long elapsed = now - notif.timestamp;

            float alpha;
            float slideX;
            if (elapsed < FADE_IN_DURATION) {
                float progress = (float) elapsed / FADE_IN_DURATION;
                alpha = progress;
                slideX = (1.0F - progress) * 80.0F;
            } else if (elapsed < FADE_IN_DURATION + DISPLAY_DURATION) {
                alpha = 1.0F;
                slideX = 0.0F;
            } else {
                float progress = (float) (elapsed - FADE_IN_DURATION - DISPLAY_DURATION) / FADE_OUT_DURATION;
                alpha = 1.0F - progress;
                slideX = progress * 80.0F;
            }

            alpha = Math.max(0.0F, Math.min(1.0F, alpha));

            String formattedTitle = ChatColors.formatColor(notif.title);
            String formattedMessage = ChatColors.formatColor(notif.message);
            int titleWidth = mc.fontRendererObj.getStringWidth(formattedTitle);
            int messageWidth = mc.fontRendererObj.getStringWidth(formattedMessage);
            int maxTextWidth = Math.max(titleWidth, messageWidth);
            int boxWidth = maxTextWidth + PADDING_X * 2 + BAR_WIDTH + 2;

            float x = screenWidth - boxWidth - 4 + slideX;
            float y = screenHeight - 30 - yOffset - boxHeight;

            int bgAlpha = (int) (alpha * 180);
            int bgColor = (bgAlpha << 24);

            int barColor = new Color(255, 170, 0, (int) (alpha * 255)).getRGB();

            RenderUtil.enableRenderState();
            RenderUtil.drawRect(x, y, x + boxWidth, y + boxHeight, bgColor);
            RenderUtil.drawRect(x, y, x + BAR_WIDTH, y + boxHeight, barColor);
            RenderUtil.disableRenderState();

            GlStateManager.disableDepth();
            int textAlpha = (int) (alpha * 255);
            int titleColor = (textAlpha << 24) | 0xFFFFFF;
            int messageColor = (textAlpha << 24) | 0xFFFFFF;
            float textX = x + BAR_WIDTH + 2 + PADDING_X;
            mc.fontRendererObj.drawStringWithShadow(
                    formattedTitle,
                    textX,
                    y + PADDING_Y,
                    titleColor
            );
            mc.fontRendererObj.drawStringWithShadow(
                    formattedMessage,
                    textX,
                    y + PADDING_Y + fontHeight + LINE_SPACING,
                    messageColor
            );
            GlStateManager.enableDepth();

            yOffset += boxHeight + MARGIN;
            displayed++;
        }
    }

    private static class Notification {
        final String title;
        final String message;
        final long timestamp;

        Notification(String title, String message, long timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}

