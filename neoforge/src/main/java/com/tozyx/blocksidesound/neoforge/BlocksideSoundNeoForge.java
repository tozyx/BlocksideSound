package com.tozyx.blocksidesound.neoforge;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = BlocksideSound.MOD_ID, dist = Dist.CLIENT)
public final class BlocksideSoundNeoForge {
    public BlocksideSoundNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BlocksideSoundClient.init(FMLPaths.CONFIGDIR.get()));
    }
}
