package com.tozyx.blocksidesound.client.config;

import com.tozyx.blocksidesound.client.rain.RainMaterial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared configuration model for file editing and the vanilla settings screen. */
public final class SoundConfig {
    public int schemaVersion = 1;
    public boolean enabled = true;
    public double volume = 1.0;
    public Rain rain = new Rain();
    public Weather weather = new Weather();

    /** Background replacements are independent of material drops and their sampler. */
    public static final class Weather {
        public Replacement rain = new Replacement();
        public Replacement thunder = new Replacement();
    }

    public static final class Replacement {
        public boolean enabled = true;
        public double volume = 1.0;
    }

    public static final class Rain {
        public boolean enabled = true;
        public double volume = 1.0;
        public int radius = 12;
        public int verticalRange = 16;
        public int scanIntervalTicks = 4;
        public int scanBudget = 64;
        public int maxActiveSounds = 8;
        public boolean debugLogging = false;
        public boolean debugParticles = false;
        public List<Override> overrides = new ArrayList<>();
        public Map<String, Material> materials = defaults();
        public Material plantDrips = new Material(0.25, 40);

        private static Map<String, Material> defaults() {
            Map<String, Material> materials = new LinkedHashMap<>();
            for (RainMaterial material : RainMaterial.values()) {
                if (material != RainMaterial.VANILLA) materials.put(material.id(), new Material());
            }
            return materials;
        }
    }

    public static final class Material {
        public boolean enabled = true;
        public double volume = 1.0;
        public double triggerProbability = 0.35;
        public int attemptIntervalTicks = 10;
        public List<String> exclude = new ArrayList<>();

        public Material() {}

        public Material(double probability, int interval) {
            triggerProbability = probability;
            attemptIntervalTicks = interval;
        }
    }

    public static final class Override {
        public String match = "";
        public String material = "";
    }

    public void validate() {
        if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported config schema: " + schemaVersion);
        volume = unit(volume, 1.0);
        if (weather == null) weather = new Weather();
        if (weather.rain == null) weather.rain = new Replacement();
        if (weather.thunder == null) weather.thunder = new Replacement();
        weather.rain.volume = unit(weather.rain.volume, 1.0);
        weather.thunder.volume = unit(weather.thunder.volume, 1.0);
        if (rain == null) rain = new Rain();
        rain.volume = unit(rain.volume, 1.0);
        rain.radius = clamp(rain.radius, 4, 32);
        rain.verticalRange = clamp(rain.verticalRange, 4, 64);
        rain.scanIntervalTicks = clamp(rain.scanIntervalTicks, 1, 40);
        rain.scanBudget = clamp(rain.scanBudget, 1, 256);
        rain.maxActiveSounds = clamp(rain.maxActiveSounds, 1, 32);
        if (rain.materials == null) rain.materials = Rain.defaults();
        for (String key : rain.materials.keySet()) {
            if (RainMaterial.byId(key) == RainMaterial.VANILLA) {
                throw new IllegalArgumentException("Unknown material settings: " + key);
            }
        }
        for (RainMaterial type : RainMaterial.values()) {
            if (type == RainMaterial.VANILLA) continue;
            Material material = rain.materials.get(type.id());
            if (material == null) {
                material = new Material();
                rain.materials.put(type.id(), material);
            }
            validateMaterial(material);
        }
        if (rain.plantDrips == null) rain.plantDrips = new Material(0.25, 40);
        validateMaterial(rain.plantDrips);
        if (rain.overrides == null) rain.overrides = new ArrayList<>();
        for (Override override : rain.overrides) {
            if (override == null) throw new IllegalArgumentException("Null material override");
            BlockSelector.parse(override.match);
            if (RainMaterial.byId(override.material) == RainMaterial.VANILLA && !"vanilla".equals(override.material)) {
                throw new IllegalArgumentException("Unknown override material: " + override.material);
            }
        }
    }

    private static void validateMaterial(Material material) {
        material.volume = unit(material.volume, 1.0);
        material.triggerProbability = unit(material.triggerProbability, 0.35);
        material.attemptIntervalTicks = clamp(material.attemptIntervalTicks, 1, 1200);
        if (material.exclude == null) material.exclude = new ArrayList<>();
        for (String selector : material.exclude) BlockSelector.parse(selector);
    }

    private static double unit(double value, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, 0.0, 1.0) : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }
}
