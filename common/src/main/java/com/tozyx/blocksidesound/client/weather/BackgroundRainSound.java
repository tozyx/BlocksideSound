package com.tozyx.blocksidesound.client.weather;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Diffuse background; material drops retain their own positional sources. */
final class BackgroundRainSound extends AbstractTickableSoundInstance {
    private final VolumeFade fade = new VolumeFade();

    BackgroundRainSound(WeatherSoundPolicy.Event event) {
        super(SoundEvent.createVariableRangeEvent(event.replacement()), SoundSource.WEATHER, RandomSource.create());
        looping = true;
        relative = true;
        attenuation = Attenuation.NONE;
        volume = 0;
        pitch = 1;
    }

    void target(float volume) {
        fade.target(volume);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        volume = fade.tick();
        if (fade.silent()) stop();
    }
}
