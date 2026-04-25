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

    // マーカーファイル名
    private static final String MARKER_NAME = ".kaguya_update";

    /** 実行中JARの場所からmodsディレクトリを特定する（CWD依存を避けるため） */
    private static File resolveModsDir() {
        try {
            File jar = new File(
                AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // JARがmods/直下にある場合: jar.getParentFile() = mods/
            if (jar.isFile() && jar.getParentFile() != null) {
                return jar.getParentFile();
            }
        } catch (Exception ignored) {}
        // フォールバック: CWD相対
        return new File("mods");
    }

    /**
     * 起動時に呼ぶ。マーカーファイルに基づいて更新を確定し、不要ファイルを削除する。
     * マーカーがない場合は .old ファイルの掃除のみ行う。
     * running が特定できなくても安全（グローバルスキャン削除を行わない）。
     */
    public static void startup() {
        File modsDir = resolveModsDir();
        if (!modsDir.isDirectory()) return;

        // 1. .old ファイルを掃除（前回ロックで消せなかった旧JAR）
        File[] dotOld = modsDir.listFiles(
            (dir, name) -> name.startsWith("KaguyaClient") && name.endsWith(".old")
        );
        if (dotOld != null) {
            for (File f : dotOld) f.delete();
        }

        // 2. マーカーがなければ何もしない
        File markerFile = new File(modsDir, MARKER_NAME);
        if (!markerFile.exists()) return;

        try {
            String content = new String(Files.readAllBytes(markerFile.toPath()), StandardCharsets.UTF_8).trim();
            String[] lines = content.split("\n", 2);
            if (lines.length < 2) { markerFile.delete(); return; }

            File oldJar = new File(lines[0].trim());
            File tmpJar = new File(lines[1].trim());
            if (!tmpJar.exists()) { markerFile.delete(); return; }

            // .tmp → 正式ファイル名（.tmp を除いた名前）にリネーム
            String tmpName = tmpJar.getName();
            String jarName = tmpName.endsWith(".tmp") ? tmpName.substring(0, tmpName.length() - 4) : tmpName;
            File newJar = new File(modsDir, jarName);
            // 既存ファイルがあれば先に削除（Windowsではrenameto失敗する）
            if (newJar.exists()) newJar.delete();
            if (!tmpJar.renameTo(newJar)) {
                System.err.println("[Kaguya] rename failed: " + tmpJar + " -> " + newJar);
                return;
            }

            // 旧JARを削除。ロックされている場合は .old にリネームして次回起動で掃除
            if (oldJar.exists()) {
                if (!oldJar.delete()) {
                    File renamed = new File(oldJar.getParentFile(), oldJar.getName() + ".old");
                    oldJar.renameTo(renamed); // 失敗しても次の掃除で拾えるよう .old で残す
                }
            }
        } catch (Exception e) {
            System.err.println("[Kaguya] startup cleanup error: " + e.getMessage());
        } finally {
            markerFile.delete();
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
            // 現在実行中のJARパスを取得
            File currentJar = null;
            try {
                File f = new File(
                    AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI()
                );
                if (f.exists() && f.getName().endsWith(".jar")) currentJar = f;
            } catch (Exception ignored) {}

            // modsディレクトリをJAR位置から特定（CWD依存を避ける）
            File modsDir = (currentJar != null) ? currentJar.getParentFile() : resolveModsDir();

            // 新JARは .tmp として保存（Forge がロードしない拡張子）
            File tmpJar = new File(modsDir, fileName + ".tmp");

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
                Files.copy(in, tmpJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                conn.disconnect();
            }

            // マーカーファイルに旧JARパスと新.tmpパスを記録（次回起動時の置換フロー用）
            File markerFile = new File(modsDir, MARKER_NAME);
            String oldPath = (currentJar != null) ? currentJar.getAbsolutePath() : "";
            String marker = oldPath + "\n" + tmpJar.getAbsolutePath();
            Files.write(markerFile.toPath(), marker.getBytes(StandardCharsets.UTF_8));

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
