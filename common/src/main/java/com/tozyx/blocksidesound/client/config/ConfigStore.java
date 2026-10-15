package com.tozyx.blocksidesound.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tozyx.blocksidesound.BlocksideSound;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

/** Keeps the last valid configuration when a hand-edited file cannot be loaded. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Path legacyPath;
    private SoundConfig current = new SoundConfig();

    public ConfigStore(Path directory) {
        path = directory.resolve("blocksidesound-client.json");
        legacyPath = directory.resolve("blocksidefoley-client.json");
    }

    public SoundConfig current() {
        return current;
    }

    /** A detached editing copy: cancelling a screen must never change live settings. */
    public SoundConfig snapshot() {
        return copyOf(current);
    }

    private static SoundConfig copyOf(SoundConfig config) {
        return GSON.fromJson(GSON.toJson(config), SoundConfig.class);
    }

    public boolean reload() {
        Path source = path;
        try {
            if (!Files.exists(path) && Files.exists(legacyPath)) source = legacyPath;
            if (!Files.exists(source)) {
                save(new SoundConfig());
                return true;
            }
            SoundConfig loaded = GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), SoundConfig.class);
            if (loaded == null) throw new IllegalArgumentException("Config must be a JSON object");
            loaded.validate();
            if (source.equals(legacyPath)) {
                save(loaded);
                BlocksideSound.LOGGER.info("Migrated config {} to {}; original file preserved", legacyPath, path);
            } else {
                current = loaded;
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            BlocksideSound.LOGGER.error("Cannot load {}; keeping last valid settings and leaving the file untouched", source, exception);
            return false;
        }
    }

    public void save(SoundConfig config) throws IOException {
        SoundConfig saved = copyOf(config);
        saved.validate();
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "blocksidesound-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(saved) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            current = saved;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
