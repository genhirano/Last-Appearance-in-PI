import model.pi.SurvivalResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalResult のテスト.
 *
 * 1. compareTo() の正確性（小・大・同値）
 * 2. update() の動作確認
 */
public class SurvivalResultTest extends TestBase {

    // ---- compareTo() ----

    /** 自分の発見位置が相手より小さい → 負の値を返す */
    @Test
    void compareTo_自分が小さい場合はマイナスを返す() {
        SurvivalResult smaller = new SurvivalResult("123", 5L);
        SurvivalResult larger  = new SurvivalResult("456", 10L);

        assertTrue(smaller.compareTo(larger) < 0,
                "findPos が小さいほうは負の値を返すべき");
    }

    /** 自分の発見位置が相手より大きい → 正の値を返す */
    @Test
    void compareTo_自分が大きい場合はプラスを返す() {
        SurvivalResult larger  = new SurvivalResult("456", 10L);
        SurvivalResult smaller = new SurvivalResult("123", 5L);

        assertTrue(larger.compareTo(smaller) > 0,
                "findPos が大きいほうは正の値を返すべき");
    }

    /** 同じ発見位置 → 0 を返す */
    @Test
    void compareTo_同じ場合はゼロを返す() {
        SurvivalResult a = new SurvivalResult("123", 100L);
        SurvivalResult b = new SurvivalResult("456", 100L);

        assertEquals(0, a.compareTo(b),
                "findPos が等しい場合は 0 を返すべき");
    }

    /** 自分自身と比較しても 0 を返す */
    @Test
    void compareTo_自分自身との比較はゼロ() {
        SurvivalResult a = new SurvivalResult("999", 42L);

        assertEquals(0, a.compareTo(a));
    }

    /** compareTo() は Collections.sort() で正しく使われる（推移律確認） */
    @Test
    void compareTo_推移律_a_lt_b_かつ_b_lt_c_ならば_a_lt_c() {
        SurvivalResult a = new SurvivalResult("1", 1L);
        SurvivalResult b = new SurvivalResult("2", 2L);
        SurvivalResult c = new SurvivalResult("3", 3L);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(c) < 0);
        assertTrue(a.compareTo(c) < 0, "推移律が成立すること");
    }

    // ---- update() ----

    /** update() で target と findPos が別オブジェクトの値で上書きされる */
    @Test
    void update_別オブジェクトのデータで更新される() {
        SurvivalResult base   = new SurvivalResult("000", -1L);
        SurvivalResult latest = new SurvivalResult("999", 12345L);

        base.update(latest);

        assertEquals("999",   base.getTarget());
        assertEquals(12345L,  base.getFindPos());
    }

    /** update() 後も元のオブジェクトは変化しない */
    @Test
    void update_元のオブジェクトは変化しない() {
        SurvivalResult base   = new SurvivalResult("111", 10L);
        SurvivalResult latest = new SurvivalResult("999", 99L);

        base.update(latest);

        // latest は変化していない
        assertEquals("999", latest.getTarget());
        assertEquals(99L,   latest.getFindPos());
    }
}
