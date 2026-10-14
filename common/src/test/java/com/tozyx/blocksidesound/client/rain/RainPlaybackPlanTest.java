package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RainPlaybackPlanTest {
    private static final Set<RainSound> AVAILABLE = Set.of(RainSound.WOOD, RainSound.METAL, RainSound.GLASS,
            RainSound.FOLIAGE, RainSound.GRANULAR, RainSound.WATER, RainSound.SOFT, RainSound.PLANT_DRIP);
    private static SoundConfig certainConfig() {
        SoundConfig config = new SoundConfig();
        for (var settings : config.rain.materials.values()) {
            settings.triggerProbability = 1;
            settings.attemptIntervalTicks = 1;
        }
        config.rain.plantDrips.triggerProbability = 1;
        config.rain.plantDrips.attemptIntervalTicks = 1;
        return config;
    }

    private static RainSurface surface(int x, int z, RainMaterial material, boolean enhanced, boolean drip) {
        return new RainSurface(new BlockPos(x, 64, z), new Vec3(x + 0.5, 65, z + 0.5),
                new MaterialResolver.Match(material, enhanced, drip), 0);
    }

    @Test void retainsRealPositionsAndSeparatesNegativeRegions() {
        var left = surface(-1, 0, RainMaterial.WOOD, true, false);
        var right = surface(8, 0, RainMaterial.WOOD, true, false);
        var plan = new RainPlaybackPlan();
        var random = RandomSource.create(2);
        var first = plan.poll(0, List.of(left, right), certainConfig(), AVAILABLE, Set.of(), 8, random);
        assertEquals(1, first.size());
        var second = plan.poll(1, List.of(left, right), certainConfig(), AVAILABLE, Set.of(first.getFirst().group()), 8, random);
        assertEquals(1, second.size());
        assertNotEquals(first.getFirst().group(), second.getFirst().group());
        assertEquals(Set.of(left.position(), right.position()),
                Set.of(first.getFirst().surface().position(), second.getFirst().surface().position()));
        assertEquals(Set.of(-1, 1), Set.of(first.getFirst().group().x(), second.getFirst().group().x()));
    }

    @Test void densityDoesNotMultiplyAttemptsAndIntervalIsRespected() {
        var config = certainConfig();
        config.rain.materials.get("wood").attemptIntervalTicks = 7;
        var plan = new RainPlaybackPlan();
        var random = RandomSource.create(3);
        var surfaces = new ArrayList<RainSurface>();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++)
            surfaces.add(surface(x, z, RainMaterial.WOOD, true, false));
        long last = -100;
        int count = 0;
        for (long tick = 0; tick < 70; tick++) {
            var requests = plan.poll(tick, surfaces, config, AVAILABLE, Set.of(), 8, random);
            assertTrue(requests.size() <= 1);
            if (!requests.isEmpty()) {
                assertTrue(tick - last >= 7);
                last = tick;
                count++;
            }
        }
        assertEquals(10, count);
    }

    @Test void zeroProbabilityDisabledAndMissingMaterialsDoNotPlay() {
        var config = certainConfig();
        config.rain.materials.get("wood").triggerProbability = 0;
        config.rain.materials.get("metal").enabled = false;
        var requests = new RainPlaybackPlan().poll(0, List.of(
                surface(0, 0, RainMaterial.WOOD, true, false),
                surface(1, 0, RainMaterial.METAL, true, false),
                surface(2, 0, RainMaterial.STONE, true, false),
                surface(3, 0, RainMaterial.GLASS, false, false),
                surface(4, 0, RainMaterial.VANILLA, false, false)), config, AVAILABLE, Set.of(), 8, RandomSource.create(4));
        assertTrue(requests.isEmpty());
    }

    @Test void plantLayerRemainsIndependentOfFoliageEnhancement() {
        var plant = surface(0, 0, RainMaterial.FOLIAGE, false, true);
        var config = certainConfig();
        config.rain.materials.get("foliage").enabled = false;
        var plan = new RainPlaybackPlan();
        var requests = plan.poll(0, List.of(plant), config, AVAILABLE, Set.of(), 8, RandomSource.create(5));
        assertEquals(RainSound.PLANT_DRIP, requests.getFirst().group().sound());
        config.rain.plantDrips.enabled = false;
        assertTrue(plan.poll(1, List.of(plant), config, AVAILABLE, Set.of(), 8, RandomSource.create(5)).isEmpty());
    }

    @Test void globalSlotsAndPerKindOverlapLimitsApplyAtProbabilityOne() {
        var config = certainConfig();
        var plan = new RainPlaybackPlan();
        var occupied = Set.of(new RainPlaybackPlan.Group(0, 0, RainSound.WOOD),
                new RainPlaybackPlan.Group(1, 0, RainSound.WOOD));
        var surfaces = List.of(surface(16, 0, RainMaterial.WOOD, true, false),
                surface(0, 8, RainMaterial.METAL, true, false),
                surface(8, 8, RainMaterial.GLASS, true, false));
        assertTrue(plan.poll(0, surfaces, config, AVAILABLE, occupied, 0, RandomSource.create(6)).isEmpty());
        var requests = plan.poll(1, surfaces, config, AVAILABLE, occupied, 1, RandomSource.create(6));
        assertEquals(1, requests.size());
        assertNotEquals(RainSound.WOOD, requests.getFirst().group().sound());
        assertTrue(plan.poll(2, surfaces, config, AVAILABLE, occupied, 8, RandomSource.create(6)).size() <= 2);
    }

    @Test void scarceMaterialGetsAnOpportunityBesideManyWoodRegions() {
        var surfaces = new ArrayList<RainSurface>();
        for (int i = 0; i < 20; i++) surfaces.add(surface(i * 8, 0, RainMaterial.WOOD, true, false));
        surfaces.add(surface(0, 8, RainMaterial.METAL, true, false));
        var config = certainConfig();
        var plan = new RainPlaybackPlan();
        var random = RandomSource.create(7);
        var first = plan.poll(0, surfaces, config, AVAILABLE, Set.of(), 1, random).getFirst();
        var second = plan.poll(1, surfaces, config, AVAILABLE, Set.of(), 1, random).getFirst();
        assertEquals(Set.of(RainSound.WOOD, RainSound.METAL), Set.of(first.group().sound(), second.group().sound()));
    }

    @Test void newMaterialResourcesCanBecomeAvailableWithoutChangingTheScheduler() {
        var stone = surface(0, 0, RainMaterial.STONE, true, false);
        var plan = new RainPlaybackPlan();
        var config = certainConfig();
        assertTrue(plan.poll(0, List.of(stone), config, AVAILABLE, Set.of(), 8, RandomSource.create(9)).isEmpty());
        var requests = plan.poll(1, List.of(stone), config, Set.of(RainSound.STONE),
                Set.of(), 8, RandomSource.create(9));
        assertEquals(RainSound.STONE, requests.getFirst().group().sound());
    }

    @Test void worldOrConfigResetDropsOldTimers() {
        var config = certainConfig();
        var surfaces = List.of(surface(0, 0, RainMaterial.WOOD, true, false));
        var plan = new RainPlaybackPlan();
        assertEquals(1, plan.poll(1000, surfaces, config, AVAILABLE, Set.of(), 8, RandomSource.create(8)).size());
        plan.clear();
        assertEquals(1, plan.poll(0, surfaces, config, AVAILABLE, Set.of(), 8, RandomSource.create(8)).size());
        config.rain.enabled = false;
        assertTrue(plan.poll(1, surfaces, config, AVAILABLE, Set.of(), 8, RandomSource.create(8)).isEmpty());
    }
}
