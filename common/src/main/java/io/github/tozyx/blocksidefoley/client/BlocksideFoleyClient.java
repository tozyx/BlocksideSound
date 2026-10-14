package io.github.tozyx.blocksidefoley.client;

import io.github.tozyx.blocksidefoley.BlocksideFoley;

/** Shared client entry point for future sound features. */
public final class BlocksideFoleyClient {
    private BlocksideFoleyClient() {
    }

    public static void init() {
        BlocksideFoley.LOGGER.info("Blockside Foley client initialized ({})", BlocksideFoley.id("client"));
    }
}
