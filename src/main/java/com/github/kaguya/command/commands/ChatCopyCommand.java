package com.github.kaguya.command.commands;

import com.github.kaguya.Myau;
import com.github.kaguya.command.Command;
import com.github.kaguya.util.ChatUtil;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatCopyCommand extends Command {
    private static final Map<Integer, String> messageStore = new LinkedHashMap<Integer, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
            return size() > 200;
        }
    };
    private static int nextId = 0;

    public ChatCopyCommand() {
        super(new ArrayList<>(Arrays.asList("cc")));
    }

    public static int store(String text) {
        int id = nextId++;
        messageStore.put(id, text);
        return id;
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) return;
        try {
            int id = Integer.parseInt(args.get(1));
            String text = messageStore.get(id);
            if (text != null) {
                StringSelection sel = new StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                ChatUtil.sendFormatted(String.format("%s&aCopied to clipboard!&r", Myau.clientName));
            }
        } catch (NumberFormatException ignored) {
        }
    }
}

