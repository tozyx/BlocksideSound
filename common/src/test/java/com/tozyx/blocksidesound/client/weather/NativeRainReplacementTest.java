package com.tozyx.blocksidesound.client.weather;

import com.google.gson.JsonParser;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import com.tozyx.blocksidesound.client.mixin.AbstractSoundInstanceAccessor;
import com.tozyx.blocksidesound.client.rain.OggDuration;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeRainReplacementTest {
    private static SimpleSoundInstance original(WeatherSoundPolicy.Event profile, float volume, float pitch,
            double x, double y, double z) {
        return new NativeRequest(profile, volume, pitch, false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false);
    }

    // Unit tests do not launch Mixin. This fixture exposes the same raw request fields through
    // the accessor contract, and rejects resource-dependent getters before/after resolution.
    private static final class NativeRequest extends SimpleSoundInstance implements AbstractSoundInstanceAccessor {
        NativeRequest(WeatherSoundPolicy.Event profile, float volume, float pitch, boolean looping, int delay,
                SoundInstance.Attenuation attenuation, double x, double y, double z, boolean relative) {
            super(profile.original(), SoundSource.WEATHER, volume, pitch, RandomSource.create(2),
                    looping, delay, attenuation, x, y, z, relative);
        }
        @Override public float blocksidesound$getBaseVolume() { return volume; }
        @Override public float blocksidesound$getBasePitch() { return pitch; }
        @Override public float getVolume() { throw new AssertionError("Do not inherit old resource gain"); }
        @Override public float getPitch() { throw new AssertionError("Do not inherit old resource pitch"); }
    }

    @Test void outdoorReplacementKeepsNativePositionPitchAndBaseVolume() {
        var nativeRain = original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 12.5, 65, -7.5);
        var replacement = new BackgroundRainSound(nativeRain, WeatherSoundPolicy.Event.RAIN.replacement(), .5);
        assertEquals(.1F, replacement.getVolume(), 1e-6);
        assertEquals(1, replacement.getPitch());
        assertEquals(nativeRain.getX(), replacement.getX());
        assertEquals(nativeRain.getY(), replacement.getY());
        assertEquals(nativeRain.getZ(), replacement.getZ());
        assertEquals(SoundSource.WEATHER, replacement.getSource());
        assertFalse(replacement.isRelative());
        assertEquals(SoundInstance.Attenuation.LINEAR, replacement.getAttenuation());
        assertFalse(replacement.isLooping());
        assertEquals(0, replacement.getDelay());
    }

    @Test void shelteredReplacementUsesNaturalSpeedAndNativeQuietGain() {
        var nativeRain = original(WeatherSoundPolicy.Event.RAIN_ABOVE, .1F, .5F, -2, 70, 8);
        var replacement = new BackgroundRainSound(nativeRain, WeatherSoundPolicy.Event.RAIN_ABOVE.replacement(), 1);
        assertEquals(.1F, replacement.getVolume(), 1e-6);
        assertEquals(1F, replacement.getPitch());
        assertEquals(nativeRain.getY(), replacement.getY());
    }

    @Test void relativeAttenuationAndRepeatPropertiesAreForwardedRatherThanInvented() {
        var nativeRain = new NativeRequest(WeatherSoundPolicy.Event.RAIN,
                .3F, .9F, true, 7, SoundInstance.Attenuation.NONE, 4, 5, 6, true);
        var replacement = new BackgroundRainSound(nativeRain, WeatherSoundPolicy.Event.RAIN.replacement(), .8);
        assertEquals(.24F, replacement.getVolume(), 1e-6);
        assertEquals(.9F, replacement.getPitch());
        assertTrue(replacement.isRelative());
        assertEquals(SoundInstance.Attenuation.NONE, replacement.getAttenuation());
        assertTrue(replacement.isLooping());
        assertEquals(7, replacement.getDelay());
    }

    @Test void separateNativeRequestsKeepSeparateWorldPositionsAndDoNotRewriteOldVoices() {
        var first = new BackgroundRainSound(original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 1, 64, 2),
                WeatherSoundPolicy.Event.RAIN.replacement(), 1);
        var second = new BackgroundRainSound(original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 7, 67, 9),
                WeatherSoundPolicy.Event.RAIN.replacement(), 1);
        assertEquals(1, first.getX());
        assertEquals(7, second.getX());
        assertEquals(.2F, first.getVolume(), 1e-6);
        assertEquals(.2F, second.getVolume(), 1e-6);
        assertFalse(TickableSoundInstance.class.isAssignableFrom(BackgroundRainSound.class),
                "Native audio lifetime must not use another Java tick/EOF envelope");
    }

    @Test void modGainIsAppliedOnceWithoutRainStrengthOrLayerCompensation() {
        var nativeRain = original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 0, 64, 0);
        var config = new SoundConfig();
        config.volume = .8;
        config.weather.rain.volume = .5;
        var decision = WeatherSoundPolicy.select(nativeRain.getLocation(), config, id -> true);
        var replacement = new BackgroundRainSound(nativeRain, decision.event(), decision.volumeMultiplier());
        assertEquals(.08F, replacement.getVolume(), 1e-6);
    }

    @Test void replacementCannotEnterTheNativeWeatherHookAgain() {
        var replacement = new BackgroundRainSound(original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 0, 64, 0),
                WeatherSoundPolicy.Event.RAIN.replacement(), 1);
        assertEquals(WeatherSoundPolicy.Action.KEEP_ORIGINAL,
                WeatherSoundPolicy.select(replacement.getLocation(), new SoundConfig(), id -> {
                    fail("Replacement must not be intercepted recursively");
                    return false;
                }).action());
    }

    @Test void outdoorEventUsesFullSpatialMonoTakesAtReferenceGain() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/blocksidesound/sounds.json")) {
            assertNotNull(input);
            var events = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var sounds = events.getAsJsonObject("weather.rain").getAsJsonArray("sounds");
            assertEquals(6, sounds.size());
            for (int i = 0; i < sounds.size(); i++) {
                var sound = sounds.get(i).getAsJsonObject();
                assertEquals("blocksidesound:weather/rain" + (i + 7), sound.get("name").getAsString());
                assertEquals(.7, sound.get("volume").getAsDouble(), 1e-6);
                try (var audio = getClass().getResourceAsStream(
                        "/assets/blocksidesound/sounds/weather/rain" + (i + 7) + ".ogg")) {
                    assertNotNull(audio);
                    byte[] bytes = audio.readAllBytes();
                    assertEquals(1, vorbisChannels(bytes), "World-positioned rain must spatialize");
                    double seconds = OggDuration.seconds(new java.io.ByteArrayInputStream(bytes)).orElseThrow();
                    assertTrue(seconds > 2 && seconds < 3);
                }
            }
        }
    }

    @Test void shelteredEventUsesFilteredMonoRoofSamples() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/blocksidesound/sounds.json")) {
            assertNotNull(input);
            var events = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var sounds = events.getAsJsonObject("weather.rain.above").getAsJsonArray("sounds");
            assertEquals(6, sounds.size());
            for (int i = 0; i < sounds.size(); i++) {
                assertEquals("blocksidesound:weather/rain_above" + (i + 7),
                        sounds.get(i).getAsJsonObject().get("name").getAsString());
            }
        }
        try (var input = getClass().getResourceAsStream("/META-INF/licenses/blocksidesound-rain-audio-manifest.json")) {
            assertNotNull(input);
            var rows = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            int count = 0;
            for (var value : rows) {
                var row = value.getAsJsonObject();
                if (!row.get("output").getAsString().contains("/weather/rain_above")) continue;
                assertEquals(1, row.get("channels").getAsInt());
                assertEquals(2800, row.get("source_lowpass_hz").getAsInt());
                assertEquals(1, row.get("playback_pitch").getAsDouble());
                assertTrue(row.get("source_gain").getAsDouble() <= 1.5);
                count++;
            }
            assertEquals(6, count);
        }
    }

    @Test void wallObstructedNormalRainUsesShelteredGainWithoutSlowingOrMoving() {
        var request = original(WeatherSoundPolicy.Event.RAIN, .2F, 1, 7.5, 63.5, .5);
        var sound = new BackgroundRainSound(request, WeatherSoundPolicy.Event.RAIN_ABOVE.replacement(), .8);
        assertEquals(.08F, sound.getVolume(), 1e-6);
        assertEquals(1, sound.getPitch());
        assertEquals(request.getX(), sound.getX());
        assertEquals(request.getY(), sound.getY());
        assertEquals(request.getZ(), sound.getZ());
        assertFalse(sound.isLooping());
    }

    private static int vorbisChannels(byte[] bytes) {
        byte[] marker = {1, 'v', 'o', 'r', 'b', 'i', 's'};
        outer: for (int i = 0; i + 12 < bytes.length; i++) {
            for (int j = 0; j < marker.length; j++) if (bytes[i+j] != marker[j]) continue outer;
            return Byte.toUnsignedInt(bytes[i+11]);
        }
        throw new AssertionError("Missing Vorbis identification header");
    }

    @Test void nativePropertyAccessorIsRegisteredInSharedClientMixins() throws Exception {
        try (var input = getClass().getResourceAsStream("/blocksidesound.client.mixins.json")) {
            assertNotNull(input);
            var mixins = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            boolean found = false;
            for (var value : mixins.getAsJsonArray("client")) {
                if (value.getAsString().equals("AbstractSoundInstanceAccessor")) found = true;
            }
            assertTrue(found);
        }
    }
}
