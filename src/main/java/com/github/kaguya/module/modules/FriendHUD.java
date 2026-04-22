package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.property.properties.ModeProperty;
import com.github.kaguya.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FriendHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int ROW_PADDING_X = 5;
    private static final int ROW_PADDING_Y = 3;
    private static final int ROW_GAP = 2;
    private static final int COL_GAP = 4;

    private boolean isDragging = false;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartOffX, dragStartOffY;

    public final IntProperty opacity = new IntProperty("opacity", 70, 0, 100);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final IntProperty offX = new IntProperty("offset-x", 5, -9999, 9999);
    public final IntProperty offY = new IntProperty("offset-y", 5, -9999, 9999);

    public FriendHUD() {
        super("FriendHUD", false, true);
    }

    private float getDeltaY(EntityPlayer friend) {
        return (float) (friend.posY - mc.thePlayer.posY);
    }

    private float getDistance(EntityPlayer friend) {
        return (float) mc.thePlayer.getDistanceToEntity(friend);
    }

    private float getDirectionAngle(EntityPlayer friend) {
        double dx = friend.posX - mc.thePlayer.posX;
        double dz = friend.posZ - mc.thePlayer.posZ;
        float yawToFriend = MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI));
        float delta = MathHelper.wrapAngleTo180_float(yawToFriend - mc.thePlayer.rotationYaw);
        return delta - 90f;
    }

    private String formatDeltaY(float dy) {
        int idy = (int) dy;
        if (dy >= 0.5f) return "+" + idy;
        if (dy <= -0.5f) return String.valueOf(idy);
        return "±0";
    }

    private int hpColor(float hp) {
        if (hp > 14f) return 0x55FF55;
        if (hp > 7f)  return 0xFFFF55;
        return 0xFF5555;
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        // 描画距離内のフレンドを収集
        List<EntityPlayer> friends = new ArrayList<>();
        for (Object obj : mc.theWorld.playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) obj;
            if (p == mc.thePlayer) continue;
            if (Kaguya.friendManager.isFriend(p.getName())) {
                friends.add(p);
            }
        }
        if (friends.isEmpty()) return;

        // 距離でソート（近い順）
        friends.sort((a, b) -> Float.compare(getDistance(a), getDistance(b)));

        ScaledResolution sr = new ScaledResolution(mc);
        int fontH    = mc.fontRendererObj.FONT_HEIGHT;
        int headSize = fontH;
        float rowH   = fontH + ROW_GAP;

        // カラム幅計算
        int arrowW   = mc.fontRendererObj.getStringWidth(">");
        int maxHpW   = mc.fontRendererObj.getStringWidth("20");
        int maxNameW = 0, maxDistW = 0, maxDyW = 0;
        for (EntityPlayer friend : friends) {
            int hw = mc.fontRendererObj.getStringWidth(String.valueOf((int) friend.getHealth()));
            int nw = mc.fontRendererObj.getStringWidth(friend.getName());
            int dw = mc.fontRendererObj.getStringWidth(String.valueOf((int) getDistance(friend)));
            int yw = mc.fontRendererObj.getStringWidth(formatDeltaY(getDeltaY(friend)));
            if (hw > maxHpW)   maxHpW   = hw;
            if (nw > maxNameW) maxNameW = nw;
            if (dw > maxDistW) maxDistW = dw;
            if (yw > maxDyW)   maxDyW   = yw;
        }

        // 固定カラム開始X（列揃え）
        float xHead  = ROW_PADDING_X;
        float xName  = xHead  + headSize  + COL_GAP;
        float xDist  = xName  + maxNameW  + COL_GAP;
        float xArrow = xDist  + maxDistW  + COL_GAP;
        float xDy    = xArrow + arrowW    + COL_GAP;
        float xHp    = xDy    + maxDyW    + COL_GAP;

        float hudWidth  = xHp + maxHpW + ROW_PADDING_X;
        float hudHeight = rowH * friends.size() + ROW_PADDING_Y * 2 - ROW_GAP;

        // 座標計算
        float px = this.offX.getValue().floatValue();
        switch (this.posX.getValue()) {
            case 1: px += sr.getScaledWidth() / 2f - hudWidth / 2f; break;
            case 2: px = sr.getScaledWidth() - hudWidth - px; break;
        }
        float py = this.offY.getValue().floatValue();
        switch (this.posY.getValue()) {
            case 1: py += sr.getScaledHeight() / 2f - hudHeight / 2f; break;
            case 2: py = sr.getScaledHeight() - hudHeight - py; break;
        }

        // 背景描画
        float alpha = this.opacity.getValue() / 100.0f;
        int bgColor = new Color(0f, 0f, 0f, alpha).getRGB();

        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, 0f);
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0f, 0f, hudWidth, hudHeight, bgColor);
        RenderUtil.disableRenderState();

        // 各行描画
        for (int i = 0; i < friends.size(); i++) {
            EntityPlayer friend = friends.get(i);
            float textY = ROW_PADDING_Y + i * rowH;

            GlStateManager.disableDepth();

            // スキンアイコン（NameTagsと同パターン）
            if (friend instanceof AbstractClientPlayer) {
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                mc.getTextureManager().bindTexture(((AbstractClientPlayer) friend).getLocationSkin());
                int headX = (int) xHead;
                int headY = (int) textY;
                Gui.drawScaledCustomSizeModalRect(headX, headY, 8.0f, 8.0f, 8, 8, headSize, headSize + 1, 64.0f, 64.0f);
                Gui.drawScaledCustomSizeModalRect(headX, headY, 40.0f, 8.0f, 8, 8, headSize, headSize + 1, 64.0f, 64.0f);
            }

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // プレイヤー名
            Color friendColor = Kaguya.friendManager.getColor();
            mc.fontRendererObj.drawString(friend.getName(), xName, textY, friendColor.getRGB(), true);

            // 3D距離（右寄せ）
            String dist = String.valueOf((int) getDistance(friend));
            int distW = mc.fontRendererObj.getStringWidth(dist);
            mc.fontRendererObj.drawString(dist, xDist + maxDistW - distW, textY, 0xAAAAAA, true);

            // 方向矢印（影なし、GL回転）
            float angle = getDirectionAngle(friend);
            GlStateManager.pushMatrix();
            GlStateManager.translate(xArrow + arrowW / 2f, textY + fontH / 2f, 0f);
            GlStateManager.rotate(angle, 0f, 0f, 1f);
            mc.fontRendererObj.drawString(">", -arrowW / 2, -fontH / 2, 0xFFFFFF, false);
            GlStateManager.popMatrix();

            // Y軸差（右寄せ）
            String dy = formatDeltaY(getDeltaY(friend));
            int dyW = mc.fontRendererObj.getStringWidth(dy);
            float dyDiff = getDeltaY(friend);
            int dyColor = dyDiff > 0.05f ? 0x55FF55 : (dyDiff < -0.05f ? 0xFF5555 : 0xAAAAAA);
            mc.fontRendererObj.drawString(dy, xDy + maxDyW - dyW, textY, dyColor, true);

            // HP（右寄せ）
            float hp = friend.getHealth();
            String hpStr = String.valueOf((int) hp);
            int hpW = mc.fontRendererObj.getStringWidth(hpStr);
            mc.fontRendererObj.drawString(hpStr, xHp + maxHpW - hpW, textY, hpColor(hp), true);

            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
        }

        GlStateManager.popMatrix();

        // ドラッグ処理
        if (mc.currentScreen != null) {
            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
            boolean mouseOver = mouseX >= px && mouseX <= px + hudWidth
                    && mouseY >= py && mouseY <= py + hudHeight;
            if (Mouse.isButtonDown(0)) {
                if (!this.isDragging && mouseOver) {
                    this.isDragging = true;
                    this.dragStartMouseX = mouseX;
                    this.dragStartMouseY = mouseY;
                    this.dragStartOffX = this.offX.getValue();
                    this.dragStartOffY = this.offY.getValue();
                }
                if (this.isDragging) {
                    int deltaX = mouseX - this.dragStartMouseX;
                    int deltaY = mouseY - this.dragStartMouseY;
                    this.offX.setValue(this.dragStartOffX + (this.posX.getValue() == 2 ? -deltaX : deltaX));
                    this.offY.setValue(this.dragStartOffY + (this.posY.getValue() == 2 ? -deltaY : deltaY));
                }
            } else {
                this.isDragging = false;
            }
        }
    }
}