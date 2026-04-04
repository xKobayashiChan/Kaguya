package com.github.kaguya.events;

import com.github.kaguya.event.events.Event;
import com.github.kaguya.event.types.EventType;

public class TickEvent implements Event {
    private final EventType type;

    public TickEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return this.type;
    }
}
