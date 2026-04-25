package com.github.kaguya.module.modules;

import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.IntProperty;

public class HideClientText extends Module {

    /** ClientHUD が自分のテキストを描画中はtrue — Mixin 側でチェックして除外する */
    public static boolean clientHUDRendering = false;

    public final IntProperty thresholdX = new IntProperty("threshold-x", 120, 50, 500);
    public final IntProperty thresholdY = new IntProperty("threshold-y", 12,  5, 100);

    public HideClientText() {
        super("HideClientText", false, true);
    }
}
