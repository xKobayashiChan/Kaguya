package com.github.kaguya.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Firebase Realtime Database を使った認証マネージャー。
 *
 * ■ Firebase データ構造:
 *   /users/{userId}
 *     passwordHash: SHA-256(userId + ":" + password)
 *     hwid: ""  ← 初回ログイン時に自動登録
 *     username: "表示名"
 *
 *   /mcnames/{mcName}: userId   ← MC名→ユーザーIDのインデックス（パスワード等を含まない）
 *
 * ■ 認証情報の設定:
 *   src/main/resources/firebase.properties に記載（.gitignore済み）
 *   firebase.url=https://xxx-default-rtdb.firebaseio.com
 *   firebase.secret=YOUR_SECRET
 *
 * ■ Security Rules（推奨）:
 *   {
 *     "rules": {
 *       ".read": false,
 *       ".write": false
 *     }
 *   }
 */
public class AuthManager {

    private static final String FIREBASE_URL;
    private static final String FIREBASE_SECRET;

    static {
        Properties props = new Properties();
        try (InputStream is = AuthManager.class.getResourceAsStream("/firebase.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {}
        FIREBASE_URL = props.getProperty("firebase.url", "");
        FIREBASE_SECRET = props.getProperty("firebase.secret", "");
    }

    private static boolean authenticated = false;
    private static String currentUserId = null;
    private static String statusMessage = "";
    private static volatile String currentNick = null;

    // MC名 → KaguyaユーザーID のマッピングキャッシュ（/mcnames から取得）
    private static final ConcurrentHashMap<String, String> mcNameToUserId = new ConcurrentHashMap<>();
    private static volatile long lastFetchTime = 0;
    private static final long FETCH_INTERVAL_MS = 60_000;
    private static final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    // 前回Firebaseに保存したMC名（差分更新用）
    private static volatile String lastSavedMcName = "";

    // ==================== 公開API ====================

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static String getStatusMessage() {
        return statusMessage;
    }

    /**
     * HWIDで自動ログインを試みる。別スレッドから呼ぶこと。
     * /hwids/{hwid} インデックスからuserIdを取得し、userレコードのHWIDと照合する。
     *
     * @return 認証成功ならtrue
     */
    public static boolean autoLogin() {
        try {
            statusMessage = "Connecting...";
            String hwid = HWIDUtil.getHWID();

            // HWIDインデックスからuserIdを取得
            String response = httpGet(buildUrl("/hwids/" + hwid + ".json"));
            if (response == null || response.equals("null") || response.trim().isEmpty()) {
                statusMessage = "";
                return false;
            }

            // JSON文字列値の引用符を除去
            String userId = response.trim().replaceAll("^\"|\"$", "");
            try {
                sanitizeUserId(userId);
            } catch (IllegalArgumentException e) {
                statusMessage = "";
                return false;
            }

            // userレコードのHWIDと照合（改ざん防止）
            String userResponse = httpGet(buildUrl("/users/" + userId + ".json"));
            if (userResponse == null || userResponse.equals("null")) {
                statusMessage = "";
                return false;
            }

            JsonElement element = new JsonParser().parse(userResponse);
            if (!element.isJsonObject()) {
                statusMessage = "";
                return false;
            }

            JsonObject userData = element.getAsJsonObject();
            String storedHWID = userData.has("hwid") ? userData.get("hwid").getAsString() : "";
            if (!hwid.equals(storedHWID)) {
                statusMessage = "";
                return false;
            }

            authenticated = true;
            currentUserId = userId;
            statusMessage = "\u00a7aAuto login successful!";

            new Thread(() -> {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.getSession() != null) {
                        updateMcName(userId, mc.getSession().getUsername());
                    }
                } catch (Exception ignored) {}
                fetchMcNames();
            }, "KaguyaAuth-Post").start();

            return true;

        } catch (Exception e) {
            statusMessage = "";
            return false;
        }
    }

