package com.example.lexiyaddons.gui;

import com.example.lexiyaddons.LexiyAddons;
import com.example.lexiyaddons.module.Category;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * カテゴリパネル — 1つのカテゴリに属するモジュール一覧を描画する
 * ドラッグで移動可能、右クリックで折りたたみ可能
 */
public class CategoryPanel {
    private final Category category;
    private int x, y;
    private final int width = 100;
    private final int headerHeight = 20;
    private final int moduleHeight = 16;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private boolean expanded = true;

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
    }

    public void draw(int mouseX, int mouseY, FontRenderer fr) {
        // ドラッグ中はパネル位置を更新
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }

        // ── ヘッダー描画 ──
        Gui.drawRect(x, y, x + width, y + headerHeight, getCategoryColor());
        fr.drawStringWithShadow(category.getDisplayName(), x + 4, y + 6, 0xFFFFFFFF);
        // 展開/折りたたみアイコン
        String arrow = expanded ? "-" : "+";
        fr.drawStringWithShadow(arrow, x + width - 10, y + 6, 0xFFFFFFFF);

        if (!expanded) return;

        // ── モジュール一覧描画 ──
        List<Module> modules = LexiyAddons.getInstance().getModuleManager().getModulesByCategory(category);
        int yOffset = y + headerHeight;

        for (Module module : modules) {
            boolean hovered = isHovered(mouseX, mouseY, x, yOffset, width, moduleHeight);
            int bgColor = hovered ? 0xCC333333 : 0xCC1A1A1A;
            Gui.drawRect(x, yOffset, x + width, yOffset + moduleHeight, bgColor);

            // ON = 緑、OFF = グレー
            int textColor = module.isEnabled() ? 0xFF00FF00 : 0xFFAAAAAA;
            fr.drawStringWithShadow(module.getName(), x + 4, yOffset + 4, textColor);

            // キーバインド表示
            if (module.getKeyBind() != Keyboard.KEY_NONE) {
                String keyName = Keyboard.getKeyName(module.getKeyBind());
                int keyWidth = fr.getStringWidth(keyName);
                fr.drawStringWithShadow(keyName, x + width - keyWidth - 4, yOffset + 4, 0xFF666666);
            }

            yOffset += moduleHeight;
        }

        // 下部ボーダー
        Gui.drawRect(x, yOffset, x + width, yOffset + 2, getCategoryColor());
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        // ── 左クリック ──
        if (button == 0) {
            // ヘッダークリック → ドラッグ開始
            if (isHovered(mouseX, mouseY, x, y, width, headerHeight)) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return;
            }

            // モジュールクリック → トグル
            if (expanded) {
                List<Module> modules = LexiyAddons.getInstance().getModuleManager().getModulesByCategory(category);
                int yOffset = y + headerHeight;
                for (Module module : modules) {
                    if (isHovered(mouseX, mouseY, x, yOffset, width, moduleHeight)) {
                        module.toggle();
                        return;
                    }
                    yOffset += moduleHeight;
                }
            }
        }

        // ── 右クリック → 展開/折りたたみ ──
        if (button == 1) {
            if (isHovered(mouseX, mouseY, x, y, width, headerHeight)) {
                expanded = !expanded;
            }
        }
    }

    public void mouseReleased() {
        dragging = false;
    }

    private boolean isHovered(int mouseX, int mouseY, int rx, int ry, int rw, int rh) {
        return mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;
    }

    private int getCategoryColor() {
        switch (category) {
            case COMBAT:   return 0xFFCC3333; // 赤
            case MOVEMENT: return 0xFF3366CC; // 青
            case RENDER:   return 0xFF9933CC; // 紫
            case PLAYER:   return 0xFF33CC33; // 緑
            case MISC:     return 0xFF999999; // グレー
            default:       return 0xFF666666;
        }
    }
}

