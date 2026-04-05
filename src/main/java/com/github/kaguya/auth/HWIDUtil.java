package com.github.kaguya.auth;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * HWID (Hardware ID) を生成するユーティリティ。
 * MACアドレス + OS情報 + ユーザー名を組み合わせてSHA-256ハッシュを生成する。
 */
public class HWIDUtil {

    private static String cachedHWID = null;

    /**
     * HWIDを取得する（キャッシュあり）
     */
    public static String getHWID() {
        if (cachedHWID == null) {
            cachedHWID = generateHWID();
        }
        return cachedHWID;
    }

    private static String generateHWID() {
        try {
            StringBuilder sb = new StringBuilder();

            // MACアドレス
            sb.append(getMACAddress());

            // OS情報
            sb.append(System.getProperty("os.name", "unknown"));
            sb.append(System.getProperty("os.arch", "unknown"));

            // ユーザー名
            sb.append(System.getProperty("user.name", "unknown"));

            // コンピューター名 (環境変数)
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName == null) {
                computerName = System.getenv("HOSTNAME");
            }
            if (computerName != null) {
                sb.append(computerName);
            }

            // プロセッサ数（変わりにくい特徴）
            sb.append(Runtime.getRuntime().availableProcessors());

            return sha256(sb.toString());
        } catch (Exception e) {
            // フォールバック: 取得できる情報だけでハッシュ生成
            String fallback = System.getProperty("os.name", "") +
                    System.getProperty("user.name", "") +
                    System.getProperty("os.arch", "");
            return sha256(fallback);
        }
    }

    private static String getMACAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0 && !ni.isLoopback() && !ni.isVirtual()) {
                    StringBuilder macStr = new StringBuilder();
                    for (byte b : mac) {
                        macStr.append(String.format("%02X", b));
                    }
                    return macStr.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "NO_MAC";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * パスワードをSHA-256でハッシュ化する
     */
    public static String hashPassword(String password) {
        return sha256(password);
    }
}

