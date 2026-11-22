import model.pi.StageOnePartition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StageOnePartitionTest {
    @Test void smallRangeUsesOnlyNonemptyGroups() {
        StageOnePartition p = new StageOnePartition(0, 9, 3, 10);
        assertEquals(10, p.groupCount());
        assertEquals(4, p.batchCount());
        for (int i = 0; i < 10; i++) {
            assertEquals(i, p.start(i));
            assertEquals(i, p.end(i));
        }
    }

    @Test void partitionsAreBalancedAndCoverRange() {
        for (int count = 1; count <= 200; count++) {
            StageOnePartition p = new StageOnePartition(100, 99 + count, 3, 10);
            long next = 100, min = Long.MAX_VALUE, max = 0;
            for (int i = 0; i < p.groupCount(); i++) {
                assertEquals(next, p.start(i));
                long size = p.end(i) - p.start(i) + 1;
                assertTrue(size > 0);
                min = Math.min(min, size);
                max = Math.max(max, size);
                next = p.end(i) + 1;
            }
            assertEquals(100 + count, next);
            assertTrue(max - min <= 1);
        }
    }

    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StageOnePartition(1, 0, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> new StageOnePartition(0, 9, 0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> new StageOnePartition(0, 0, 3, 10).end(1));
    }
}
