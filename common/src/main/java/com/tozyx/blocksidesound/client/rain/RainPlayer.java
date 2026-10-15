package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Maintains positional material emitters; background rain is managed by WeatherPlayer. */
public final class RainPlayer {
    private final Map<ResourceLocation, OptionalDouble> durations = new HashMap<>();
    private final RainPlaybackPlan plan = new RainPlaybackPlan();
    private final List<Active> active = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private ClientLevel level;
    private Vec3 previousListener;
    private long ticks;

    public void clear(SoundManager sounds) {
        for (Active playing : active) sounds.stop(playing.instance());
        active.clear();
        durations.clear();
        plan.clear();
        level = null;
        previousListener = null;
        ticks = 0;
    }

    public void tick(Minecraft minecraft, RainSampler sampler, SoundConfig config, Vec3 listener) {
        SoundManager sounds = minecraft.getSoundManager();
        ClientLevel world = minecraft.level;
        if (world != level || (previousListener != null
                && previousListener.distanceToSqr(listener) > config.rain.radius * config.rain.radius)) {
            clear(sounds);
            level = world;
        }
        previousListener = listener;
        if (world == null || !config.rain.enabled || config.volume <= 0 || config.rain.volume <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.WEATHER) <= 0) {
            clear(sounds);
            return;
        }
        long tick = ticks++;
        List<RainPlaybackPlan.Group> occupied = new ArrayList<>();
        Iterator<Active> iterator = active.iterator();
        while (iterator.hasNext()) {
            Active playing = iterator.next();
            if (playing.instance().isStopped()
                    || tick - playing.startedAt() > 20 && !sounds.isActive(playing.instance())) {
                sounds.stop(playing.instance());
                iterator.remove();
                continue;
            }
            RainSurface fresh = sampler.revalidate(world, listener, playing.surface());
            if (fresh == null || !playing.group().sound().accepts(fresh)
                    || fresh.position().distanceToSqr(listener) >= audibleDistanceSquared()) {
                if (!playing.instance().releasing()) plan.retry(playing.group());
                playing.instance().release();
            } else {
                playing.instance().gain(gain(config, world, playing.group().sound()) * playing.layerGain());
            }
            occupied.add(playing.group()); // Existing takes continue while the next independent voice starts.
        }

        List<RainSurface> candidates = sampler.surfaces().stream()
                .filter(surface -> surface.position().distanceToSqr(listener) < audibleDistanceSquared()).toList();
        Set<RainSound> available = EnumSet.noneOf(RainSound.class);
        for (RainSound kind : RainSound.values()) {
            var event = sounds.getSoundEvent(kind.id());
            if (event != null && event.getWeight() > 0) available.add(kind);
        }
        for (RainPlaybackPlan.Request request : plan.poll(tick, candidates, config, available, occupied,
                config.rain.maxActiveSounds - new HashSet<>(occupied).size(), random, listener)) {
            RainSound kind = request.group().sound();
            RainSurface fresh = sampler.revalidate(world, listener, request.surface());
            // Recheck the actual top surface, not just the cached block/material.
            if (fresh == null || !kind.accepts(fresh)) continue;
            var event = sounds.getSoundEvent(kind.id());
            if (event == null || event.getWeight() == 0) continue;
            float gain = gain(config, world, kind);
            float pitch = 0.95F + random.nextFloat() * 0.1F;
            Vec3 position = fresh.position();
            MaterialRainSound instance = new MaterialRainSound(kind, gain, pitch, random, position);
            if (instance.resolve(sounds) == null || instance.getSound() == SoundManager.EMPTY_SOUND
                    || instance.getSound() == SoundManager.INTENTIONALLY_EMPTY_SOUND) continue;
            OptionalDouble seconds = durations.computeIfAbsent(instance.getSound().getPath(),
                    key -> duration(minecraft, key));
            float layerGain = kind == RainSound.PLANT_DRIP ? 1 : layerGain(seconds, instance.getPitch());
            instance.gain(gain * layerGain);
            sounds.play(instance);
            active.add(new Active(request.group(), fresh, instance, tick, layerGain));
            if (config.rain.debugLogging) {
                BlocksideSound.LOGGER.info("Rain play: event={}, position={}, gain={}, active={}",
                        kind.id(), position, gain * layerGain, active.size());
            }
        }
    }

    // Fixed per-voice compensation, not a live voice-count divisor that pumps whenever a take ends.
    static float layerGain(OptionalDouble seconds, float pitch) {
        double duration = seconds.orElse(5) / Math.clamp(pitch, 0.5F, 2F);
        return (float) Math.clamp(Math.sqrt(1.6 / Math.max(.1, duration)), .25, 1);
    }

    private static OptionalDouble duration(Minecraft minecraft, ResourceLocation path) {
        try {
            var resource = minecraft.getResourceManager().getResource(path);
            if (resource.isPresent()) {
                try (var input = resource.get().open()) { return OggDuration.seconds(input); }
            }
        } catch (IOException exception) {
            BlocksideSound.LOGGER.debug("Could not inspect rain audio for layer gain: {}", path, exception);
        }
        return OptionalDouble.empty();
    }

    private static float gain(SoundConfig config, ClientLevel world, RainSound kind) {
        return (float) (config.volume * config.rain.volume * kind.settings(config).volume
                * world.getRainLevel(1.0F));
    }

    private static int audibleDistanceSquared() {
        return RainSound.ATTENUATION_DISTANCE * RainSound.ATTENUATION_DISTANCE;
    }

    private record Active(RainPlaybackPlan.Group group, RainSurface surface,
            MaterialRainSound instance, long startedAt, float layerGain) {}
}
