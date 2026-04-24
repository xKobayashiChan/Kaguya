package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Cape extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final File CAPE_FILE = new File("./config/Myau/", "cape.png");
    private static ResourceLocation capeTexture = null;
    private static long lastModified = 0;

    public Cape() {
        super("Cape", false);
    }

    /**
     * Returns the custom cape texture.
     * Priority: ./config/Myau/cape.png (外部) → JAR内蔵 /cape.png
     * 外部ファイルは変更検知で自動リロードする。
     */
    public static ResourceLocation getCapeTexture() {
        if (CAPE_FILE.exists()) {
            long currentModified = CAPE_FILE.lastModified();
            if (capeTexture == null || currentModified != lastModified) {
                loadCapeFromFile();
                lastModified = currentModified;
            }
        } else if (capeTexture == null) {
            loadCapeFromJar();
        }
        return capeTexture;
    }

    /**
     * 外部ファイル ./config/Myau/cape.png からロード。
     */
    private static void loadCapeFromFile() {
        try {
            BufferedImage image = ImageIO.read(CAPE_FILE);
            if (image == null) {
                System.err.println("[Kaguya] Failed to read cape image: " + CAPE_FILE.getAbsolutePath());
                return;
            }
            releaseTexture();
            capeTexture = mc.getTextureManager().getDynamicTextureLocation(
                    "kaguya_cape", new DynamicTexture(image));
        } catch (Exception e) {
            System.err.println("[Kaguya] Error loading cape texture:");
            e.printStackTrace();
        }
    }

    /**
     * JAR内蔵の /cape.png からロード。
     */
    private static void loadCapeFromJar() {
        try (java.io.InputStream is = Cape.class.getResourceAsStream("/cape.png")) {
            if (is == null) return; // リソースが存在しない場合は何もしない
            BufferedImage image = ImageIO.read(is);
            if (image == null) return;
            releaseTexture();
            capeTexture = mc.getTextureManager().getDynamicTextureLocation(
                    "kaguya_cape", new DynamicTexture(image));
        } catch (Exception e) {
            System.err.println("[Kaguya] Error loading built-in cape texture:");
            e.printStackTrace();
        }
    }

    private static void releaseTexture() {
        if (capeTexture != null) {
            mc.getTextureManager().deleteTexture(capeTexture);
            capeTexture = null;
        }
    }

    /**
     * Returns true if the given player should have the custom cape applied.
     * Applies to: self and friends, only when the Cape module is enabled.
     */
    public static boolean shouldApplyCape(String playerName) {
        if (Kaguya.moduleManager == null) return false;
        Cape cape = (Cape) Kaguya.moduleManager.modules.get(Cape.class);
        if (cape == null || !cape.isEnabled()) return false;

        // Apply to self
        if (mc.thePlayer != null && mc.thePlayer.getName().equals(playerName)) {
            return true;
        }

        // Apply to friends
        return Kaguya.friendManager != null && Kaguya.friendManager.isFriend(playerName);
    }

    @Override
    public void onEnabled() {
        // 外部ファイルもJAR内蔵リソースも両方ない場合のみ警告
        if (!CAPE_FILE.exists() && Cape.class.getResource("/cape.png") == null) {
            ChatUtil.sendFormatted(
                String.format("%sCape file not found: &o%s&r", Kaguya.clientName, CAPE_FILE.getPath())
            );
        }
    }

    @Override
    public void onDisabled() {
        releaseTexture();
        lastModified = 0;
    }
}

