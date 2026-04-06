package com.github.kaguya.ui;

import com.github.kaguya.KaguyaClient;
import com.github.kaguya.module.modules.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.kaguya.Kaguya;
import com.github.kaguya.module.Module;
import com.github.kaguya.module.modules.*;
import com.github.kaguya.ui.components.CategoryComponent;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGui extends GuiScreen {
    private static ClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;

    public ClickGui() {
        instance = this;

        List<Module> combatModules = new ArrayList<>();
        combatModules.add(Kaguya.moduleManager.getModule(AimAssist.class));
        combatModules.add(Kaguya.moduleManager.getModule(AutoClicker.class));
        combatModules.add(Kaguya.moduleManager.getModule(KillAura.class));
        combatModules.add(Kaguya.moduleManager.getModule(Wtap.class));
        combatModules.add(Kaguya.moduleManager.getModule(Velocity.class));
        combatModules.add(Kaguya.moduleManager.getModule(Freeze.class));
        combatModules.add(Kaguya.moduleManager.getModule(Reach.class));
        combatModules.add(Kaguya.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(Kaguya.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(Kaguya.moduleManager.getModule(AntiFireball.class));
        combatModules.add(Kaguya.moduleManager.getModule(LagRange.class));
        combatModules.add(Kaguya.moduleManager.getModule(HitBox.class));
        combatModules.add(Kaguya.moduleManager.getModule(MoreKB.class));
        combatModules.add(Kaguya.moduleManager.getModule(Refill.class));
        combatModules.add(Kaguya.moduleManager.getModule(HitSelect.class));

        List<Module> movementModules = new ArrayList<>();
        movementModules.add(Kaguya.moduleManager.getModule(AntiAFK.class));
        movementModules.add(Kaguya.moduleManager.getModule(Fly.class));
        movementModules.add(Kaguya.moduleManager.getModule(Speed.class));
        movementModules.add(Kaguya.moduleManager.getModule(LongJump.class));
        movementModules.add(Kaguya.moduleManager.getModule(Sprint.class));
        movementModules.add(Kaguya.moduleManager.getModule(SafeWalk.class));
        movementModules.add(Kaguya.moduleManager.getModule(Jesus.class));
        movementModules.add(Kaguya.moduleManager.getModule(Blink.class));
        movementModules.add(Kaguya.moduleManager.getModule(NoFall.class));
        movementModules.add(Kaguya.moduleManager.getModule(NoSlow.class));
        movementModules.add(Kaguya.moduleManager.getModule(KeepSprint.class));
        movementModules.add(Kaguya.moduleManager.getModule(Eagle.class));
        movementModules.add(Kaguya.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(Kaguya.moduleManager.getModule(AntiVoid.class));

        List<Module> renderModules = new ArrayList<>();
        renderModules.add(Kaguya.moduleManager.getModule(Cape.class));
        renderModules.add(Kaguya.moduleManager.getModule(ESP.class));
        renderModules.add(Kaguya.moduleManager.getModule(Chams.class));
        renderModules.add(Kaguya.moduleManager.getModule(FriendTransparency.class));
        renderModules.add(Kaguya.moduleManager.getModule(FullBright.class));
        renderModules.add(Kaguya.moduleManager.getModule(Tracers.class));
        renderModules.add(Kaguya.moduleManager.getModule(NameTags.class));
        renderModules.add(Kaguya.moduleManager.getModule(Xray.class));
        renderModules.add(Kaguya.moduleManager.getModule(TargetHUD.class));
        renderModules.add(Kaguya.moduleManager.getModule(Indicators.class));
        renderModules.add(Kaguya.moduleManager.getModule(BedESP.class));
        renderModules.add(Kaguya.moduleManager.getModule(ItemESP.class));
        renderModules.add(Kaguya.moduleManager.getModule(Camera.class));
        renderModules.add(Kaguya.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(Kaguya.moduleManager.getModule(HUD.class));
        renderModules.add(Kaguya.moduleManager.getModule(ClientHUD.class));
        renderModules.add(Kaguya.moduleManager.getModule(GuiModule.class));
        renderModules.add(Kaguya.moduleManager.getModule(ChestESP.class));
        renderModules.add(Kaguya.moduleManager.getModule(Trajectories.class));
        renderModules.add(Kaguya.moduleManager.getModule(Radar.class));

        List<Module> playerModules = new ArrayList<>();
        playerModules.add(Kaguya.moduleManager.getModule(AutoHeal.class));
        playerModules.add(Kaguya.moduleManager.getModule(AutoTool.class));
        playerModules.add(Kaguya.moduleManager.getModule(ChestStealer.class));
        playerModules.add(Kaguya.moduleManager.getModule(InvManager.class));
        playerModules.add(Kaguya.moduleManager.getModule(InvWalk.class));
        playerModules.add(Kaguya.moduleManager.getModule(Scaffold.class));
        playerModules.add(Kaguya.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(Kaguya.moduleManager.getModule(SpeedMine.class));
        playerModules.add(Kaguya.moduleManager.getModule(FastPlace.class));
        playerModules.add(Kaguya.moduleManager.getModule(GhostHand.class));
        playerModules.add(Kaguya.moduleManager.getModule(MCF.class));
        playerModules.add(Kaguya.moduleManager.getModule(AntiDebuff.class));

        List<Module> miscModules = new ArrayList<>();
        miscModules.add(Kaguya.moduleManager.getModule(Spammer.class));
        miscModules.add(Kaguya.moduleManager.getModule(BedNuker.class));
        miscModules.add(Kaguya.moduleManager.getModule(BedTracker.class));
        miscModules.add(Kaguya.moduleManager.getModule(Denick.class));
        miscModules.add(Kaguya.moduleManager.getModule(LightningTracker.class));
        miscModules.add(Kaguya.moduleManager.getModule(NoRotate.class));
        miscModules.add(Kaguya.moduleManager.getModule(NickHider.class));
        miscModules.add(Kaguya.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(Kaguya.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(Kaguya.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(Kaguya.moduleManager.getModule(ChatCopy.class));
        miscModules.add(Kaguya.moduleManager.getModule(InventoryClicker.class));
        miscModules.add(Kaguya.moduleManager.getModule(DiscordRichPresence.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);

        for (Module module : Kaguya.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList = new ArrayList<>();
        int topOffset = 5;


        CategoryComponent combat = new CategoryComponent("Combat", combatModules);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 20;

        CategoryComponent movement = new CategoryComponent("Movement", movementModules);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 20;

        CategoryComponent render = new CategoryComponent("Render", renderModules);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 20;

        CategoryComponent player = new CategoryComponent("Player", playerModules);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 20;

        CategoryComponent misc = new CategoryComponent("Misc", miscModules);
        misc.setY(topOffset);
        categoryList.add(misc);

        loadPositions();
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        mc.fontRendererObj.drawStringWithShadow("Kaguya Client " + KaguyaClient.VERSION, 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> btnCat = categoryList.iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.mousePressed(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.setPin(!category.isPin());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                c.mouseDown(x, y, mouseButton);
            }
        }

    }

    public void mouseReleased(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> iterator = categoryList.iterator();

        CategoryComponent categoryComponent;
        while (iterator.hasNext()) {
            categoryComponent = iterator.next();
            if (mouseButton == 0) {
                categoryComponent.mousePressed(false);
            }
        }

        iterator = categoryList.iterator();

        while (true) {
            do {
                do {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    categoryComponent = iterator.next();
                } while (!categoryComponent.isOpened());
            } while (categoryComponent.getModules().isEmpty());

            for (Component component : categoryComponent.getModules()) {
                component.mouseReleased(x, y, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = categoryList.iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
