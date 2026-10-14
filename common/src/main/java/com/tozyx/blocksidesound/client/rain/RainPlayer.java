package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Only manages this mod's local sounds; vanilla rain remains the background layer. */
public final class RainPlayer {
    private final RainPlaybackPlan plan = new RainPlaybackPlan();
    private final List<Active> active = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private ClientLevel level;
    private Vec3 previousListener;
    private long ticks;

    public void clear(SoundManager sounds) {
        for (Active playing : active) sounds.stop(playing.instance());
        active.clear();
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
                || world.getRainLevel(1.0F) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.WEATHER) <= 0) {
            clear(sounds);
            return;
        }
        long tick = ticks++;
        Set<RainPlaybackPlan.Group> occupied = new HashSet<>();
        Iterator<Active> iterator = active.iterator();
        while (iterator.hasNext()) {
            Active playing = iterator.next();
            RainSurface fresh = sampler.revalidate(world, listener, playing.surface());
            if (fresh == null || !playing.group().sound().accepts(fresh)
                    || fresh.position().distanceToSqr(listener) >= audibleDistanceSquared()
                    || (tick - playing.startedAt() > 2 && !sounds.isActive(playing.instance()))) {
                sounds.stop(playing.instance());
                iterator.remove();
            } else occupied.add(playing.group());
        }

        List<RainSurface> candidates = sampler.surfaces().stream()
                .filter(surface -> surface.position().distanceToSqr(listener) < audibleDistanceSquared()).toList();
        Set<RainSound> available = EnumSet.noneOf(RainSound.class);
        for (RainSound kind : RainSound.values()) {
            var event = sounds.getSoundEvent(kind.id());
            if (event != null && event.getWeight() > 0) available.add(kind);
        }
        for (RainPlaybackPlan.Request request : plan.poll(tick, candidates, config, available, occupied,
                config.rain.maxActiveSounds - active.size(), random)) {
            RainSound kind = request.group().sound();
            RainSurface fresh = sampler.revalidate(world, listener, request.surface());
            // Recheck the actual top surface, not just the cached block/material.
            if (fresh == null || !kind.accepts(fresh)) continue;
            var event = sounds.getSoundEvent(kind.id());
            if (event == null || event.getWeight() == 0) continue;
            float gain = (float) (config.volume * config.rain.volume * kind.settings(config).volume
                    * world.getRainLevel(1.0F));
            float pitch = 0.95F + random.nextFloat() * 0.1F;
            Vec3 position = fresh.position();
            SimpleSoundInstance instance = new SimpleSoundInstance(SoundEvent.createVariableRangeEvent(kind.id()),
                    SoundSource.WEATHER, gain, pitch, random, position.x, position.y + 0.05, position.z);
            sounds.play(instance);
            active.add(new Active(request.group(), fresh, instance, tick));
            if (config.rain.debugLogging) {
                BlocksideSound.LOGGER.info("Rain play: event={}, position={}, gain={}, active={}",
                        kind.id(), position, gain, active.size());
            }
        }
    }

    private static int audibleDistanceSquared() {
        return RainSound.ATTENUATION_DISTANCE * RainSound.ATTENUATION_DISTANCE;
    }

    private record Active(RainPlaybackPlan.Group group, RainSurface surface,
            SimpleSoundInstance instance, long startedAt) {}
}
