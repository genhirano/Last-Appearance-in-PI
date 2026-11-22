package model.pi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurvivalListSplitTest {
    @Test void splitsSparseSurvivorsIntoIndependentBalancedValueRanges() {
        long start = 999999999999999900L;
        SurvivalList source = new SurvivalList(18, start, start + 11);
        for (int i : new int[] {0, 2, 3, 5, 6, 8, 10, 11}) {
            source.discover(Long.toString(start + i), (long) i);
        }

        List<SurvivalList> parts = source.splitSurvivors(3);
        assertEquals(3, parts.size());
        assertEquals(Arrays.asList(2, 1, 1), Arrays.asList(parts.get(0).size(), parts.get(1).size(), parts.get(2).size()));
        List<String> merged = new ArrayList<>();
        for (SurvivalList part : parts) {
            assertEquals(0, part.getDiscoveredCount());
            merged.addAll(part);
        }
        assertEquals(new ArrayList<>(source), merged);
        assertEquals(Long.toString(start + 1), parts.get(0).get(0));
        assertEquals(Long.toString(start + 4), parts.get(0).get(1));
        parts.get(0).discover(Long.toString(start + 1), 42L);
        assertTrue(source.contains(Long.toString(start + 1)));
        assertArrayEquals(new long[] {42}, parts.get(0).getDiscoveredPositions());
        assertFalse(parts.get(0).contains(Long.toString(start + 2)));
    }

    @Test void emptyAndSmallListsDoNotCreateEmptyGroups() {
        SurvivalList list = new SurvivalList(1, 0, 1);
        assertEquals(2, list.splitSurvivors(10).size());
        list.discover("0", 1L);
        list.discover("1", 2L);
        assertEquals(Collections.emptyList(), list.splitSurvivors(3));
        assertThrows(IllegalArgumentException.class, () -> list.splitSurvivors(0));
    }
}
