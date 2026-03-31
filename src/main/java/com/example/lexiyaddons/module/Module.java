package com.example.lexiyaddons.module;

import org.lwjgl.input.Keyboard;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private int keyBind;
    private boolean enabled;

    public Module(String name, String description, Category category, int keyBind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyBind = keyBind;
        this.enabled = false;
    }

    /** モジュール有効時に呼ばれる */
    public void onEnable() {}

    /** モジュール無効時に呼ばれる */
    public void onDisable() {}

    /** 毎ティック呼ばれる（有効時のみ） */
    public void onTick() {}

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) onEnable();
            else onDisable();
        }
    }

    /** onEnable/onDisable を呼ばずに状態だけ変更する */
    public void setEnabledSilent(boolean enabled) {
        this.enabled = enabled;
    }

    // ── Getters / Setters ──

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

