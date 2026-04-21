package com.example.shopmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.data.I18n;
import net.minecraft.client.Minecraft;

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
    }
}