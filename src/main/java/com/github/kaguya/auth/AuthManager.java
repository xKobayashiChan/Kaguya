package com.github.kaguya.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firebase Realtime Database を使った認証マネージャー。
 *
 * ■ Firebaseのセットアップ手順:
 * 1. https://console.firebase.google.com/ でプロジェクトを作成
 * 2. Realtime Database を作成（リージョンは任意）
 * 3. Database URL を下の FIREBASE_URL に設定
 * 4. Database Secret を FIREBASE_SECRET に設定
 *    (プロジェクト設定 → サービスアカウント → Database Secrets)
 *
 * ■ ユーザー登録は Firebase Console から手動で行う:
 *    /users/{userId} に以下のJSONを追加:
 *    {
 *      "passwordHash": "(SHA-256ハッシュ済みパスワード)",
 *      "hwid": "",
 *      "username": "表示名"
 *    }
 *    ※ HWIDUtil.hashPassword("平文パスワード") でハッシュ値を取得可能
 *
 * ■ Security Rules（推奨）:
 *    {
 *      "rules": {
 *        ".read": false,
 *        ".write": false
 *      }
 *    }
 *    → REST APIではDatabase Secretで認証するため、ルールをロックしてOK
 */
public class AuthManager {

    // ========== ★ ここを自分のFirebaseプロジェクトに書き換えてください ★ ==========
    private static final String FIREBASE_URL = "https://kaguya-auth-default-rtdb.firebaseio.com";
    private static final String FIREBASE_SECRET = "x4oa1n8gabAWUBj3VUCw8C3xnfxSFquxux7WDuyg";
    // ===========================================================================

    private static boolean authenticated = false;
    private static String currentUserId = null;
    private static String statusMessage = "";

    // MC名 → KaguyaユーザーID のマッピング（全ユーザー分）
    private static final ConcurrentHashMap<String, String> mcNameToUserId = new ConcurrentHashMap<>();
    private static long lastFetchTime = 0;
    private static final long FETCH_INTERVAL = 60_000; // 60秒ごとに更新
    private static String lastSavedMcName = ""; // 前回Firebaseに保存したMC名

    /**
     * 認証済みかどうか
     */
    public static boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * 現在のユーザーID
     */
    public static String getCurrentUserId() {
        return currentUserId;
    }

    /**
     * 最後のステータスメッセージ（エラー内容等）
     */
    public static String getStatusMessage() {
        return statusMessage;
    }

    /**
     * ログインを試行する。
     * 別スレッドから呼ぶことを推奨（UIをブロックしないため）。
     *
     * @param userId   ユーザーID
     * @param password 平文パスワード
     * @return 認証成功ならtrue
     */
    public static boolean login(String userId, String password) {
        try {
            statusMessage = "Connecting...";

            // Firebase から該当ユーザーのデータを取得
            String url = String.format("%s/users/%s.json?auth=%s", FIREBASE_URL, userId, FIREBASE_SECRET);
            String response = httpGet(url);

            if (response == null || response.equals("null") || response.trim().isEmpty()) {
                statusMessage = "\u00a7cUser not found.";
                return false;
            }

            JsonElement element = new JsonParser().parse(response);
            if (!element.isJsonObject()) {
                statusMessage = "\u00a7cInvalid user data.";
                return false;
            }

            JsonObject userData = element.getAsJsonObject();

            // パスワード検証
            String storedHash = userData.has("passwordHash") ? userData.get("passwordHash").getAsString() : "";
            String inputHash = HWIDUtil.hashPassword(password);

            if (!storedHash.equals(inputHash)) {
                statusMessage = "\u00a7cIncorrect password.";
                return false;
            }

            // HWID検証
            String hwid = HWIDUtil.getHWID();
            String storedHWID = userData.has("hwid") ? userData.get("hwid").getAsString() : "";

            if (storedHWID.isEmpty()) {
                // 初回ログイン: HWIDを登録
                JsonObject update = new JsonObject();
                update.addProperty("hwid", hwid);
                String updateUrl = String.format("%s/users/%s.json?auth=%s", FIREBASE_URL, userId, FIREBASE_SECRET);
                httpPatch(updateUrl, update.toString());
                statusMessage = "\u00a7aHWID registered. Login successful!";
            } else if (!storedHWID.equals(hwid)) {
                // HWID不一致
                statusMessage = "\u00a7cHWID mismatch. This account is bound to another PC.";
                return false;
            } else {
                statusMessage = "\u00a7aLogin successful!";
            }

            authenticated = true;
            currentUserId = userId;

            // MC名をFirebaseに保存（別スレッドで）
            new Thread(() -> {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.getSession() != null) {
                        String mcName = mc.getSession().getUsername();
                        updateMcName(userId, mcName);
                    }
                } catch (Exception ignored) {}
                // ユーザー一覧を取得
                fetchAllUsers();
            }, "KaguyaMcName").start();

