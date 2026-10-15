package com.tozyx.blocksidesound.client.mixin;

import com.tozyx.blocksidesound.client.gui.SoundConfigScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends OptionsSubScreen {
    protected SoundOptionsScreenMixin(Screen parent, Options options, Component title) {
        super(parent, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void blocksidesound$addSettings(CallbackInfo callback) {
        list.addSmall(Button.builder(Component.translatable("blocksidesound.config.title"), button -> {
            list.applyUnsavedChanges();
            minecraft.setScreen(new SoundConfigScreen((Screen) (Object) this));
        }).width(150).build(), null);
    }
}
