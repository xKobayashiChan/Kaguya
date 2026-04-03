package com.example.lexiyaddons.event;

import com.example.lexiyaddons.event.events.EventStoppable;
import org.junit.Test;

import static org.junit.Assert.*;

public class EventStoppableTest {

    // Concrete subclass for testing
    private static class TestStoppableEvent extends EventStoppable {
    }

    @Test
    public void newEventIsNotStopped() {
        TestStoppableEvent event = new TestStoppableEvent();
        assertFalse(event.isStopped());
    }

    @Test
    public void stopMakesEventStopped() {
        TestStoppableEvent event = new TestStoppableEvent();
        event.stop();
        assertTrue(event.isStopped());
    }

    @Test
    public void stopIsIdempotent() {
        TestStoppableEvent event = new TestStoppableEvent();
        event.stop();
        event.stop();
        assertTrue(event.isStopped());
    }
}
