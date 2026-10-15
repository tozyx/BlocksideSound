package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.client.mixin.AbstractSoundInstanceAccessor;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Native timing and position; spatial mono samples, with filtering instead of slowed sheltered rain. */
final class BackgroundRainSound extends AbstractSoundInstance {
    private WeighedSoundEvents event;
    private boolean resolved;
    private float resourceVolume = 1;
    private float resourcePitch = 1;

    BackgroundRainSound(SoundInstance original, ResourceLocation replacement, double multiplier) {
        super(replacement, original.getSource(), RandomSource.create());
        float nativeVolume = original instanceof AbstractSoundInstanceAccessor properties
                ? properties.blocksidesound$getBaseVolume() : original.getVolume();
        float nativePitch = original instanceof AbstractSoundInstanceAccessor properties
                ? properties.blocksidesound$getBasePitch() : original.getPitch();
        volume = nativeVolume * (float) multiplier;
        pitch = nativePitch;
        if (replacement.equals(WeatherSoundPolicy.Event.RAIN_ABOVE.replacement())) {
            if (original.getLocation().equals(WeatherSoundPolicy.Event.RAIN_ABOVE.original())) {
                // Vanilla's half speed suits its own above sample. Our already-filtered recording
                // must retain the speed and rhythm of actual rainfall instead of stretching to 5 s.
                pitch *= 2;
            } else {
                // A normal native request can also be behind a wall. Match the native sheltered
                // source level, without applying the weather/master sliders a second time.
                volume *= .5F;
            }
        }
        x = original.getX();
        y = original.getY();
        z = original.getZ();
        relative = original.isRelative();
        attenuation = original.getAttenuation();
        looping = original.isLooping();
        delay = original.getDelay();
    }

    @Override public float getVolume() { return volume * resourceVolume; }
    @Override public float getPitch() { return pitch * resourcePitch; }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        if (!resolved) {
            event = super.resolve(manager);
            resolved = true;
            if (sound != null) {
                resourceVolume = sound.getVolume().sample(random);
                resourcePitch = sound.getPitch().sample(random);
            }
        }
        return event;
    }
}
