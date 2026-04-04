package com.github.kaguya.module.modules;

import com.github.kaguya.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", false, true);
    }

    public String stripObfuscated(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("§k", "");
    }
}
