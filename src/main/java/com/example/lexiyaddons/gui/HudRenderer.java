package com.example.lexiyaddons.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStream;
import java.util.Properties;

/**
 * 画面右下に "Develop - YYMMDD" を常時表示する HUD
 */
public class HudRenderer {

    private final String hudText;

    public HudRenderer() {
        String date = "unknown";
        try {
            InputStream is = getClass().getResourceAsStream("/lexiyaddons.properties");
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                date = props.getProperty("build.date", "unknown");
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudText = "Develop - " + date;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = event.resolution;

        // 左上: クライアント名
        fr.drawStringWithShadow("Takuma Client Unosouma Edition", 2, 2, 0xFFFFFFFF);

        // 右下: ビルド日付
        int textWidth = fr.getStringWidth(hudText);
        int x = sr.getScaledWidth() - textWidth - 2;
        int y = sr.getScaledHeight() - fr.FONT_HEIGHT - 2;

        fr.drawStringWithShadow(hudText, x, y, 0xFFAAAAAA);
    }
}

