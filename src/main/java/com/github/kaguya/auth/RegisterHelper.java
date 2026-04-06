package com.github.kaguya.auth;

/**
 * ユーザー登録用ヘルパーツール。
 * コマンドラインから実行して、Firebase に登録するための JSON を生成する。
 *
 * 使い方:
 *   java RegisterHelper <userId> <password>
 *
 * 出力された JSON を Firebase Console の Realtime Database に手動で追加する:
 *   /users/{userId} にペースト
 *
 * また、/mcnames/{mcName} = "{userId}" のエントリも初回ログイン時に自動作成される。
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

        if (!userId.matches("[a-zA-Z0-9_\\-]{1,64}")) {
            System.out.println("Error: userId must match [a-zA-Z0-9_-]{1,64}");
            return;
        }

        // ソルト付きハッシュ: SHA-256("{userId}:{password}")
        String passwordHash = HWIDUtil.hashPassword(userId, password);

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
        System.out.println("※ hwid は空のままにしてください。初回ログイン時に自動登録されます。");
        System.out.println("※ HWID をリセットしたい場合は Firebase Console で hwid を空文字に戻してください。");
        System.out.println("※ /mcnames エントリは初回ログイン時に自動作成されます。");
    }
}
