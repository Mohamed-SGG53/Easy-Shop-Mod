package com.example.shopmod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.example.shopmod.ShopMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side skin helper for the Easy Shop Mod.
 * Reads skins from config/Easy Shop Mod/AllPlayerSkins/{uuid}.png
 * Extracts the 8x8 head face + overlay from the skin PNG.
 * Scales the face using manual integer pixel mapping for pixel-perfect rendering.
 * Default fallback: gray square.
 */
@Environment(EnvType.CLIENT)
public final class SkinHelper {

    private static final Path MY_SKIN_DIR = Paths.get("config", "Easy Shop Mod", "My Skin");
    private static final Path ALL_SKINS_DIR = Paths.get("config", "Easy Shop Mod", "AllPlayerSkins");

    // Cache: UUID -> scaled face BufferedImage (ready to render)
    private static final ConcurrentHashMap<UUID, BufferedImage> FACE_CACHE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    // Default skin face cache (loaded once from mod resources)
    private static BufferedImage DEFAULT_FACE = null;

    private SkinHelper() {}

    // ==================== My Skin ====================

    /**
     * Read a random PNG file from config/Easy Shop Mod/My Skin/
     * If multiple PNGs exist, picks one randomly.
     * Renames it to {uuid}.png and returns the raw bytes.
     * @return PNG bytes, or null if no PNG found
     */
    public static byte[] readMySkinPng(UUID uuid) {
        try {
            if (!Files.exists(MY_SKIN_DIR)) {
                Files.createDirectories(MY_SKIN_DIR);
                return null;
            }

            // Collect all PNG files
            List<Path> pngFiles = new ArrayList<>();
            for (Path file : Files.list(MY_SKIN_DIR).toArray(Path[]::new)) {
                if (file.toString().toLowerCase().endsWith(".png") && Files.isRegularFile(file)) {
                    pngFiles.add(file);
                }
            }

            if (pngFiles.isEmpty()) return null;

            // Pick one randomly if multiple
            Path chosenFile;
            if (pngFiles.size() > 1) {
                Collections.shuffle(pngFiles, RANDOM);
                System.out.println("[ShopMod] Found " + pngFiles.size() + " PNGs in My Skin, picked: " + pngFiles.get(0).getFileName());
            }
            chosenFile = pngFiles.get(0);

            byte[] data = Files.readAllBytes(chosenFile);

            // Rename to {uuid}.png (only if not already named correctly)
            Path targetFile = MY_SKIN_DIR.resolve(uuid.toString() + ".png");
            if (!chosenFile.equals(targetFile)) {
                Files.move(chosenFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[ShopMod] Renamed " + chosenFile.getFileName() + " -> " + uuid.toString() + ".png");
            }

            System.out.println("[ShopMod] Read my skin: " + targetFile.getFileName() + " (" + data.length + " bytes)");
            return data;

        } catch (Exception e) {
            System.out.println("[ShopMod] Error reading my skin: " + e.getMessage());
            return null;
        }
    }

    // ==================== AllPlayerSkins ====================

    /**
     * Save a skin PNG to config/Easy Shop Mod/AllPlayerSkins/{uuid}.png
     */
    public static void saveOtherSkin(UUID uuid, byte[] pngData) {
        try {
            Files.createDirectories(ALL_SKINS_DIR);
            Path file = ALL_SKINS_DIR.resolve(uuid.toString() + ".png");
            Files.write(file, pngData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("[ShopMod] Saved skin to AllPlayerSkins: " + uuid + " (" + pngData.length + " bytes)");

            FACE_CACHE.remove(uuid);
        } catch (Exception e) {
            System.out.println("[ShopMod] Error saving skin to AllPlayerSkins: " + e.getMessage());
        }
    }

    // ==================== Face Drawing ====================

    /**
     * Draw a player face with 1px outer border at the given position.
     * @param size Total size including border (e.g. 22 = 1px border + 20px face + 1px border)
     * The 8x8 face is scaled using manual pixel mapping for perfectly even pixel distribution.
     */
    public static void drawPlayerFace(GuiGraphicsExtractor ctx, UUID uuid, int x, int y, int size) {
        int innerSize = size - 2;

        // Draw 1px black outer border
        ctx.fill(x, y, x + size, y + size, 0xFF000000);

        if (innerSize <= 0) return;

        BufferedImage face = null;

        if (uuid != null) {
            face = FACE_CACHE.get(uuid);
            if (face == null) {
                face = loadFaceImage(uuid);
                if (face != null) {
                    FACE_CACHE.put(uuid, face);
                }
            }
        }

        // If no player skin found, load default face from mod resources
        if (face == null) {
            face = getDefaultFace();
        }

        if (face != null) {
            drawScaledFace(ctx, face, x + 1, y + 1, innerSize);
        } else {
            // Fallback: gray square (only if default.png is missing)
            ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFFAAAAAA);
        }
    }

    /**
     * Draw an 8x8 face scaled to targetSize using manual integer pixel mapping.
     * Each source pixel gets (targetSize / 8) or (targetSize / 8 + 1) screen pixels,
     * distributed as evenly as possible — no NEAREST_NEIGHBOR artifacts.
     */
    private static void drawScaledFace(GuiGraphicsExtractor ctx, BufferedImage face, int x, int y, int targetSize) {
        int srcW = face.getWidth();
        int srcH = face.getHeight();

        for (int sy = 0; sy < srcH; sy++) {
            int yStart = (sy * targetSize) / srcH;
            int yEnd = ((sy + 1) * targetSize) / srcH;

            for (int sx = 0; sx < srcW; sx++) {
                int color = face.getRGB(sx, sy);
                int alpha = (color >> 24) & 0xFF;
                if (alpha < 10) continue;

                int xStart = (sx * targetSize) / srcW;
                int xEnd = ((sx + 1) * targetSize) / srcW;

                ctx.fill(x + xStart, y + yStart, x + xEnd, y + yEnd, color);
            }
        }
    }

    /**
     * Load and extract 8x8 composite face from AllPlayerSkins/{uuid}.png.
     */
    private static BufferedImage loadFaceImage(UUID uuid) {
        try {
            Path pngFile = ALL_SKINS_DIR.resolve(uuid.toString() + ".png");
            if (!Files.exists(pngFile)) return null;

            byte[] pngBytes = Files.readAllBytes(pngFile);
            BufferedImage skinImg = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (skinImg == null) {
                System.out.println("[ShopMod] ImageIO failed to read PNG for " + uuid);
                return null;
            }

            BufferedImage face = extractFace(skinImg);
            if (face != null) {
                System.out.println("[ShopMod] Loaded face for " + uuid + " (" + skinImg.getWidth() + "x" + skinImg.getHeight() + " skin)");
            }
            return face;

        } catch (Exception e) {
            System.out.println("[ShopMod] Error loading face for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract 8x8 face from a Minecraft skin image with overlay compositing.
     * Base face:   X:8-15,  Y:8-15  (64x64 skin)
     * Overlay:     X:40-47, Y:8-15  (hat/glasses/mask layer)
     */
    private static BufferedImage extractFace(BufferedImage skinImg) {
        int w = skinImg.getWidth();
        int h = skinImg.getHeight();

        int faceX, faceY;
        int overlayX = -1, overlayY = -1;
        boolean hasOverlay = false;

        if (w >= 64 && h >= 64) {
            faceX = 8;
            faceY = 8;
            overlayX = 40;
            overlayY = 8;
            hasOverlay = true;
        } else if (w == 8 && h == 8) {
            return skinImg;
        } else if (w >= 8 && h >= 8) {
            faceX = Math.max(0, (w - 8) / 2);
            faceY = Math.max(0, (h - 8) / 2);
        } else {
            return null;
        }

        if (faceX + 8 > w || faceY + 8 > h) return null;

        BufferedImage face = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

        for (int py = 0; py < 8; py++) {
            for (int px = 0; px < 8; px++) {
                int baseColor = skinImg.getRGB(faceX + px, faceY + py);

                if (hasOverlay && overlayX + 8 <= w) {
                    int overlayColor = skinImg.getRGB(overlayX + px, overlayY + py);
                    int overlayAlpha = (overlayColor >> 24) & 0xFF;

                    if (overlayAlpha > 0) {
                        face.setRGB(px, py, overlayColor);
                    } else {
                        face.setRGB(px, py, baseColor);
                    }
                } else {
                    face.setRGB(px, py, baseColor);
                }
            }
        }

        return face;
    }

    /**
     * Load default face from mod resources (default.png).
     * This is a full Minecraft skin PNG (64x64) from which
     * the 8x8 face is extracted, same as any player skin.
     * Loaded once and cached for all future calls.
     */
    private static BufferedImage getDefaultFace() {
        if (DEFAULT_FACE != null) return DEFAULT_FACE;

        try {
            InputStream is = ShopMod.class.getClassLoader().getResourceAsStream("default.png");
            if (is == null) {
                System.out.println("[ShopMod] default.png not found in mod resources!");
                return null;
            }
            BufferedImage skinImg = ImageIO.read(is);
            is.close();

            if (skinImg == null) {
                System.out.println("[ShopMod] Failed to read default.png");
                return null;
            }

            DEFAULT_FACE = extractFace(skinImg);
            if (DEFAULT_FACE != null) {
                System.out.println("[ShopMod] Loaded default face from default.png ("
                    + skinImg.getWidth() + "x" + skinImg.getHeight() + " skin)");
            }
            return DEFAULT_FACE;

        } catch (Exception e) {
            System.out.println("[ShopMod] Error loading default skin: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clear all cached face images (called when reconnecting to a server).
     */
    public static void clearCache() {
        FACE_CACHE.clear();
        System.out.println("[ShopMod] Face cache cleared");
    }
}
