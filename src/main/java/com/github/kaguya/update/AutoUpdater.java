package com.github.kaguya.update;

import com.github.kaguya.BuildConstants;
import com.github.kaguya.Kaguya;
import com.github.kaguya.util.ChatUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class AutoUpdater {

    private static final String GITHUB_API = "https://api.github.com";
    private static final String GITHUB_TOKEN;
    private static final String GITHUB_REPO;

    static {
        Properties props = new Properties();
        try (InputStream is = AutoUpdater.class.getResourceAsStream("/firebase.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        GITHUB_TOKEN = props.getProperty("github.token", "");
        GITHUB_REPO  = props.getProperty("github.repo", "");
    }

    /**
     * 起動時に呼ぶ。現在実行中ではない古い KaguyaClient*.jar を削除する。
     */
    public static void startup() {
        File modsDir = new File("mods");
        if (!modsDir.isDirectory()) return;

        // 現在実行中のJARパスを取得
        File currentJar = null;
        try {
            currentJar = new File(
                AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
        } catch (Exception ignored) {}

        final File running = currentJar;

        // KaguyaClient*.jar で現在実行中でないものをすべて削除
        File[] oldFiles = modsDir.listFiles(
            (dir, name) -> name.startsWith("KaguyaClient") && name.endsWith(".jar")
        );
        if (oldFiles == null) return;
        for (File f : oldFiles) {
            if (running != null && f.getAbsolutePath().equals(running.getAbsolutePath())) continue;
            f.delete();
        }

        // 旧形式の .old ファイルも念のため削除
        File[] dotOld = modsDir.listFiles(
            (dir, name) -> name.startsWith("KaguyaClient") && name.endsWith(".old")
        );
        if (dotOld != null) {
            for (File f : dotOld) f.delete();
        }
    }

    /**
     * ログイン後に呼ぶ。GitHub Releases API で最新バージョンを確認し、
     * 新バージョンがあればダウンロードして再起動を促す。
     */
    public static void checkForUpdate() {
        if (GITHUB_TOKEN.isEmpty() || GITHUB_REPO.isEmpty()) return;
        new Thread(() -> {
            try {
                String response = githubGet("/repos/" + GITHUB_REPO + "/releases/latest");
                if (response == null) return;

                JsonObject release = new JsonParser().parse(response).getAsJsonObject();
                String latestVersion = release.get("tag_name").getAsString();

                if (latestVersion.equals(BuildConstants.VERSION)) return; // 最新

                // JARアセットを探す
                JsonArray assets = release.getAsJsonArray("assets");
                String assetApiUrl = null;
                String assetFileName = null;
                for (JsonElement el : assets) {
                    JsonObject asset = el.getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".jar")) {
                        assetApiUrl  = asset.get("url").getAsString();
                        assetFileName = name;
                        break;
                    }
                }
                if (assetApiUrl == null) return;

                downloadUpdate(latestVersion, assetApiUrl, assetFileName);

            } catch (Exception ignored) {}
        }, "KaguyaUpdater").start();
    }

    // ==================== 内部処理 ====================

    private static void downloadUpdate(String newVersion, String assetApiUrl, String fileName) {
        try {
            File modsDir = new File("mods");
            File newJar = new File(modsDir, fileName);

            // GitHub release asset のダウンロード（private repo はリダイレクトが入る）
            HttpURLConnection conn = openGitHubConnection(assetApiUrl, "application/octet-stream");
            int status = conn.getResponseCode();

            // リダイレクト追従（Location ヘッダーへ再接続）
            if (status == 302 || status == 301) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) new URL(location).openConnection();
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(120_000);
                conn.setRequestProperty("Accept", "application/octet-stream");
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, newJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                conn.disconnect();
            }

            // プレイヤーがインゲームになるまで待ってから通知
            final String msg = Kaguya.clientName
                    + "&aNew Kaguya available! &b" + newVersion + "&a was downloaded."
                    + " Please restart Minecraft.&r";
            new Thread(() -> {
                Minecraft mc2 = Minecraft.getMinecraft();
                for (int i = 0; i < 120; i++) { // 最大60秒待機
                    if (mc2.thePlayer != null) {
                        mc2.addScheduledTask(() -> ChatUtil.sendFormatted(msg));
                        return;
                    }
                    try { Thread.sleep(500); } catch (InterruptedException ignored) { return; }
                }
            }, "KaguyaUpdater-Notify").start();

        } catch (Exception e) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                ChatUtil.sendFormatted(
                    Kaguya.clientName + "&cアップデートのダウンロードに失敗しました: " + e.getMessage() + "&r"
                )
            );
        }
    }

    private static HttpURLConnection openGitHubConnection(String url, String accept) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(120_000);
        return conn;
    }

    private static String githubGet(String path) throws IOException {
        HttpURLConnection conn = openGitHubConnection(GITHUB_API + path, "application/vnd.github+json");
        try {
            if (conn.getResponseCode() != 200) return null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }
}
