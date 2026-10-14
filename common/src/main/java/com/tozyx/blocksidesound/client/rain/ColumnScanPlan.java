package com.tozyx.blocksidesound.client.rain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Complete circular coverage, near columns first, with a persistent batch cursor. */
public final class ColumnScanPlan {
    private final List<Offset> offsets;
    private int cursor;

    public ColumnScanPlan(int radius) {
        if (radius < 0 || radius > 32) throw new IllegalArgumentException("Radius outside 0..32");
        List<Offset> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) offsets.add(new Offset(x, z));
            }
        }
        offsets.sort(Comparator.comparingInt(Offset::distanceSquared)
                .thenComparingDouble(offset -> Math.atan2(offset.z, offset.x)));
        this.offsets = List.copyOf(offsets);
    }

    public List<Offset> next(int budget) {
        List<Offset> batch = new ArrayList<>();
        for (int index = 0; index < Math.min(Math.max(0, budget), offsets.size()); index++) {
            batch.add(offsets.get(cursor));
            cursor = (cursor + 1) % offsets.size();
        }
        return batch;
    }

    public int size() {
        return offsets.size();
    }

    public void reset() {
        cursor = 0;
    }

    public record Offset(int x, int z) {
        public int distanceSquared() { return x * x + z * z; }
    }
}