    /**
     * ログインを試行する。別スレッドから呼ぶこと（UIブロック防止）。
     */
    public static boolean login(String userId, String password) {
        try {
            sanitizeUserId(userId);
        } catch (IllegalArgumentException e) {
            statusMessage = "\u00a7cInvalid User ID format.";
            return false;
        }

        try {
            statusMessage = "Connecting...";

            String url = buildUrl("/users/" + userId + ".json");
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

            // パスワード検証（userIdをソルトとして使用）
            String storedHash = userData.has("passwordHash") ? userData.get("passwordHash").getAsString() : "";
            String inputHash = HWIDUtil.hashPassword(userId, password);

            if (!storedHash.equals(inputHash)) {
                // 旧形式（ソルトなし）で再検証し、一致すれば新形式に自動移行
                String legacyHash = HWIDUtil.hashPasswordLegacy(password);
                if (!storedHash.equals(legacyHash)) {
                    statusMessage = "\u00a7cIncorrect password.";
                    return false;
                }
                // 旧ハッシュが一致 → 新ハッシュに更新
                JsonObject hashUpdate = new JsonObject();
                hashUpdate.addProperty("passwordHash", inputHash);
                httpPatch(buildUrl("/users/" + userId + ".json"), hashUpdate.toString());
            }

            // HWID検証
            String hwid = HWIDUtil.getHWID();
            String storedHWID = userData.has("hwid") ? userData.get("hwid").getAsString() : "";

            if (storedHWID.isEmpty()) {
                // 初回ログイン: HWIDを登録
                JsonObject update = new JsonObject();
                update.addProperty("hwid", hwid);
                httpPatch(buildUrl("/users/" + userId + ".json"), update.toString());
                // HWIDインデックスも書き込む（次回から自動ログイン可能）
                httpPut(buildUrl("/hwids/" + hwid + ".json"), "\"" + userId + "\"");
                statusMessage = "\u00a7aHWID registered. Login successful!";
            } else if (!storedHWID.equals(hwid)) {
                statusMessage = "\u00a7cHWID mismatch. This account is bound to another PC.";
                return false;
            } else {
                statusMessage = "\u00a7aLogin successful!";
                // HWIDインデックスが未作成の場合（既存ユーザー対応）は書き込む
                new Thread(() -> {
                    try {
                        String idx = httpGet(buildUrl("/hwids/" + hwid + ".json"));
                        if (idx == null || idx.equals("null")) {
                            httpPut(buildUrl("/hwids/" + hwid + ".json"), "\"" + userId + "\"");
                        }
                    } catch (Exception ignored) {}
                }, "KaguyaHwidIndex").start();
            }

            authenticated = true;
            currentUserId = userId;

            // MC名更新 & mcnamesキャッシュ取得（別スレッド）
            new Thread(() -> {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.getSession() != null) {
                        updateMcName(userId, mc.getSession().getUsername());
                    }
                } catch (Exception ignored) {}
                fetchMcNames();
            }, "KaguyaAuth-Post").start();

