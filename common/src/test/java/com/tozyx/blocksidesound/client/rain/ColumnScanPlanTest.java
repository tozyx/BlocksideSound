package com.tozyx.blocksidesound.client.rain;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColumnScanPlanTest {
    @Test void boundedBatchesCoverCircleAndNeverLeaveIt() {
        ColumnScanPlan plan = new ColumnScanPlan(12);
        Set<ColumnScanPlan.Offset> visited = new HashSet<>();
        int rounds = (plan.size() + 63) / 64;
        for (int batch = 0; batch < rounds; batch++) {
            var offsets = plan.next(64);
            assertTrue(offsets.size() <= 64);
            assertEquals(offsets.size(), new HashSet<>(offsets).size());
            for (var offset : offsets) {
                assertTrue(offset.distanceSquared() <= 144);
                visited.add(offset);
            }
        }
        Set<ColumnScanPlan.Offset> expected = new HashSet<>();
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
            if (x * x + z * z <= 144) expected.add(new ColumnScanPlan.Offset(x, z));
        }
        assertEquals(expected, visited);
    }

    @Test void prioritizesListenerColumnAndHandlesBudgetLargerThanArea() {
        ColumnScanPlan plan = new ColumnScanPlan(1);
        assertEquals(new ColumnScanPlan.Offset(0, 0), plan.next(1).getFirst());
        assertEquals(5, plan.next(256).size());
        assertEquals(0, plan.next(0).size());
    }
}
