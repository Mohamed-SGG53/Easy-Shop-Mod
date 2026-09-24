package com.example.shopmod.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Server-side storage for player skin PNG files.
 * Skins are saved to: config/Easy Shop Mod/PlayerSkins/{uuid}.png
 */
public class PlayerSkinStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("ShopMod");

    /**
     * Get the server-level skin storage directory: config/Easy Shop Mod/PlayerSkins/
     */
    public static Path getSkinsDir() {
        Path dir = Paths.get("config", "Easy Shop Mod", "PlayerSkins");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("[ShopMod] Failed to create skin directory: {}", e.getMessage());
        }
        return dir;
    }

    /**
     * Save a player's skin PNG bytes on the server.
     */
    public static void saveSkin(UUID uuid, byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length == 0) return;
        Path dir = getSkinsDir();
        Path file = dir.resolve(uuid.toString() + ".png");
        try {
            Files.write(file, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("[ShopMod] Saved skin for {} ({} bytes)", uuid, pngBytes.length);
        } catch (IOException e) {
            LOGGER.warn("[ShopMod] Failed to save skin for {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * Read a player's skin PNG bytes from the server.
     */
    public static byte[] getSkin(UUID uuid) {
        Path file = getSkinsDir().resolve(uuid.toString() + ".png");
        if (!Files.exists(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            LOGGER.warn("[ShopMod] Failed to read skin for {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a skin exists for the given UUID.
     */
    public static boolean hasSkin(UUID uuid) {
        return Files.exists(getSkinsDir().resolve(uuid.toString() + ".png"));
    }
}
