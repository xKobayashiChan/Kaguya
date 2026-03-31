package com.example.lexiyaddons.command;

import com.example.lexiyaddons.LexiyAddons;
import com.example.lexiyaddons.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * "@" で始まるチャットコマンドを処理する
 *
 * 対応コマンド:
 *   @toggle <モジュール名>  — モジュールのON/OFF
 *   @help                   — コマンド一覧とモジュール一覧を表示
 */
public class CommandHandler {

    private static final String PREFIX = "@";

    /**
     * チャットメッセージを処理する
     * @return true ならコマンドとして処理済み（サーバーに送信しない）
     */
    public static boolean handleCommand(String message) {
        if (!message.startsWith(PREFIX)) return false;

        // Modが初期化されていなければコマンドを無視
        if (LexiyAddons.getInstance() == null || LexiyAddons.getInstance().getModuleManager() == null) {
            return false;
        }

        String[] args = message.substring(PREFIX.length()).trim().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) return false;

        String command = args[0].toLowerCase();

        switch (command) {
            case "toggle":
                handleToggle(args);
                return true;
            case "help":
                handleHelp();
                return true;
            default:
                sendMessage("\u00A7c不明なコマンド: " + command + " \u00A77(@help で一覧表示)");
                return true;
        }
    }

    // ── @toggle <module> ──────────────────────────────────────

    private static void handleToggle(String[] args) {
        if (args.length < 2) {
            sendMessage("\u00A7c使い方: @toggle <モジュール名>");
            return;
        }

        String moduleName = args[1];
        Module module = LexiyAddons.getInstance().getModuleManager().getModuleByName(moduleName);

        if (module != null) {
            module.toggle();
            String status = module.isEnabled() ? "\u00A7aON" : "\u00A7cOFF";
            sendMessage("\u00A77" + module.getName() + " \u00A7f\u2192 " + status);
        } else {
            sendMessage("\u00A7cモジュール '" + moduleName + "' が見つかりません");
        }
    }

    // ── @help ─────────────────────────────────────────────────

    private static void handleHelp() {
        sendMessage("\u00A7e=== LexiyAddons コマンド ===");
        sendMessage("\u00A7f@toggle <名前> \u00A77- モジュールのON/OFF");
        sendMessage("\u00A7f@help          \u00A77- このヘルプを表示");
        sendMessage("\u00A7e--- モジュール一覧 ---");

        for (Module module : LexiyAddons.getInstance().getModuleManager().getModules()) {
            String status = module.isEnabled() ? "\u00A7aON" : "\u00A7cOFF";
            sendMessage("\u00A7f  " + module.getName()
                    + " \u00A78[" + module.getCategory().getDisplayName() + "] "
                    + status);
        }
    }

    // ── ユーティリティ ────────────────────────────────────────

    private static void sendMessage(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00A7b[LexiyAddons] " + text));
        }
    }
}

