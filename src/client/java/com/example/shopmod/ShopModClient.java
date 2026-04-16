package com.example.shopmod;

import net.fabricmc.api.ClientModInitializer;

import com.example.shopmod.client.ClientPacketHandler;

public class ShopModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPacketHandler.register();
    }
}
