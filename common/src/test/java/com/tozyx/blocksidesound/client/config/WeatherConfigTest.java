package com.tozyx.blocksidesound.client.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WeatherConfigTest {
    @TempDir Path directory;

    @Test void oldConfigRetainsMaterialSettingsAndGainsWeatherDefaults() throws Exception {
        String original = "{\"volume\":0.7,\"rain\":{\"enabled\":false,\"radius\":20}}";
        Path path = directory.resolve("blocksidesound-client.json");
        Files.writeString(path, original);
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        assertEquals(1, store.current().schemaVersion);
        assertEquals(0.7, store.current().volume);
        assertFalse(store.current().rain.enabled);
        assertEquals(20, store.current().rain.radius);
        assertTrue(store.current().weather.rain.enabled);
        assertTrue(store.current().weather.thunder.enabled);
        assertEquals(1, store.current().weather.rain.volume);
        assertEquals(1, store.current().weather.thunder.volume);
        assertEquals(original, Files.readString(path));
    }

    @Test void savedWeatherSettingsRoundTripAndEditingCopiesStayDetached() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        SoundConfig draft = store.snapshot();
        draft.weather.rain.enabled = false;
        draft.weather.rain.volume = 0.2;
        draft.weather.thunder.volume = 0.6;
        assertTrue(store.current().weather.rain.enabled);
        assertEquals(1, store.current().weather.thunder.volume);
        store.save(draft);
        draft.weather.thunder.volume = 0.1;
        assertEquals(0.6, store.current().weather.thunder.volume);
        ConfigStore next = new ConfigStore(directory);
        assertTrue(next.reload());
        assertFalse(next.current().weather.rain.enabled);
        assertEquals(0.2, next.current().weather.rain.volume);
        assertTrue(next.current().weather.thunder.enabled);
        assertEquals(0.6, next.current().weather.thunder.volume);
    }

    @Test void nullWeatherSectionsAreRepaired() throws Exception {
        Path path = directory.resolve("blocksidesound-client.json");
        Files.writeString(path, "{\"weather\":null}");
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        assertNotNull(store.current().weather.rain);
        Files.writeString(path, "{\"weather\":{\"rain\":null,\"thunder\":null}}");
        assertTrue(store.reload());
        assertTrue(store.current().weather.rain.enabled);
        assertTrue(store.current().weather.thunder.enabled);
    }

    @Test void weatherVolumesAreFiniteAndBounded() {
        SoundConfig config = new SoundConfig();
        config.weather.rain.volume = Double.NaN;
        config.weather.thunder.volume = -5;
        config.validate();
        assertEquals(1, config.weather.rain.volume);
        assertEquals(0, config.weather.thunder.volume);
        config.weather.rain.volume = 50;
        config.weather.thunder.volume = Double.POSITIVE_INFINITY;
        config.validate();
        assertEquals(1, config.weather.rain.volume);
        assertEquals(1, config.weather.thunder.volume);
    }

    @Test void malformedWeatherPreservesLastValidSettingsAndFile() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        SoundConfig settings = store.snapshot();
        settings.weather.thunder.enabled = false;
        store.save(settings);
        Path path = directory.resolve("blocksidesound-client.json");
        String broken = "{\"weather\":{\"thunder\":\"invalid\"}}";
        Files.writeString(path, broken);
        assertFalse(store.reload());
        assertFalse(store.current().weather.thunder.enabled);
        assertEquals(broken, Files.readString(path));
    }
}
