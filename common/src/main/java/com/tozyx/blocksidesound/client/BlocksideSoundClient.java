package com.tozyx.blocksidesound.client;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.ConfigStore;
import com.tozyx.blocksidesound.client.rain.RainPlayer;
import com.tozyx.blocksidesound.client.rain.RainSampler;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Shared client lifecycle; loader modules supply only their configuration directory. */
public final class BlocksideSoundClient {
    private static final RainSampler RAIN = new RainSampler();
    private static final RainPlayer PLAYER = new RainPlayer();
    private static ConfigStore config;
    private BlocksideSoundClient() {
    }

    public static void init(Path configDirectory) {
        config = new ConfigStore(configDirectory);
        reloadConfig();
        BlocksideSound.LOGGER.info("BlocksideSound client initialized ({})", BlocksideSound.id("client"));
    }

    public static void reloadConfig() {
        if (config == null) return;
        PLAYER.clear(Minecraft.getInstance().getSoundManager());
        // A broken first-launch file still leaves valid defaults available.
        if (config.reload()) BlocksideSound.LOGGER.info("Rain sound config loaded");
        RAIN.configure(config.current().rain);
    }

    public static void tick(Minecraft minecraft) {
        if (config == null) return;
        if (minecraft.level == null || minecraft.player == null) {
            RAIN.clear();
            PLAYER.clear(minecraft.getSoundManager());
            return;
        }
        if (minecraft.isPaused()) return;
        var listener = minecraft.gameRenderer.getMainCamera().getPosition();
        RAIN.tick(minecraft.level, listener);
        PLAYER.tick(minecraft, RAIN, config.current(), listener);
    }
}
