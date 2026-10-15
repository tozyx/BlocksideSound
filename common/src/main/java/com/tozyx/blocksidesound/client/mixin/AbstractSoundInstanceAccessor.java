package com.tozyx.blocksidesound.client.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read request properties before sample resolution, without sampling old resource gain or pitch twice. */
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor("volume")
    float blocksidesound$getBaseVolume();

    @Accessor("pitch")
    float blocksidesound$getBasePitch();
}
