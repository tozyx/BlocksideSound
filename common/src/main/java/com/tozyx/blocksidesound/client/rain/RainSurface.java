package com.tozyx.blocksidesound.client.rain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record RainSurface(BlockPos block, Vec3 position, MaterialResolver.Match match, long checkedAt) {}
