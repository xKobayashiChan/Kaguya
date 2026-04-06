package com.github.kaguya.init;

import com.github.kaguya.update.AutoUpdater;

public class Initializer {
    public Initializer() {
        System.out.println("Meow!");
        AutoUpdater.startup();
    }
}
