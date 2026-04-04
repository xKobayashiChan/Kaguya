package com.github.kaguya.events;

import com.github.kaguya.event.events.Event;
import com.github.kaguya.event.types.EventType;
import net.minecraft.entity.EntityLivingBase;

public class RenderLivingEvent implements Event {
    private final EventType type;
    private final EntityLivingBase entity;

    public RenderLivingEvent(EventType type, EntityLivingBase entityLivingBase) {
        this.type = type;
        this.entity = entityLivingBase;
    }

    public EventType getType() {
        return this.type;
    }

    public EntityLivingBase getEntity() {
        return this.entity;
    }
}
