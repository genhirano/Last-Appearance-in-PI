import model.pi.StagedSearch;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StagedSearchTest {
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
}
