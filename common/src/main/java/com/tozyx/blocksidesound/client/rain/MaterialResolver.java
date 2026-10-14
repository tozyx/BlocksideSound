package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.client.config.BlockSelector;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class MaterialResolver {
    private final SoundConfig.Rain config;
    private final List<Rule> overrides = new ArrayList<>();
    private final Map<RainMaterial, List<BlockSelector>> exclusions = new EnumMap<>(RainMaterial.class);
    private final List<BlockSelector> dripExclusions;

    public MaterialResolver(SoundConfig.Rain config) {
        this.config = config;
        for (SoundConfig.Override override : config.overrides) {
            overrides.add(new Rule(BlockSelector.parse(override.match), RainMaterial.byId(override.material)));
        }
        // Exact IDs precede tags. Stable order resolves ties in the same layer.
        overrides.sort(java.util.Comparator.comparing(rule -> rule.selector.tag()));
        for (RainMaterial material : RainMaterial.values()) {
            if (material != RainMaterial.VANILLA) {
                exclusions.put(material, config.materials.get(material.id()).exclude.stream().map(BlockSelector::parse).toList());
            }
        }
        dripExclusions = config.plantDrips.exclude.stream().map(BlockSelector::parse).toList();
    }

    public Match resolve(BlockState state, boolean waterSurface) {
        RainMaterial material = classify(state, waterSurface);
        // Once classified, an exclusion must not silently reclassify the block.
        boolean enabled = material != RainMaterial.VANILLA && config.enabled
                && config.materials.get(material.id()).enabled
                && exclusions.get(material).stream().noneMatch(selector -> selector.matches(state));
        boolean plantDrips = !waterSurface && supportsPlantDrips(state) && config.enabled
                && config.plantDrips.enabled && dripExclusions.stream().noneMatch(selector -> selector.matches(state));
        return new Match(material, enabled, plantDrips);
    }

    private RainMaterial classify(BlockState state, boolean waterSurface) {
        for (Rule rule : overrides) {
            if (rule.selector.matches(state)) return rule.material;
        }
        if (waterSurface) return RainMaterial.WATER;
        if (state.getFluidState().is(FluidTags.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                || CampfireBlock.isLitCampfire(state)) return RainMaterial.VANILLA;
        if (state.is(BlockTags.LEAVES) || state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.SMALL_DRIPLEAF)
                || state.is(Blocks.LILY_PAD)) return RainMaterial.FOLIAGE;
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.ALL_SIGNS)) return RainMaterial.WOOD;
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.SNOW)) return RainMaterial.SOFT;
        if (state.is(BlockTags.TERRACOTTA) || state.is(Blocks.TERRACOTTA) || state.getBlock() instanceof GlazedTerracottaBlock
                || state.is(Blocks.BRICKS) || state.is(Blocks.BRICK_SLAB)
                || state.is(Blocks.BRICK_STAIRS) || state.is(Blocks.BRICK_WALL)) return RainMaterial.CERAMIC;
        // Several soil blocks share vanilla's GRAVEL sound type with actual gravel.
        if (state.is(BlockTags.DIRT) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)) return RainMaterial.GROUND;
        SoundType sound = state.getSoundType();
        if (is(sound, SoundType.WOOD, SoundType.NETHER_WOOD, SoundType.BAMBOO_WOOD, SoundType.CHERRY_WOOD,
                SoundType.STEM, SoundType.SCAFFOLDING, SoundType.CHISELED_BOOKSHELF)) return RainMaterial.WOOD;
        if (is(sound, SoundType.METAL, SoundType.ANVIL, SoundType.COPPER, SoundType.COPPER_BULB,
                SoundType.COPPER_GRATE, SoundType.NETHERITE_BLOCK, SoundType.CHAIN,
                SoundType.LANTERN, SoundType.HEAVY_CORE)) return RainMaterial.METAL;
        if (is(sound, SoundType.GLASS, SoundType.AMETHYST, SoundType.AMETHYST_CLUSTER)) return RainMaterial.GLASS;
        if (is(sound, SoundType.BIG_DRIPLEAF, SoundType.SMALL_DRIPLEAF, SoundType.AZALEA_LEAVES,
                SoundType.CHERRY_LEAVES, SoundType.LILY_PAD, SoundType.VINE)) return RainMaterial.FOLIAGE;
        if (is(sound, SoundType.GRASS, SoundType.WET_GRASS, SoundType.MUD, SoundType.PACKED_MUD,
                SoundType.ROOTED_DIRT, SoundType.MOSS, SoundType.MOSS_CARPET, SoundType.SOUL_SOIL)) return RainMaterial.GROUND;
        if (is(sound, SoundType.SAND, SoundType.GRAVEL, SoundType.SOUL_SAND,
                SoundType.SUSPICIOUS_SAND, SoundType.SUSPICIOUS_GRAVEL)) return RainMaterial.GRANULAR;
        if (is(sound, SoundType.WOOL, SoundType.SNOW, SoundType.POWDER_SNOW, SoundType.SPONGE,
                SoundType.WET_SPONGE)) return RainMaterial.SOFT;
        if (is(sound, SoundType.DECORATED_POT, SoundType.DECORATED_POT_CRACKED, SoundType.MUD_BRICKS)) return RainMaterial.CERAMIC;
        if (is(sound, SoundType.STONE, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS,
                SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE, SoundType.TUFF,
                SoundType.TUFF_BRICKS, SoundType.POLISHED_TUFF, SoundType.CALCITE,
                SoundType.BASALT, SoundType.DRIPSTONE_BLOCK)) return RainMaterial.STONE;
        return RainMaterial.VANILLA;
    }

    public static boolean supportsPlantDrips(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.SMALL_DRIPLEAF);
    }

    private static boolean is(SoundType actual, SoundType... candidates) {
        for (SoundType candidate : candidates) if (candidate == actual) return true;
        return false;
    }

    private record Rule(BlockSelector selector, RainMaterial material) {}
    public record Match(RainMaterial material, boolean enhanced, boolean plantDrips) {}
}
