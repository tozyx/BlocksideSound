package io.github.tozyx.blocksidefoley.fabric;

import io.github.tozyx.blocksidefoley.client.BlocksideFoleyClient;
import net.fabricmc.api.ClientModInitializer;

public final class BlocksideFoleyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlocksideFoleyClient.init();
    }
}
