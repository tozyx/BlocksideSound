package com.tozyx.blocksidesound.client.mixin;

import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void blocksidesound$weather(SoundInstance sound, CallbackInfo callback) {
        if (BlocksideSoundClient.replaceWeatherSound(sound)) callback.cancel();
    }
}
