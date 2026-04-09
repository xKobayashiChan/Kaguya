package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Health extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty x = new IntProperty("x", 10, -9999, 9999);
    public final IntProperty y = new IntProperty("y", 10, -9999, 9999);

    private boolean isDragging = false;
    private int dragStartMouseX = 0;
    private int dragStartMouseY = 0;
    private int dragStartX = 0;
    private int dragStartY = 0;

    private static final Color HEART_COLOR = new Color(255, 45, 45);

    public Health() {
        super("Health", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        float hp = mc.thePlayer.getHealth();
        float abs = mc.thePlayer.getAbsorptionAmount();
        float maxHp = mc.thePlayer.getMaxHealth();
        float ratio = Math.min(Math.max(hp / maxHp, 0.0F), 1.0F);
        Color hpColor = ColorUtil.getHealthBlend(ratio);

        String hpText = String.format("%.1f", hp);
        String heartText = "❤";
        String absText = abs > 0 ? String.format("%.1f", abs) : "";
        String absHeartText = abs > 0 ? "❤" : "";
        int hpWidth = mc.fontRendererObj.getStringWidth(hpText);
        int heartWidth = mc.fontRendererObj.getStringWidth(heartText);
        int spaceWidth = abs > 0 ? mc.fontRendererObj.getStringWidth(" ") : 0;
        int absWidth = mc.fontRendererObj.getStringWidth(absText);
        int absHeartWidth = mc.fontRendererObj.getStringWidth(absHeartText);
        float posX = this.x.getValue();
        float posY = this.y.getValue();
        int goldColor = new Color(255, 210, 60).getRGB();

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float cx = posX;
        mc.fontRendererObj.drawStringWithShadow(hpText, cx, posY, hpColor.getRGB());
        cx += hpWidth;
        mc.fontRendererObj.drawStringWithShadow(heartText, cx, posY, HEART_COLOR.getRGB());
        cx += heartWidth;
        if (abs > 0) {
            cx += spaceWidth;
            mc.fontRendererObj.drawStringWithShadow(absText, cx, posY, goldColor);
            cx += absWidth;
            mc.fontRendererObj.drawStringWithShadow(absHeartText, cx, posY, goldColor);
            cx += absHeartWidth;
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();

        if (mc.currentScreen instanceof GuiChat) {
            ScaledResolution sr = new ScaledResolution(mc);
            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

            float totalWidth = hpWidth + heartWidth + spaceWidth + absWidth + absHeartWidth;
            float totalHeight = mc.fontRendererObj.FONT_HEIGHT;
            boolean mouseOver = mouseX >= posX && mouseX <= posX + totalWidth
                    && mouseY >= posY && mouseY <= posY + totalHeight;

            if (Mouse.isButtonDown(0)) {
                if (!this.isDragging && mouseOver) {
                    this.isDragging = true;
                    this.dragStartMouseX = mouseX;
                    this.dragStartMouseY = mouseY;
                    this.dragStartX = this.x.getValue();
                    this.dragStartY = this.y.getValue();
                }
                if (this.isDragging) {
                    this.x.setValue(this.dragStartX + (mouseX - this.dragStartMouseX));
                    this.y.setValue(this.dragStartY + (mouseY - this.dragStartMouseY));
                }
            } else {
                this.isDragging = false;
            }
        } else {
            this.isDragging = false;
        }
    }
}
