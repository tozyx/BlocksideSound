package com.tozyx.blocksidesound.client.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** One collision-shape ray per native rain request, rather than another area scan. */
final class RainOcclusion {
    private RainOcclusion() {}

    static boolean blocked(BlockGetter world, Vec3 listener, Vec3 nativeSource) {
        BlockPos surface = BlockPos.containing(nativeSource);
        var shape = world.getBlockState(surface).getCollisionShape(world, surface);
        double top = Math.max(shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y),
                world.getFluidState(surface).getHeight(world, surface));
        // Vanilla emits from the rain-hit block's centre. Tracing to that centre would
        // incorrectly treat every exposed floor as an obstruction. Aim just above its top.
        Vec3 rainPoint = new Vec3(nativeSource.x,
                Math.max(nativeSource.y, surface.getY() + top + .01), nativeSource.z);
        return world.clip(new ClipContext(listener, rainPoint, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.BLOCK;
    }
}
