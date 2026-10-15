package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static com.tozyx.blocksidesound.client.weather.WeatherSoundPolicy.Action.*;
import static com.tozyx.blocksidesound.client.weather.WeatherSoundPolicy.Event.*;
import static org.junit.jupiter.api.Assertions.*;

class WeatherSoundPolicyTest {
    @Test void routesOnlyReadyWeatherEventsWithIndependentGain() {
        SoundConfig config = new SoundConfig();
        config.volume = 0.8;
        config.weather.rain.volume = 0.5;
        config.weather.thunder.volume = 0.25;
        for (var event : WeatherSoundPolicy.Event.values()) {
            var result = WeatherSoundPolicy.select(event.original(), config, id -> true);
            assertEquals(REPLACE, result.action());
            assertEquals(event.replacement(), result.event());
            assertEquals(event == THUNDER ? 0.2 : 0.4, result.volumeMultiplier(), 1.0e-9);
        }
    }

    @Test void missingVariantDoesNotSuppressItsOriginalOrBlockOtherVariants() {
        SoundConfig config = new SoundConfig();
        var rain = WeatherSoundPolicy.select(RAIN.original(), config, RAIN.replacement()::equals);
        var above = WeatherSoundPolicy.select(RAIN_ABOVE.original(), config, RAIN.replacement()::equals);
        var thunder = WeatherSoundPolicy.select(THUNDER.original(), config, RAIN.replacement()::equals);
        assertEquals(REPLACE, rain.action());
        assertEquals(KEEP_ORIGINAL, above.action());
        assertEquals(RAIN_ABOVE.original(), above.event());
        assertEquals(KEEP_ORIGINAL, thunder.action());
    }

    @Test void disabledRainAndThunderAreIndependent() {
        SoundConfig config = new SoundConfig();
        config.weather.rain.enabled = false;
        assertEquals(KEEP_ORIGINAL, WeatherSoundPolicy.select(RAIN.original(), config, id -> true).action());
        assertEquals(KEEP_ORIGINAL, WeatherSoundPolicy.select(RAIN_ABOVE.original(), config, id -> true).action());
        assertEquals(REPLACE, WeatherSoundPolicy.select(THUNDER.original(), config, id -> true).action());
        config.weather.rain.enabled = true;
        config.weather.thunder.enabled = false;
        assertEquals(REPLACE, WeatherSoundPolicy.select(RAIN.original(), config, id -> true).action());
        assertEquals(KEEP_ORIGINAL, WeatherSoundPolicy.select(THUNDER.original(), config, id -> true).action());
    }

    @Test void disablingModLeavesOriginalsWithoutQueryingResources() {
        SoundConfig config = new SoundConfig();
        config.enabled = false;
        for (var event : WeatherSoundPolicy.Event.values()) {
            var result = WeatherSoundPolicy.select(event.original(), config, id -> {
                fail("Disabled replacement must not query resources");
                return false;
            });
            assertEquals(KEEP_ORIGINAL, result.action());
            assertEquals(1.0, result.volumeMultiplier());
        }
    }

    @Test void zeroVolumeMutesReadyReplacementWithoutReenablingOriginal() {
        SoundConfig config = new SoundConfig();
        config.weather.rain.volume = 0;
        assertEquals(MUTE, WeatherSoundPolicy.select(RAIN.original(), config, id -> true).action());
        assertEquals(REPLACE, WeatherSoundPolicy.select(THUNDER.original(), config, id -> true).action());
        config.volume = 0;
        var result = WeatherSoundPolicy.select(THUNDER.original(), config, id -> true);
        assertEquals(MUTE, result.action());
        assertEquals(0, result.volumeMultiplier());
        assertEquals(KEEP_ORIGINAL, WeatherSoundPolicy.select(THUNDER.original(), config, id -> false).action());
    }

    @Test void unrelatedAndReplacementEventsPassThroughUnchanged() {
        SoundConfig config = new SoundConfig();
        for (var id : List.of(ResourceLocation.parse("minecraft:entity.lightning_bolt.impact"),
                ResourceLocation.parse("othermod:weather.rain"), ResourceLocation.parse("minecraft:weather.rain.extra"),
                ResourceLocation.parse("minecraft:ambient.cave"), RAIN.replacement(), THUNDER.replacement())) {
            var result = WeatherSoundPolicy.select(id, config, ignored -> {
                fail("Unrelated sounds must not enter the weather resource lookup");
                return false;
            });
            assertEquals(KEEP_ORIGINAL, result.action());
            assertEquals(id, result.event());
        }
    }

    @Test void materialDropSettingsDoNotControlWeatherBackgroundOrThunder() {
        SoundConfig config = new SoundConfig();
        config.rain.enabled = false;
        config.rain.volume = 0;
        config.rain.plantDrips.enabled = false;
        for (var event : WeatherSoundPolicy.Event.values()) {
            var result = WeatherSoundPolicy.select(event.original(), config, id -> true);
            assertEquals(REPLACE, result.action());
            assertEquals(1, result.volumeMultiplier());
        }
    }
}
