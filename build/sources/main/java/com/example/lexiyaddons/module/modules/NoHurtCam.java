package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.module.Module;
import com.example.lexiyaddons.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }
}
