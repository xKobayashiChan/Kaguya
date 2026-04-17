package com.github.kaguya.command.commands;

import com.github.kaguya.Kaguya;
import com.github.kaguya.command.Command;
import com.github.kaguya.module.modules.ClientHUD;
import com.github.kaguya.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class CnameCommand extends Command {
    public CnameCommand() {
        super(new ArrayList<>(Arrays.asList("cname", "cn")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ClientHUD clientHUD = (ClientHUD) Kaguya.moduleManager.modules.get(ClientHUD.class);
        if (args.size() < 2) {
            ChatUtil.sendFormatted(Kaguya.clientName + "Current name: &b" + clientHUD.clientDisplayName + "&r");
            ChatUtil.sendFormatted(Kaguya.clientName + "Usage: .cname <name> / .cname reset&r");
            return;
        }
        String arg = args.get(1);
        if (arg.equalsIgnoreCase("reset")) {
            clientHUD.clientDisplayName = "Kaguya";
            ChatUtil.sendFormatted(Kaguya.clientName + "Client name reset to &bKaguya&r.");
        } else {
            clientHUD.clientDisplayName = arg;
            ChatUtil.sendFormatted(Kaguya.clientName + "Client name set to &b" + arg + "&r.");
        }
    }
}
