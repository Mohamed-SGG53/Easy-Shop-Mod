package com.example.shopmod.screen;

import com.example.shopmod.network.ModPackets;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // ==================== Skin face rendering via reflection ====================
    // Mirrors Yarn approach: mc.getPlayerSkinCache().get(profile) → SkinTextures → PlayerSkinDrawer.draw()
    // All class/method names discovered at runtime to avoid Official Mapping name mismatches.

    private static volatile boolean skinSystemReady = false;
    private static volatile Method mcGetSkinCacheMethod = null;    // mc.getXxxSkinCache()
    private static volatile Object skinCacheRef = null;             // the cache object
    private static volatile Method cacheGetMethod = null;           // cache.get(ResolvableProfile) → Entry
    private static volatile Method entryGetTexturesMethod = null;   // entry.getTextures() → SkinTextures
    private static volatile Method drawerDrawMethod = null;         // PlayerSkinDrawer.draw(ctx, textures, x, y, size)
    private static volatile Class<?> drawerClass = null;            // PlayerSkinDrawer class
    private static volatile Method createProfileMethod = null;      // ResolvableProfile.ofStatic(profile) or equivalent
    private static volatile Constructor<?> profileStaticCtor = null;// ResolvableProfile.Static constructor
    private static volatile Method skinTexturesTextureMethod = null;// skinTextures.texture() → Identifier (for manual blit)
    private static volatile Method defaultSkinHelperMethod = null;  // DefaultSkinHelper.getSkinTextures(uuid)

    private static class ShopEntry {
        final String ownerName; final UUID uuid; final int offerCount; final boolean isOwnShop;
        ShopEntry(String ownerName, UUID uuid, int offerCount, boolean isOwnShop) {
            this.ownerName = ownerName; this.uuid = uuid; this.offerCount = offerCount; this.isOwnShop = isOwnShop;
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
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ==================== Skin System Initialization ====================

    /**
     * One-time init: discovers all Official Mapping class/method names via reflection.
     * Searches for the same pipeline as Yarn: Minecraft → SkinCache → get(profile) → textures → draw.
     */
    private static synchronized void initSkinSystem() {
        if (skinSystemReady) return;
        skinSystemReady = true;

        try {
            // Step 1: Find ResolvableProfile.Static constructor (for creating profile from GameProfile)
            Class<?> rpClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            for (Class<?> inner : rpClass.getDeclaredClasses()) {
                for (Constructor<?> ctor : inner.getDeclaredConstructors()) {
                    Class<?>[] pt = ctor.getParameterTypes();
                    if (pt.length == 1 || pt.length == 2) {
                        ctor.setAccessible(true);
                        profileStaticCtor = ctor;
                        break;
                    }
                }
                if (profileStaticCtor != null) break;
            }

            // Also try static factory methods on ResolvableProfile
            for (Method m : rpClass.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) &&
                    m.getParameterCount() == 1 &&
                    m.getParameterTypes()[0].isAssignableFrom(GameProfile.class)) {
                    m.setAccessible(true);
                    createProfileMethod = m;
                    break;
                }
            }

            // Step 2: Find DefaultSkinHelper.getSkinTextures(UUID) for fallback
            for (String dshClass : new String[]{
                "net.minecraft.client.resources.DefaultSkinHelper",
                "net.minecraft.client.util.DefaultSkinHelper",
                "net.minecraft.client.gui.components.player.DefaultSkinHelper"
            }) {
                try {
                    Class<?> cls = Class.forName(dshClass);
                    for (Method m : cls.getDeclaredMethods()) {
                        if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) &&
                            m.getParameterCount() == 1 && m.getParameterTypes()[0] == UUID.class) {
                            m.setAccessible(true);
                            defaultSkinHelperMethod = m;
                            break;
                        }
                    }
                    if (defaultSkinHelperMethod != null) break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Step 3: Find the skin cache on Minecraft instance
            // The cache has a method accepting ResolvableProfile as parameter
            Object mc = net.minecraft.client.Minecraft.getInstance();

            // First try known method name patterns
            for (Method m : mc.getClass().getMethods()) {
                if (m.getParameterCount() != 0 || m.getReturnType() == void.class) continue;
                String n = m.getName();
                if (n.contains("Skin") || n.contains("skin") || n.contains("Profile") || n.contains("profile")) {
                    try {
                        Object obj = m.invoke(mc);
                        if (obj == null) continue;
                        Method getM = findCacheGetMethod(obj.getClass(), rpClass);
                        if (getM != null) {
                            mcGetSkinCacheMethod = m;
                            skinCacheRef = obj;
                            cacheGetMethod = getM;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Broader search if not found
            if (skinCacheRef == null) {
                for (Method m : mc.getClass().getMethods()) {
                    if (m.getParameterCount() != 0 || m.getReturnType() == void.class || m.getReturnType().isPrimitive()) continue;
                    try {
                        Object obj = m.invoke(mc);
                        if (obj == null) continue;
                        Method getM = findCacheGetMethod(obj.getClass(), rpClass);
                        if (getM != null) {
                            mcGetSkinCacheMethod = m;
                            skinCacheRef = obj;
                            cacheGetMethod = getM;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Step 4: If cache found, discover entry.getTextures() and texture() methods
            if (cacheGetMethod != null) {
                Class<?> entryClass = cacheGetMethod.getReturnType();
                // Find getTextures() or similar
                for (Method m : entryClass.getMethods()) {
                    if (m.getParameterCount() == 0 && !m.getReturnType().equals(void.class) &&
                        !m.getReturnType().isPrimitive()) {
                        entryGetTexturesMethod = m;
                        break;
                    }
                }
            }

            // Step 5: Find PlayerSkinDrawer (or Official equivalent)
            for (String className : new String[]{
                "net.minecraft.client.gui.PlayerSkinDrawer",
                "net.minecraft.client.gui.PlayerFaceRenderer",
                "net.minecraft.client.gui.components.PlayerSkinDrawer",
                "net.minecraft.client.gui.components.PlayerFaceRenderer",
                "net.minecraft.client.gui.components.player.PlayerSkinDrawer",
                "net.minecraft.client.gui.components.player.PlayerFaceRenderer"
            }) {
                try {
                    Class<?> cls = Class.forName(className);
                    for (Method m : cls.getDeclaredMethods()) {
                        if (m.getName().equals("draw")) {
                            drawerClass = cls;
                            drawerDrawMethod = m;
                            m.setAccessible(true);
                            break;
                        }
                    }
                    if (drawerDrawMethod != null) break;
                } catch (ClassNotFoundException ignored) {}
            }

            // Step 6: Find skinTextures.texture() → Identifier (for manual face blit fallback)
            if (entryGetTexturesMethod != null) {
                Class<?> texturesClass = entryGetTexturesMethod.getReturnType();
                for (Method m : texturesClass.getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == Identifier.class) {
                        skinTexturesTextureMethod = m;
                        break;
                    }
                }
            }

        } catch (Exception e) {
            // Silently fail - will fall back to head item rendering
        }
    }

    /**
     * Find a method on a class that accepts ResolvableProfile as parameter.
     * This identifies the skin cache's "get(profile)" method.
     */
    private static Method findCacheGetMethod(Class<?> clazz, Class<?> rpClass) {
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(rpClass)) {
                return m;
            }
        }
        return null;
    }

    // ==================== Profile & Skin Retrieval ====================

    /**
     * Create a ResolvableProfile from a GameProfile using reflection.
     * Mirrors Yarn's ProfileComponent.ofStatic(profile).
     */
    private static Object createResolvableProfile(UUID uuid, String name) {
        try {
            GameProfile profile = new GameProfile(uuid, name != null ? name : "");

            // Try static factory method first (e.g., ResolvableProfile.of(profile))
            if (createProfileMethod != null) {
                return createProfileMethod.invoke(null, profile);
            }

            // Try constructor: ResolvableProfile.Static(Either.left(profile), null)
            if (profileStaticCtor != null) {
                Class<?> eitherClass = Class.forName("com.mojang.datafixers.util.Either");
                Object eitherLeft = eitherClass.getDeclaredMethod("left", Object.class).invoke(null, profile);
                Class<?>[] pt = profileStaticCtor.getParameterTypes();
                if (pt.length == 1) return profileStaticCtor.newInstance(eitherLeft);
                if (pt.length == 2) return profileStaticCtor.newInstance(eitherLeft, null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Get skin textures for a player. Mirrors Yarn's getSkinTextures().
     */
    private static Object getSkinTextures(UUID uuid, String name) {
        try {
            Object rp = createResolvableProfile(uuid, name);
            if (rp == null || cacheGetMethod == null || skinCacheRef == null) return null;

            Object entry = cacheGetMethod.invoke(skinCacheRef, rp);
            if (entry == null || entryGetTexturesMethod == null) return null;

            return entryGetTexturesMethod.invoke(entry);
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== Face Drawing ====================

    /**
     * Draw player face from skin textures.
     * Tries: PlayerSkinDrawer.draw() → manual blit from skin texture → fallback
     */
    private boolean drawFaceFromSkin(GuiGraphics ctx, UUID uuid, String name, int x, int y) {
        if (!skinSystemReady) initSkinSystem();

        Object skinTextures = getSkinTextures(uuid, name);

        if (skinTextures != null) {
            // Approach 1: Use PlayerSkinDrawer.draw(ctx, textures, x, y, size)
            if (drawerDrawMethod != null) {
                try {
                    drawerDrawMethod.invoke(null, ctx, skinTextures, x, y, FACE_SIZE);
                    return true;
                } catch (Exception ignored) {}
            }

            // Approach 2: Extract texture Identifier from SkinTextures and blit face manually
            if (skinTexturesTextureMethod != null) {
                try {
                    Identifier texture = (Identifier) skinTexturesTextureMethod.invoke(skinTextures);
                    if (texture != null) {
                        return blitFaceFromTexture(ctx, texture, x, y);
                    }
                } catch (Exception ignored) {}
            }

            // Approach 3: Search for any method on skinTextures that returns an Identifier
            try {
                for (Method m : skinTextures.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == Identifier.class) {
                        Identifier texture = (Identifier) m.invoke(skinTextures);
                        if (texture != null) {
                            skinTexturesTextureMethod = m; // cache for next time
                            return blitFaceFromTexture(ctx, texture, x, y);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Approach 4: Try DefaultSkinHelper fallback
        if (defaultSkinHelperMethod != null) {
            try {
                Object defaultTextures = defaultSkinHelperMethod.invoke(null, uuid);
                if (defaultTextures != null) {
                    if (drawerDrawMethod != null) {
                        try {
                            drawerDrawMethod.invoke(null, ctx, defaultTextures, x, y, FACE_SIZE);
                            return true;
                        } catch (Exception ignored) {}
                    }
                    for (Method m : defaultTextures.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && m.getReturnType() == Identifier.class) {
                            Identifier texture = (Identifier) m.invoke(defaultTextures);
                            if (texture != null) return blitFaceFromTexture(ctx, texture, x, y);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    /**
     * Blit the face portion (8x8 UV at 8,8 on a 64x64 skin texture) scaled to FACE_SIZE.
     */
    private static boolean blitFaceFromTexture(GuiGraphics ctx, Identifier texture, int x, int y) {
        try {
            // Try the scaled blit (8-arg or 10-arg with dest dimensions)
            java.lang.reflect.Method blit10 = ctx.getClass().getMethod("blit",
                Identifier.class, int.class, int.class, int.class, int.class,
                float.class, float.class, int.class, int.class, int.class);
            blit10.invoke(ctx, texture, x, y, FACE_SIZE, FACE_SIZE, 8, 8, 8, 8, 64, 64);
            return true;
        } catch (NoSuchMethodException e1) {
            try {
                // Standard 8-arg blit: blit(texture, x, y, u, v, w, h, texW, texH)
                int pad = (FACE_SIZE - 8) / 2;
                ctx.blit(texture, x + pad, y + pad, 8, 8, 8, 8, 64, 64);
                return true;
            } catch (Exception e2) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Head item fallback (when skin system unavailable) ====================

    private static final Map<UUID, ItemStack> HEAD_CACHE = new HashMap<>();

    private static ItemStack createHeadViaNbtCodec(UUID uuid, String name) {
        try {
            CompoundTag headNbt = new CompoundTag();
            headNbt.putString("id", "minecraft:player_head");
            headNbt.putInt("count", 1);
            CompoundTag components = new CompoundTag();
            CompoundTag profileTag = new CompoundTag();
            profileTag.putString("name", name != null ? name : "");
            long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
            ListTag idList = new ListTag();
            idList.add(IntTag.valueOf((int) (msb >> 32))); idList.add(IntTag.valueOf((int) msb));
            idList.add(IntTag.valueOf((int) (lsb >> 32))); idList.add(IntTag.valueOf((int) lsb));
            profileTag.put("id", idList);
            components.put("minecraft:profile", profileTag);
            headNbt.put("components", components);
            return ItemStack.CODEC.parse(NbtOps.INSTANCE, headNbt).result().orElse(new ItemStack(Items.PLAYER_HEAD));
        } catch (Exception ignored) {}
        return null;
    }

    private static ItemStack buildPlayerHead(UUID uuid, String name) {
        ItemStack h = createHeadViaNbtCodec(uuid, name);
        return h != null ? h : new ItemStack(Items.PLAYER_HEAD);
    }

    // ==================== Main Avatar Drawing ====================

    /**
     * Draw player avatar: tries skin face → head item fallback.
     */
    private void drawPlayerAvatar(GuiGraphics ctx, UUID uuid, String name, int x, int y) {
        // Dark background
        ctx.fill(x, y, x + FACE_SIZE, y + FACE_SIZE, 0xFF333333);
        drawBorder(ctx, x, y, FACE_SIZE, FACE_SIZE, 0xFF555555);

        if (uuid == null) {
            // No UUID - show first letter
            String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            ctx.drawCenteredString(font, Component.literal(initial), x + FACE_SIZE / 2, y + 5, 0xFFFFFFFF);
            return;
        }

        // Try 2D face from skin textures
        if (drawFaceFromSkin(ctx, uuid, name, x + 1, y + 1)) return;

        // Fallback: render head item
        ItemStack head = HEAD_CACHE.computeIfAbsent(uuid, u -> buildPlayerHead(u, name));
        ctx.renderItem(head, x + 1, y + 1);
    }

    // ==================== Screen methods ====================

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        if (selectedShop >= 0 && selectedShop < getTotalEntries())
            addRenderableWidget(Button.builder(Component.literal("Open"), btn -> openSelected()).bounds(px + W / 2 - 85, py + H - 25, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose()).bounds(px + W / 2 + 15, py + H - 25, 70, 20).build());
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

    @Override public boolean isPauseScreen() { return false; }
}
