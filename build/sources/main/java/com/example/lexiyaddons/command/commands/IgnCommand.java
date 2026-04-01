package com.example.lexiyaddons.command.commands;

import com.example.lexiyaddons.Myau;
import com.example.lexiyaddons.command.Command;
import com.example.lexiyaddons.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;

public class IgnCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public IgnCommand() {
        super(new ArrayList<String>(Arrays.asList("username", "name", "ign")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Session session = mc.getSession();
        if (session != null) {
            String username = session.getUsername();
            if (!StringUtils.isNullOrEmpty(username)) {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null);
                    ChatUtil.sendFormatted(String.format("%sYour username has been copied to the clipboard (&o%s&r)&r", Myau.clientName, username));
                } catch (Exception e) {
                    ChatUtil.sendFormatted(String.format("%sFailed to copy&r", Myau.clientName));
                }
            }
        }
    }
}
