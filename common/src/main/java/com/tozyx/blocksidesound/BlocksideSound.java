package com.tozyx.blocksidesound;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlocksideSound {
    public static final String MOD_ID = "blocksidesound";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private BlocksideSound() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
