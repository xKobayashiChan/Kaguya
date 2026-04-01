package com.example.lexiyaddons.events;

import com.example.lexiyaddons.event.events.Event;

public class RaytraceEvent implements Event {
    private double range;

    public RaytraceEvent(double range) {
        this.range = range;
    }

    public double getRange() {
        return this.range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
