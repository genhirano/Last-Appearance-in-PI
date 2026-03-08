import model.pi.SurvivalSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalSet のテスト（カバレッジ 0% → フル検証）.
 *
 * 1. 指定範囲の全要素が含まれる
 * 2. ゼロパディングが正しく行われる
 * 3. start == end（単一要素）
 * 4. start > end（空セット）
 * 5. 範囲が桁数最大を超えた場合の打ち切り
 * 6. 重複がない（Set の性質）
 * 7. 1桁全範囲
 * 8. 2桁全範囲
 */
public class SurvivalSetTest extends TestBase {

    /** 指定範囲の全要素が含まれる */
    @Test
    void constructor_指定範囲の要素が全て含まれる() {
        SurvivalSet ss = new SurvivalSet(2, 0, 9);
        assertEquals(10, ss.size());
        assertTrue(ss.contains("00"));
        assertTrue(ss.contains("05"));
        assertTrue(ss.contains("09"));
    }

    /** ゼロパディングが桁数に合わせて正しく行われる */
    @Test
    void constructor_ゼロパディングが正しく行われる() {
        SurvivalSet ss = new SurvivalSet(3, 0, 5);
        assertTrue(ss.contains("000"));
        assertTrue(ss.contains("001"));
        assertTrue(ss.contains("005"));
        assertFalse(ss.contains("6"),   "パディングなしの \"6\" は含まれないこと");
        assertFalse(ss.contains("006"), "範囲外の \"006\" は含まれないこと（range は 0〜5）");
    }

    /** start == end のとき単一要素セットが生成される */
    @Test
    void constructor_startとendが同じ場合は単一要素を作成する() {
        SurvivalSet ss = new SurvivalSet(3, 42, 42);
        assertEquals(1, ss.size());
        assertTrue(ss.contains("042"));
    }

    /** start > end のとき IllegalArgumentException が投げられる */
    @Test
    void constructor_startがendより大きい場合はIllegalArgumentExceptionを投げる() {
        assertThrows(IllegalArgumentException.class,
                () -> new SurvivalSet(3, 10, 5),
                "start > end のとき IllegalArgumentException が投げられること");
    }

    /** 範囲が桁数最大を超えた場合は最大桁（999...9）で打ち切られる */
    @Test
    void constructor_範囲が桁数の最大を超えた場合に最大桁で打ち切られる() {
        SurvivalSet ss = new SurvivalSet(3, 990, 9999);
        assertTrue(ss.contains("999"), "\"999\" が含まれること");
        assertFalse(ss.contains("1000"), "4桁になる要素は追加されないこと");
    }

    /** HashSet なので重複がない */
    @Test
    void constructor_重複がない() {
        SurvivalSet ss = new SurvivalSet(2, 0, 99);
        assertEquals(100, ss.size(), "2桁の全パターン100件が重複なく格納されること");
    }

    /** 1桁全範囲（0〜9）が正しく格納される */
    @Test
    void constructor_1桁全範囲() {
        SurvivalSet ss = new SurvivalSet(1, 0, 9);
        assertEquals(10, ss.size());
        for (int i = 0; i <= 9; i++) {
            assertTrue(ss.contains(String.valueOf(i)),
                    "\"" + i + "\" が含まれること");
        }
    }

    /** 2桁全範囲（00〜99）が正しく格納される */
    @Test
    void constructor_2桁全範囲() {
        SurvivalSet ss = new SurvivalSet(2, 0, 99);
        for (int i = 0; i <= 99; i++) {
            String expected = String.format("%02d", i);
            assertTrue(ss.contains(expected), "\"" + expected + "\" が含まれること");
        }
    }
}
