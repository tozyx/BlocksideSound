package io.github.tozyx.blocksidefoley.neoforge;

import io.github.tozyx.blocksidefoley.BlocksideFoley;
import io.github.tozyx.blocksidefoley.client.BlocksideFoleyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = BlocksideFoley.MOD_ID, dist = Dist.CLIENT)
public final class BlocksideFoleyNeoForge {
    public BlocksideFoleyNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(BlocksideFoleyClient::init);
    }
}
