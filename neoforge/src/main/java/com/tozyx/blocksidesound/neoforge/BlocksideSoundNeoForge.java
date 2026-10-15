package com.tozyx.blocksidesound.neoforge;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import com.tozyx.blocksidesound.client.gui.SoundConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = BlocksideSound.MOD_ID, dist = Dist.CLIENT)
public final class BlocksideSoundNeoForge {
    public BlocksideSoundNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new SoundConfigScreen(parent));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BlocksideSoundClient.init(FMLPaths.CONFIGDIR.get()));
    }
}
