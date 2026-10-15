package com.tozyx.blocksidesound.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.tozyx.blocksidesound.client.gui.SoundConfigScreen;

/** Loaded by Mod Menu only when it is installed; no runtime menu dependency. */
public final class BlocksideSoundModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SoundConfigScreen::new;
    }
}
