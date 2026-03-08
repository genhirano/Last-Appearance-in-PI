import model.pi.SurvivalResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalResult のエッジケーステスト.
 *
 * 1. constructor - 初期値の確認
 * 2. compareTo() - デフォルト値 -1L との比較
 * 3. compareTo() - Long.MAX_VALUE との比較
 * 4. compareTo() - findPos == 0 の境界値
 * 5. update() - 自分自身で更新
 * 6. setTarget() / setFindPos() の動作確認
 */
public class SurvivalResultEdgeCaseTest extends TestBase {

    /** constructor で渡した値が getter で正しく取得できる */
    @Test
    void constructor_初期値が正しく設定される() {
        SurvivalResult sr = new SurvivalResult("123", 42L);
        assertEquals("123", sr.getTarget());
        assertEquals(42L,   sr.getFindPos());
    }

    /** SearchThread の初期値パターン（"", -1L）が正常に生成できる */
    @Test
    void constructor_デフォルト初期値パターンが生成できる() {
        SurvivalResult sr = new SurvivalResult("", -1L);
        assertEquals("",  sr.getTarget());
        assertEquals(-1L, sr.getFindPos());
    }

    /** -1L（未発見）と正の位置の比較 */
    @Test
    void compareTo_デフォルトのマイナス1Lは正値より小さい() {
        SurvivalResult notFound = new SurvivalResult("", -1L);
        SurvivalResult found    = new SurvivalResult("123", 100L);
        assertTrue(notFound.compareTo(found) < 0, "未発見(-1L)は発見済みより小さいこと");
    }

    /** Long.MAX_VALUE は通常値より大きい */
    @Test
    void compareTo_LongMaxValueは通常値より大きい() {
        SurvivalResult big    = new SurvivalResult("999", Long.MAX_VALUE);
        SurvivalResult normal = new SurvivalResult("123", 100L);
        assertTrue(big.compareTo(normal) > 0, "Long.MAX_VALUE は 100L より大きいこと");
    }

    /** findPos == 0 は -1L より大きい */
    @Test
    void compareTo_findPosがゼロはマイナス1Lより大きい() {
        SurvivalResult zero     = new SurvivalResult("000", 0L);
        SurvivalResult negative = new SurvivalResult("", -1L);
        assertTrue(zero.compareTo(negative) > 0, "findPos=0 は -1L より大きいこと");
    }

    /** update(self) で自分自身を渡してもクラッシュせず値が維持される */
    @Test
    void update_自分自身で更新しても値は変わらない() {
        SurvivalResult sr = new SurvivalResult("abc", 99L);
        sr.update(sr);
        assertEquals("abc", sr.getTarget());
        assertEquals(99L,   sr.getFindPos());
    }

    /** setTarget() で target を更新できる */
    @Test
    void setTarget_値を更新できる() {
        SurvivalResult sr = new SurvivalResult("old", 1L);
        sr.setTarget("new");
        assertEquals("new", sr.getTarget());
    }

    /** setFindPos() で findPos を更新できる */
    @Test
    void setFindPos_値を更新できる() {
        SurvivalResult sr = new SurvivalResult("abc", 1L);
        sr.setFindPos(9999L);
        assertEquals(9999L, sr.getFindPos());
    }

    /** setTarget(null) を呼んでも getter は null を返す（NPE しない） */
    @Test
    void setTarget_nullを設定できる() {
        SurvivalResult sr = new SurvivalResult("abc", 1L);
        sr.setTarget(null);
        assertNull(sr.getTarget());
    }
}
