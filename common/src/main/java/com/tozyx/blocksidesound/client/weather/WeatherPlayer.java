package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Owns only replacement weather sounds. The sound-engine hook executes after native delay scheduling. */
public final class WeatherPlayer {
    private final Map<WeatherSoundPolicy.Event, Loop> loops = new EnumMap<>(WeatherSoundPolicy.Event.class);
    private final List<Thunder> thunder = new ArrayList<>();
    private ClientLevel level;
    private Vec3 previousListener;
    private WeatherSoundPolicy.Event rainEvent;
    private float exposure;
    private long ticks;

    public void clear(SoundManager sounds) {
        clearRain(sounds);
        for (Thunder playing : thunder) sounds.stop(playing.sound());
        thunder.clear();
        level = null;
        previousListener = null;
        rainEvent = null;
        exposure = 0;
        ticks = 0;
    }

    private void clearRain(SoundManager sounds) {
        for (Loop loop : loops.values()) sounds.stop(loop.sound());
        loops.clear();
    }

    private void updateWorld(Minecraft minecraft) {
        Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
        if (level != minecraft.level || previousListener != null && previousListener.distanceToSqr(listener) > 64 * 64) {
            clear(minecraft.getSoundManager());
            level = minecraft.level;
        }
        previousListener = listener;
    }

    public void tick(Minecraft minecraft, SoundConfig config) {
        SoundManager sounds = minecraft.getSoundManager();
        if (!config.enabled || minecraft.level == null || minecraft.player == null) {
            clear(sounds);
            return;
        }
        updateWorld(minecraft);
        if (minecraft.isPaused()) return;
        if (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.WEATHER) <= 0) {
            clear(sounds);
            return;
        }
        long tick = ticks++;
        thunder.removeIf(playing -> tick - playing.started() > 20 && !sounds.isActive(playing.sound()));
        if (!config.weather.rain.enabled || config.volume * config.weather.rain.volume <= 0) {
            clearRain(sounds);
            return;
        }
        if (tick % 5 == 0) sampleRain(minecraft);
        float strength = minecraft.level.getRainLevel(1.0F);
        Iterator<Map.Entry<WeatherSoundPolicy.Event, Loop>> iterator = loops.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Loop loop = entry.getValue();
            if (loop.sound().isStopped() || !available(sounds, entry.getKey().replacement())
                    || tick - loop.started() > 20 && !sounds.isActive(loop.sound())) {
                sounds.stop(loop.sound());
                iterator.remove();
            } else {
                loop.sound().target(entry.getKey() == rainEvent ? target(config, strength) : 0);
            }
        }
        if (rainEvent == null || target(config, strength) <= 0 || loops.containsKey(rainEvent)) return;
        var decision = WeatherSoundPolicy.select(rainEvent.original(), config, id -> available(sounds, id));
        if (decision.action() != WeatherSoundPolicy.Action.REPLACE) return;
        BackgroundRainSound sound = new BackgroundRainSound(rainEvent);
        sound.target(target(config, strength));
        sounds.play(sound);
        loops.put(rainEvent, new Loop(sound, tick));
    }

    private float target(SoundConfig config, float strength) {
        float shelterGain = rainEvent == WeatherSoundPolicy.Event.RAIN_ABOVE ? 0.35F : 1;
        return (float) (config.volume * config.weather.rain.volume * strength * exposure * shelterGain);
    }

    /** Twenty-five loaded heightmap columns every five ticks, independent of material settings. */
    private void sampleRain(Minecraft minecraft) {
        ClientLevel world = minecraft.level;
        BlockPos listener = BlockPos.containing(minecraft.gameRenderer.getMainCamera().getPosition());
        int wet = 0;
        for (int dx = -8; dx <= 8; dx += 4) {
            for (int dz = -8; dz <= 8; dz += 4) {
                int x = listener.getX() + dx;
                int z = listener.getZ() + dz;
                if (!world.hasChunkAt(new BlockPos(x, listener.getY(), z))) continue;
                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y <= world.getMinBuildHeight() || Math.abs(y - listener.getY()) > 16) continue;
                BlockPos surface = new BlockPos(x, y, z);
                if (world.getBiome(surface).value().getPrecipitationAt(surface) == Biome.Precipitation.RAIN) wet++;
            }
        }
        exposure = wet / 25.0F;
        rainEvent = wet == 0 ? null : world.canSeeSky(listener)
                ? WeatherSoundPolicy.Event.RAIN : WeatherSoundPolicy.Event.RAIN_ABOVE;
    }

    /** Return true only when this instance has been replaced or deliberately muted. */
    public boolean replace(Minecraft minecraft, SoundConfig config, SoundInstance original) {
        if (minecraft.level == null || minecraft.player == null || original.getSource() != SoundSource.WEATHER) return false;
        SoundManager sounds = minecraft.getSoundManager();
        var decision = WeatherSoundPolicy.select(original.getLocation(), config, id -> available(sounds, id));
        if (decision.action() == WeatherSoundPolicy.Action.KEEP_ORIGINAL) return false;
        if (decision.action() == WeatherSoundPolicy.Action.MUTE) return true;
        if (!original.getLocation().equals(WeatherSoundPolicy.Event.THUNDER.original())) return true;
        if (!original.canPlaySound()) return true;
        // Resolve native range and gain once, at the already-scheduled native playback time.
        if (original.resolve(sounds) == null || original.getSound() == SoundManager.EMPTY_SOUND
                || original.getSound() == SoundManager.INTENTIONALLY_EMPTY_SOUND) return false;
        updateWorld(minecraft);
        Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
        double distance = original.isRelative() ? 0 : listener.distanceToSqr(original.getX(), original.getY(), original.getZ());
        ResourceLocation profile = ThunderSound.profile(distance);
        ResourceLocation event = available(sounds, profile) ? profile : decision.event();
        ThunderSound sound = new ThunderSound(original, event, decision.volumeMultiplier());
        // Bound extreme command/mod-driven storms without tying thunder to the material-drop budget.
        if (thunder.size() >= 8) sounds.stop(thunder.removeFirst().sound());
        sounds.play(sound);
        thunder.add(new Thunder(sound, ticks));
        return true;
    }

    public static boolean available(SoundManager sounds, ResourceLocation id) {
        var event = sounds.getSoundEvent(id);
        return event != null && event.getWeight() > 0;
    }

    private record Loop(BackgroundRainSound sound, long started) {}
    private record Thunder(ThunderSound sound, long started) {}
}
