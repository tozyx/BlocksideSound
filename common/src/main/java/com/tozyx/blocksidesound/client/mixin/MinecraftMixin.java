package com.tozyx.blocksidesound.client.mixin;

import com.tozyx.blocksidesound.client.BlocksideSoundClient;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void blocksidesound$tick(CallbackInfo callback) {
        BlocksideSoundClient.tick((Minecraft) (Object) this);
    }

    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void blocksidesound$reload(CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        BlocksideSoundClient.reloadConfig();
    }
}
