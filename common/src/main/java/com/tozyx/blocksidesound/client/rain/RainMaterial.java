package com.tozyx.blocksidesound.client.rain;

import java.util.Locale;

public enum RainMaterial {
    STONE, WOOD, METAL, GLASS, CERAMIC, FOLIAGE, GROUND, GRANULAR, WATER, SOFT, VANILLA;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RainMaterial byId(String id) {
        if (id == null) return VANILLA;
        for (RainMaterial material : values()) {
            if (material.id().equals(id)) return material;
        }
        return VANILLA;
    }
}
