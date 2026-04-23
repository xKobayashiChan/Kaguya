package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.BlinkModules;
import com.github.kaguya.enums.ChatColors;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.Render2DEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.management.NotificationManager;
import com.github.kaguya.mixins.IAccessorGuiChat;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.*;
import com.github.kaguya.util.ColorUtil;
import com.github.kaguya.util.RenderUtil;
import com.github.kaguya.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"}, () -> false);
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"}, () -> false);
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 4096, () -> false);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 4096, () -> false);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final FloatProperty gap = new FloatProperty("gap", 0.0F, -2.0F, 5.0F);
    // Animation
    private static final float ANIM_APPEAR_SPEED = 8f;  // alpha/sec
    private static final float ANIM_DIE_SPEED    = 6f;  // alpha/sec
    private static final float ANIM_SLIDE_SPEED  = 120f; // px/sec

    private final List<Module> renderOrder = new ArrayList<>();
    private final Map<Module, AnimState> animStates = new HashMap<>();
    private long lastFrameMs = -1;

    private static class AnimState {
        float alpha   = 0f;
        float xOffset = 0f;
        boolean dying = false;
    }

    private boolean isDragging = false;
    private int dragStartMouseX = 0;
    private int dragStartMouseY = 0;
    private int dragStartAbsX = 0;
    private int dragStartAbsY = 0;

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }
        return moduleSuffix;
    }

    private static int withAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(
                this.getModuleName(module), this.getModuleSuffix(module)
        );
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = mc.fontRendererObj.getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + mc.fontRendererObj.getStringWidth(str);
            }
        }
        return width;
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    public HUD() {
        super("HUD", true, true);
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue());
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(this.custom1.getValue()),
                        new Color(this.custom2.getValue())
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                }
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
        );
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Kaguya.moduleManager.modules.values().stream().filter(module -> module.isEnabled() && !module.isHidden()).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Kaguya.commandManager != null && Kaguya.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
            float x = (float) this.offsetX.getValue()
                    + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
            float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
            if (this.posX.getValue() == 1) {
                x = (float) scaledResolution.getScaledWidth() - x;
            }
            if (this.posY.getValue() == 1) {
                y = (float) scaledResolution.getScaledHeight() - y - height * this.scale.getValue();
            }
            float startX = x;
            float startY = y;
            // --- Animation sync ---
            long nowMs = System.currentTimeMillis();
            float dt = lastFrameMs < 0 ? 0f : Math.min((nowMs - lastFrameMs) / 1000f, 0.05f);
            lastFrameMs = nowMs;

            // Mark removed modules as dying
            for (Module m : renderOrder) {
                AnimState s = animStates.get(m);
                if (s != null && !s.dying && !activeModules.contains(m)) {
                    s.dying = true;
                }
            }
            // Add new modules / re-enable dying ones
            for (Module m : activeModules) {
                if (!animStates.containsKey(m)) {
                    AnimState s = new AnimState();
                    animStates.put(m, s);
                    // Insert after all non-dying modules with >= width
                    int insertIdx = 0;
                    int mWidth = getModuleWidth(m);
                    for (int i = 0; i < renderOrder.size(); i++) {
                        AnimState rs = animStates.get(renderOrder.get(i));
                        if (rs != null && !rs.dying && getModuleWidth(renderOrder.get(i)) >= mWidth) {
                            insertIdx = i + 1;
                        }
                    }
                    renderOrder.add(insertIdx, m);
                } else {
                    animStates.get(m).dying = false;
                }
            }
            // Update alpha/xOffset and remove dead
            Iterator<Module> animIter = renderOrder.iterator();
            while (animIter.hasNext()) {
                Module m = animIter.next();
                AnimState s = animStates.get(m);
                if (s == null) { animIter.remove(); continue; }
                if (s.dying) {
                    s.alpha   = Math.max(0f, s.alpha - dt * ANIM_DIE_SPEED);
                    s.xOffset = Math.min(s.xOffset + dt * ANIM_SLIDE_SPEED, 80f);
                    if (s.alpha <= 0f) {
                        animStates.remove(m);
                        animIter.remove();
                    }
                } else {
                    s.alpha   = Math.min(1f, s.alpha + dt * ANIM_APPEAR_SPEED);
                    s.xOffset = 0f;
                }
            }
            // --- End animation sync ---

            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            long l = System.currentTimeMillis();
            long offset = 0L;
            for (Module module : renderOrder) {
                AnimState anim = animStates.get(module);
                if (anim == null) continue;
                float alpha   = anim.alpha;
                float slideX  = anim.xOffset; // screen pixels to shift rightward

                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                int color = withAlpha(this.getColor(l, offset).getRGB(), alpha);
                // drawX: slide direction follows position-x (LEFT=left, RIGHT=right)
                float slideDir = this.posX.getValue() == 0 ? -1f : 1f;
                float drawX = x / this.scale.getValue() + slideX * slideDir;
                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F * alpha).getRGB();
                    RenderUtil.drawRect(
                            drawX - 1.0F - (this.posX.getValue() == 0 ? 0.0F : totalWidth),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                            drawX + 1.0F + (this.posX.getValue() == 0 ? totalWidth : 0.0F),
                            y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                            bgColor
                    );
                }
                if (this.showBar.getValue()) {
                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(
                                drawX + (this.posX.getValue() == 0 ? -3.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                drawX + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                        RenderUtil.drawRect(
                                drawX + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                drawX + (this.posX.getValue() == 0 ? -1.0F : 3.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                (color & 16579836) >> 2 | color & 0xFF000000
                        );
                    } else {
                        RenderUtil.drawRect(
                                drawX + (this.posX.getValue() == 0 ? -2.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                                drawX + (this.posX.getValue() == 0 ? -1.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                    }
                }
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                if (this.shadow.getValue()) {
                    mc.fontRendererObj
                            .drawStringWithShadow(moduleName, drawX - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                } else {
                    mc.fontRendererObj
                            .drawString(
                                    moduleName,
                                    drawX - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                    y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                    color,
                                    false
                            );
                }
                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width = (float) mc.fontRendererObj.getStringWidth(moduleName) + 3.0F;
                    int suffixColor = withAlpha(ChatColors.GRAY.toAwtColor(), alpha);
                    for (String string : moduleSuffix) {
                        if (this.shadow.getValue()) {
                            mc.fontRendererObj
                                    .drawStringWithShadow(
                                            string,
                                            drawX - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                            y / this.scale.getValue(),
                                            suffixColor
                                    );
                        } else {
                            mc.fontRendererObj
                                    .drawString(
                                            string,
                                            drawX - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                            y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                            suffixColor,
                                            false
                                    );
                        }
                        width += (float) mc.fontRendererObj.getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }
                // Y step scaled by alpha: dying modules give up space proportionally
                y += (height + (this.shadow.getValue() ? 1.0F : 0.0F) + this.gap.getValue()) * this.scale.getValue() * alpha * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                offset++;
            }
            GlStateManager.disableBlend();
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Kaguya.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Kaguya.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        mc.fontRendererObj
                                .drawString(
                                        String.valueOf(movementPacketSize),
                                        (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                                - (float) mc.fontRendererObj.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                        (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                        this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                        this.shadow.getValue()
                                );
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();

            if (mc.currentScreen instanceof GuiChat) {
                float maxWidth = 0.0F;
                for (Module module : this.activeModules) {
                    float moduleWidth = (float) this.getModuleWidth(module);
                    if (moduleWidth > maxWidth) {
                        maxWidth = moduleWidth;
                    }
                }
                float lineHeight = (height + (this.shadow.getValue() ? 1.0F : 0.0F) + this.gap.getValue()) * this.scale.getValue();
                float totalHeight = (this.activeModules.isEmpty() ? 1.0F : (float) this.activeModules.size()) * lineHeight;
                float left = this.posX.getValue() == 0
                        ? startX - (this.showBar.getValue() ? (this.shadow.getValue() ? 3.0F : 2.0F) * this.scale.getValue() : 1.0F)
                        : startX - maxWidth * this.scale.getValue() - 1.0F;
                float right = this.posX.getValue() == 0
                        ? startX + maxWidth * this.scale.getValue() + 1.0F
                        : startX + (this.showBar.getValue() ? (this.shadow.getValue() ? 3.0F : 2.0F) * this.scale.getValue() : 1.0F);
                float top = this.posY.getValue() == 0 ? startY - this.scale.getValue() : startY - totalHeight - this.scale.getValue();
                float bottom = this.posY.getValue() == 0 ? startY + totalHeight + this.scale.getValue() : startY + this.scale.getValue();

                int mouseX = Mouse.getX() * scaledResolution.getScaledWidth() / mc.displayWidth;
                int mouseY = scaledResolution.getScaledHeight() - Mouse.getY() * scaledResolution.getScaledHeight() / mc.displayHeight - 1;
                boolean mouseOver = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
                if (Mouse.isButtonDown(0)) {
                    if (!this.isDragging && mouseOver) {
                        this.isDragging = true;
                        this.dragStartMouseX = mouseX;
                        this.dragStartMouseY = mouseY;
                        // 絶対座標で記録
                        this.dragStartAbsX = this.posX.getValue() == 0
                                ? this.offsetX.getValue()
                                : scaledResolution.getScaledWidth() - this.offsetX.getValue();
                        this.dragStartAbsY = this.posY.getValue() == 0
                                ? this.offsetY.getValue()
                                : scaledResolution.getScaledHeight() - this.offsetY.getValue();
                    }
                    if (this.isDragging) {
                        int sw = scaledResolution.getScaledWidth();
                        int sh = scaledResolution.getScaledHeight();
                        int absX = Math.max(0, Math.min(this.dragStartAbsX + (mouseX - this.dragStartMouseX), sw));
                        int absY = Math.max(0, Math.min(this.dragStartAbsY + (mouseY - this.dragStartMouseY), sh));
                        // 象限でposX/posY自動切替
                        int newPosX = absX < sw / 2 ? 0 : 1;
                        int newPosY = absY < sh / 2 ? 0 : 1;
                        this.posX.setValue(newPosX);
                        this.posY.setValue(newPosY);
                        this.offsetX.setValue(newPosX == 0 ? absX : sw - absX);
                        this.offsetY.setValue(newPosY == 0 ? absY : sh - absY);
                    }
                } else {
                    this.isDragging = false;
                }
            } else {
                this.isDragging = false;
            }
        }

        NotificationManager.render();
    }
}
