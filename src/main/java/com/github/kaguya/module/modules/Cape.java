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
     * Returns the custom cape texture, loading from ./config/Myau/cape.png.
     * Automatically reloads if the file has been modified.
     */
    public static ResourceLocation getCapeTexture() {
        if (CAPE_FILE.exists()) {
            long currentModified = CAPE_FILE.lastModified();
            if (capeTexture == null || currentModified != lastModified) {
                capeTexture = loadCapeTexture();
                lastModified = currentModified;
            }
        }
        return capeTexture;
    }

    /**
     * Loads the cape texture from ./config/Myau/cape.png
     */
    private static ResourceLocation loadCapeTexture() {
        try {
            BufferedImage image = ImageIO.read(CAPE_FILE);
            if (image == null) {
                System.err.println("[Kaguya] Failed to read cape image: " + CAPE_FILE.getAbsolutePath());
                return null;
            }
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            return mc.getTextureManager().getDynamicTextureLocation("kaguya_cape", dynamicTexture);
        } catch (Exception e) {
            System.err.println("[Kaguya] Error loading cape texture: " + e.getMessage());
            return null;
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
        if (!CAPE_FILE.exists()) {
            ChatUtil.sendFormatted(
                String.format("%sCape file not found: &o%s&r", Kaguya.clientName, CAPE_FILE.getPath())
            );
        }
    }

    @Override
    public void onDisabled() {
        capeTexture = null;
        lastModified = 0;
    }
}

