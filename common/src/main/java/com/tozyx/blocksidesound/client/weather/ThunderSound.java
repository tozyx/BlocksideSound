package com.tozyx.blocksidesound.client.weather;

import com.tozyx.blocksidesound.BlocksideSound;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Keeps native timing/position/range while making the mod gain independent of thunder's huge source volume. */
final class ThunderSound extends AbstractSoundInstance {
    private final int nativeRange;

    ThunderSound(SoundInstance original, ResourceLocation event, double gain) {
        super(event, SoundSource.WEATHER, RandomSource.create());
        nativeRange = range(original.getVolume(), original.getSound().getAttenuationDistance());
        volume = gain(original.getVolume(), gain);
        pitch = original.getPitch();
        x = original.getX();
        y = original.getY();
        z = original.getZ();
        relative = original.isRelative();
        attenuation = original.getAttenuation();
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents event = super.resolve(manager);
        if (event != null && sound != SoundManager.EMPTY_SOUND && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            sound = new Sound(sound.getLocation(), sound.getVolume(), sound.getPitch(), sound.getWeight(),
                    sound.getType(), sound.shouldStream(), sound.shouldPreload(), nativeRange);
        }
        return event;
    }

    static ResourceLocation profile(double distanceSquared) {
        String band = distanceSquared < 64 * 64 ? "close" : distanceSquared < 160 * 160 ? "medium" : "far";
        return BlocksideSound.id("weather.thunder." + band);
    }

    static float gain(float nativeVolume, double multiplier) {
        if (!Float.isFinite(nativeVolume) || !Double.isFinite(multiplier)) return 0;
        return (float) (Math.clamp(nativeVolume, 0.0F, 1.0F) * Math.clamp(multiplier, 0.0, 1.0));
    }

    static int range(float nativeVolume, int attenuationDistance) {
        double range = Math.max(1.0, Float.isFinite(nativeVolume) ? nativeVolume : 1.0) * Math.max(1, attenuationDistance);
        return (int) Math.clamp(Math.ceil(range), 1, Integer.MAX_VALUE);
    }
}
