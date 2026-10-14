package com.tozyx.blocksidesound.fabric;

import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class BlocksideSoundFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlocksideSoundClient.init(FabricLoader.getInstance().getConfigDir());
    }
}
