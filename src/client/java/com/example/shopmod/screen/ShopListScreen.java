package com.example.shopmod.screen;

import com.example.shopmod.network.ModPackets;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ShopListScreen extends Screen {

    private final List<ShopEntry> shops = new ArrayList<>();
    private boolean playerHasShop;
    private final String playerName;
    private final UUID playerUuid;
    private int selectedShop = -1;
    private int scrollOffset = 0;
    private long lastClickTime = 0;

    private static final int ROW_HEIGHT = 24, VISIBLE_SHOPS = 7, FACE_SIZE = 18;
    private static final int W = 300, H = 230;

    private static final Map<UUID, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Path SKIN_DIR = Paths.get("config", "shopmod", "skins");

    private static volatile boolean skinMethodsResolved = false;
    private static volatile Object skinCacheRef = null;
    private static volatile Method cacheGetterMethod = null;
    private static volatile Method cacheGetByUUID = null;
    private static volatile Method cacheGetByRP = null;
    private static volatile Method entrySkinMethod = null;

    private static volatile boolean resolvePending = true;
    private static final Set<UUID> diskLoaded = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> diskSaved = ConcurrentHashMap.newKeySet();

    private static volatile Method defaultSkinMethod = null;
    private static volatile boolean defaultSkinResolved = false;

    private static class ShopEntry {
        final String ownerName;
        final UUID uuid;
        final int offerCount;
        final boolean isOwnShop;

        ShopEntry(String ownerName, UUID uuid, int offerCount, boolean isOwnShop) {
            this.ownerName = ownerName;
            this.uuid = uuid;
            this.offerCount = offerCount;
            this.isOwnShop = isOwnShop;
        }
    }

    public ShopListScreen(ModPackets.OpenShopsListPayload payload) {
        super(Component.literal("Shops List"));
        this.playerHasShop = payload.playerHasShop();
        this.playerName = payload.playerName();
        this.playerUuid = new UUID(payload.playerUuidMost(), payload.playerUuidLeast());
        for (ModPackets.ShopEntryInfo info : payload.shops()) {
            boolean isOwn = info.ownerName().equals(playerName);
            UUID uuid = (info.uuidMost() != 0 || info.uuidLeast() != 0) ? new UUID(info.uuidMost(), info.uuidLeast()) : null;
            shops.add(new ShopEntry(info.ownerName(), uuid, info.offerCount(), isOwn));
        }
        selectedShop = shops.isEmpty() ? -1 : 0;
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ==================== Default Skin (Steve/Alex) via Reflection ====================

    private static synchronized void resolveDefaultSkinMethod() {
        if (defaultSkinResolved) return;
        defaultSkinResolved = true;
        try {
            Method uuidMethod = null;
            Method noArgMethod = null;

            for (Method m : DefaultPlayerSkin.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != PlayerSkin.class) continue;

                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == UUID.class) {
                    uuidMethod = m;
                } else if (m.getParameterCount() == 0 && noArgMethod == null) {
                    noArgMethod = m;
                }
            }

            if (uuidMethod != null) {
                defaultSkinMethod = uuidMethod;
            } else if (noArgMethod != null) {
                defaultSkinMethod = noArgMethod;
            }

            if (defaultSkinMethod != null) {
                System.out.println("[ShopMod] Default skin method: " + defaultSkinMethod.getName() + " (params: " + defaultSkinMethod.getParameterCount() + ")");
            }
        } catch (Exception e) {
            System.out.println("[ShopMod] Default skin discovery error: " + e.getMessage());
        }
    }

    private void drawDefaultSkin(GuiGraphics ctx, UUID uuid, String name, int x, int y) {
        try {
            if (!defaultSkinResolved) resolveDefaultSkinMethod();

            if (defaultSkinMethod != null) {
                PlayerSkin skin;
                if (defaultSkinMethod.getParameterCount() == 1) {
                    skin = (PlayerSkin) defaultSkinMethod.invoke(null, uuid != null ? uuid : new UUID(0, 0));
                } else {
                    skin = (PlayerSkin) defaultSkinMethod.invoke(null);
                }
                if (skin != null) {
                    PlayerFaceRenderer.draw(ctx, skin, x, y, FACE_SIZE);
                    return;
                }
            }
            ctx.fill(x, y, x + FACE_SIZE, y + FACE_SIZE, 0xFF6B4226);
        } catch (Exception e) {
            System.out.println("[ShopMod] Default skin draw error: " + e.getMessage());
            ctx.fill(x, y, x + FACE_SIZE, y + FACE_SIZE, 0xFF6B4226);
        }
    }

    // ==================== Skin Provider Discovery (Two-Phase) ====================

    private static synchronized void resolveSkinMethods() {
        if (skinMethodsResolved) return;
        skinMethodsResolved = true;
        try {
            Object mc = Minecraft.getInstance();
            Method uuidGetter = null, uuidCacheM = null, uuidSkinM = null;
            Method rpGetter = null, rpCacheM = null, rpSkinM = null;

            for (Method getter : mc.getClass().getMethods()) {
                if (getter.getParameterCount() != 0) continue;
                Class<?> rt = getter.getReturnType();
                if (rt.equals(void.class) || rt.isPrimitive() || rt.equals(String.class)) continue;
                for (Method m : rt.getMethods()) {
                    if (m.getParameterCount() != 1 || m.getReturnType().equals(void.class)) continue;
                    boolean isUUID = (m.getParameterTypes()[0] == UUID.class);
                    boolean isRP = (!isUUID && m.getParameterTypes()[0].isAssignableFrom(ResolvableProfile.class));
                    if (!isUUID && !isRP) continue;
                    Method sm = findSkinGetter(m.getReturnType());
                    if (sm == null) continue;
                    if (isUUID && uuidGetter == null) { uuidGetter = getter; uuidCacheM = m; uuidSkinM = sm; }
                    if (isRP && rpGetter == null) { rpGetter = getter; rpCacheM = m; rpSkinM = sm; }
                }
                if (uuidGetter != null && rpGetter != null) break;
            }

            if (uuidGetter != null) {
                try {
                    Object obj = uuidGetter.invoke(mc);
                    if (obj != null) { cacheGetByUUID = uuidCacheM; entrySkinMethod = uuidSkinM; skinCacheRef = obj; cacheGetterMethod = uuidGetter; }
                } catch (Exception ignored) {}
            }
            if (cacheGetByUUID == null && rpGetter != null) {
                try {
                    Object obj = rpGetter.invoke(mc);
                    if (obj != null) { cacheGetByRP = rpCacheM; entrySkinMethod = rpSkinM; skinCacheRef = obj; cacheGetterMethod = rpGetter; }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[ShopMod Skin] Init error: " + e.getMessage());
        }
    }

    private static Method findSkinGetter(Class<?> entryClass) {
        for (Method m : entryClass.getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == PlayerSkin.class) return m;
        }
        return null;
    }

    // ==================== Skin Texture Extraction ====================

    /**
     * Extracts texture ResourceLocation string and model type from PlayerSkin.
     * Uses only toString() pattern matching - NO Class.forName().
     */
    private static String[] extractSkinData(PlayerSkin skin) {
        if (skin == null) return null;
        String textureStr = null;
        String modelStr = "WIDE";
        try {
            for (Method m : skin.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String mName = m.getName();
                if (mName.equals("hashCode") || mName.equals("equals") || mName.equals("toString")
                    || mName.equals("getClass") || mName.equals("notify") || mName.equals("notifyAll") || mName.equals("wait"))
                    continue;
                try {
                    Object val = m.invoke(skin);
                    if (val == null || val == skin) continue;
                    String s = val.toString();
                    if (s.equals(skin.toString()) || val instanceof Boolean || val instanceof String) continue;
                    if (val.getClass().getName().contains("Optional")) continue;

                    if ("WIDE".equals(s) || "SLIM".equals(s)) { modelStr = s; continue; }

                    String sLow = s.toLowerCase();
                    if (sLow.contains("cape") || sLow.contains("elytra")) continue;

                    // Case 1: Wrapper with texturePath=
                    if (s.contains("texturePath=")) {
                        int idx = s.indexOf("texturePath=") + "texturePath=".length();
                        int end = s.indexOf(",", idx);
                        if (end == -1) end = s.indexOf("]", idx);
                        if (end == -1) end = s.length();
                        String extracted = s.substring(idx, end).trim();
                        if (extracted.contains(":") && !extracted.contains("capes") && !extracted.contains("elytra")) {
                            textureStr = extracted;
                        }
                        continue;
                    }

                    // Case 2: URL (skip)
                    if (s.startsWith("http://") || s.startsWith("https://")) continue;

                    // Case 3: Direct ResourceLocation: "namespace:path"
                    if (s.contains(":") && s.contains("/") && !s.contains("[") && !s.contains("class_")
                        && !s.contains("capes") && !s.contains("elytra") && s.length() < 300) {
                        textureStr = s;
                        continue;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return textureStr != null ? new String[]{textureStr, modelStr} : null;
    }

    // ==================== Disk Persistence ====================

    /**
     * Saves skin to disk as .skin text file.
     * Reads from SKIN_CACHE directly - NO getConnection() dependency!
     * Can be called from any thread.
     */
    private static void saveSkinToDisk(UUID uuid, String name) {
        if (diskSaved.contains(uuid)) return;
        try {
            PlayerSkin skin = SKIN_CACHE.get(uuid);
            if (skin == null) {
                System.out.println("[ShopMod] saveSkinToDisk: no skin in cache for " + name);
                return;
            }
            String[] data = extractSkinData(skin);
            if (data == null) {
                System.out.println("[ShopMod] saveSkinToDisk: could not extract data for " + name);
                return;
            }
            Files.createDirectories(SKIN_DIR);
            Path file = SKIN_DIR.resolve(uuid.toString() + ".skin");
            Path tmp = SKIN_DIR.resolve(uuid.toString() + ".skin.tmp");
            Files.writeString(tmp, data[0] + "\n" + data[1] + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            diskSaved.add(uuid);
            System.out.println("[ShopMod] saveSkinToDisk: saved " + name + " -> " + file);
        } catch (Exception e) {
            System.out.println("[ShopMod] saveSkinToDisk error for " + name + ": " + e.getMessage());
        }
    }

    /**
     * Loads skin from .skin text file and reconstructs PlayerSkin via reflection.
     */
    private static PlayerSkin loadSkinFromDisk(UUID uuid) {
        if (diskLoaded.contains(uuid)) return null;
        diskLoaded.add(uuid);
        Path file = SKIN_DIR.resolve(uuid.toString() + ".skin");
        if (!Files.exists(file)) return null;
        try {
            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty() || lines.get(0).trim().isEmpty()) return null;
            String textureLine = lines.get(0).trim();
            String modelType = lines.size() > 1 && "SLIM".equals(lines.get(1).trim()) ? "SLIM" : "WIDE";

            if (textureLine.contains("[") || textureLine.contains(",") || textureLine.contains("class_")
                || textureLine.contains("capes") || textureLine.contains("elytra")) {
                Files.deleteIfExists(file);
                return null;
            }

            String namespace = "minecraft", path = textureLine;
            int colonIdx = textureLine.indexOf(':');
            if (colonIdx > 0) { namespace = textureLine.substring(0, colonIdx); path = textureLine.substring(colonIdx + 1); }

            Object resourceLocation;
            try {
                Class<?> rlClass = Class.forName("net.minecraft.class_2960");
                resourceLocation = rlClass.getConstructor(String.class, String.class).newInstance(namespace, path);
            } catch (ClassNotFoundException e) {
                Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
                resourceLocation = rlClass.getConstructor(String.class, String.class).newInstance(namespace, path);
            }

            Object modelEnum;
            try {
                Class<?> modelClass = Class.forName("net.minecraft.class_5605$class_5606");
                modelEnum = Enum.valueOf((Class<Enum>) modelClass, modelType);
            } catch (ClassNotFoundException e) {
                Class<?> modelClass = Class.forName("net.minecraft.world.entity.player.PlayerSkin$Model");
                modelEnum = Enum.valueOf((Class<Enum>) modelClass, modelType);
            }

            Constructor<?> skinCtor = PlayerSkin.class.getDeclaredConstructors()[0];
            skinCtor.setAccessible(true);
            PlayerSkin ps = (PlayerSkin) skinCtor.newInstance(resourceLocation, modelEnum, null);
            System.out.println("[ShopMod] loadSkinFromDisk: loaded " + textureLine + " model=" + modelType + " for " + uuid);
            return ps;
        } catch (Exception e) {
            System.out.println("[ShopMod] loadSkinFromDisk error: " + e.getMessage());
            try { Files.deleteIfExists(file); } catch (Exception ignored) {}
        }
        return null;
    }

    // ==================== Main Skin Loading ====================

    private static PlayerSkin getPlayerSkin(UUID uuid, String name) {
        if (!skinMethodsResolved) resolveSkinMethods();
        if (uuid == null) return null;

        PlayerSkin cached = SKIN_CACHE.get(uuid);
        if (cached != null) return cached;

        if (cacheGetByUUID != null && entrySkinMethod != null && skinCacheRef != null) {
            try {
                Object entry = cacheGetByUUID.invoke(skinCacheRef, uuid);
                if (entry != null) {
                    Object skin = entrySkinMethod.invoke(entry);
                    if (skin instanceof PlayerSkin ps) {
                        SKIN_CACHE.put(uuid, ps);
                        saveSkinToDisk(uuid, name);
                        return ps;
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            Object player = Minecraft.getInstance().player;
            if (player != null) {
                Method getUUID = player.getClass().getMethod("getUUID");
                if (getUUID.invoke(player).equals(uuid)) {
                    for (Method m : player.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && m.getReturnType() == PlayerSkin.class) {
                            Object skin = m.invoke(player);
                            if (skin instanceof PlayerSkin ps) {
                                SKIN_CACHE.put(uuid, ps);
                                saveSkinToDisk(uuid, name);
                                return ps;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            Object mc = Minecraft.getInstance();
            Method getConnection = mc.getClass().getMethod("getConnection");
            Object connection = getConnection.invoke(mc);
            if (connection != null) {
                for (Method m : connection.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == UUID.class) {
                        try {
                            Object playerInfo = m.invoke(connection, uuid);
                            if (playerInfo != null) {
                                for (Method pm : playerInfo.getClass().getMethods()) {
                                    if (pm.getParameterCount() == 0 && pm.getReturnType() == PlayerSkin.class) {
                                        Object skin = pm.invoke(playerInfo);
                                        if (skin instanceof PlayerSkin ps) {
                                            SKIN_CACHE.put(uuid, ps);
                                            saveSkinToDisk(uuid, name);
                                            return ps;
                                        }
                                    }
                                }
                                for (Method pm : playerInfo.getClass().getMethods()) {
                                    if (pm.getParameterCount() == 0 && pm.getReturnType() == GameProfile.class) {
                                        GameProfile gp = (GameProfile) pm.invoke(playerInfo);
                                        if (gp != null && skinCacheRef != null) {
                                            PlayerSkin ps = skinFromProfile(gp);
                                            if (ps != null) {
                                                SKIN_CACHE.put(uuid, ps);
                                                saveSkinToDisk(uuid, name);
                                                return ps;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        PlayerSkin diskSkin = loadSkinFromDisk(uuid);
        if (diskSkin != null) {
            SKIN_CACHE.put(uuid, diskSkin);
            return diskSkin;
        }

        return null;
    }

    private static PlayerSkin skinFromProfile(GameProfile profile) {
        try {
            if (cacheGetByRP != null && entrySkinMethod != null) {
                for (Method m : ResolvableProfile.class.getMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].isAssignableFrom(GameProfile.class) && m.getReturnType() == ResolvableProfile.class) {
                        Object rp = m.invoke(null, profile);
                        if (rp != null) { Object entry = cacheGetByRP.invoke(skinCacheRef, rp); if (entry != null) { Object skin = entrySkinMethod.invoke(entry); if (skin instanceof PlayerSkin ps) return ps; } }
                    }
                }
            }
            if (skinCacheRef != null) {
                for (Method m : skinCacheRef.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(GameProfile.class) && !m.getReturnType().equals(void.class)) {
                        try {
                            Object result = m.invoke(skinCacheRef, profile);
                            if (result instanceof PlayerSkin ps) return ps;
                            if (result != null) { Method sm = findSkinGetter(result.getClass()); if (sm != null) { Object skin = sm.invoke(result); if (skin instanceof PlayerSkin ps) return ps; } }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== Avatar Drawing ====================

    private void drawPlayerAvatar(GuiGraphics ctx, UUID uuid, String name, int x, int y) {
        if (uuid != null) {
            PlayerSkin cached = SKIN_CACHE.get(uuid);
            if (cached != null) {
                try { PlayerFaceRenderer.draw(ctx, cached, x, y, FACE_SIZE); return; }
                catch (Exception ignored) {}
            }
        }
        drawDefaultSkin(ctx, uuid, name, x, y);
    }

    // ==================== Screen Methods ====================

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        if (selectedShop >= 0 && selectedShop < getTotalEntries())
            addRenderableWidget(Button.builder(Component.literal("Open"), btn -> openSelected())
                .bounds(px + W / 2 - 85, py + H - 25, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
            .bounds(px + W / 2 + 15, py + H - 25, 70, 20).build());
    }

    private int getTotalEntries() { return playerHasShop ? shops.size() : shops.size() + 1; }
    private boolean isCreateEntry(int index) { return !playerHasShop && index == 0; }
    private ShopEntry getShopEntry(int displayIndex) {
        int idx = playerHasShop ? displayIndex : displayIndex - 1;
        return (idx < 0 || idx >= shops.size()) ? null : shops.get(idx);
    }

    private void openSelected() {
        if (selectedShop < 0 || selectedShop >= getTotalEntries()) return;
        if (isCreateEntry(selectedShop)) ClientPlayNetworking.send(new ModPackets.CreateShopFromListPayload());
        else { ShopEntry e = getShopEntry(selectedShop); if (e != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(e.ownerName)); }
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 24, 0xFF553311);
        ctx.drawCenteredString(font, Component.literal("Shops List"), px + W / 2, py + 7, 0xFFFFFFFF);

        int listX = px + 8, listY = py + 30, listW = W - 16, listH = VISIBLE_SHOPS * ROW_HEIGHT;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, listH, 0xFF666666);

        int totalEntries = getTotalEntries();
        if (totalEntries == 0 && !playerHasShop) {
            ctx.drawCenteredString(font, Component.literal("No shops yet"), px + W / 2, listY + listH / 2 - 4, 0xFFFFFFFF);
        } else {
            int endIdx = Math.min(scrollOffset + VISIBLE_SHOPS, totalEntries);
            for (int i = scrollOffset; i < endIdx; i++) {
                int rowY = listY + (i - scrollOffset) * ROW_HEIGHT, rowX = listX + 2, rowW = listW - 4;
                boolean isSelected = (i == selectedShop);
                boolean hovered = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT;
                if (isSelected) ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x80FFD700);
                else if (hovered) ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x40FFFFFF);

                if (isCreateEntry(i)) {
                    drawPlayerAvatar(ctx, playerUuid, playerName, rowX + 3, rowY + 3);
                    ctx.drawString(font, Component.literal("Create Your Shop"), rowX + FACE_SIZE + 8, rowY + 7, 0xFF55FF55);
                    ctx.drawString(font, Component.literal("+"), rowX + rowW - 16, rowY + 7, 0xFF55FF55);
                } else {
                    ShopEntry entry = getShopEntry(i);
                    if (entry != null) {
                        drawPlayerAvatar(ctx, entry.uuid, entry.ownerName, rowX + 3, rowY + 3);
                        ctx.drawString(font, Component.literal(entry.ownerName + "'s Shop"), rowX + FACE_SIZE + 8, rowY + 4, entry.isOwnShop ? 0xFF55FFFF : 0xFFFFFFFF);
                        String offerComp = "(" + entry.offerCount + (entry.offerCount == 1 ? " Offer)" : " Offers)");
                        int offerWidth = font.width(offerComp);
                        ctx.drawString(font, Component.literal(offerComp), rowX + rowW - offerWidth - 6, rowY + 4, 0xFFAAAAAA);
                    }
                }
            }
            if (totalEntries > VISIBLE_SHOPS) {
                int maxScroll = totalEntries - VISIBLE_SHOPS;
                int thumbH = Math.max(20, listH * VISIBLE_SHOPS / totalEntries);
                int thumbY = listY + (listH - thumbH) * scrollOffset / maxScroll;
                ctx.fill(listX + listW - 5, listY, listX + listW - 2, listY + listH, 0xFF555555);
                ctx.fill(listX + listW - 5, thumbY, listX + listW - 2, thumbY + thumbH, 0xFFAAAAAA);
            }
        }
        ctx.drawCenteredString(font, Component.literal(shops.size() + (shops.size() == 1 ? " Shop" : " Shops")), px + W / 2, py + H - 48, 0xFFAAAAAA);
        super.render(ctx, mx, my, delta);

        // After first render: load skins in background thread
        if (resolvePending) {
            resolvePending = false;
            final List<ShopEntry> shopCopy = new ArrayList<>(shops);
            new Thread("ShopMod-SkinLoader") {
                @Override
                public void run() {
                    try {
                        resolveDefaultSkinMethod();
                        System.out.println("[ShopMod] BG: Resolving skin methods...");
                        resolveSkinMethods();
                        System.out.println("[ShopMod] BG: Pre-loading " + shopCopy.size() + " shop skins...");
                        for (ShopEntry entry : shopCopy) {
                            if (entry.uuid != null && !SKIN_CACHE.containsKey(entry.uuid)) {
                                try {
                                    PlayerSkin skin = getPlayerSkin(entry.uuid, entry.ownerName);
                                    if (skin != null) {
                                        System.out.println("[ShopMod] BG: Loaded skin for " + entry.ownerName);
                                    }
                                } catch (Exception e) {
                                    System.out.println("[ShopMod] BG: Failed skin for " + entry.ownerName + ": " + e.getMessage());
                                }
                            }
                        }
                        System.out.println("[ShopMod] BG: All skin loading complete");
                    } catch (Exception e) {
                        System.out.println("[ShopMod] BG error: " + e.getMessage());
                    }
                }
            }.start();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 8, listY = py + 30;
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (mx >= listX && mx < listX + W - 16 && my >= listY && my < listY + VISIBLE_SHOPS * ROW_HEIGHT) {
            int clickedIdx = scrollOffset + (int) ((my - listY) / ROW_HEIGHT);
            if (clickedIdx >= 0 && clickedIdx < getTotalEntries()) {
                long now = System.currentTimeMillis();
                if (clickedIdx == selectedShop && (now - lastClickTime) < 400 && button == 0) { openSelected(); lastClickTime = 0; }
                else { selectedShop = clickedIdx; lastClickTime = now; }
                this.children().clear(); init(); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, getTotalEntries() - VISIBLE_SHOPS);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, maxScroll));
        if (selectedShop < scrollOffset) selectedShop = scrollOffset;
        if (selectedShop >= scrollOffset + VISIBLE_SHOPS) selectedShop = scrollOffset + VISIBLE_SHOPS - 1;
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}