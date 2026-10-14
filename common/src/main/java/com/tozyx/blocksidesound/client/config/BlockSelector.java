package com.tozyx.blocksidesound.client.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

/** A block ID or an existing block tag, prefixed with '#'. */
public record BlockSelector(ResourceLocation id, boolean tag) {
    public static BlockSelector parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Empty block selector");
        boolean tag = text.startsWith("#");
        String identifier = tag ? text.substring(1) : text;
        if (!identifier.contains(":")) throw new IllegalArgumentException("Use namespace:block or #namespace:tag: " + text);
        ResourceLocation id = ResourceLocation.tryParse(identifier);
        if (id == null) throw new IllegalArgumentException("Invalid block selector: " + text);
        return new BlockSelector(id, tag);
    }

    public boolean matches(BlockState state) {
        return tag ? state.is(TagKey.create(Registries.BLOCK, id))
                : id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