            return true;

        } catch (Exception e) {
            statusMessage = "\u00a7cConnection error: " + e.getMessage();
            return false;
        }
    }

    public static String getCurrentNick() {
        return currentNick;
    }

    /**
     * 自分のnick名をFirebaseに登録する。他のKaguyaユーザーのタブ画面に自分のIDが表示される。
     * 別スレッドから呼ぶこと。
     */
    public static void setNick(String nickName) {
        if (!authenticated || currentUserId == null) return;
        new Thread(() -> {
            try {
                clearNickInternal();
                httpPut(buildUrl("/nicks/" + nickName + ".json"), "\"" + currentUserId + "\"");
                JsonObject update = new JsonObject();
                update.addProperty("nick", nickName);
                httpPatch(buildUrl("/users/" + currentUserId + ".json"), update.toString());
                currentNick = nickName;
                mcNameToUserId.put(nickName, currentUserId);
            } catch (Exception ignored) {}
        }, "KaguyaSetNick").start();
    }

    /**
     * 登録済みのnick名をFirebaseから削除する。
     * 別スレッドから呼ぶこと。
     */
    public static void clearNick() {
        if (!authenticated || currentUserId == null) return;
        new Thread(() -> {
            try {
                clearNickInternal();
            } catch (Exception ignored) {}
        }, "KaguyaClearNick").start();
    }

    private static void clearNickInternal() throws IOException {
        // メモリキャッシュのnickを削除
        if (currentNick != null && !currentNick.isEmpty()) {
            httpDelete(buildUrl("/nicks/" + currentNick + ".json"));
            mcNameToUserId.remove(currentNick);
            currentNick = null;
        }
        // Firebaseのユーザーレコードに残っているnickも削除
        String userResponse = httpGet(buildUrl("/users/" + currentUserId + ".json"));
        if (userResponse == null || userResponse.equals("null")) return;
        JsonElement el = new JsonParser().parse(userResponse);
        if (!el.isJsonObject()) return;
        JsonObject userData = el.getAsJsonObject();
        if (userData.has("nick")) {
            String storedNick = userData.get("nick").getAsString();
            if (!storedNick.isEmpty()) {
                httpDelete(buildUrl("/nicks/" + storedNick + ".json"));
                mcNameToUserId.remove(storedNick);
            }
            JsonObject update = new JsonObject();
            update.addProperty("nick", "");
            httpPatch(buildUrl("/users/" + currentUserId + ".json"), update.toString());
        }
    }

    public static void logout() {
        authenticated = false;
        currentUserId = null;
        statusMessage = "";
        currentNick = null;
        mcNameToUserId.clear();
        lastSavedMcName = "";
        lastFetchTime = 0;
    }

    /**
     * MC名からKaguyaユーザーIDを取得する（タブ表示用）。
     * キャッシュが古ければバックグラウンドで更新する。
     */
    public static String getUserIdByMcName(String mcName) {
        if (authenticated && System.currentTimeMillis() - lastFetchTime > FETCH_INTERVAL_MS) {
            new Thread(AuthManager::fetchMcNames, "KaguyaFetch").start();
        }
        return mcNameToUserId.get(mcName);
    }

    // ==================== 内部処理 ====================

    /**
     * /mcnames インデックスを取得してキャッシュを更新する。
     * MC名のみ含むインデックスのため、パスワードハッシュ等のデータは取得しない。
     */
    private static void fetchMcNames() {
        if (!fetchInProgress.compareAndSet(false, true)) return;
        try {
            // MC名が変わっていたら先に更新
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

            ConcurrentHashMap<String, String> newMap = new ConcurrentHashMap<>();

            // /mcnames（本名インデックス）を取得
            String mcNamesResponse = httpGet(buildUrl("/mcnames.json"));
            if (mcNamesResponse != null && !mcNamesResponse.equals("null")) {
                JsonElement el = new JsonParser().parse(mcNamesResponse);
                if (el.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            newMap.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }

            // /nicks（nick共有インデックス）を取得して統合
            String nicksResponse = httpGet(buildUrl("/nicks.json"));
            if (nicksResponse != null && !nicksResponse.equals("null")) {
                JsonElement el = new JsonParser().parse(nicksResponse);
                if (el.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            newMap.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }

            if (newMap.isEmpty()) return;
            mcNameToUserId.clear();
            mcNameToUserId.putAll(newMap);
            lastFetchTime = System.currentTimeMillis();

        } catch (Exception ignored) {
        } finally {
            fetchInProgress.set(false);
        }
    }

    /**
     * MC名を /users/{userId}/mcName と /mcnames/{mcName} の両方に保存する。
     * 旧MC名のインデックスエントリは削除する。
     */
    private static void updateMcName(String userId, String mcName) {
        try {
            // 旧MC名のインデックスを削除
            if (!lastSavedMcName.isEmpty() && !lastSavedMcName.equals(mcName)) {
                httpDelete(buildUrl("/mcnames/" + lastSavedMcName + ".json"));
            }

            // ユーザーデータのmcNameフィールドを更新
            JsonObject userUpdate = new JsonObject();
            userUpdate.addProperty("mcName", mcName);
            httpPatch(buildUrl("/users/" + userId + ".json"), userUpdate.toString());

            // mcnamesインデックスを更新（値はuserId文字列）
            httpPut(buildUrl("/mcnames/" + mcName + ".json"), "\"" + userId + "\"");

            lastSavedMcName = mcName;
        } catch (Exception ignored) {}
    }

    /**
     * userId に使用できない文字が含まれていないか検証する（パストラバーサル防止）。
     */
    private static void sanitizeUserId(String userId) {
        if (userId == null || !userId.matches("[a-zA-Z0-9_\\-]{1,64}")) {
            throw new IllegalArgumentException("Invalid userId: " + userId);
        }
    }

    private static String buildUrl(String path) {
        return FIREBASE_URL + path + "?auth=" + FIREBASE_SECRET;
    }

    // ==================== HTTP ユーティリティ ====================

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlString, "GET");
            int code = conn.getResponseCode();
            if (code != 200) return null;
            return readStream(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void httpPatch(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            // Java の HttpURLConnection は PATCH 非対応のため X-HTTP-Method-Override を使用
            conn = openConnection(urlString, "POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void httpPut(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlString, "PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void httpDelete(String urlString) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlString, "DELETE");
            conn.getResponseCode();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String urlString, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private static String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}