import model.pi.SurvivalList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SurvivalList のエッジケーステスト.
 *
 * 1. getCommonPrefix() - 空リスト（修正済みバグの回帰テスト）
 * 2. getCommonPrefix() - 単一要素
 * 3. constructor - start == end（単一要素）
 * 4. constructor - start > end（空リスト）
 * 5. constructor - 範囲が桁数最大を超えた場合の打ち切り
 */
public class SurvivalListEdgeCaseTest extends TestBase {

    /** 空リストで getCommonPrefix() を呼んでも例外が出ず空文字を返す（修正済みバグの回帰テスト） */
    @Test
    void getCommonPrefix_空リストは空文字を返す() {
        SurvivalList sl = new SurvivalList(3, 0, 0); // ["000"]
        sl.discover("000", 1L); // 全要素を discover して空にする
        assertTrue(sl.isEmpty(), "前提: リストが空であること");
        assertEquals("", sl.getCommonPrefix(), "空リストの getCommonPrefix() は空文字を返すこと");
    }

    /** 単一要素のとき getCommonPrefix() はその要素そのものを返す */
    @Test
    void getCommonPrefix_単一要素はその要素を返す() {
        SurvivalList sl = new SurvivalList(3, 5, 5); // ["005"]
        assertEquals("005", sl.getCommonPrefix(), "単一要素の場合はその値が共通プレフィックスとなること");
    }

    /** start == end のとき単一要素リストが生成される */
    @Test
    void constructor_startとendが同じ場合は単一要素リストを作成する() {
        SurvivalList sl = new SurvivalList(3, 5, 5);
        assertEquals(1, sl.size());
        assertEquals("005", sl.get(0));
    }

    /** start > end のとき IllegalArgumentException が投げられる */
    @Test
    void constructor_startがendより大きい場合はIllegalArgumentExceptionを投げる() {
        assertThrows(IllegalArgumentException.class,
                () -> new SurvivalList(3, 10, 5),
                "start > end のとき IllegalArgumentException が投げられること");
    }

    /** 範囲が桁数最大（999...9）を超えた場合は 999...9 で打ち切られる */
    @Test
    void constructor_範囲が桁数の最大を超えた場合に最大桁で打ち切られる() {
        SurvivalList sl = new SurvivalList(3, 990, 9999); // "990"〜"999" までが有効、"1000"は4桁で打ち切り
        assertTrue(sl.contains("999"), "\"999\" が含まれること");
        assertFalse(sl.contains("1000"), "4桁になる要素は追加されないこと");
        assertEquals("999", sl.get(sl.size() - 1), "末尾要素が \"999\" であること");
    }

    /** discover で空になったあとも getCommonPrefix() が連続呼び出しに耐える */
    @Test
    void getCommonPrefix_空リストへの連続呼び出しもクラッシュしない() {
        SurvivalList sl = new SurvivalList(2, 0, 1); // ["00", "01"]
        sl.discover("00", 10L);
        sl.discover("01", 20L);
        assertTrue(sl.isEmpty());

        // 複数回呼んでも例外が出ないこと
        assertEquals("", sl.getCommonPrefix());
        assertEquals("", sl.getCommonPrefix());
    }
}
