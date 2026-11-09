import model.pi.SurvivalList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalList の大きな桁（int の範囲を超える値）・境界・省メモリ構造のテスト.
 *
 * 1. 10桁（int 最大値 2,147,483,647 をまたぐ範囲）で正しく動作する
 * 2. 12桁・18桁（long の範囲）で正しく動作する
 * 3. 桁数 0 / 19、負の start、start > end は例外
 * 4. end が全桁9を超える場合は全桁9で打ち切り、start が全桁9を超える場合は空
 * 5. 100万件のリストが短時間で生成でき、要素が正しい
 */
public class SurvivalListLongRangeTest {

    @Test
    void 十桁_int最大値をまたぐ範囲で正しく動作する() {
        long intMax = Integer.MAX_VALUE; // 2,147,483,647
        SurvivalList sl = new SurvivalList(10, intMax - 5, intMax + 5);
        assertEquals(11, sl.size());
        assertEquals("2147483642", sl.get(0));
        assertEquals("2147483647", sl.get(5));
        assertEquals("2147483648", sl.get(6), "int 最大値 + 1 が正しく扱えること");
        assertEquals("2147483652", sl.get(10));
        assertEquals("21474836", sl.getCommonPrefix(), "\"2147483642\" と \"2147483652\" の共通部分");

        assertTrue(sl.contains("2147483648"));
        assertEquals(6, sl.indexOf("2147483648"));
        sl.discover("2147483648", 123456789012L);
        assertFalse(sl.contains("2147483648"));
        assertEquals("2147483649", sl.get(6));
        assertEquals(123456789012L, sl.getDiscoverdInfo().get(0).getFindPos());
    }

    @Test
    void 十二桁_本番想定の範囲で正しく動作する() {
        SurvivalList sl = new SurvivalList(12, 999_999_900_000L, 999_999_999_999L);
        assertEquals(100_000, sl.size());
        assertEquals("999999900000", sl.get(0));
        assertEquals("999999999999", sl.get(99_999));
        assertEquals("9999999", sl.getCommonPrefix());

        sl.discover("999999950000", 1L);
        sl.discover(0, 2L);
        assertEquals(99_998, sl.size());
        assertEquals("999999900001", sl.get(0));
        // 元の位置 50001 から、手前で削除された 2 件（900000, 950000）分ずれる
        assertEquals(49_999, sl.indexOf("999999950001"));
        assertEquals("999999950001", sl.get(49_999));
        assertFalse(sl.contains("999999950000"));
        assertFalse(sl.contains("999999900000"));
        assertEquals(2, sl.getDiscoveredCount());
    }

    @Test
    void 十八桁_long最大範囲で正しく動作する() {
        long start = 999_999_999_999_999_990L;
        SurvivalList sl = new SurvivalList(18, start, 999_999_999_999_999_999L);
        assertEquals(10, sl.size());
        assertEquals("999999999999999990", sl.get(0));
        assertEquals("999999999999999999", sl.get(9));
        assertEquals("99999999999999999", sl.getCommonPrefix());
        sl.discover("999999999999999999", 10L);
        assertEquals("999999999999999998", sl.get(sl.size() - 1));
        assertEquals("99999999999999999", sl.getCommonPrefix());
    }

    @Test
    void 桁数や範囲が不正なら例外() {
        assertThrows(IllegalArgumentException.class, () -> new SurvivalList(0, 0, 9));
        assertThrows(IllegalArgumentException.class, () -> new SurvivalList(19, 0, 9));
        assertThrows(IllegalArgumentException.class, () -> new SurvivalList(3, -1, 9));
        assertThrows(IllegalArgumentException.class, () -> new SurvivalList(3, 10, 9));
    }

    @Test
    void endが全桁9を超える場合は打ち切り_startが超える場合は空() {
        SurvivalList sl = new SurvivalList(3, 995, 100_000L);
        assertEquals(5, sl.size());
        assertEquals("999", sl.get(4));

        SurvivalList empty = new SurvivalList(3, 1000, 2000);
        assertTrue(empty.isEmpty());
        assertEquals("", empty.getCommonPrefix());
        assertFalse(empty.contains("1000"));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
    }

    @Test
    void 百万件のリストが生成でき要素と反復が正しい() {
        long t0 = System.nanoTime();
        SurvivalList sl = new SurvivalList(12, 500_000_000_000L, 500_000_999_999L);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(1_000_000, sl.size());
        assertTrue(ms < 5000, "100万件の生成が5秒以内であること（実測 " + ms + " ms）");

        assertEquals("500000000000", sl.get(0));
        assertEquals("500000999999", sl.get(999_999));
        assertEquals("500000", sl.getCommonPrefix());

        // 反復は昇順に全件
        Iterator<String> it = sl.iterator();
        long expect = 500_000_000_000L;
        int count = 0;
        while (it.hasNext()) {
            String s = it.next();
            if (count % 99_991 == 0) {
                assertEquals(String.format("%012d", expect), s);
            }
            expect++;
            count++;
        }
        assertEquals(1_000_000, count);

        // 全件発見しても発見位置は全件、詳細は直近分だけ
        for (int i = 0; i < 1_000_000; i++) {
            sl.discover(0, (long) i);
        }
        assertTrue(sl.isEmpty());
        assertEquals(1_000_000, sl.getDiscoveredCount());
        assertEquals(1_000_000, sl.getDiscoveredPositions().length);
        ArrayList<SurvivalList.DiscoverdInfo> recent = sl.getDiscoverdInfo();
        assertEquals(SurvivalList.RECENT_DISCOVERED_CAPACITY, recent.size());
        assertEquals("500000999999", recent.get(recent.size() - 1).getTarget(), "直近詳細の最後は最後に発見した要素");
    }
}
