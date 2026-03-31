package com.example.lexiyaddons.module;

import com.example.lexiyaddons.module.modules.ClickGuiModule;
import com.example.lexiyaddons.module.modules.FullBright;
import com.example.lexiyaddons.module.modules.Sprint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // ── Movement ──
        modules.add(new Sprint());

        // ── Render ──
        modules.add(new FullBright());

        // ── Misc ──
        modules.add(new ClickGuiModule());

        // ★ 新しいモジュールはここに追加 ★
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return modules.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}

