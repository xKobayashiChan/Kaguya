package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.enums.BlinkModules;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.LoadWorldEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.property.properties.ModeProperty;

public class Blink extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "PULSE"});
    public final IntProperty ticks = new IntProperty("ticks", 20, 0, 1200);

    public Blink() {
        super("Blink", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (!Kaguya.blinkManager.getBlinkingModule().equals(BlinkModules.BLINK)) {
                this.setEnabled(false);
            } else {
                if (this.ticks.getValue() > 0 && Kaguya.blinkManager.countMovement() > (long) this.ticks.getValue()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            this.setEnabled(false);
                            break;
                        case 1:
                            Kaguya.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            Kaguya.blinkManager.setBlinkState(true, BlinkModules.BLINK);
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
        Kaguya.blinkManager.setBlinkState(false, Kaguya.blinkManager.getBlinkingModule());
        Kaguya.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        Kaguya.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}
