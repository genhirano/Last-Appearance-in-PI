import model.pi.SurvivalList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalList 内部構造（生存フラグ＋フェニック木）の正確性テスト.
 *
 * 単純な ArrayList を「正解」として、ランダムな順序で discover したときに
 * get / indexOf / contains / size / 反復 / getCommonPrefix が常に一致することを確認する.
 */
public class SurvivalListStructureTest {

    @ParameterizedTest
    @CsvSource({
            // length, start, end, seed
            "1, 0, 9, 1",
            "2, 0, 99, 2",
            "3, 100, 357, 3",
            "4, 0, 1000, 4",
            "5, 12345, 14000, 5",
            "6, 0, 4095, 6",   // 2のべき乗境界
            "6, 0, 4096, 7",
    })
    void ランダムに発見してもArrayListと同じ振る舞いをする(int length, int start, int end, long seed) {
        SurvivalList sl = new SurvivalList(length, start, end);
        List<String> ref = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            ref.add(String.format("%0" + length + "d", i));
        }
        assertSame(sl, sl);
        assertEquals(ref, new ArrayList<>(sl), "初期状態が一致すること");

        Random r = new Random(seed);
        long pos = 0;
        while (!ref.isEmpty()) {
            // 文字列指定とインデックス指定を交互に使う
            int idx = r.nextInt(ref.size());
            String target = ref.get(idx);
            if (r.nextBoolean()) {
                sl.discover(target, pos++);
            } else {
                assertEquals(target, sl.get(idx));
                sl.discover(idx, pos++);
            }
            ref.remove(idx);

            assertEquals(ref.size(), sl.size());
            assertFalse(sl.contains(target));
            assertEquals(-1, sl.indexOf(target));

            // 何件かランダムに抜き取り検査
            for (int k = 0; k < 3 && !ref.isEmpty(); k++) {
                int j = r.nextInt(ref.size());
                assertEquals(ref.get(j), sl.get(j), "get が一致すること");
                assertEquals(j, sl.indexOf(ref.get(j)), "indexOf が一致すること");
                assertTrue(sl.contains(ref.get(j)));
            }
            if (!ref.isEmpty()) {
                assertEquals(ref.get(0), sl.get(0));
                assertEquals(ref.get(ref.size() - 1), sl.get(sl.size() - 1));
            }
            // 時々全体を比較
            if (ref.size() % 97 == 0) {
                assertEquals(ref, new ArrayList<>(sl), "反復結果が一致すること");
                assertEquals(commonPrefix(ref), sl.getCommonPrefix(), "共通プレフィックスが一致すること");
            }
        }
        assertTrue(sl.isEmpty());
        assertEquals(end - start + 1, sl.getDiscoveredCount());
        assertEquals(end - start + 1, sl.getDiscoveredPositions().length);
        assertEquals(Math.min(end - start + 1, SurvivalList.RECENT_DISCOVERED_CAPACITY),
                sl.getDiscoverdInfo().size(), "詳細は直近分のみ保持すること");

        // 発見位置のスナップショットは昇順
        long[] positions = sl.getDiscoveredPositions();
        for (int i = 1; i < positions.length; i++) {
            assertTrue(positions[i - 1] <= positions[i], "発見位置が昇順であること");
        }
    }

    @Test
    void 範囲外や形式違いの文字列は含まれない() {
        SurvivalList sl = new SurvivalList(3, 100, 199);
        assertFalse(sl.contains("099"));
        assertFalse(sl.contains("200"));
        assertFalse(sl.contains("15"));
        assertFalse(sl.contains("1a5"));
        assertFalse(sl.contains(null));
        assertFalse(sl.contains(150));
        assertEquals(-1, sl.indexOf("200"));
        assertThrows(RuntimeException.class, () -> sl.discover("200", 0L));
    }

    @Test
    void 発見済みの再発見は例外() {
        SurvivalList sl = new SurvivalList(2, 0, 9);
        sl.discover("05", 1L);
        assertThrows(RuntimeException.class, () -> sl.discover("05", 2L));
    }

    @Test
    void 範囲外インデックスは例外() {
        SurvivalList sl = new SurvivalList(2, 0, 9);
        assertThrows(IndexOutOfBoundsException.class, () -> sl.get(10));
        assertThrows(IndexOutOfBoundsException.class, () -> sl.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sl.discover(10, 0L));
    }

    @Test
    void 変更系メソッドは使用できない() {
        SurvivalList sl = new SurvivalList(2, 0, 9);
        assertThrows(RuntimeException.class, () -> sl.add("10"));
        assertThrows(RuntimeException.class, () -> sl.set(0, "10"));
        assertThrows(RuntimeException.class, () -> sl.remove(0));
    }

    private static String commonPrefix(List<String> list) {
        if (list.isEmpty()) {
            return "";
        }
        String a = list.get(0);
        String b = list.get(list.size() - 1);
        int i = 0;
        while (i < a.length() && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return a.substring(0, i);
    }
}
