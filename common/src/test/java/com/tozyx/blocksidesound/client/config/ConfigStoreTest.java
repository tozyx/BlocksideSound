package com.tozyx.blocksidesound.client.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path directory;

    @Test void createsPortableDefaultsAndRoundTripsSelectors() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        store.current().rain.materials.get("wood").exclude.add("#minecraft:logs");
        store.current().rain.materials.get("wood").exclude.add("othermod:roof");
        store.save(store.current());
        ConfigStore reloaded = new ConfigStore(directory);
        assertTrue(reloaded.reload());
        assertEquals(store.current().rain.materials.get("wood").exclude,
                reloaded.current().rain.materials.get("wood").exclude);
        assertEquals(12, reloaded.current().rain.radius);
        assertEquals(10, reloaded.current().rain.materials.size());
    }

    @Test void editingSnapshotDoesNotMutateLiveSettingsOrFile() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        String original = Files.readString(directory.resolve("blocksidesound-client.json"));
        SoundConfig draft = store.snapshot();
        draft.enabled = false;
        draft.rain.materials.get("wood").exclude.add("minecraft:oak_planks");
        SoundConfig.Override rule = new SoundConfig.Override();
        rule.match = "#minecraft:logs";
        rule.material = "metal";
        draft.rain.overrides.add(rule);
        assertTrue(store.current().enabled);
        assertTrue(store.current().rain.materials.get("wood").exclude.isEmpty());
        assertTrue(store.current().rain.overrides.isEmpty());
        assertEquals(original, Files.readString(directory.resolve("blocksidesound-client.json")));
    }

    @Test void savingDraftDetachesAllNestedSettings() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        SoundConfig draft = store.snapshot();
        draft.enabled = false;
        draft.rain.plantDrips.exclude.add("#minecraft:leaves");
        SoundConfig.Override rule = new SoundConfig.Override();
        rule.match = "minecraft:glass";
        rule.material = "metal";
        draft.rain.overrides.add(rule);
        store.save(draft);
        draft.enabled = true;
        draft.rain.plantDrips.exclude.clear();
        rule.material = "wood";
        assertFalse(store.current().enabled);
        assertEquals(1, store.current().rain.plantDrips.exclude.size());
        assertEquals("metal", store.current().rain.overrides.getFirst().material);
        ConfigStore reloaded = new ConfigStore(directory);
        assertTrue(reloaded.reload());
        assertFalse(reloaded.current().enabled);
        assertEquals("metal", reloaded.current().rain.overrides.getFirst().material);
    }

    @Test void failedSaveKeepsActiveConfiguration() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        SoundConfig draft = store.snapshot();
        draft.enabled = false;
        Path file = directory.resolve("blocksidesound-client.json");
        Files.delete(file);
        Files.createDirectory(file);
        Files.writeString(file.resolve("keep.txt"), "occupied");
        assertThrows(java.io.IOException.class, () -> store.save(draft));
        assertTrue(store.current().enabled);
        assertEquals("occupied", Files.readString(file.resolve("keep.txt")));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }

    @Test void oldSchemaWithoutMasterSwitchRemainsEnabled() throws Exception {
        Files.writeString(directory.resolve("blocksidesound-client.json"), "{\"volume\":0.6}");
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        assertTrue(store.current().enabled);
        assertEquals(0.6, store.current().volume);
    }

    @Test void malformedFileKeepsPreviousConfigAndOriginalBytes() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        store.current().rain.radius = 20;
        store.save(store.current());
        Path file = directory.resolve("blocksidesound-client.json");
        String broken = "{ rain: this is broken";
        Files.writeString(file, broken);
        assertFalse(store.reload());
        assertEquals(20, store.current().rain.radius);
        assertEquals(broken, Files.readString(file));
    }

    @Test void futureSchemaIsNotReplacedWithOldDefaults() throws Exception {
        Path file = directory.resolve("blocksidesound-client.json");
        String content = "{\"schemaVersion\":2,\"volume\":0.3}";
        Files.writeString(file, content);
        ConfigStore store = new ConfigStore(directory);
        assertFalse(store.reload());
        assertEquals(content, Files.readString(file));
    }

    @Test void validatesBoundsAndRejectsAmbiguousSelectors() {
        SoundConfig config = new SoundConfig();
        config.rain.radius = Integer.MAX_VALUE;
        config.rain.scanBudget = 0;
        config.rain.materials.get("metal").triggerProbability = 2.0;
        config.validate();
        assertEquals(32, config.rain.radius);
        assertEquals(1, config.rain.scanBudget);
        assertEquals(1.0, config.rain.materials.get("metal").triggerProbability);
        config.rain.materials.get("metal").exclude.add("#missing_namespace");
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test void migratesLegacySettingsWithoutChangingOriginalFile() throws Exception {
        Path legacy = directory.resolve("blocksidefoley-client.json");
        String content = "{\"schemaVersion\":1,\"rain\":{\"radius\":20,\"debugLogging\":true}}";
        Files.writeString(legacy, content);
        ConfigStore store = new ConfigStore(directory);
        assertTrue(store.reload());
        assertEquals(20, store.current().rain.radius);
        assertTrue(store.current().rain.debugLogging);
        assertTrue(Files.exists(directory.resolve("blocksidesound-client.json")));
        assertEquals(content, Files.readString(legacy));
    }

    @Test void newSettingsTakePrecedenceOverLegacyFile() throws Exception {
        ConfigStore store = new ConfigStore(directory);
        SoundConfig config = new SoundConfig();
        config.rain.radius = 24;
        store.save(config);
        Files.writeString(directory.resolve("blocksidefoley-client.json"), "{\"rain\":{\"radius\":8}}");
        assertTrue(store.reload());
        assertEquals(24, store.current().rain.radius);
    }

    @Test void invalidLegacySettingsArePreservedWithoutCreatingNewFile() throws Exception {
        Path legacy = directory.resolve("blocksidefoley-client.json");
        Files.writeString(legacy, "broken");
        ConfigStore store = new ConfigStore(directory);
        assertFalse(store.reload());
        assertEquals("broken", Files.readString(legacy));
        assertFalse(Files.exists(directory.resolve("blocksidesound-client.json")));
    }
}
