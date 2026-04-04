package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.ChatColors;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.property.properties.ModeProperty;
import com.github.kaguya.util.ColorUtil;
import com.github.kaguya.util.RenderUtil;
import com.github.kaguya.util.TeamUtil;
import com.github.kaguya.util.TimerUtil;
import com.github.kaguya.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final float ENTITY_TEXT_GAP = 10.0F;

    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    private boolean isDragging = false;
    private int dragStartMouseX = 0;
    private int dragStartMouseY = 0;
    private int dragStartOffX = 0;
    private int dragStartOffY = 0;
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final BooleanProperty entity = new BooleanProperty("entity", true);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty animations = new BooleanProperty("animations", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Kaguya.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }


    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Kaguya.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Kaguya.targetManager.getColor();
            }
            return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
        }
        return new Color(-1);
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase entityLivingBase = this.target;
            this.target = this.resolveTarget();
            if (this.target != null) {
                float abs = this.target.getAbsorptionAmount() / 2.0F;
                float heal = this.target.getHealth() / 2.0F + abs;
                if (this.target != entityLivingBase) {
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                }
                if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = this.target.getMaxHealth() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }
                float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
                float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
                Color targetColor = this.getTargetColor(this.target);
                Color healthBarColor = ColorUtil.getHealthBlend(healthRatio);
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
                int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
                String healthText = String.format("%s❤", healthFormat.format(heal));
                int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
                float fontH = mc.fontRendererObj.FONT_HEIGHT;
                float hpScale = 1.5F;
                float paddingTop = 3.0F;
                float paddingSides = 3.0F;
                float gapText = 2.0F;
                float gapEntityToBar = 3.0F;
                float barHeight = 5.0F;
                float paddingBottom = 4.0F;
                float hpTextHeight = fontH * hpScale;
                float contentWidth = Math.max((float) targetNameWidth, (float) healthTextWidth * 1.5F);

                float entityRenderScale = 0.0F;
                float entityOffset = 0.0F;
                float scaledEntityHeight = 0.0F;
                if (this.entity.getValue()) {
                    entityRenderScale = 16.0F;
                    scaledEntityHeight = Math.max(18.0F, entityRenderScale * Math.max(this.target.height, 0.5F));
                    entityOffset = entityRenderScale + ENTITY_TEXT_GAP;
                }

                float textBlockHeight = fontH + gapText + hpTextHeight;
                float textStartX = this.entity.getValue() ? entityOffset : 0.0F;
                float entityTop = this.entity.getValue() ? paddingTop : 0.0F;
                float textTop = this.entity.getValue()
                        ? paddingTop + Math.max(0.0F, (scaledEntityHeight - textBlockHeight) / 2.0F)
                        : paddingTop;
                float contentBottom = this.entity.getValue()
                        ? Math.max(entityTop + scaledEntityHeight, textTop + textBlockHeight)
                        : textTop + textBlockHeight;
                float barTop = contentBottom + gapEntityToBar;
                float hudHeight = barTop + barHeight + paddingBottom;
                hudHeight = Math.max(hudHeight, 44.0F);

                float barTotalWidth = Math.max(textStartX + 80.0F, textStartX + paddingSides + contentWidth + paddingSides);
                float entityCenterX = paddingSides + entityRenderScale / 2.0F;
                float entityCenterY = hudHeight / 2.0F;
                float posX = this.offX.getValue().floatValue();
                switch (this.posX.getValue()) {
                    case 1:
                        posX += (float) scaledResolution.getScaledWidth() / 2.0F - barTotalWidth / 2.0F;
                        break;
                    case 2:
                        posX *= -1.0F;
                        posX += (float) scaledResolution.getScaledWidth() - barTotalWidth;
                }
                float posY = this.offY.getValue().floatValue();
                switch (this.posY.getValue()) {
                    case 1:
                        posY += (float) scaledResolution.getScaledHeight() / 2.0F - hudHeight / 2.0F;
                        break;
                    case 2:
                        posY *= -1.0F;
                        posY += (float) scaledResolution.getScaledHeight() - hudHeight;
                }
                GlStateManager.pushMatrix();
                GlStateManager.translate(posX, posY, -450.0F);
                RenderUtil.enableRenderState();
                int backgroundColor = new Color(0.0F, 0.0F, 0.0F, 75.0F / 100.0F).getRGB();
                int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
                RenderUtil.drawRect(0.0F, 0.0F, barTotalWidth, hudHeight, backgroundColor);
                if (outlineColor != 0) {
                    RenderUtil.drawLine(0.0F, 0.0F, barTotalWidth, 0.0F, 1.5F, outlineColor);
                    RenderUtil.drawLine(0.0F, hudHeight, barTotalWidth, hudHeight, 1.5F, outlineColor);
                    RenderUtil.drawLine(0.0F, 0.0F, 0.0F, hudHeight, 1.5F, outlineColor);
                    RenderUtil.drawLine(barTotalWidth, 0.0F, barTotalWidth, hudHeight, 1.5F, outlineColor);
                }
                float barLeft = textStartX + paddingSides;
                float barBottom = barTop + barHeight;
                RenderUtil.drawRect(barLeft, barTop, barTotalWidth - paddingSides, barBottom, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
                RenderUtil.drawRect(barLeft, barTop, barLeft + healthRatio * (barTotalWidth - paddingSides - barLeft), barBottom, healthBarColor.getRGB());
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.fontRendererObj.drawString(targetNameText, textStartX + paddingSides, textTop, -1, this.shadow.getValue());
                GlStateManager.pushMatrix();
                GlStateManager.scale(hpScale, hpScale, 1.0F);
                mc.fontRendererObj.drawString(healthText, (textStartX + paddingSides) / hpScale, (textTop + fontH + gapText) / hpScale, healthBarColor.getRGB(), this.shadow.getValue());
                GlStateManager.popMatrix();
                if (this.entity.getValue()) {
                    boolean wasHideGUI = mc.gameSettings.hideGUI;
                    mc.gameSettings.hideGUI = true;
                    float entityFeetY = entityCenterY + scaledEntityHeight / 2.0F;
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GuiInventory.drawEntityOnScreen(
                            (int) entityCenterX, (int) entityFeetY, (int) entityRenderScale,
                            30.0F, -(float) (hudHeight / 2.0F),
                            this.target
                    );
                    mc.gameSettings.hideGUI = wasHideGUI;
                }
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
                if (mc.currentScreen instanceof GuiChat && this.chatPreview.getValue()) {
                    int mouseX = Mouse.getX() * scaledResolution.getScaledWidth() / mc.displayWidth;
                    int mouseY = scaledResolution.getScaledHeight() - Mouse.getY() * scaledResolution.getScaledHeight() / mc.displayHeight - 1;
                    float hudLeft = posX;
                    float hudTop = posY;
                    float hudRight = hudLeft + barTotalWidth;
                    float hudBottom = hudTop + hudHeight;
                    boolean mouseOver = mouseX >= hudLeft && mouseX <= hudRight
                            && mouseY >= hudTop && mouseY <= hudBottom;
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
                            int newOffX = this.dragStartOffX + (this.posX.getValue() == 2 ? -deltaX : deltaX);
                            int newOffY = this.dragStartOffY + (this.posY.getValue() == 2 ? -deltaY : deltaY);
                            this.offX.setValue(Math.max(-255, Math.min(255, newOffX)));
                            this.offY.setValue(Math.max(-255, Math.min(255, newOffY)));
                        }
                    } else {
                        this.isDragging = false;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
