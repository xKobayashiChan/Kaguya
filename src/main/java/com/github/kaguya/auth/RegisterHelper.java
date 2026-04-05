package com.github.kaguya.auth;

/**
 * ユーザー登録用のヘルパーツール。
 * コマンドラインから実行して、Firebase に登録するためのJSON を生成する。
 *
 * 使い方:
 *   java RegisterHelper <userId> <password>
 *
 * 出力されたJSONをFirebase Console の Realtime Database に手動で追加する:
 *   /users/{userId} にペースト
 */
public class RegisterHelper {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java RegisterHelper <userId> <password>");
            System.out.println();
            System.out.println("Example: java RegisterHelper player1 mySecretPass123");
            return;
        }

        String userId = args[0];
        String password = args[1];
        String passwordHash = HWIDUtil.hashPassword(password);

        System.out.println("=== Firebase Registration Data ===");
        System.out.println();
        System.out.println("Path: /users/" + userId);
        System.out.println();
        System.out.println("JSON:");
        System.out.println("{");
        System.out.println("  \"passwordHash\": \"" + passwordHash + "\",");
        System.out.println("  \"hwid\": \"\",");
        System.out.println("  \"username\": \"" + userId + "\"");
        System.out.println("}");
        System.out.println();
        System.out.println("※ hwidは空のままにしてください。初回ログイン時に自動登録されます。");
        System.out.println("※ HWIDをリセットしたい場合は、Firebase Consoleでhwidを空文字に戻してください。");
    }
}

