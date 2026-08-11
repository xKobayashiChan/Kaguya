package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.event.types.Priority;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.mixins.IAccessorMinecraft;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty speed = new FloatProperty("speed", 1.0F, 0.01F, 10.0F);

    public Timer() {
        super("Timer", false);
    }

    @Override
    public void onDisabled() {
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) timer.timerSpeed = 1.0F;
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) timer.timerSpeed = this.speed.getValue();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.speed.getValue())};
    }
}
