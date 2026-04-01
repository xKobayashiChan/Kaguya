package com.example.lexiyaddons.module.modules;

import com.example.lexiyaddons.event.EventTarget;
import com.example.lexiyaddons.event.types.EventType;
import com.example.lexiyaddons.event.types.Priority;
import com.example.lexiyaddons.events.TickEvent;
import com.example.lexiyaddons.mixins.IAccessorEntityLivingBase;
import com.example.lexiyaddons.module.Module;
import com.example.lexiyaddons.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty delay = new IntProperty("delay", 3, 0, 8);

    public NoJumpDelay() {
        super("NoJumpDelay", false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((IAccessorEntityLivingBase) mc.thePlayer)
                    .setJumpTicks(Math.min(((IAccessorEntityLivingBase) mc.thePlayer).getJumpTicks(), this.delay.getValue() + 1));
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
