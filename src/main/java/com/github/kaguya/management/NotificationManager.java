package com.github.kaguya.management;

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

    public static void show(String message) {
        notifications.add(new Notification(message, System.currentTimeMillis()));
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
        int boxHeight = fontHeight + PADDING_Y * 2;

        float yOffset = 0;

        for (int i = notifications.size() - 1; i >= 0; i--) {
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

            String formatted = ChatColors.formatColor(notif.message);
            int textWidth = mc.fontRendererObj.getStringWidth(formatted);
            int boxWidth = textWidth + PADDING_X * 2 + BAR_WIDTH + 2;

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
            int textColor = (textAlpha << 24) | 0xFFFFFF;
            mc.fontRendererObj.drawStringWithShadow(
                    formatted,
                    x + BAR_WIDTH + 2 + PADDING_X,
                    y + PADDING_Y,
                    textColor
            );
            GlStateManager.enableDepth();

            yOffset += boxHeight + MARGIN;
        }
    }

    private static class Notification {
        final String message;
        final long timestamp;

        Notification(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}

