package com.github.kaguya.ui;

import com.github.kaguya.KaguyaClient;
import com.github.kaguya.Kaguya;
import com.github.kaguya.config.Config;
import com.github.kaguya.util.KeyBindUtil;
import com.github.kaguya.module.Module;
import com.github.kaguya.module.modules.*;
import com.github.kaguya.ui.components.CategoryComponent;
import com.github.kaguya.ui.components.ModuleComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ClickGui extends GuiScreen {

    private static ClickGui instance;

    // ---- Color palette (orange × nezumi-gray × white) ----
    private static final int C_ORANGE      = new Color(230, 126,  34).getRGB();
    private static final int C_OVERLAY     = new Color(  0,   0,   0, 120).getRGB();
    private static final int C_PANEL       = new Color( 36,  36,  36, 250).getRGB();
    private static final int C_LIST        = new Color( 28,  28,  28, 250).getRGB();
    private static final int C_SETTINGS    = new Color( 42,  42,  42, 250).getRGB();
    private static final int C_TAB_IDLE    = new Color( 52,  52,  52).getRGB();
    private static final int C_TAB_HOVER   = new Color( 68,  68,  68).getRGB();
    private static final int C_ITEM_SEL    = new Color( 58,  58,  58).getRGB();
    private static final int C_ITEM_HOVER  = new Color( 48,  48,  48).getRGB();
    private static final int C_DIVIDER     = new Color( 18,  18,  18).getRGB();
    private static final int C_SEPARATOR   = new Color( 62,  62,  62).getRGB();
    private static final int C_WHITE       = Color.WHITE.getRGB();
    private static final int C_DIM         = new Color(140, 140, 140).getRGB();
    private static final int C_SCROLLBAR   = new Color(255, 255, 255,  55).getRGB();
    private static final int C_FLASH_ON    = new Color(230, 126,  34, 80).getRGB();
    private static final int C_FLASH_OFF   = new Color( 60,  60,  60, 80).getRGB();

    // ---- Layout ----
    private static final int PANEL_W  = 460;
    private static final int PANEL_H  = 280;
    private static final int TAB_H    = 18;
    private static final int LIST_W   = 130;
    private static final int ITEM_H   = 16;
    private static final int HEADER_H = 18;
    private static final int BTN_W    = 70;
    private static final int BTN_H    = 12;
    private static final int SETT_W   = PANEL_W - LIST_W - 1;

    // ---- Animation ----
    private static final float FADE_SPEED     =  7f;
    private static final float SLIDE_SPEED    = 14f;
    private static final long  FLASH_DURATION = 380L;

    // ---- State ----
    private final ArrayList<CategoryComponent> categories;
    private int selectedTab = 0;
    private ModuleComponent selectedModule = null;
    private ModuleComponent displayModule  = null;  // keeps last module during slide-out

    private int    listScroll     = 0;
    private double animListScroll = 0;
    private int    settScroll     = 0;
    private double animSettScroll = 0;

    private long saveFlashUntil = 0;

    // ---- Animation state ----
    private long  lastFrameNs     = -1;
    private float panelAlpha      = 0f;
    private float tabSlideOffset  = 0f;   // horizontal offset for module list (px)
    private float settSlideOffset = 0f;   // 0=visible, SETT_W=hidden (off to right)

    private final Map<String, Long> toggleFlashMap = new HashMap<>();

    // ---- Config tab state ----
    private int    configScroll     = 0;
    private double animConfigScroll = 0;
    private GuiTextField configSaveField;

    public ClickGui() {
        instance = this;
        categories = new ArrayList<>();
        buildCategories();
    }

    @Override
    public void initGui() {
        super.initGui();
        lastFrameNs     = -1;
        panelAlpha      = 0f;
        tabSlideOffset  = 0f;
        settSlideOffset = selectedModule != null ? 0f : SETT_W;
        configSaveField = new GuiTextField(0, mc.fontRendererObj, 0, 0, SETT_W - 58, 12);
        configSaveField.setMaxStringLength(64);
        configSaveField.setText(Config.lastConfig != null ? Config.lastConfig : "default");
    }

    @Override
    public void updateScreen() {
        if (configSaveField != null) configSaveField.updateCursorCounter();
    }

    private int  totalTabs()   { return categories.size() + 1; }
    private boolean isConfigTab() { return selectedTab == categories.size(); }

    private void buildCategories() {
        Comparator<Module> byName = Comparator.comparing(m -> m.getName().toLowerCase());

        List<Module> combat = new ArrayList<>();
        combat.add(Kaguya.moduleManager.getModule(AimAssist.class));
        combat.add(Kaguya.moduleManager.getModule(AutoClicker.class));
        combat.add(Kaguya.moduleManager.getModule(KillAura.class));
        combat.add(Kaguya.moduleManager.getModule(Wtap.class));
        combat.add(Kaguya.moduleManager.getModule(Velocity.class));
        combat.add(Kaguya.moduleManager.getModule(Freeze.class));
        combat.add(Kaguya.moduleManager.getModule(Reach.class));
        combat.add(Kaguya.moduleManager.getModule(TargetStrafe.class));
        combat.add(Kaguya.moduleManager.getModule(NoHitDelay.class));
        combat.add(Kaguya.moduleManager.getModule(AntiFireball.class));
        combat.add(Kaguya.moduleManager.getModule(LagRange.class));
        combat.add(Kaguya.moduleManager.getModule(HitBox.class));
        combat.add(Kaguya.moduleManager.getModule(MoreKB.class));
        combat.add(Kaguya.moduleManager.getModule(Refill.class));
        combat.add(Kaguya.moduleManager.getModule(HitSelect.class));
        combat.add(Kaguya.moduleManager.getModule(TimerRangev999.class));
        combat.add(Kaguya.moduleManager.getModule(com.github.kaguya.module.modules.Timer.class));
        combat.sort(byName);

        List<Module> movement = new ArrayList<>();
        movement.add(Kaguya.moduleManager.getModule(AntiAFK.class));
        movement.add(Kaguya.moduleManager.getModule(Fly.class));
        movement.add(Kaguya.moduleManager.getModule(Speed.class));
        movement.add(Kaguya.moduleManager.getModule(LongJump.class));
        movement.add(Kaguya.moduleManager.getModule(Sprint.class));
        movement.add(Kaguya.moduleManager.getModule(SafeWalk.class));
        movement.add(Kaguya.moduleManager.getModule(Jesus.class));
        movement.add(Kaguya.moduleManager.getModule(Blink.class));
        movement.add(Kaguya.moduleManager.getModule(NoFall.class));
        movement.add(Kaguya.moduleManager.getModule(NoSlow.class));
        movement.add(Kaguya.moduleManager.getModule(KeepSprint.class));
        movement.add(Kaguya.moduleManager.getModule(Eagle.class));
        movement.add(Kaguya.moduleManager.getModule(NoJumpDelay.class));
        movement.add(Kaguya.moduleManager.getModule(AntiVoid.class));
        movement.add(Kaguya.moduleManager.getModule(Zenith.class));
        movement.sort(byName);

        List<Module> render = new ArrayList<>();
        render.add(Kaguya.moduleManager.getModule(Cape.class));
        render.add(Kaguya.moduleManager.getModule(ESP.class));
        render.add(Kaguya.moduleManager.getModule(Chams.class));
        render.add(Kaguya.moduleManager.getModule(ClosestPlayerHUD.class));
        render.add(Kaguya.moduleManager.getModule(FriendHUD.class));
        render.add(Kaguya.moduleManager.getModule(FriendTransparency.class));
        render.add(Kaguya.moduleManager.getModule(FullBright.class));
        render.add(Kaguya.moduleManager.getModule(Tracers.class));
        render.add(Kaguya.moduleManager.getModule(NameTags.class));
        render.add(Kaguya.moduleManager.getModule(Xray.class));
        render.add(Kaguya.moduleManager.getModule(TargetHUD.class));
        render.add(Kaguya.moduleManager.getModule(Indicators.class));
        render.add(Kaguya.moduleManager.getModule(BedESP.class));
        render.add(Kaguya.moduleManager.getModule(ItemESP.class));
        render.add(Kaguya.moduleManager.getModule(Camera.class));
        render.add(Kaguya.moduleManager.getModule(NoHurtCam.class));
        render.add(Kaguya.moduleManager.getModule(HUD.class));
        render.add(Kaguya.moduleManager.getModule(Health.class));
        render.add(Kaguya.moduleManager.getModule(ClientHUD.class));
        render.add(Kaguya.moduleManager.getModule(HideClientText.class));
        render.add(Kaguya.moduleManager.getModule(GuiModule.class));
        render.add(Kaguya.moduleManager.getModule(ChestESP.class));
        render.add(Kaguya.moduleManager.getModule(Trajectories.class));
        render.add(Kaguya.moduleManager.getModule(Radar.class));
        render.sort(byName);

        List<Module> player = new ArrayList<>();
        player.add(Kaguya.moduleManager.getModule(AutoHeal.class));
        player.add(Kaguya.moduleManager.getModule(AutoTool.class));
        player.add(Kaguya.moduleManager.getModule(ChestStealer.class));
        player.add(Kaguya.moduleManager.getModule(InvManager.class));
        player.add(Kaguya.moduleManager.getModule(InvWalk.class));
        player.add(Kaguya.moduleManager.getModule(Scaffold.class));
        player.add(Kaguya.moduleManager.getModule(AutoBlockIn.class));
        player.add(Kaguya.moduleManager.getModule(SpeedMine.class));
        player.add(Kaguya.moduleManager.getModule(FastPlace.class));
        player.add(Kaguya.moduleManager.getModule(GhostHand.class));
        player.add(Kaguya.moduleManager.getModule(MCF.class));
        player.add(Kaguya.moduleManager.getModule(AntiDebuff.class));
        player.add(Kaguya.moduleManager.getModule(AutoWater.class));
        player.sort(byName);

        List<Module> misc = new ArrayList<>();
        misc.add(Kaguya.moduleManager.getModule(AuthSync.class));
        misc.add(Kaguya.moduleManager.getModule(Spammer.class));
        misc.add(Kaguya.moduleManager.getModule(BedNuker.class));
        misc.add(Kaguya.moduleManager.getModule(BedTracker.class));
        misc.add(Kaguya.moduleManager.getModule(Denick.class));
        misc.add(Kaguya.moduleManager.getModule(LightningTracker.class));
        misc.add(Kaguya.moduleManager.getModule(NoRotate.class));
        misc.add(Kaguya.moduleManager.getModule(NickHider.class));
        misc.add(Kaguya.moduleManager.getModule(AntiObbyTrap.class));
        misc.add(Kaguya.moduleManager.getModule(AntiObfuscate.class));
        misc.add(Kaguya.moduleManager.getModule(AutoAnduril.class));
        misc.add(Kaguya.moduleManager.getModule(ChatCopy.class));
        misc.add(Kaguya.moduleManager.getModule(InventoryClicker.class));
        misc.add(Kaguya.moduleManager.getModule(DiscordRichPresence.class));
        misc.add(Kaguya.moduleManager.getModule(HeldItemDetect.class));
        misc.add(Kaguya.moduleManager.getModule(PacketListener.class));
        misc.sort(byName);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combat);
        registered.addAll(movement);
        registered.addAll(render);
        registered.addAll(player);
        registered.addAll(misc);
        for (Module m : Kaguya.moduleManager.modules.values()) {
            if (!registered.contains(m)) {
                throw new RuntimeException(m.getClass().getName() + " is unregistered to click gui.");
            }
        }

        categories.add(new CategoryComponent("Combat",   combat));
        categories.add(new CategoryComponent("Movement", movement));
        categories.add(new CategoryComponent("Render",   render));
        categories.add(new CategoryComponent("Player",   player));
        categories.add(new CategoryComponent("Misc",     misc));
    }

    public static ClickGui getInstance() { return instance; }

    private int panelX(int sw) { return (sw - PANEL_W) / 2; }
    private int panelY(int sh) { return (sh - PANEL_H) / 2; }

    // =====================================================================
    // Animation helpers
    // =====================================================================

    private float getDt() {
        long now = System.nanoTime();
        if (lastFrameNs < 0) { lastFrameNs = now; return 0.016f; }
        float dt = (now - lastFrameNs) / 1_000_000_000f;
        lastFrameNs = now;
        return Math.min(dt, 0.05f);
    }

    private static float lerp(float from, float to, float speed, float dt) {
        float t = 1f - (float) Math.exp(-speed * dt);
        return from + (to - from) * t;
    }

    /** panelAlpha を既存の alpha チャンネルに掛け合わせる */
    private int a(int color) {
        int existingA = (color >> 24) & 0xFF;
        int newA = (int) (existingA * panelAlpha);
        return (color & 0x00FFFFFF) | (newA << 24);
    }

    private static int mulAlpha(int color, float alpha) {
        int existingA = (color >> 24) & 0xFF;
        int newA = (int) (existingA * alpha);
        return (color & 0x00FFFFFF) | (newA << 24);
    }

    // =====================================================================
    // drawScreen
    // =====================================================================
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float dt = getDt();

        // Advance animations
        panelAlpha     = lerp(panelAlpha, 1f, FADE_SPEED, dt);
        tabSlideOffset = lerp(tabSlideOffset, 0f, SLIDE_SPEED, dt);
        float settTarget = (selectedModule != null) ? 0f : SETT_W;
        settSlideOffset = lerp(settSlideOffset, settTarget, SLIDE_SPEED, dt);

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int px = panelX(sw);
        int py = panelY(sh);

        int contentY = py + TAB_H;
        int contentH = PANEL_H - TAB_H;
        int settX    = px + LIST_W + 1;

        // ---- Overlay ----
        drawRect(0, 0, sw, sh, a(C_OVERLAY));

        // ---- Panel background ----
        Gui.drawRect(px, py, px + PANEL_W, py + PANEL_H, a(C_PANEL));

        // ---- Tab bar ----
        drawTabBar(mouseX, mouseY, px, py);

        // ---- List area background ----
        Gui.drawRect(px, contentY, px + LIST_W, contentY + contentH, a(C_LIST));

        // ---- Divider ----
        Gui.drawRect(px + LIST_W, contentY, px + LIST_W + 1, contentY + contentH, a(C_DIVIDER));

        // ---- Settings area background ----
        Gui.drawRect(settX, contentY, px + PANEL_W, contentY + contentH, a(C_SETTINGS));

        // ---- Scroll interpolation ----
        animListScroll  += (listScroll  - animListScroll)  * 0.2;
        animSettScroll  += (settScroll  - animSettScroll)  * 0.2;
        animConfigScroll += (configScroll - animConfigScroll) * 0.2;

        if (isConfigTab()) {
            // ---- Config tab ----
            drawConfigTab(mouseX, mouseY, px, contentY, contentH, sr);
        } else {
            // ---- Module list (with tab slide) ----
            drawModuleList(mouseX, mouseY, px, contentY, contentH, sr);

            // ---- Settings panel (with slide-in/out) ----
            if (displayModule != null && settSlideOffset < SETT_W - 0.5f) {
                drawSettingsPanel(mouseX, mouseY, settX, contentY, contentH, sr);
                if (selectedModule != null) selectedModule.update(mouseX, mouseY);
            }
        }

        // ---- Mouse wheel scroll ----
        int wheel = Mouse.getDWheel();
        if (wheel != 0) handleScroll(wheel, mouseX, mouseY, px, contentY, contentH, settX);

        // ---- Save button (module tabs only) ----
        if (!isConfigTab()) {
            int btnX = px + PANEL_W - BTN_W - 4;
            int btnY = py + PANEL_H - BTN_H - 4;
            drawSaveButton(mouseX, mouseY, btnX, btnY);
        }

        // ---- Version label ----
        mc.fontRendererObj.drawStringWithShadow(
            "Kaguya " + KaguyaClient.VERSION,
            4, sh - 3 - mc.fontRendererObj.FONT_HEIGHT,
            C_ORANGE
        );
    }

    // ---- Tab bar ----
    private void drawTabBar(int mouseX, int mouseY, int px, int py) {
        int n    = totalTabs();
        int tabW = PANEL_W / n;
        for (int i = 0; i < n; i++) {
            int tx  = px + i * tabW;
            boolean act = (i == selectedTab);
            boolean hov = mouseX >= tx && mouseX < tx + tabW && mouseY >= py && mouseY < py + TAB_H;
            int bgColor = act ? C_ORANGE : (hov ? C_TAB_HOVER : C_TAB_IDLE);
            Gui.drawRect(tx, py, tx + tabW, py + TAB_H, a(bgColor));
            if (i > 0) Gui.drawRect(tx, py, tx + 1, py + TAB_H, a(C_DIVIDER));
            String name  = (i < categories.size()) ? categories.get(i).getName() : "Config";
            int    textX = tx + tabW / 2 - mc.fontRendererObj.getStringWidth(name) / 2;
            int    textY = py + TAB_H / 2 - mc.fontRendererObj.FONT_HEIGHT / 2;
            mc.fontRendererObj.drawStringWithShadow(name, textX, textY, act ? C_WHITE : C_DIM);
        }
    }

    // ---- Module list ----
    private void drawModuleList(int mouseX, int mouseY, int px, int contentY, int contentH, ScaledResolution sr) {
        CategoryComponent cat     = categories.get(selectedTab);
        List<Component>   modules = cat.getModules();
        int totalH    = modules.size() * ITEM_H;
        int maxScroll = Math.max(0, totalH - contentH);
        if (listScroll     > maxScroll) listScroll     = maxScroll;
        if (animListScroll > maxScroll) animListScroll = maxScroll;

        double scale  = sr.getScaleFactor();
        int    bottom = contentY + contentH;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(px * scale),
            (int)((sr.getScaledHeight() - bottom) * scale),
            (int)(LIST_W * scale),
            (int)(contentH * scale)
        );

        // Tab slide: translate items horizontally
        GlStateManager.pushMatrix();
        GlStateManager.translate(tabSlideOffset, 0, 0);

        for (int i = 0; i < modules.size(); i++) {
            ModuleComponent mod = (ModuleComponent) modules.get(i);
            int iy = contentY + i * ITEM_H - (int) animListScroll;
            if (iy + ITEM_H <= contentY || iy >= contentY + contentH) continue;

            boolean isSel   = (selectedModule == mod);
            boolean isHov   = mouseX >= px && mouseX < px + LIST_W && mouseY >= iy && mouseY < iy + ITEM_H;
            boolean enabled = mod.mod.isEnabled();

            if (isSel) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, a(C_ITEM_SEL));
                Gui.drawRect(px, iy, px + 2,      iy + ITEM_H, a(C_ORANGE));
            } else if (isHov) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, a(C_ITEM_HOVER));
            }

            // Toggle flash overlay
            Long flashTime = toggleFlashMap.get(mod.mod.getName());
            if (flashTime != null) {
                long elapsed = System.currentTimeMillis() - flashTime;
                if (elapsed < FLASH_DURATION) {
                    float fp = 1f - (float) elapsed / FLASH_DURATION;
                    fp = fp * fp; // ease-out
                    int flashColor = mulAlpha(enabled ? C_FLASH_ON : C_FLASH_OFF, fp);
                    Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, flashColor);
                }
            }

            int textY = iy + (ITEM_H - mc.fontRendererObj.FONT_HEIGHT) / 2;
            mc.fontRendererObj.drawStringWithShadow(mod.mod.getName(), px + 5, textY, enabled ? C_WHITE : C_DIM);

            // Enabled dot
            int dotX = px + LIST_W - 8;
            if (enabled) {
                Gui.drawRect(dotX, iy + ITEM_H / 2 - 2, dotX + 4, iy + ITEM_H / 2 + 2, a(C_ORANGE));
            }

            // Key binding label
            int key = mod.mod.getKey();
            if (key != 0) {
                String keyLabel = "<" + KeyBindUtil.getKeyName(key) + ">";
                int keyW = mc.fontRendererObj.getStringWidth(keyLabel);
                int keyX = dotX - 3 - keyW;
                mc.fontRendererObj.drawStringWithShadow(keyLabel, keyX, textY, C_DIM);
            }
        }

        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // List scrollbar
        if (totalH > contentH) {
            float barY = contentY + (float) animListScroll * contentH / totalH;
            float barH = (float) contentH * contentH / totalH;
            Gui.drawRect(px + LIST_W - 2, (int) barY, px + LIST_W, (int)(barY + barH), a(C_SCROLLBAR));
        }
    }

    // ---- Settings panel ----
    private void drawSettingsPanel(int mouseX, int mouseY, int settX, int contentY, int contentH, ScaledResolution sr) {
        if (displayModule == null) return;

        double scale  = sr.getScaleFactor();
        int    bottom = contentY + contentH;

        // Settings slide: translate horizontally (0=visible, SETT_W=hidden)
        GlStateManager.pushMatrix();
        GlStateManager.translate(settSlideOffset, 0, 0);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(settX * scale),
            (int)((sr.getScaledHeight() - bottom) * scale),
            (int)(SETT_W * scale),
            (int)(contentH * scale)
        );

        // Header: module name
        String name = displayModule.mod.getName();
        mc.fontRendererObj.drawStringWithShadow(name, settX + 7, contentY + 4, C_ORANGE);

        // HIDE / SHOW button
        boolean isHidden   = displayModule.mod.isHidden();
        String  hideLabel  = isHidden ? "HIDE" : "SHOW";
        int hideLabelW = mc.fontRendererObj.getStringWidth(hideLabel);
        int hideLabelX = settX + SETT_W - hideLabelW - 6;
        mc.fontRendererObj.drawStringWithShadow(hideLabel, hideLabelX, contentY + 4, isHidden ? C_DIM : C_ORANGE);

        // Separator line
        Gui.drawRect(settX + 4, contentY + HEADER_H - 1, settX + SETT_W - 4, contentY + HEADER_H, a(C_SEPARATOR));

        int settingsAreaTop = contentY + HEADER_H;
        int settingsAreaH   = contentH - HEADER_H;

        int totalH    = displayModule.getSettingsHeight();
        int maxScroll = Math.max(0, totalH - settingsAreaH);
        if (settScroll     > maxScroll) settScroll     = maxScroll;
        if (animSettScroll > maxScroll) animSettScroll = maxScroll;

        displayModule.category.setX(settX + 4);
        displayModule.category.setY(contentY - (int) animSettScroll);
        displayModule.category.setWidth(SETT_W - 8);
        displayModule.setComponentStartAt(0);
        displayModule.drawSettings(new AtomicInteger(0));

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.popMatrix();

        // Settings scrollbar (outside translate so it stays fixed)
        if (totalH > settingsAreaH) {
            float barY = settingsAreaTop + (float) animSettScroll * settingsAreaH / totalH;
            float barH = (float) settingsAreaH * settingsAreaH / totalH;
            Gui.drawRect(settX + SETT_W - 2, (int) barY, settX + SETT_W, (int)(barY + barH), a(C_SCROLLBAR));
        }
    }

    // ---- Config tab ----
    private void drawConfigTab(int mouseX, int mouseY, int px, int contentY, int contentH, ScaledResolution sr) {
        List<String> configs = getConfigNames();
        int settX  = px + LIST_W + 1;
        int bottom = contentY + contentH;
        double scale = sr.getScaleFactor();

        // -- Left: config list --
        int totalH    = configs.size() * ITEM_H;
        int maxScroll = Math.max(0, totalH - contentH);
        if (configScroll     > maxScroll) configScroll     = maxScroll;
        if (animConfigScroll > maxScroll) animConfigScroll = maxScroll;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(px * scale),
            (int)((sr.getScaledHeight() - bottom) * scale),
            (int)(LIST_W * scale),
            (int)(contentH * scale)
        );

        String active = Config.lastConfig;
        for (int i = 0; i < configs.size(); i++) {
            String cfg = configs.get(i);
            int iy = contentY + i * ITEM_H - (int) animConfigScroll;
            if (iy + ITEM_H <= contentY || iy >= bottom) continue;

            boolean isActive = cfg.equals(active);
            boolean isHov    = mouseX >= px && mouseX < px + LIST_W && mouseY >= iy && mouseY < iy + ITEM_H;

            if (isActive) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, a(C_ITEM_SEL));
                Gui.drawRect(px, iy, px + 2,      iy + ITEM_H, a(C_ORANGE));
            } else if (isHov) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, a(C_ITEM_HOVER));
            }

            int textY = iy + (ITEM_H - mc.fontRendererObj.FONT_HEIGHT) / 2;
            mc.fontRendererObj.drawStringWithShadow(cfg, px + 5, textY, isActive ? C_WHITE : C_DIM);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (totalH > contentH) {
            float barY = contentY + (float) animConfigScroll * contentH / totalH;
            float barH = (float) contentH * contentH / totalH;
            Gui.drawRect(px + LIST_W - 2, (int) barY, px + LIST_W, (int)(barY + barH), a(C_SCROLLBAR));
        }

        // -- Right: actions panel --
        int fh = mc.fontRendererObj.FONT_HEIGHT;

        // Header
        mc.fontRendererObj.drawStringWithShadow("Config Manager", settX + 7, contentY + 4, C_ORANGE);
        Gui.drawRect(settX + 4, contentY + HEADER_H - 1, settX + SETT_W - 4, contentY + HEADER_H, a(C_SEPARATOR));

        // Active config
        int row1 = contentY + HEADER_H + 6;
        String activeLabel = "Active: " + (active != null ? active : "none");
        mc.fontRendererObj.drawStringWithShadow(activeLabel, settX + 7, row1, C_DIM);

        // Save-as section
        int row2 = row1 + fh + 10;
        mc.fontRendererObj.drawStringWithShadow("Save as:", settX + 7, row2, C_WHITE);

        int fieldY  = row2 + fh + 4;
        int fieldX  = settX + 7;
        int fieldW  = configSaveField.width;
        int saveBtnX = fieldX + fieldW + 6;
        int saveBtnW = 42;

        // Text field border + background
        Gui.drawRect(fieldX - 1, fieldY - 2, fieldX + fieldW + 1, fieldY + 12, a(C_DIVIDER));
        configSaveField.xPosition = fieldX;
        configSaveField.yPosition = fieldY;
        configSaveField.drawTextBox();

        // Save button
        boolean hovSave = mouseX >= saveBtnX && mouseX < saveBtnX + saveBtnW
                       && mouseY >= fieldY - 2 && mouseY < fieldY + 12;
        Gui.drawRect(saveBtnX, fieldY - 2, saveBtnX + saveBtnW, fieldY + 12, a(hovSave ? C_TAB_HOVER : C_TAB_IDLE));
        String saveLbl = "Save";
        mc.fontRendererObj.drawStringWithShadow(saveLbl,
            saveBtnX + saveBtnW / 2 - mc.fontRendererObj.getStringWidth(saveLbl) / 2,
            fieldY, C_WHITE);

        // Separator
        int row3 = fieldY + 18;
        Gui.drawRect(settX + 4, row3, settX + SETT_W - 4, row3 + 1, a(C_SEPARATOR));

        // Hints
        mc.fontRendererObj.drawStringWithShadow("Left-click : Load config", settX + 7, row3 + 5, C_DIM);
    }

    private void handleConfigTabClick(int x, int y, int btn, int px, int contentY, int contentH, int settX) {
        // Config list (left panel)
        if (x >= px && x < px + LIST_W && y >= contentY && y < contentY + contentH) {
            List<String> configs = getConfigNames();
            for (int i = 0; i < configs.size(); i++) {
                int iy = contentY + i * ITEM_H - (int) animConfigScroll;
                if (y >= iy && y < iy + ITEM_H) {
                    String cfg = configs.get(i);
                    if (btn == 0) {
                        // Left-click: load
                        new Config(cfg, false).load();
                        configSaveField.setText(cfg);
                    }
                    return;
                }
            }
        }

        // Right panel: text field click
        if (x >= settX && x < settX + SETT_W) {
            configSaveField.mouseClicked(x, y, btn);

            // Save button hit test (matches drawConfigTab positioning)
            int fh     = mc.fontRendererObj.FONT_HEIGHT;
            int row1   = contentY + HEADER_H + 6;
            int row2   = row1 + fh + 10;
            int fieldY = row2 + fh + 4;
            int fieldX = settX + 7;
            int fieldW = configSaveField.width;
            int saveBtnX = fieldX + fieldW + 6;
            int saveBtnW = 42;

            if (btn == 0 && x >= saveBtnX && x < saveBtnX + saveBtnW
                         && y >= fieldY - 2 && y < fieldY + 12) {
                String name = configSaveField.getText().trim();
                if (!name.isEmpty()) {
                    new Config(name, false).save();
                }
            }
        }
    }

    private List<String> getConfigNames() {
        File dir = new File("./config/Myau/");
        List<String> names = new ArrayList<>();
        if (!dir.isDirectory()) return names;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json") && !n.endsWith(".tmp.json"));
        if (files == null) return names;
        for (File f : files) names.add(f.getName().substring(0, f.getName().length() - 5));
        Collections.sort(names);
        return names;
    }

    // ---- Save button ----
    private void drawSaveButton(int mouseX, int mouseY, int bx, int by) {
        boolean flashing = System.currentTimeMillis() < saveFlashUntil;
        boolean hovered  = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
        int bg = flashing ? new Color(80, 200, 80).getRGB()
                          : (hovered ? C_TAB_HOVER : C_TAB_IDLE);
        Gui.drawRect(bx,             by,             bx + BTN_W, by + BTN_H, a(bg));
        Gui.drawRect(bx,             by,             bx + BTN_W, by + 1,     a(C_ORANGE));
        Gui.drawRect(bx,             by + BTN_H - 1, bx + BTN_W, by + BTN_H, a(C_ORANGE));
        Gui.drawRect(bx,             by,             bx + 1,     by + BTN_H, a(C_ORANGE));
        Gui.drawRect(bx + BTN_W - 1, by,             bx + BTN_W, by + BTN_H, a(C_ORANGE));
        String label = flashing ? "Saved!" : "Save Config";
        int lx = bx + BTN_W / 2 - mc.fontRendererObj.getStringWidth(label) / 2;
        int ly = by + BTN_H / 2 - mc.fontRendererObj.FONT_HEIGHT / 2;
        mc.fontRendererObj.drawStringWithShadow(label, lx, ly, C_WHITE);
    }

    // ---- Scroll handler ----
    private void handleScroll(int wheel, int mouseX, int mouseY,
                               int px, int contentY, int contentH, int settX) {
        int amount = wheel > 0 ? -1 : 1;

        if (mouseX >= px && mouseX < px + LIST_W
                && mouseY >= contentY && mouseY < contentY + contentH) {
            if (isConfigTab()) {
                int totalH    = getConfigNames().size() * ITEM_H;
                int maxScroll = Math.max(0, totalH - contentH);
                configScroll = Math.max(0, Math.min(configScroll + amount * 14, maxScroll));
            } else {
                int totalH    = categories.get(selectedTab).getModules().size() * ITEM_H;
                int maxScroll = Math.max(0, totalH - contentH);
                listScroll = Math.max(0, Math.min(listScroll + amount * 14, maxScroll));
            }
        } else if (!isConfigTab() && selectedModule != null
                && mouseX >= settX && mouseX < settX + SETT_W
                && mouseY >= contentY && mouseY < contentY + contentH) {
            int totalH    = selectedModule.getSettingsHeight();
            int avail     = contentH - HEADER_H;
            int maxScroll = Math.max(0, totalH - avail);
            settScroll = Math.max(0, Math.min(settScroll + amount * 14, maxScroll));
        }
    }

    // =====================================================================
    // Input
    // =====================================================================
    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int px = panelX(sw);
        int py = panelY(sh);

        // Save button
        int btnX = px + PANEL_W - BTN_W - 4;
        int btnY = py + PANEL_H - BTN_H - 4;
        if (mouseButton == 0 && x >= btnX && x < btnX + BTN_W && y >= btnY && y < btnY + BTN_H) {
            new Config(Config.lastConfig != null ? Config.lastConfig : "default", false).save();
            saveFlashUntil = System.currentTimeMillis() + 800;
            return;
        }

        int contentY = py + TAB_H;
        int contentH = PANEL_H - TAB_H;
        int settX    = px + LIST_W + 1;

        // Tab bar
        if (y >= py && y < py + TAB_H && x >= px && x < px + PANEL_W) {
            int n    = totalTabs();
            int tabW = PANEL_W / n;
            for (int i = 0; i < n; i++) {
                int tx = px + i * tabW;
                if (x >= tx && x < tx + tabW) {
                    if (selectedTab != i) {
                        int dir = i > selectedTab ? 1 : -1;
                        tabSlideOffset  = dir * LIST_W * 0.7f;
                        selectedTab     = i;
                        listScroll      = 0;
                        animListScroll  = 0;
                        configScroll    = 0;
                        animConfigScroll = 0;
                        setSelectedModule(null);
                    }
                    return;
                }
            }
        }

        // Config tab handling
        if (isConfigTab()) {
            handleConfigTabClick(x, y, mouseButton, px, contentY, contentH, settX);
            return;
        }

        // Settings header → HIDE/SHOW or toggle ON/OFF
        if (selectedModule != null
                && x >= settX && x < settX + SETT_W
                && y >= contentY && y < contentY + HEADER_H
                && mouseButton == 0) {
            boolean isHid     = selectedModule.mod.isHidden();
            String  hideLabel = isHid ? "HIDE" : "SHOW";
            int hideLabelW = mc.fontRendererObj.getStringWidth(hideLabel);
            int hideLabelX = settX + SETT_W - hideLabelW - 6;
            if (x >= hideLabelX && x < hideLabelX + hideLabelW) {
                selectedModule.mod.setHidden(!isHid);
            } else {
                selectedModule.mod.toggle();
                toggleFlashMap.put(selectedModule.mod.getName(), System.currentTimeMillis());
            }
            return;
        }

        // Module list → toggle + select
        if (x >= px && x < px + LIST_W
                && y >= contentY && y < contentY + contentH) {
            List<Component> modules = categories.get(selectedTab).getModules();
            for (int i = 0; i < modules.size(); i++) {
                ModuleComponent mod = (ModuleComponent) modules.get(i);
                int iy = contentY + i * ITEM_H - (int) animListScroll;
                if (y >= iy && y < iy + ITEM_H) {
                    if (mouseButton == 0) {
                        mod.mod.toggle();
                        toggleFlashMap.put(mod.mod.getName(), System.currentTimeMillis());
                    }
                    setSelectedModule(mod);
                    return;
                }
            }
        }

        // Settings panel → forward to components
        if (selectedModule != null
                && x >= settX && x < settX + SETT_W
                && y >= contentY + HEADER_H && y < contentY + contentH) {
            selectedModule.mouseDownSettings(x, y, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int x, int y, int mouseButton) {
        if (selectedModule != null) selectedModule.mouseReleasedSettings(x, y, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int key) {
        if (isConfigTab()) {
            if (key == 1) { // ESC
                mc.displayGuiScreen(null);
            } else if (key == 28 && configSaveField.isFocused()) { // Enter = save
                String name = configSaveField.getText().trim();
                if (!name.isEmpty()) new Config(name, false).save();
            } else {
                configSaveField.textboxKeyTyped(typedChar, key);
            }
            return;
        }
        if (key == 1) {
            if (selectedModule != null && selectedModule.isAnyBinding()) {
                selectedModule.keyTypedSettings(typedChar, key);
            } else {
                mc.displayGuiScreen(null);
            }
        } else if (selectedModule != null) {
            selectedModule.keyTypedSettings(typedChar, key);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ---- helper ----
    private void setSelectedModule(ModuleComponent mod) {
        if (selectedModule != null) selectedModule.panelExpand = false;
        selectedModule = mod;
        settScroll     = 0;
        animSettScroll = 0;
        if (selectedModule != null) {
            selectedModule.panelExpand = true;
            displayModule = selectedModule; // update display target when selecting
        }
        // displayModule is kept when deselecting (for slide-out animation)
    }
}
