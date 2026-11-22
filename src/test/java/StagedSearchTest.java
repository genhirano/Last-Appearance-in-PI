import model.pi.StagedSearch;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import controller.StoreController;
import model.pi.SearchThread;
import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
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

    @Test void パイロットがバッチ1を兼ねても生存率100パーセントで先頭位置を取りこぼさない() throws Exception {
        // 生存率100%では各スレッドが窓0を走査せずに閾値到達とみなすため、パイロットの補完走査が必要になる。
        // π小数部先頭は "1415..." なので、"1" / "14" は1桁目に初出する。各ケースでこれらの候補がパイロット（バッチ1）側に入るようにする。
        int[][] cases = {{2, 14, 23, 2, 3}, {2, 13, 99, 3, 10}, {1, 0, 9, 1, 1}, {1, 1, 9, 4, 10}};
        for (int[] c : cases) {
            StoreController.survivalProgressMap.clear();
            SurvivalResult expected = SearchThreadRealDataTest.runSurvival(c[0], c[1], c[2], 190,
                    SearchThreadRealDataTest.ycdFileList(1));
            SurvivalList combined = new SurvivalList(c[0], c[1], c[2]);
            SurvivalResult actual = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, c[3], c[4], 100)
                    .search(c[0], c[1], c[2], combined);
            assertNotNull(actual);
            assertEquals(expected.getTarget(), actual.getTarget());
            assertEquals(expected.getFindPos(), actual.getFindPos());
            assertTrue(combined.isEmpty());
            assertEquals(1L, combined.getDiscoveredPositions()[0]);
        }
        StoreController.survivalProgressMap.clear();
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

    @Test void stageTwoUsesMultipleGroupsAndPreservesResults() throws Exception {
        StoreController.survivalProgressMap.clear();
        class ObservedList extends SurvivalList {
            List<?> stageTwoGroups;
            ObservedList() { super(3, 0, 999); }
            @Override public synchronized void discover(String target, Long position) {
                if (Integer.valueOf(2).equals(StoreController.survivalProgressMap.get("SEARCH_STAGE"))
                        && stageTwoGroups == null) {
                    stageTwoGroups = (List<?>) StoreController.survivalProgressMap.get("STAGE1_WORKERS");
                }
                super.discover(target, position);
            }
        }
        ObservedList combined = new ObservedList();
        SurvivalResult expected = SearchThreadRealDataTest.runSurvival(3, 0, 999, 190,
                SearchThreadRealDataTest.ycdFileList(1));
        SurvivalResult actual = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 190, 4, 2, 100)
                .search(3, 0, 999, combined);
        assertNotNull(combined.stageTwoGroups);
        assertEquals(4, combined.stageTwoGroups.size());
        for (Object size : combined.stageTwoGroups) assertTrue((Integer) size > 0);
        assertEquals(expected.getTarget(), actual.getTarget());
        assertEquals(expected.getFindPos(), actual.getFindPos());
        assertEquals(1000, combined.getDiscoveredCount());
        assertEquals(100.0, StoreController.survivalProgressMap.get("STAGE1_PROGRESS"));
        StoreController.survivalProgressMap.clear();
    }

    @Test void stageTwoPreservesUnresolvedCandidatesAndDiscoveryPositions() throws Exception {
        StoreController.survivalProgressMap.clear();
        SurvivalList expected = new SurvivalList(6, 100000, 100199);
        try (YCD_SeqProvider provider = new YCD_SeqProvider(1, SearchThreadRealDataTest.ycdFileList(1), 6, 19000)) {
            while (provider.hasNext()) new SearchThread(expected, provider.next()).run();
        }
        SurvivalList combined = new SurvivalList(6, 100000, 100199);
        SurvivalResult result = new StagedSearch(SearchThreadRealDataTest.ycdFileList(1), 19000, 4, 2, 100)
                .search(6, 100000, 100199, combined);
        assertNull(result);
        assertFalse(expected.isEmpty());
        assertEquals(new ArrayList<>(expected), new ArrayList<>(combined));
        assertArrayEquals(expected.getDiscoveredPositions(), combined.getDiscoveredPositions());
        assertEquals(expected.getDiscoveredCount(), combined.getDiscoveredCount());
        StoreController.survivalProgressMap.clear();
    }
}
