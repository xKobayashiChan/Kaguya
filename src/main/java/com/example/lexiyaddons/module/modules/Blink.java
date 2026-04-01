package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.Myau;
import com.example.lexiyaddons.enums.BlinkModules;
import com.example.lexiyaddons.event.EventTarget;
import com.example.lexiyaddons.event.types.EventType;
import com.example.lexiyaddons.event.types.Priority;
import com.example.lexiyaddons.events.LoadWorldEvent;
import com.example.lexiyaddons.events.TickEvent;
import com.example.lexiyaddons.module.Module;
import com.example.lexiyaddons.property.properties.IntProperty;
import com.example.lexiyaddons.property.properties.ModeProperty;

public class Blink extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "PULSE"});
    public final IntProperty ticks = new IntProperty("ticks", 20, 0, 1200);

    public Blink() {
        super("Blink", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (!Myau.blinkManager.getBlinkingModule().equals(BlinkModules.BLINK)) {
                this.setEnabled(false);
            } else {
                if (this.ticks.getValue() > 0 && Myau.blinkManager.countMovement() > (long) this.ticks.getValue()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            this.setEnabled(false);
                            break;
                        case 1:
                            Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.setEnabled(false);
    }

    @Override
    public void onEnabled() {
        Myau.blinkManager.setBlinkState(false, Myau.blinkManager.getBlinkingModule());
        Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}
