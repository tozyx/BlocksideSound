package io.github.tozyx.blocksidefoley;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlocksideFoley {
    public static final String MOD_ID = "blocksidefoley";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private BlocksideFoley() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
