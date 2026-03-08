import model.pi.SurvivalList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalList.discover() のテスト.
 *
 * 1. discover(String, Long) でリストから要素が削除されること
 * 2. discover(String, Long) で発見済みリストに記録されること
 * 3. 存在しない要素を discover() するとRuntimeExceptionが発生すること
 * 4. discover(int, Long) でインデックス指定による削除が行えること
 * 5. 複数回の discover() が正しく累積されること
 */
public class SurvivalListDiscoverTest extends TestBase {

    /** discover(String, Long) 後、対象がリストから削除される */
    @Test
    void discover_文字列指定で要素がリストから削除される() {
        SurvivalList sl = new SurvivalList(3, 0, 9); // ["000","001",...,"009"]

        assertTrue(sl.contains("003"));
        sl.discover("003", 100L);
        assertFalse(sl.contains("003"), "発見後はリストに残らないこと");
    }

    /** discover(String, Long) 後、発見済みリストに追加される */
    @Test
    void discover_発見済みリストに追加される() {
        SurvivalList sl = new SurvivalList(3, 0, 9);

        assertEquals(0, sl.getDiscoverdInfo().size());
        sl.discover("005", 200L);

        assertEquals(1, sl.getDiscoverdInfo().size());
        assertEquals("005", sl.getDiscoverdInfo().get(0).getTarget());
        assertEquals(200L,  sl.getDiscoverdInfo().get(0).getFindPos());
    }

    /** 存在しない要素を discover() すると RuntimeException が発生する */
    @Test
    void discover_存在しない要素は例外を投げる() {
        SurvivalList sl = new SurvivalList(3, 0, 5); // "000".."005"

        assertThrows(RuntimeException.class,
                () -> sl.discover("999", 0L),
                "リストにない要素を discover() したら例外が出ること");
    }

    /** discover(int, Long) でインデックス指定による削除が行える */
    @Test
    void discover_インデックス指定で要素がリストから削除される() {
        SurvivalList sl = new SurvivalList(3, 0, 9);
        String first = sl.get(0); // "000"

        sl.discover(0, 50L);

        assertFalse(sl.contains(first), "インデックス0の要素が削除されること");
        assertEquals(1, sl.getDiscoverdInfo().size());
        assertEquals(first, sl.getDiscoverdInfo().get(0).getTarget());
    }

    /** 複数回 discover() すると全て発見済みリストに累積される */
    @Test
    void discover_複数回発見が累積される() {
        SurvivalList sl = new SurvivalList(3, 0, 99); // "000".."099"

        sl.discover("010", 10L);
        sl.discover("020", 20L);
        sl.discover("030", 30L);

        assertEquals(3, sl.getDiscoverdInfo().size(), "3回の発見が全て記録されること");
        assertFalse(sl.contains("010"));
        assertFalse(sl.contains("020"));
        assertFalse(sl.contains("030"));
    }

    /** discover() で発見済みリストには findDateTime が記録される */
    @Test
    void discover_発見日時が記録される() {
        SurvivalList sl = new SurvivalList(3, 0, 9);

        sl.discover("001", 42L);

        assertNotNull(sl.getDiscoverdInfo().get(0).getFindDateTime(),
                "発見日時がnullでないこと");
    }

    /** SurvivalList の remove(int) は使用禁止で RuntimeException を投げる */
    @Test
    void remove_使用禁止メソッドは例外を投げる() {
        SurvivalList sl = new SurvivalList(3, 0, 9);

        assertThrows(RuntimeException.class,
                () -> sl.remove(0),
                "remove(int) は使用禁止であること");
    }
}
