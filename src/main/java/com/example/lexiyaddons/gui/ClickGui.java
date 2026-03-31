package com.example.lexiyaddons.gui;

import com.example.lexiyaddons.LexiyAddons;
import com.example.lexiyaddons.module.Category;
import com.example.lexiyaddons.module.modules.ClickGuiModule;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI — カテゴリごとにパネルを表示し、モジュールをクリックでトグルできる
 *
 * ・左クリック  : モジュールのON/OFF
 * ・左ドラッグ  : パネルの移動
 * ・右クリック  : パネルの展開/折りたたみ
 * ・ESC / RSHIFT: GUIを閉じる
 */
public class ClickGui extends GuiScreen {
    private final List<CategoryPanel> panels = new ArrayList<>();

    public ClickGui() {
        int x = 10;
        for (Category category : Category.values()) {
            panels.add(new CategoryPanel(category, x, 10));
            x += 110;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        for (CategoryPanel panel : panels) {
            panel.draw(mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (CategoryPanel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (CategoryPanel panel : panels) {
            panel.mouseReleased();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // 右SHIFTでも閉じる
        if (keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        // GUI が閉じたらモジュールの状態をリセット
        ClickGuiModule mod = LexiyAddons.getInstance().getModuleManager().getModuleByClass(ClickGuiModule.class);
        if (mod != null && mod.isEnabled()) {
            mod.setEnabledSilent(false);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

