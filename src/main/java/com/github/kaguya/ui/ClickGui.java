package com.github.kaguya.ui;

import com.github.kaguya.KaguyaClient;
import com.github.kaguya.Kaguya;
import com.github.kaguya.module.Module;
import com.github.kaguya.module.modules.*;
import com.github.kaguya.ui.components.CategoryComponent;
import com.github.kaguya.ui.components.ModuleComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
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

    // ---- Layout ----
    private static final int PANEL_W  = 340;
    private static final int PANEL_H  = 210;
    private static final int TAB_H    = 16;
    private static final int LIST_W   = 110;
    private static final int ITEM_H   = 14;
    private static final int HEADER_H = 16;

    // ---- State ----
    private final ArrayList<CategoryComponent> categories;
    private int selectedTab = 0;
    private ModuleComponent selectedModule = null;

    private int    listScroll     = 0;
    private double animListScroll = 0;
    private int    settScroll     = 0;
    private double animSettScroll = 0;

    public ClickGui() {
        instance = this;
        categories = new ArrayList<>();
        buildCategories();
    }

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
        movement.sort(byName);

        List<Module> render = new ArrayList<>();
        render.add(Kaguya.moduleManager.getModule(Cape.class));
        render.add(Kaguya.moduleManager.getModule(ESP.class));
        render.add(Kaguya.moduleManager.getModule(Chams.class));
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

    public static ClickGui getInstance() {
        return instance;
    }

    // ---- coordinate helpers ----
    private int panelX(int sw) { return (sw - PANEL_W) / 2; }
    private int panelY(int sh) { return (sh - PANEL_H) / 2; }

    // =====================================================================
    // drawScreen
    // =====================================================================
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int px = panelX(sw);
        int py = panelY(sh);

        int contentY = py + TAB_H;
        int contentH = PANEL_H - TAB_H;
        int settX    = px + LIST_W + 1;
        int settW    = PANEL_W - LIST_W - 1;

        // dim the world behind
        drawRect(0, 0, sw, sh, C_OVERLAY);

        // main panel
        Gui.drawRect(px, py, px + PANEL_W, py + PANEL_H, C_PANEL);

        // tab bar
        drawTabBar(mouseX, mouseY, px, py);

        // list area
        Gui.drawRect(px, contentY, px + LIST_W, contentY + contentH, C_LIST);

        // divider
        Gui.drawRect(px + LIST_W, contentY, px + LIST_W + 1, contentY + contentH, C_DIVIDER);

        // settings area
        Gui.drawRect(settX, contentY, px + PANEL_W, contentY + contentH, C_SETTINGS);

        // animate scroll
        animListScroll += (listScroll - animListScroll) * 0.2;
        animSettScroll += (settScroll - animSettScroll) * 0.2;

        // module list
        drawModuleList(mouseX, mouseY, px, contentY, contentH, sr);

        // settings panel
        if (selectedModule != null) {
            drawSettingsPanel(mouseX, mouseY, settX, settW, contentY, contentH, sr);
            selectedModule.update(mouseX, mouseY);
        }

        // scroll from mouse wheel
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            handleScroll(wheel, mouseX, mouseY, px, contentY, contentH, settX);
        }

        mc.fontRendererObj.drawStringWithShadow(
            "Kaguya " + KaguyaClient.VERSION,
            4, sh - 3 - mc.fontRendererObj.FONT_HEIGHT,
            C_ORANGE
        );
    }

    // ---- tab bar ----
    private void drawTabBar(int mouseX, int mouseY, int px, int py) {
        int n    = categories.size();
        int tabW = PANEL_W / n;
        for (int i = 0; i < n; i++) {
            int tx      = px + i * tabW;
            boolean act = (i == selectedTab);
            boolean hov = mouseX >= tx && mouseX < tx + tabW && mouseY >= py && mouseY < py + TAB_H;
            Gui.drawRect(tx, py, tx + tabW, py + TAB_H, act ? C_ORANGE : (hov ? C_TAB_HOVER : C_TAB_IDLE));
            if (i > 0) Gui.drawRect(tx, py, tx + 1, py + TAB_H, C_DIVIDER);
            String name  = categories.get(i).getName();
            int    textX = tx + tabW / 2 - mc.fontRendererObj.getStringWidth(name) / 2;
            int    textY = py + TAB_H / 2 - mc.fontRendererObj.FONT_HEIGHT / 2;
            mc.fontRendererObj.drawStringWithShadow(name, textX, textY, act ? C_WHITE : C_DIM);
        }
    }

    // ---- module list ----
    private void drawModuleList(int mouseX, int mouseY, int px, int contentY, int contentH, ScaledResolution sr) {
        CategoryComponent cat     = categories.get(selectedTab);
        List<Component>   modules = cat.getModules();
        int totalH   = modules.size() * ITEM_H;
        int maxScroll = Math.max(0, totalH - contentH);
        if (listScroll > maxScroll)     listScroll     = maxScroll;
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

        for (int i = 0; i < modules.size(); i++) {
            ModuleComponent mod = (ModuleComponent) modules.get(i);
            int iy = contentY + i * ITEM_H - (int) animListScroll;
            if (iy + ITEM_H <= contentY || iy >= contentY + contentH) continue;

            boolean isSel  = (selectedModule == mod);
            boolean isHov  = mouseX >= px && mouseX < px + LIST_W && mouseY >= iy && mouseY < iy + ITEM_H;
            boolean enabled = mod.mod.isEnabled();

            if (isSel) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, C_ITEM_SEL);
                Gui.drawRect(px, iy, px + 2,       iy + ITEM_H, C_ORANGE);
            } else if (isHov) {
                Gui.drawRect(px, iy, px + LIST_W, iy + ITEM_H, C_ITEM_HOVER);
            }

            mc.fontRendererObj.drawStringWithShadow(mod.mod.getName(), px + 5, iy + 3, enabled ? C_WHITE : C_DIM);

            // orange dot when enabled
            if (enabled) {
                Gui.drawRect(px + LIST_W - 8, iy + ITEM_H / 2 - 2,
                             px + LIST_W - 4, iy + ITEM_H / 2 + 2, C_ORANGE);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // list scrollbar
        if (totalH > contentH) {
            float barY = contentY + (float) animListScroll * contentH / totalH;
            float barH = (float) contentH * contentH / totalH;
            Gui.drawRect(px + LIST_W - 2, (int) barY,
                         px + LIST_W,     (int)(barY + barH), C_SCROLLBAR);
        }
    }

    // ---- settings panel ----
    private void drawSettingsPanel(int mouseX, int mouseY, int settX, int settW,
                                   int contentY, int contentH, ScaledResolution sr) {
        // header: module name
        String name    = selectedModule.mod.getName();
        boolean enabled = selectedModule.mod.isEnabled();
        mc.fontRendererObj.drawStringWithShadow(name, settX + 7, contentY + 4, C_ORANGE);

        // ON / OFF badge on the right
        String badge   = enabled ? "ON" : "OFF";
        int badgeColor = enabled ? C_ORANGE : C_DIM;
        int badgeX     = settX + settW - mc.fontRendererObj.getStringWidth(badge) - 6;
        mc.fontRendererObj.drawStringWithShadow(badge, badgeX, contentY + 4, badgeColor);

        // thin separator
        Gui.drawRect(settX + 4, contentY + HEADER_H - 1, settX + settW - 4, contentY + HEADER_H, C_SEPARATOR);

        int settingsAreaTop = contentY + HEADER_H;
        int settingsAreaH   = contentH - HEADER_H;

        // total height of settings
        int totalH    = selectedModule.getSettingsHeight();
        int maxScroll = Math.max(0, totalH - settingsAreaH);
        if (settScroll > maxScroll)     settScroll     = maxScroll;
        if (animSettScroll > maxScroll) animSettScroll = maxScroll;

        // point category at settings panel (used by all child components)
        // children start at offsetY=16 (setComponentStartAt(0) → y = 0+16),
        // so category.y = contentY puts first item at contentY+16 = settingsAreaTop
        selectedModule.category.setX(settX + 4);
        selectedModule.category.setY(contentY - (int) animSettScroll);
        selectedModule.category.setWidth(settW - 8);
        selectedModule.setComponentStartAt(0);

        double scale  = sr.getScaleFactor();
        int    bottom = settingsAreaTop + settingsAreaH;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(settX * scale),
            (int)((sr.getScaledHeight() - bottom) * scale),
            (int)(settW * scale),
            (int)(settingsAreaH * scale)
        );

        selectedModule.drawSettings(new AtomicInteger(0));

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // settings scrollbar
        if (totalH > settingsAreaH) {
            float barY = settingsAreaTop + (float) animSettScroll * settingsAreaH / totalH;
            float barH = (float) settingsAreaH * settingsAreaH / totalH;
            Gui.drawRect(settX + settW - 2, (int) barY,
                         settX + settW,     (int)(barY + barH), C_SCROLLBAR);
        }
    }

    // ---- scroll ----
    private void handleScroll(int wheel, int mouseX, int mouseY,
                               int px, int contentY, int contentH, int settX) {
        int amount = wheel > 0 ? -1 : 1; // -1 = scroll up
        int settW  = PANEL_W - LIST_W - 1;

        if (mouseX >= px && mouseX < px + LIST_W
                && mouseY >= contentY && mouseY < contentY + contentH) {
            int totalH    = categories.get(selectedTab).getModules().size() * ITEM_H;
            int maxScroll = Math.max(0, totalH - contentH);
            listScroll = Math.max(0, Math.min(listScroll + amount * 12, maxScroll));

        } else if (selectedModule != null
                && mouseX >= settX && mouseX < settX + settW
                && mouseY >= contentY && mouseY < contentY + contentH) {
            int totalH    = selectedModule.getSettingsHeight();
            int avail     = contentH - HEADER_H;
            int maxScroll = Math.max(0, totalH - avail);
            settScroll = Math.max(0, Math.min(settScroll + amount * 12, maxScroll));
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

        int contentY = py + TAB_H;
        int contentH = PANEL_H - TAB_H;
        int settX    = px + LIST_W + 1;
        int settW    = PANEL_W - LIST_W - 1;

        // tab bar
        if (y >= py && y < py + TAB_H && x >= px && x < px + PANEL_W) {
            int n    = categories.size();
            int tabW = PANEL_W / n;
            for (int i = 0; i < n; i++) {
                int tx = px + i * tabW;
                if (x >= tx && x < tx + tabW) {
                    if (selectedTab != i) {
                        selectedTab    = i;
                        listScroll     = 0;
                        animListScroll = 0;
                        setSelectedModule(null);
                    }
                    return;
                }
            }
        }

        // settings header → toggle ON/OFF
        if (selectedModule != null
                && x >= settX && x < settX + settW
                && y >= contentY && y < contentY + HEADER_H
                && mouseButton == 0) {
            selectedModule.mod.toggle();
            return;
        }

        // module list → toggle + select
        if (x >= px && x < px + LIST_W
                && y >= contentY && y < contentY + contentH) {
            List<Component> modules = categories.get(selectedTab).getModules();
            for (int i = 0; i < modules.size(); i++) {
                ModuleComponent mod = (ModuleComponent) modules.get(i);
                int iy = contentY + i * ITEM_H - (int) animListScroll;
                if (y >= iy && y < iy + ITEM_H) {
                    if (mouseButton == 0) mod.mod.toggle();
                    setSelectedModule(mod);
                    return;
                }
            }
        }

        // settings panel → forward to components
        if (selectedModule != null
                && x >= settX && x < settX + settW
                && y >= contentY + HEADER_H && y < contentY + contentH) {
            selectedModule.mouseDownSettings(x, y, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int x, int y, int mouseButton) {
        if (selectedModule != null) {
            selectedModule.mouseReleasedSettings(x, y, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            mc.displayGuiScreen(null);
        } else if (selectedModule != null) {
            selectedModule.keyTypedSettings(typedChar, key);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ---- helper ----
    private void setSelectedModule(ModuleComponent mod) {
        if (selectedModule != null) selectedModule.panelExpand = false;
        selectedModule    = mod;
        settScroll        = 0;
        animSettScroll    = 0;
        if (selectedModule != null) selectedModule.panelExpand = true;
    }
}
