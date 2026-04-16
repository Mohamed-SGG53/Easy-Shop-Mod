package com.example.shopmod;

import com.example.shopmod.client.ClientPacketHandler;
import net.fabricmc.api.ClientModInitializer;

public class ShopModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPacketHandler.register();
    }
}
