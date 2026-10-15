package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** Pure routing policy shared by the background controller and native weather interception. */
public final class WeatherSoundPolicy {
    private WeatherSoundPolicy() {}

    public enum Event {
        RAIN("weather.rain", "weather.rain", true),
        RAIN_ABOVE("weather.rain.above", "weather.rain.above", true),
        THUNDER("entity.lightning_bolt.thunder", "weather.thunder", false);

        private final ResourceLocation original;
        private final ResourceLocation replacement;
        private final boolean rain;

        Event(String original, String replacement, boolean rain) {
            this.original = ResourceLocation.withDefaultNamespace(original);
            this.replacement = BlocksideSound.id(replacement);
            this.rain = rain;
        }

        public ResourceLocation original() {
            return original;
        }

        public ResourceLocation replacement() {
            return replacement;
        }

        public SoundConfig.Replacement settings(SoundConfig config) {
            return rain ? config.weather.rain : config.weather.thunder;
        }
    }

    public enum Action { KEEP_ORIGINAL, REPLACE, MUTE }

    /** Volume is the mod multiplier, not native source volume, distance or weather-channel volume. */
    public record Decision(Action action, ResourceLocation event, double volumeMultiplier) {}

    /**
     * Requires a validated config. A replacement must have a nonempty, resolved sound event.
     * Missing material is a preparation fallback, not a layer mixed with a ready replacement.
     */
    public static Decision select(ResourceLocation original, SoundConfig config,
                                  Predicate<ResourceLocation> available) {
        Event event = forOriginal(original);
        if (event == null || !config.enabled) return keep(original);
        SoundConfig.Replacement settings = event.settings(config);
        if (!settings.enabled || !available.test(event.replacement())) return keep(original);
        double volume = config.volume * settings.volume;
        return new Decision(volume > 0 ? Action.REPLACE : Action.MUTE, event.replacement(), volume);
    }

    private static Event forOriginal(ResourceLocation original) {
        for (Event event : Event.values()) {
            if (event.original().equals(original)) return event;
        }
        return null;
    }

    private static Decision keep(ResourceLocation original) {
        return new Decision(Action.KEEP_ORIGINAL, original, 1.0);
    }
}
