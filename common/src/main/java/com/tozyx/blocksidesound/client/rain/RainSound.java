package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import net.minecraft.resources.ResourceLocation;

/** Stable event names; absent resource definitions leave vanilla audio intact. */
public enum RainSound {
    STONE(RainMaterial.STONE, "stone"),
    CERAMIC(RainMaterial.CERAMIC, "ceramic"),
    GROUND(RainMaterial.GROUND, "ground"),
    WOOD(RainMaterial.WOOD, "wood"),
    METAL(RainMaterial.METAL, "metal"),
    GLASS(RainMaterial.GLASS, "glass"),
    FOLIAGE(RainMaterial.FOLIAGE, "foliage"),
    GRANULAR(RainMaterial.GRANULAR, "granular"),
    WATER(RainMaterial.WATER, "water"),
    SOFT(RainMaterial.SOFT, "soft"),
    PLANT_DRIP(null, "plant_drip");

    public static final int ATTENUATION_DISTANCE = 16;
    private final RainMaterial material;
    private final ResourceLocation id;

    RainSound(RainMaterial material, String name) {
        this.material = material;
        id = BlocksideSound.id("rain." + name);
    }

    public ResourceLocation id() {
        return id;
    }

    public SoundConfig.Material settings(SoundConfig config) {
        return material == null ? config.rain.plantDrips : config.rain.materials.get(material.id());
    }

    public boolean accepts(RainSurface surface) {
        return this == PLANT_DRIP ? surface.match().plantDrips()
                : surface.match().enhanced() && surface.match().material() == material;
    }

    public static RainSound forMaterial(RainMaterial material) {
        return switch (material) {
            case STONE -> STONE;
            case CERAMIC -> CERAMIC;
            case GROUND -> GROUND;
            case WOOD -> WOOD;
            case METAL -> METAL;
            case GLASS -> GLASS;
            case FOLIAGE -> FOLIAGE;
            case GRANULAR -> GRANULAR;
            case WATER -> WATER;
            case SOFT -> SOFT;
            default -> null;
        };
    }
}
