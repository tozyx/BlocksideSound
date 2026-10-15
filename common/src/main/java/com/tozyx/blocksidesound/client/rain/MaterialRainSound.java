package com.tozyx.blocksidesound.client.rain;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** One independent full recording; other voices may overlap it without releasing or replacing it. */
final class MaterialRainSound extends AbstractTickableSoundInstance {
    private final boolean sustained;
    private float gain;
    private MaterialEnvelope envelope;
    private boolean resolved;
    private WeighedSoundEvents event;
    private float resourceVolume = 1;
    private float resourcePitch = 1;

    MaterialRainSound(RainSound kind, float gain, float pitch, RandomSource random, Vec3 point) {
        super(SoundEvent.createVariableRangeEvent(kind.id()), SoundSource.WEATHER, random);
        this.sustained = kind != RainSound.PLANT_DRIP;
        this.gain = gain;
        this.pitch = pitch;
        x = point.x;
        y = point.y + 0.05;
        z = point.z;
        attenuation = Attenuation.LINEAR;
        relative = false;
        looping = false;
        delay = 0;
        volume = sustained ? 0 : gain;
        envelope = new MaterialEnvelope(sustained);
    }

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
        return event; // Keep one selected take and its resource gain/pitch throughout this voice.
    }

    void gain(float gain) {
        this.gain = gain;
    }

    boolean releasing() { return envelope.releasing(); }

    void release() {
        envelope.release();
    }

    @Override public boolean canStartSilent() { return true; }
    @Override public float getVolume() { return volume * resourceVolume; }
    @Override public float getPitch() { return pitch * resourcePitch; }

    @Override
    public void tick() {
        volume = gain * envelope.tick();
        if (envelope.finished()) stop();
    }
}
