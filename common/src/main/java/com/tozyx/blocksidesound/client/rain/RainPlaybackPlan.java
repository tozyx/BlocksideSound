package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Staggered independent one-shots: cooldowns limit starts, never the lifetime of existing takes. */
public final class RainPlaybackPlan {
    static final int BLOCK_COOLDOWN_TICKS = 30;
    static final int DISPATCH_SPACING_TICKS = 2;
    static final int MAX_VOICES_PER_KIND = 8;
    static final int MAX_VOICES = 64;
    private final Map<Group, Long> nextStart = new HashMap<>();
    private final Map<Source, Long> blockReady = new HashMap<>();
    private long nextDispatch;

    public void clear() {
        nextStart.clear();
        blockReady.clear();
        nextDispatch = 0;
    }

    void retry(Group group) {
        nextStart.remove(group);
    }

    /** Slots limit distinct material layers. Each layer may contain several complete overlapping takes. */
    public List<Request> poll(long tick, Collection<RainSurface> surfaces, SoundConfig config,
            Set<RainSound> available, Collection<Group> occupied, int slots, RandomSource random, Vec3 listener) {
        if (!config.enabled || !config.rain.enabled || config.volume <= 0 || config.rain.volume <= 0) {
            clear();
            return List.of();
        }
        Map<Group, List<RainSurface>> groups = new HashMap<>();
        for (RainSurface surface : surfaces) {
            RainSound kind = RainSound.forMaterial(surface.match().material());
            if (kind != null && available.contains(kind)) add(groups, surface, kind, config);
            if (available.contains(RainSound.PLANT_DRIP)) add(groups, surface, RainSound.PLANT_DRIP, config);
        }
        nextStart.keySet().retainAll(groups.keySet());
        Set<Source> validSources = new HashSet<>();
        groups.forEach((group, candidates) -> candidates.forEach(surface ->
                validSources.add(new Source(group.sound(), surface.block().asLong()))));
        blockReady.keySet().retainAll(validSources);
        if (tick < nextDispatch || occupied.size() >= MAX_VOICES) return List.of();
        Map<Group, Integer> counts = new HashMap<>();
        for (Group group : occupied) counts.merge(group, 1, Integer::sum);
        List<Group> ready = new ArrayList<>(groups.keySet());
        ready.sort(Comparator.<Group>comparingInt(group -> counts.containsKey(group) ? 1 : 0)
                .thenComparingInt(group -> group.sound() == RainSound.PLANT_DRIP ? 1 : 0)
                .thenComparingLong(group -> nextStart.getOrDefault(group, Long.MIN_VALUE))
                .thenComparingDouble(group -> groups.get(group).stream()
                        .mapToDouble(surface -> surface.position().distanceToSqr(listener)).min().orElseThrow())
                .thenComparingInt(group -> group.sound().ordinal())
                .thenComparingInt(Group::x).thenComparingInt(Group::z));
        for (Group group : ready) {
            boolean drip = group.sound() == RainSound.PLANT_DRIP;
            int voices = counts.getOrDefault(group, 0);
            if (voices >= (drip ? 1 : MAX_VOICES_PER_KIND) || voices == 0 && slots <= 0) continue;
            // Do not let separate drip regions occupy every available material layer.
            if (drip && occupied.stream().anyMatch(other -> other.sound() == RainSound.PLANT_DRIP)) continue;
            SoundConfig.Material settings = group.sound().settings(config);
            long due = nextStart.computeIfAbsent(group, key -> drip
                    ? tick + random.nextInt(settings.attemptIntervalTicks) : tick);
            if (tick < due) continue;
            List<RainSurface> eligible = groups.get(group).stream().filter(surface ->
                    drip || tick >= blockReady.getOrDefault(new Source(group.sound(), surface.block().asLong()), 0L)).toList();
            if (eligible.isEmpty()) continue;
            if (drip) {
                nextStart.put(group, tick + settings.attemptIntervalTicks);
                if (random.nextDouble() >= settings.triggerProbability) continue;
            }
            double nearest = eligible.stream().mapToDouble(surface ->
                    Math.sqrt(surface.position().distanceToSqr(listener))).min().orElseThrow();
            List<RainSurface> local = eligible.stream().filter(surface ->
                    surface.position().distanceToSqr(listener) <= (nearest + 4) * (nearest + 4)).toList();
            RainSurface surface = chooseSurface(local, listener, random);
            if (!drip) {
                nextStart.put(group, tick + BLOCK_COOLDOWN_TICKS + random.nextInt(5));
                blockReady.put(new Source(group.sound(), surface.block().asLong()),
                        tick + BLOCK_COOLDOWN_TICKS + random.nextInt(3));
            }
            nextDispatch = tick + DISPATCH_SPACING_TICKS;
            return List.of(new Request(group, surface));
        }
        return List.of();
    }

    static RainSurface chooseSurface(List<RainSurface> candidates, Vec3 listener, RandomSource random) {
        double total = 0;
        for (RainSurface surface : candidates) total += 1.0 / (1.0 + surface.position().distanceToSqr(listener));
        double choice = random.nextDouble() * total;
        for (RainSurface surface : candidates) {
            choice -= 1.0 / (1.0 + surface.position().distanceToSqr(listener));
            if (choice <= 0) return surface;
        }
        return candidates.getLast();
    }

    private static void add(Map<Group, List<RainSurface>> groups, RainSurface surface,
            RainSound sound, SoundConfig config) {
        if (!sound.accepts(surface)) return;
        SoundConfig.Material settings = sound.settings(config);
        if (!settings.enabled || settings.volume <= 0
                || sound == RainSound.PLANT_DRIP && settings.triggerProbability <= 0) return;
        Group group = sound == RainSound.PLANT_DRIP
                ? new Group(Math.floorDiv(surface.block().getX(), 8), Math.floorDiv(surface.block().getZ(), 8), sound)
                : new Group(0, 0, sound);
        groups.computeIfAbsent(group, key -> new ArrayList<>()).add(surface);
    }

    private record Source(RainSound sound, long block) {}
    public record Group(int x, int z, RainSound sound) {}
    public record Request(Group group, RainSurface surface) {}
}
