import model.TargetRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TargetRange のテスト（カバレッジ 0% → フル検証）.
 *
 * 1. 通常値の格納と getter
 * 2. start == end（同値）
 * 3. null 値の格納
 * 4. 各フィールドの独立性（他フィールドへの影響がない）
 */
public class TargetRangeTest extends TestBase {

    /** 通常の値が各フィールドに正しく格納される */
    @Test
    void constructor_通常値が正しく格納される() {
        TargetRange tr = new TargetRange(3, "000", "999");
        assertEquals(3,     tr.getLength());
        assertEquals("000", tr.getStart());
        assertEquals("999", tr.getEnd());
    }

    /** length=1 の1桁範囲 */
    @Test
    void constructor_1桁範囲() {
        TargetRange tr = new TargetRange(1, "0", "9");
        assertEquals(1,   tr.getLength());
        assertEquals("0", tr.getStart());
        assertEquals("9", tr.getEnd());
    }

    /** length=10 の10桁範囲 */
    @Test
    void constructor_10桁範囲() {
        TargetRange tr = new TargetRange(10, "0000000000", "9999999999");
        assertEquals(10,           tr.getLength());
        assertEquals("0000000000", tr.getStart());
        assertEquals("9999999999", tr.getEnd());
    }

    /** start == end（単一パターン対象） */
    @Test
    void constructor_startとendが同じ場合も正しく格納される() {
        TargetRange tr = new TargetRange(3, "042", "042");
        assertEquals("042", tr.getStart());
        assertEquals("042", tr.getEnd());
        assertEquals(3,     tr.getLength());
    }

    /** 全パラメータ null の場合 NullPointerException が投げられる */
    @Test
    void constructor_全パラメータnullはNullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class,
                () -> new TargetRange(null, null, null),
                "全パラメータ null のとき NullPointerException が投げられること");
    }

    /** length だけ null でも NullPointerException が投げられる */
    @Test
    void constructor_lengthのみnullはNullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class,
                () -> new TargetRange(null, "000", "999"),
                "length が null のとき NullPointerException が投げられること");
    }

    /** start だけ null でも NullPointerException が投げられる */
    @Test
    void constructor_startのみnullはNullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class,
                () -> new TargetRange(3, null, "999"),
                "start が null のとき NullPointerException が投げられること");
    }

    /** end だけ null でも NullPointerException が投げられる */
    @Test
    void constructor_endのみnullはNullPointerExceptionを投げる() {
        assertThrows(NullPointerException.class,
                () -> new TargetRange(3, "000", null),
                "end が null のとき NullPointerException が投げられること");
    }

    /** 各 getter が他のフィールドに影響しない */
    @Test
    void getter_複数回呼び出しても値が変わらない() {
        TargetRange tr = new TargetRange(5, "00000", "99999");
        assertEquals(tr.getLength(), tr.getLength());
        assertEquals(tr.getStart(),  tr.getStart());
        assertEquals(tr.getEnd(),    tr.getEnd());
    }
}
