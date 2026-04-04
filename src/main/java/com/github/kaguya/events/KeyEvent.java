package com.github.kaguya.events;

import com.github.kaguya.event.events.Event;

public class KeyEvent implements Event {
    private final int keyCode;

    public KeyEvent(int key) {
        this.keyCode = key;
    }

    public int getKey() {
        return this.keyCode;
    }
}
