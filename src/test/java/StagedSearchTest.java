import model.pi.StagedSearch;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import controller.StoreController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StagedSearchTest {
    @Test void 同一ブロックの複数閾値から最小位置を選び先行発見を再探索する() throws Exception {
        StoreController.survivalProgressMap.clear();
        SurvivalList combined = new SurvivalList(1, 0, 2);
        SurvivalResult result = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, 3, 1, 5)
                .search(1, 0, 2, combined);
        // π先頭の 1, 2, 0 は同じユニット内でそれぞれ1, 6, 32桁目に初出する。
        assertEquals(1L, StoreController.survivalProgressMap.get("STAGE1_DEPTH"));
        assertNotNull(result);
        assertEquals("0", result.getTarget());
        assertEquals(32L, result.getFindPos());
        assertArrayEquals(new long[] {1, 6, 32}, combined.getDiscoveredPositions());
        assertTrue(combined.isEmpty());
        StoreController.survivalProgressMap.clear();
    }
    @Test void variedPartitionsAndThresholdsMatchExistingSearch() throws Exception {
        int[][] cases = {{2, 30, 69, 1, 1, 100}, {2, 11, 23, 4, 5, 5},
                {3, 100, 159, 3, 4, 10}, {1, 0, 0, 4, 10, 5}};
        for (int[] c : cases) {
            SurvivalResult expected = SearchThreadRealDataTest.runSurvival(c[0], c[1], c[2], 190,
                    SearchThreadRealDataTest.ycdFileList(1));
            SurvivalList combined = new SurvivalList(c[0], c[1], c[2]);
            SurvivalResult actual = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, c[3], c[4], c[5])
                    .search(c[0], c[1], c[2], combined);
            assertNotNull(actual);
            assertEquals(expected.getTarget(), actual.getTarget());
            assertEquals(expected.getFindPos(), actual.getFindPos());
            assertEquals(c[2] - c[1] + 1, combined.getDiscoveredCount());
        }
    }
    @Test void stagedAndExistingSearchAgreeOnRealData() throws Exception {
        for (int length = 1; length <= 2; length++) {
            int end = length == 1 ? 9 : 99;
            SurvivalResult expected = SearchThreadRealDataTest.runSurvival(length, 0, end, 190,
                    SearchThreadRealDataTest.ycdFileList(1));
            SurvivalList combined = new SurvivalList(length, 0, end);
            SurvivalResult actual = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, 3, 10, 5)
                    .search(length, 0, end, combined);
            assertNotNull(actual);
            assertEquals(expected.getTarget(), actual.getTarget());
            assertEquals(expected.getFindPos(), actual.getFindPos());
            assertTrue(combined.isEmpty());
        }
    }

    @Test void invalidCombinedOrBoundsAreRejected() throws Exception {
        StagedSearch search = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, 3, 10, 5);
        assertThrows(IllegalArgumentException.class, () -> search.search(1, 0, 9, null));
        assertThrows(IllegalArgumentException.class, () -> search.search(1, 0, 9, new SurvivalList(2, 0, 9)));
        assertThrows(IllegalArgumentException.class, () -> search.search(1, 0, 10, new SurvivalList(1, 0, 9)));
        assertThrows(IllegalArgumentException.class, () -> search.search(1, 0, 9, new SurvivalList(1, 0, 8)));
        // 件数が一致しても範囲が異なるリストは拒否される
        assertThrows(IllegalArgumentException.class, () -> search.search(2, 10, 19, new SurvivalList(2, 0, 9)));
        // 一部発見済みで残り件数が一致するだけのリストも拒否される
        SurvivalList partiallyUsed = new SurvivalList(2, 10, 20);
        partiallyUsed.discover("20", 1L);
        assertThrows(IllegalArgumentException.class, () -> search.search(2, 10, 19, partiallyUsed));
    }
}
