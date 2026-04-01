package com.example.lexiyaddons.events;

import com.example.lexiyaddons.event.events.Event;
import com.example.lexiyaddons.event.types.EventType;

public class TickEvent implements Event {
    private final EventType type;

    public TickEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return this.type;
    }
}
