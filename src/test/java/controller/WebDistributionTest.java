package controller;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WebDistributionTest {
    @Test
    void includesRemainderAndKeepsSmallPositionCounts() {
        Map<Long, Long> distribution = Web.distribution(new long[]{1, 28, 30, 31, 32});
        assertEquals(3L, distribution.get(32L));
        assertEquals(5L, distribution.values().stream().mapToLong(Long::longValue).sum());
        Map<Long, Long> small = Web.distribution(new long[]{1, 2});
        assertEquals(1L, small.get(1L));
        assertEquals(1L, small.get(2L));
        assertEquals(2L, small.values().stream().mapToLong(Long::longValue).sum());
    }
}
