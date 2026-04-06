package com.github.kaguya.command.commands;

import com.github.kaguya.Kaguya;
import com.github.kaguya.auth.AuthManager;
import com.github.kaguya.command.Command;
import com.github.kaguya.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class NickCommand extends Command {

    public NickCommand() {
        super(new ArrayList<>(Arrays.asList("nick", "n")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!AuthManager.isAuthenticated()) {
            ChatUtil.sendFormatted(Kaguya.clientName + "&cNot logged in.&r");
            return;
        }

        if (args.size() < 2) {
            // 現在のnickを表示
            String current = AuthManager.getCurrentNick();
            if (current == null || current.isEmpty()) {
                ChatUtil.sendFormatted(Kaguya.clientName + "No nick registered.&r");
            } else {
                ChatUtil.sendFormatted(Kaguya.clientName + "Current nick: &b" + current + "&r");
            }
            ChatUtil.sendFormatted(Kaguya.clientName + "Usage: .nick set <&oname&r> / .nick clear&r");
            return;
        }

        String sub = args.get(1).toLowerCase();

        switch (sub) {
            case "set":
            case "s":
                if (args.size() < 3) {
                    ChatUtil.sendFormatted(Kaguya.clientName + "Usage: .nick set <&oname&r>&r");
                    return;
                }
                String nickName = args.get(2);
                if (!nickName.matches("[a-zA-Z0-9_]{1,16}")) {
                    ChatUtil.sendFormatted(Kaguya.clientName + "&cInvalid nick name. Use only letters, numbers, and underscores (max 16).&r");
                    return;
                }
                ChatUtil.sendFormatted(Kaguya.clientName + "Registering nick &b" + nickName + "&r...");
                AuthManager.setNick(nickName);
                ChatUtil.sendFormatted(Kaguya.clientName + "Nick &b" + nickName + "&r registered. Other Kaguya users will see your ID.&r");
                break;

            case "clear":
            case "c":
                String current = AuthManager.getCurrentNick();
                if (current == null || current.isEmpty()) {
                    ChatUtil.sendFormatted(Kaguya.clientName + "No nick to clear.&r");
                    return;
                }
                ChatUtil.sendFormatted(Kaguya.clientName + "Clearing nick &b" + current + "&r...");
                AuthManager.clearNick();
                ChatUtil.sendFormatted(Kaguya.clientName + "Nick cleared.&r");
                break;

            default:
                ChatUtil.sendFormatted(Kaguya.clientName + "Usage: .nick set <&oname&r> / .nick clear&r");
                break;
        }
    }
}