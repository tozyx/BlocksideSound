package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;

/** Tick-based scheduling, independent of the sound engine and scan density. */
public final class RainPlaybackPlan {
    private static final int REGION_SIZE = 8;
    private static final int MAX_PER_KIND = 2;
    private final Map<Group, Long> nextAttempt = new HashMap<>();
    private final Map<RainSound, Long> lastPlayed = new EnumMap<>(RainSound.class);

    public void clear() {
        nextAttempt.clear();
        lastPlayed.clear();
    }

    public List<Request> poll(long tick, Collection<RainSurface> surfaces, SoundConfig config,
            Set<RainSound> available, Set<Group> occupied, int slots, RandomSource random) {
        if (!config.rain.enabled || config.volume <= 0 || config.rain.volume <= 0) {
            clear();
            return List.of();
        }
        Map<Group, List<RainSurface>> groups = new HashMap<>();
        for (RainSurface surface : surfaces) {
            RainSound material = RainSound.forMaterial(surface.match().material());
            if (material != null && available.contains(material)) add(groups, surface, material, config);
            if (available.contains(RainSound.PLANT_DRIP)) add(groups, surface, RainSound.PLANT_DRIP, config);
        }
        // No timers survive after their surface region disappears.
        nextAttempt.keySet().retainAll(groups.keySet());
        for (Group group : groups.keySet()) {
            nextAttempt.computeIfAbsent(group,
                    key -> tick + random.nextInt(key.sound().settings(config).attemptIntervalTicks));
        }
        if (slots <= 0) return List.of();

        List<Group> ready = new ArrayList<>(groups.keySet());
        // Randomize ties, then give infrequent materials an opportunity in mixed scenes.
        for (int i = ready.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Group other = ready.get(j);
            ready.set(j, ready.get(i));
            ready.set(i, other);
        }
        ready.sort(Comparator.comparingLong(group -> lastPlayed.getOrDefault(group.sound(), Long.MIN_VALUE)));
        Map<RainSound, Integer> activeCounts = new EnumMap<>(RainSound.class);
        for (Group group : occupied) activeCounts.merge(group.sound(), 1, Integer::sum);
        Set<RainSound> started = new HashSet<>();
        List<Request> requests = new ArrayList<>();
        for (Group group : ready) {
            if (requests.size() >= Math.min(2, slots)) break;
            if (occupied.contains(group) || tick < nextAttempt.get(group)
                    || activeCounts.getOrDefault(group.sound(), 0) >= MAX_PER_KIND
                    || started.contains(group.sound())) continue;
            SoundConfig.Material settings = group.sound().settings(config);
            nextAttempt.put(group, tick + settings.attemptIntervalTicks);
            if (random.nextDouble() >= settings.triggerProbability) continue;
            List<RainSurface> candidates = groups.get(group);
            RainSurface surface = candidates.get(random.nextInt(candidates.size()));
            requests.add(new Request(group, surface));
            lastPlayed.put(group.sound(), tick);
            started.add(group.sound());
            activeCounts.merge(group.sound(), 1, Integer::sum);
        }
        return List.copyOf(requests);
    }

    private static void add(Map<Group, List<RainSurface>> groups, RainSurface surface,
            RainSound sound, SoundConfig config) {
        if (sound == null || !sound.accepts(surface)) return;
        SoundConfig.Material settings = sound.settings(config);
        if (!settings.enabled || settings.volume <= 0 || settings.triggerProbability <= 0) return;
        Group group = new Group(Math.floorDiv(surface.block().getX(), REGION_SIZE),
                Math.floorDiv(surface.block().getZ(), REGION_SIZE), sound);
        groups.computeIfAbsent(group, key -> new ArrayList<>()).add(surface);
    }

    public record Group(int x, int z, RainSound sound) {}
    public record Request(Group group, RainSurface surface) {}
}
