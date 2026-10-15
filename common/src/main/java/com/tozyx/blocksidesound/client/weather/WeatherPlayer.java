package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Owns only replacement weather sounds. The sound-engine hook executes after native delay scheduling. */
public final class WeatherPlayer {
    private final List<Rain> rain = new ArrayList<>();
    private final List<Thunder> thunder = new ArrayList<>();
    private ClientLevel level;
    private Vec3 previousListener;
    private long ticks;

    public void clear(SoundManager sounds) {
        clearRain(sounds);
        for (Thunder playing : thunder) sounds.stop(playing.sound());
        thunder.clear();
        level = null;
        previousListener = null;
        ticks = 0;
    }

    private void clearRain(SoundManager sounds) {
        for (Rain playing : rain) sounds.stop(playing.sound());
        rain.clear();
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
        rain.removeIf(playing -> tick - playing.started() > 20 && !sounds.isActive(playing.sound()));
        // Background starts only in replace(), from a genuine native weather.rain / rain.above request.
    }

    /** Return true only when this instance has been replaced or deliberately muted. */
    public boolean replace(Minecraft minecraft, SoundConfig config, SoundInstance original) {
        if (minecraft.level == null || minecraft.player == null || original.getSource() != SoundSource.WEATHER) return false;
        SoundManager sounds = minecraft.getSoundManager();
        var decision = WeatherSoundPolicy.select(original.getLocation(), config, id -> available(sounds, id));
        if (decision.action() == WeatherSoundPolicy.Action.KEEP_ORIGINAL) return false;
        if (decision.action() == WeatherSoundPolicy.Action.MUTE) return true;
        if (!original.canPlaySound()) return true;
        if (!original.getLocation().equals(WeatherSoundPolicy.Event.THUNDER.original())) {
            updateWorld(minecraft);
            // Vanilla's above check only compares heights. Rain outside a wall can still arrive
            // as weather.rain: use the filtered profile when the actual path is obstructed.
            ResourceLocation event = decision.event();
            if (original.getLocation().equals(WeatherSoundPolicy.Event.RAIN.original())
                    && rainObstructed(minecraft, original)) {
                event = WeatherSoundPolicy.Event.RAIN_ABOVE.replacement();
                if (!available(sounds, event)) return false;
            }
            BackgroundRainSound sound = new BackgroundRainSound(original, event, decision.volumeMultiplier());
            if (sound.resolve(sounds) == null || sound.getSound() == SoundManager.EMPTY_SOUND
                    || sound.getSound() == SoundManager.INTENTIONALLY_EMPTY_SOUND) return false;
            // Guard command/mod-driven floods without stopping older native-triggered recordings.
            if (rain.size() >= 64) return true;
            sounds.play(sound);
            rain.add(new Rain(sound, ticks));
            return true;
        }
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

    private static boolean rainObstructed(Minecraft minecraft, SoundInstance original) {
        if (original.isRelative() || original.getAttenuation() == SoundInstance.Attenuation.NONE) return false;
        Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 source = new Vec3(original.getX(), original.getY(), original.getZ());
        // Native rain is local. Do not trace arbitrary long command/mod requests or load chunks.
        if (listener.distanceToSqr(source) > 32 * 32) return false;
        int minX = SectionPos.blockToSectionCoord((int) Math.floor(Math.min(listener.x, source.x)));
        int maxX = SectionPos.blockToSectionCoord((int) Math.floor(Math.max(listener.x, source.x)));
        int minZ = SectionPos.blockToSectionCoord((int) Math.floor(Math.min(listener.z, source.z)));
        int maxZ = SectionPos.blockToSectionCoord((int) Math.floor(Math.max(listener.z, source.z)));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!minecraft.level.hasChunk(x, z)) return false;
            }
        }
        return RainOcclusion.blocked(minecraft.level, listener, source);
    }

    public static boolean available(SoundManager sounds, ResourceLocation id) {
        var event = sounds.getSoundEvent(id);
        return event != null && event.getWeight() > 0;
    }

    private record Rain(BackgroundRainSound sound, long started) {}
    private record Thunder(ThunderSound sound, long started) {}
}
