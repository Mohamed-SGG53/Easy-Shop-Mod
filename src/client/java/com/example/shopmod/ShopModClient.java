package com.example.shopmod;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.client.SkinHelper;
import com.example.shopmod.data.I18n;
import com.example.shopmod.network.ModPackets;
import com.example.shopmod.screen.RecoveryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class ShopModClient implements ClientModInitializer {

    private static String lastLang = "";

    @Override
    public void onInitializeClient() {
        ClientPacketHandler.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                I18n.detectLanguage(client.options.languageCode);
                lastLang = client.options.languageCode;
            } catch (Exception e) {}
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                String lang = client.options.languageCode;
                if (!lang.equals(lastLang)) {
                    lastLang = lang;
                    I18n.detectLanguage(lang);
                }
            } catch (Exception ignored) {}
        });

        // When player joins a server: upload own skin + request others' skins
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Clear skin cache for fresh start
            SkinHelper.clearCache();

            // Run in a background thread to avoid blocking
            new Thread("ShopMod-SkinUpload") {
                @Override
                public void run() {
                    try {
                        // Wait a moment for the connection to fully establish
                        Thread.sleep(1000);

                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player == null) return;

                        UUID uuid = mc.player.getUUID();
                        long most = uuid.getMostSignificantBits();
                        long least = uuid.getLeastSignificantBits();

                        // Step 1: Read own skin from config/Easy Shop Mod/My Skin/
                        byte[] mySkin = SkinHelper.readMySkinPng(uuid);

                        if (mySkin != null && mySkin.length > 0) {
                            // Also save to AllPlayerSkins so we can render our own face
                            SkinHelper.saveOtherSkin(uuid, mySkin);

                            // Upload to server
                            ClientPlayNetworking.send(new ModPackets.UploadSkinPayload(most, least, mySkin));
                            System.out.println("[ShopMod] Uploaded own skin to server (" + mySkin.length + " bytes)");
                        } else {
                            // No skin found - show warning with clickable path (must run on main thread)
                            if (mc.player != null) {
                                String msg = I18n.get("msg.skin_not_found");
                                String[] parts = msg.split("\\{path\\}", 2);

                                java.nio.file.Path skinDir = mc.gameDirectory.toPath()
                                    .resolve("config").resolve("Easy Shop Mod").resolve("My Skin");
                                String absolutePath = skinDir.toAbsolutePath().toString();
                                String displayPath = "config/Easy Shop Mod/My Skin/";

                                net.minecraft.network.chat.MutableComponent pathComp =
                                    net.minecraft.network.chat.Component.literal("[" + displayPath + "]")
                                    .setStyle(net.minecraft.network.chat.Style.EMPTY
                                        .withColor(net.minecraft.network.chat.TextColor.fromLegacyFormat(
                                            net.minecraft.ChatFormatting.YELLOW))
                                        .withUnderlined(true)
                                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenFile(absolutePath))
                                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                            net.minecraft.network.chat.Component.literal(
                                                I18n.get("skin.click_to_open")))));

                                net.minecraft.network.chat.MutableComponent fullMsg =
                                    net.minecraft.network.chat.Component.literal(parts[0]);
                                fullMsg.append(pathComp);
                                if (parts.length > 1) {
                                    fullMsg.append(net.minecraft.network.chat.Component.literal(parts[1]));
                                }

                                mc.execute(() -> mc.player.displayClientMessage(fullMsg, false));
                            }
                            System.out.println("[ShopMod] No skin PNG found in My Skin folder");
                        }

                        // Step 2: Request all other players' skins from server
                        ClientPlayNetworking.send(new ModPackets.RequestSkinsPayload(most, least));
                        System.out.println("[ShopMod] Requested other players' skins from server");

                    } catch (Exception e) {
                        System.out.println("[ShopMod] Skin upload error: " + e.getMessage());
                    }
                }
            }.start();
        });
    }
}
