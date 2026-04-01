package com.example.lexiyaddons.command.commands;

import com.example.lexiyaddons.Myau;
import com.example.lexiyaddons.command.Command;
import com.example.lexiyaddons.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(new ArrayList<>(Arrays.asList("help", "commands")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Myau.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sCommands:&r", Myau.clientName));
            for (Command command : Myau.commandManager.commands) {
                if (!(command instanceof ModuleCommand)) {
                    ChatUtil.sendFormatted(String.format("&7»&r .%s&r", String.join(" &7/&r .", command.names)));
                }
            }
        }
    }
}
