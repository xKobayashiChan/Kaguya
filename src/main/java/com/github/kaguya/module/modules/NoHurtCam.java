package com.github.kaguya.module.modules;

import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }
}
