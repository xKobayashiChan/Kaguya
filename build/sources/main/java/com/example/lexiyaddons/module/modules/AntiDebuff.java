package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.module.Module;
import com.example.lexiyaddons.property.properties.BooleanProperty;

public class AntiDebuff extends Module {
    public final BooleanProperty blindness = new BooleanProperty("blindness", true);
    public final BooleanProperty nausea = new BooleanProperty("nausea", true);

    public AntiDebuff() {
        super("AntiDebuff", false);
    }
}
