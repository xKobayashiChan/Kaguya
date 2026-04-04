package com.github.kaguya.command.commands;

import com.github.kaguya.Myau;
import com.github.kaguya.command.Command;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super(new ArrayList<>(Arrays.asList("list", "l", "modules", "myau")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Myau.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", Myau.clientName));
            for (Module module : Myau.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
