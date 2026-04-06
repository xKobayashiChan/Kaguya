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
     * 起動時に呼ぶ。前回の更新で残った *.old ファイルを削除する。
     */
    public static void startup() {
        File modsDir = new File("mods");
        if (!modsDir.isDirectory()) return;
        File[] oldFiles = modsDir.listFiles(
            (dir, name) -> name.startsWith("KaguyaClient") && name.endsWith(".old")
        );
        if (oldFiles == null) return;
        for (File f : oldFiles) f.delete();
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

            // 現在のJARを .old にリネーム（次回起動時に削除）
            renameCurrentJar();

            // メインスレッドで通知
            Minecraft.getMinecraft().addScheduledTask(() ->
                ChatUtil.sendFormatted(
                    Kaguya.clientName
                    + "&a新バージョン &b" + newVersion + "&a をダウンロードしました！"
                    + " Minecraftを再起動してください。&r"
                )
            );

        } catch (Exception e) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                ChatUtil.sendFormatted(
                    Kaguya.clientName + "&cアップデートのダウンロードに失敗しました: " + e.getMessage() + "&r"
                )
            );
        }
    }

    private static void renameCurrentJar() {
        try {
            File currentJar = new File(
                AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            if (currentJar.exists() && currentJar.getName().endsWith(".jar")) {
                File renamed = new File(currentJar.getParentFile(), currentJar.getName() + ".old");
                currentJar.renameTo(renamed);
            }
        } catch (Exception ignored) {
            // Windows でファイルがロックされている場合は失敗するが無視
            // 起動時クリーンアップで対応
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