            return true;

        } catch (Exception e) {
            statusMessage = "\u00a7cConnection error: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ログアウト
     */
    public static void logout() {
        authenticated = false;
        currentUserId = null;
        statusMessage = "";
        mcNameToUserId.clear();
    }

    /**
     * MC名からKaguyaユーザーIDを取得する。
     * 該当なしならnullを返す。
     */
    public static String getUserIdByMcName(String mcName) {
        // キャッシュが古ければバックグラウンドで更新
        if (authenticated && System.currentTimeMillis() - lastFetchTime > FETCH_INTERVAL) {
            new Thread(AuthManager::fetchAllUsers, "KaguyaFetch").start();
        }
        return mcNameToUserId.get(mcName);
    }

    /**
     * Firebaseから全ユーザーを取得して mcName → userId マッピングを更新する。
     * MC名が変わっていたらFirebaseも自動更新する。
     */
    private static void fetchAllUsers() {
        try {
            // MC名が変わっていたらFirebaseを更新（アカウントスイッチャー対応）
            if (authenticated && currentUserId != null) {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.getSession() != null) {
                        String currentMcName = mc.getSession().getUsername();
                        if (!currentMcName.equals(lastSavedMcName)) {
                            updateMcName(currentUserId, currentMcName);
                        }
                    }
                } catch (Exception ignored) {}
            }

            String url = String.format("%s/users.json?auth=%s", FIREBASE_URL, FIREBASE_SECRET);
            String response = httpGet(url);
            if (response == null || response.equals("null")) return;

            JsonElement element = new JsonParser().parse(response);
            if (!element.isJsonObject()) return;

            JsonObject users = element.getAsJsonObject();
            ConcurrentHashMap<String, String> newMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, JsonElement> entry : users.entrySet()) {
                String userId = entry.getKey();
                JsonElement val = entry.getValue();
                if (val.isJsonObject()) {
                    JsonObject user = val.getAsJsonObject();
                    if (user.has("mcName") && !user.get("mcName").getAsString().isEmpty()) {
                        newMap.put(user.get("mcName").getAsString(), userId);
                    }
                }
            }
            mcNameToUserId.clear();
            mcNameToUserId.putAll(newMap);
            lastFetchTime = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    /**
     * MC名をFirebaseに保存する
     */
    private static void updateMcName(String userId, String mcName) {
        try {
            JsonObject mcUpdate = new JsonObject();
            mcUpdate.addProperty("mcName", mcName);
            String mcUrl = String.format("%s/users/%s.json?auth=%s", FIREBASE_URL, userId, FIREBASE_SECRET);
            httpPatch(mcUrl, mcUpdate.toString());
            lastSavedMcName = mcName;
        } catch (Exception ignored) {}
    }

    // ==================== HTTP ユーティリティ ====================

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            return readStream(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void httpPatch(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            // Java の HttpURLConnection は PATCH をサポートしないので、
            // Firebase の X-HTTP-Method-Override を使う
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            conn.getResponseCode(); // レスポンスを読み捨て
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}

