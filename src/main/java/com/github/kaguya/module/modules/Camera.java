package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.mixins.IAccessorEntityRenderer;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class Camera extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final float DEFAULT_DISTANCE = 4.0F;

    public final FloatProperty distance = new FloatProperty("distance", DEFAULT_DISTANCE, 1.0F, 20.0F);

    public Camera() {
        super("Camera", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && this.isEnabled() && mc.entityRenderer != null) {
            ((IAccessorEntityRenderer) mc.entityRenderer).setThirdPersonDistance(distance.getValue());
        }
    }

    @Override
    public void onEnabled() {
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.entityRenderer != null) {
            ((IAccessorEntityRenderer) mc.entityRenderer).setThirdPersonDistance(DEFAULT_DISTANCE);
        }
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}

