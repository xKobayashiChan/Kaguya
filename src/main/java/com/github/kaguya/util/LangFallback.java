package com.github.kaguya.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lazily loads en_US.lang as a fallback translation source.
 * Uses multiple classloader strategies to avoid early Minecraft class loading issues
 * that can occur during Mixin application.
 */
public final class LangFallback {
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    private static final Pattern TRANSLATION_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*\\.[a-zA-Z0-9_][a-zA-Z0-9_.]*$");

    private static volatile Map<String, String> fallbackMap;

    private LangFallback() {}

    /**
     * Returns the en_US fallback translation map (lazy-loaded on first access).
     */
    public static Map<String, String> getFallbacks() {
        if (fallbackMap == null) {
            synchronized (LangFallback.class) {
                if (fallbackMap == null) {
                    fallbackMap = loadEnUsFallbacks();
                    System.out.println("[KaguyaClient] LangFallback loaded " + fallbackMap.size() + " entries");
                }
            }
        }
        return fallbackMap;
    }

    /**
     * Translates a key using the fallback map. Returns null if no fallback exists.
     */
    public static String translate(String key) {
        return getFallbacks().get(key);
    }

    /**
     * Translates a key with format arguments using the fallback map.
     * Returns null if no fallback exists.
     */
    public static String translateFormatted(String key, Object... args) {
        String fallback = getFallbacks().get(key);
        if (fallback == null) {
            return null;
        }

        String normalized = FORMAT_PATTERN.matcher(fallback).replaceAll("%$1s");
        if (args == null || args.length == 0) {
            return normalized;
        }

        try {
            return String.format(normalized, args);
        } catch (IllegalFormatException ignored) {
            return normalized;
        }
    }

    /**
     * Checks if a string looks like a Minecraft translation key (e.g. "menu.quit", "potion.moveSpeed").
     */
    public static boolean looksLikeTranslationKey(String text) {
        return text != null
                && text.length() > 2
                && text.length() < 80
                && !text.contains(" ")
                && TRANSLATION_KEY_PATTERN.matcher(text).matches();
    }

    private static Map<String, String> loadEnUsFallbacks() {
        String path = "assets/minecraft/lang/en_US.lang";

        InputStream stream = null;

        // Strategy 1: Thread context classloader (most reliable in Forge LaunchWrapper)
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                stream = cl.getResourceAsStream(path);
            }
        } catch (Exception ignored) {}

        // Strategy 2: This class's own classloader
        if (stream == null) {
            try {
                stream = LangFallback.class.getClassLoader().getResourceAsStream(path);
            } catch (Exception ignored) {}
        }

        // Strategy 3: System classloader
        if (stream == null) {
            try {
                stream = ClassLoader.getSystemResourceAsStream(path);
            } catch (Exception ignored) {}
        }

        // Strategy 4: Absolute path (leading slash)
        if (stream == null) {
            try {
                stream = LangFallback.class.getResourceAsStream("/" + path);
            } catch (Exception ignored) {}
        }

        if (stream == null) {
            System.err.println("[KaguyaClient] LangFallback: Could not find en_US.lang via any classloader");
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>(4096);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                int split = line.indexOf('=');
                if (split <= 0 || split == line.length() - 1) {
                    continue;
                }
                map.put(line.substring(0, split), line.substring(split + 1));
            }
        } catch (IOException e) {
            System.err.println("[KaguyaClient] LangFallback: Error reading en_US.lang: " + e.getMessage());
            return Collections.emptyMap();
        }

        return map;
    }
}

