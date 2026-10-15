package com.tozyx.blocksidesound.client;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.ConfigStore;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.io.IOException;
import com.tozyx.blocksidesound.client.rain.RainPlayer;
import com.tozyx.blocksidesound.client.rain.RainSampler;
import com.tozyx.blocksidesound.client.weather.WeatherPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Shared client lifecycle; loader modules supply only their configuration directory. */
public final class BlocksideSoundClient {
    private static final RainSampler RAIN = new RainSampler();
    private static final RainPlayer PLAYER = new RainPlayer();
    private static final WeatherPlayer WEATHER = new WeatherPlayer();
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
        WEATHER.clear(Minecraft.getInstance().getSoundManager());
        // A broken first-launch file still leaves valid defaults available.
        if (config.reload()) BlocksideSound.LOGGER.info("Rain sound config loaded");
        RAIN.configure(config.current().rain);
    }

    public static SoundConfig configSnapshot() {
        return config == null ? new SoundConfig() : config.snapshot();
    }

    /** Write successfully before replacing live settings or clearing any active sounds. */
    public static void saveConfig(SoundConfig draft) throws IOException {
        if (config == null) throw new IOException("Client configuration is not initialized");
        config.save(draft);
        PLAYER.clear(Minecraft.getInstance().getSoundManager());
        WEATHER.clear(Minecraft.getInstance().getSoundManager());
        RAIN.configure(config.current().rain);
    }

    public static boolean replaceWeatherSound(SoundInstance sound) {
        return config != null && WEATHER.replace(Minecraft.getInstance(), config.current(), sound);
    }

    public static void tick(Minecraft minecraft) {
        if (config == null) return;
        WEATHER.tick(minecraft, config.current());
        if (!config.current().enabled || minecraft.level == null || minecraft.player == null) {
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
