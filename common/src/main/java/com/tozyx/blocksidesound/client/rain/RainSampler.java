package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.BlocksideSound;
import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Budgeted surface detection; playback reuses this query to reject stale samples. */
public final class RainSampler {
    private final Map<Long, RainSurface> surfaces = new HashMap<>();
    private ClientLevel level;
    private ColumnScanPlan plan;
    private MaterialResolver resolver;
    private SoundConfig.Rain settings;
    private BlockPos lastCenter;
    private long ticks;

    public void configure(SoundConfig.Rain settings) {
        this.settings = settings;
        plan = new ColumnScanPlan(settings.radius);
        resolver = new MaterialResolver(settings);
        clear();
    }

    public void clear() {
        surfaces.clear();
        level = null;
        lastCenter = null;
        ticks = 0;
        if (plan != null) plan.reset();
    }

    public List<RainSurface> surfaces() {
        return List.copyOf(surfaces.values());
    }

    public void tick(ClientLevel world, Vec3 listener) {
        if (settings == null) return;
        if (world != level) {
            clear();
            level = world;
        }
        if (!settings.enabled || world.getRainLevel(1.0F) <= 0.0F) {
            surfaces.clear();
            ticks = 0;
            plan.reset();
            return;
        }
        BlockPos center = BlockPos.containing(listener);
        if (lastCenter != null && (Math.abs(center.getX() - lastCenter.getX()) > settings.radius
                || Math.abs(center.getZ() - lastCenter.getZ()) > settings.radius
                || Math.abs(center.getY() - lastCenter.getY()) > settings.verticalRange)) {
            surfaces.clear();
            plan.reset();
        }
        lastCenter = center;
        long currentTick = ticks++;
        // Keep samples for two full scan cycles; playback revalidates each chosen surface.
        long ttl = Math.max(40L, 2L * ((plan.size() + settings.scanBudget - 1) / settings.scanBudget)
                * settings.scanIntervalTicks);
        surfaces.values().removeIf(surface -> currentTick - surface.checkedAt() > ttl
                || !inside(surface, listener) || !world.hasChunkAt(surface.block()));
        if (currentTick % settings.scanIntervalTicks != 0) {
            if (settings.debugLogging && currentTick % 100 == 0) logSnapshot(world, listener);
            return;
        }
        int markers = 0;
        for (ColumnScanPlan.Offset offset : plan.next(settings.scanBudget)) {
            int x = center.getX() + offset.x();
            int z = center.getZ() + offset.z();
            RainSurface surface = inspect(world, listener, x, z, currentTick);
            long key = ((long) x << 32) | (z & 0xffffffffL);
            if (surface == null) surfaces.remove(key);
            else {
                surfaces.put(key, surface);
                if (settings.debugParticles && markers++ < 8) showMarker(world, surface);
            }
        }
        if (settings.debugLogging && currentTick % 100 == 0) logSnapshot(world, listener);
    }

    public RainSurface revalidate(ClientLevel world, Vec3 listener, RainSurface cached) {
        if (settings == null || !settings.enabled || world.getRainLevel(1.0F) <= 0) return null;
        RainSurface current = inspect(world, listener, cached.block().getX(), cached.block().getZ(), ticks);
        long key = ((long) cached.block().getX() << 32) | (cached.block().getZ() & 0xffffffffL);
        // A removed/covered anchor must not remain a preferred stale candidate until the next scan pass.
        if (current == null) surfaces.remove(key);
        else surfaces.put(key, current);
        return current != null && current.block().equals(cached.block()) ? current : null;
    }

    private boolean inside(RainSurface surface, Vec3 listener) {
        double dx = surface.position().x - listener.x;
        double dz = surface.position().z - listener.z;
        return dx * dx + dz * dz <= settings.radius * settings.radius
                && Math.abs(surface.position().y - listener.y) <= settings.verticalRange;
    }

    private RainSurface inspect(ClientLevel world, Vec3 listener, int x, int z, long tick) {
        if (!world.hasChunk(x >> 4, z >> 4)) return null;
        int blocking = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int top = Math.max(blocking, world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)) - 1;
        if (top < world.getMinBuildHeight() || top - listener.y > settings.verticalRange + 1) return null;
        // Outline shapes supplement the heightmap for thin, non-colliding drip plants.
        int bottom = Math.max(world.getMinBuildHeight(), Math.max(blocking - 1, top - 4));
        for (int y = top; y >= bottom; y--) {
            BlockPos position = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(position);
            if (state.isAir()) continue;
            boolean plant = MaterialResolver.supportsPlantDrips(state) || state.is(Blocks.LILY_PAD);
            VoxelShape shape = plant ? state.getShape(world, position) : state.getCollisionShape(world, position);
            double solidHeight = shape.isEmpty() ? Double.NEGATIVE_INFINITY : shape.max(Direction.Axis.Y, 0.5, 0.5);
            FluidState fluid = world.getFluidState(position);
            double fluidHeight = fluid.isEmpty() ? Double.NEGATIVE_INFINITY : fluid.getHeight(world, position);
            if (!Double.isFinite(solidHeight) && !Double.isFinite(fluidHeight)) continue;
            double height = Math.max(solidHeight, fluidHeight);
            Vec3 impact = new Vec3(x + 0.5, y + height, z + 0.5);
            BlockPos above = BlockPos.containing(impact.x, impact.y + 0.01, impact.z);
            if (Math.abs(impact.y - listener.y) > settings.verticalRange
                    || world.getBiome(above).value().getPrecipitationAt(above) != Biome.Precipitation.RAIN) return null;
            boolean water = fluid.is(FluidTags.WATER) && fluidHeight > solidHeight;
            RainSurface surface = new RainSurface(position, impact, resolver.resolve(state, water), tick);
            return inside(surface, listener) ? surface : null;
        }
        return null;
    }

    private static void showMarker(ClientLevel world, RainSurface surface) {
        Vector3f color = switch (surface.match().material()) {
            case WOOD -> new Vector3f(0.7F, 0.4F, 0.15F);
            case METAL -> new Vector3f(1.0F, 0.75F, 0.1F);
            case GLASS -> new Vector3f(0.4F, 1.0F, 1.0F);
            case FOLIAGE -> new Vector3f(0.2F, 1.0F, 0.2F);
            case WATER -> new Vector3f(0.2F, 0.4F, 1.0F);
            default -> new Vector3f(0.9F, 0.9F, 0.9F);
        };
        if (!surface.match().enhanced()) color.set(1.0F, 0.1F, 0.1F);
        Vec3 point = surface.position();
        world.addParticle(new DustParticleOptions(color, 0.8F), point.x, point.y + 0.1, point.z, 0, 0, 0);
    }

    private void logSnapshot(ClientLevel world, Vec3 listener) {
        Map<RainMaterial, Integer> counts = new EnumMap<>(RainMaterial.class);
        int enhanced = 0;
        int drips = 0;
        RainSurface nearest = null;
        for (RainSurface surface : surfaces.values()) {
            counts.merge(surface.match().material(), 1, Integer::sum);
            if (surface.match().enhanced()) enhanced++;
            if (surface.match().plantDrips()) drips++;
            if (nearest == null || surface.position().distanceToSqr(listener) < nearest.position().distanceToSqr(listener)) nearest = surface;
        }
        BlocksideSound.LOGGER.info("Rain scan: candidates={}, enhanced={}, plantDrips={}, materials={}, nearest={}",
                surfaces.size(), enhanced, drips, counts, nearest == null ? "none" : nearest.block() + " "
                        + world.getBlockState(nearest.block()) + " -> " + nearest.match().material().id());
    }
}
