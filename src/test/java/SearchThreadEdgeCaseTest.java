import model.pi.SearchThread;
import model.pi.SurvivalList;
import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchThread の Boyer-Moore エッジケーステスト.
 *
 * 1. パターン == テキスト全体
 * 2. パターンがテキストより長い → -1
 * 3. startIndex >= text.length() → -1
 * 4. startIndex == text.length() ちょうど → -1
 * 5. パターンがテキスト末尾ちょうどに存在する
 * 6. startIndex によって以前の出現がスキップされる
 * 7. 空の SurvivalList で run() してもクラッシュしない（修正済みバグの回帰テスト）
 * 8. 空の SurvivalList で SL アルゴリズムが選ばれ findPos が -1L のまま
 */
public class SearchThreadEdgeCaseTest extends TestBase {

    private static YCD_SeqProvider testProvider;

    @BeforeAll
    static void セットアップ() throws Exception {
        URL url = SearchThreadEdgeCaseTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));
        testProvider = new YCD_SeqProvider(1, fileList, 5, 100);
    }

    @AfterAll
    static void クリーンアップ() throws Exception {
        if (testProvider != null) {
            testProvider.close();
        }
    }

    // =========================================================
    // Boyer-Moore エッジケース
    // =========================================================

    /** パターンがテキスト全体と一致する場合に位置 0 を返す */
    @Test
    void boyerMooreSearch_パターンがテキスト全体と一致する() throws Exception {
        int result = invokeBoyerMooreSearch("12345", "12345", 0);
        assertEquals(0, result, "パターンがテキスト全体と一致するとき位置0を返すこと");
    }

    /** パターンがテキストより長い場合は -1 を返す */
    @Test
    void boyerMooreSearch_パターンがテキストより長い場合はマイナス1を返す() throws Exception {
        int result = invokeBoyerMooreSearch("123", "12345", 0);
        assertEquals(-1, result, "パターンがテキストより長い場合は -1 を返すこと");
    }

    /** startIndex がテキスト長を超える場合は -1 を返す */
    @Test
    void boyerMooreSearch_startIndexがテキスト長を超える場合はマイナス1を返す() throws Exception {
        int result = invokeBoyerMooreSearch("12345", "3", 10);
        assertEquals(-1, result, "startIndex がテキスト長を超える場合は -1 を返すこと");
    }

    /** startIndex がテキスト長ちょうどの場合も -1 を返す */
    @Test
    void boyerMooreSearch_startIndexがテキスト長ちょうどの場合はマイナス1を返す() throws Exception {
        int result = invokeBoyerMooreSearch("123", "3", 3);
        assertEquals(-1, result, "startIndex == text.length() のとき -1 を返すこと");
    }

    /** パターンがテキスト末尾ちょうどに存在する */
    @Test
    void boyerMooreSearch_パターンがテキスト末尾ちょうどに存在する() throws Exception {
        int result = invokeBoyerMooreSearch("1234567890", "890", 0);
        assertEquals(7, result, "テキスト末尾ちょうどのパターンが正しい位置で発見されること");
    }

    /** startIndex を指定すると、それより前の出現がスキップされる */
    @Test
    void boyerMooreSearch_startIndexより前の出現はスキップされる() throws Exception {
        // "11" は "1100001100" の位置 0 と 6 にある（index6,7 = '1','1'）
        int first  = invokeBoyerMooreSearch("1100001100", "11", 0);
        int second = invokeBoyerMooreSearch("1100001100", "11", 1);
        assertEquals(0, first,  "startIndex=0 では最初の出現位置(0)が返ること");
        assertEquals(6, second, "startIndex=1 では2番目の出現位置(6)が返ること");
    }

    // =========================================================
    // 空 SurvivalList での run() 安全性（回帰テスト）
    // =========================================================

    /** 空の SurvivalList で run() を呼んでもクラッシュしない */
    @Test
    void run_空のSurvivalListでもクラッシュしない() throws Exception {
        SurvivalList sl = new SurvivalList(3, 0, 0);
        sl.discover("000", 1L); // 全要素を削除して空にする
        assertTrue(sl.isEmpty(), "前提: SurvivalList が空であること");

        YCD_SeqProvider.Unit unit = createUnit(1L, "14159265358979323846264338327950");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join(); // 例外が伝播しないこと
    }

    /** 空の SurvivalList では検索ヒットなし → findPos が -1L のまま */
    @Test
    void run_空のSurvivalListではfindPosはマイナス1() throws Exception {
        SurvivalList sl = new SurvivalList(3, 0, 0);
        sl.discover("000", 1L);

        YCD_SeqProvider.Unit unit = createUnit(1L, "14159265358979323846264338327950");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join();

        assertNotNull(thread.getResult(), "結果オブジェクトが null でないこと");
        assertEquals(-1L, thread.getResult().getFindPos(),
                "ヒットなしのとき findPos は -1L のまま");
    }

    // =========================================================
    // ヘルパーメソッド
    // =========================================================

    private YCD_SeqProvider.Unit createUnit(long startDigit, String data) throws Exception {
        Constructor<?> ctor = null;
        for (Constructor<?> c : YCD_SeqProvider.Unit.class.getDeclaredConstructors()) {
            if (c.getParameterCount() == 4) {
                ctor = c;
                break;
            }
        }
        if (ctor == null) throw new IllegalStateException("Unit の4引数コンストラクタが見つかりません");
        ctor.setAccessible(true);
        return (YCD_SeqProvider.Unit) ctor.newInstance(testProvider, Collections.emptyMap(), startDigit, data);
    }

    private int invokeBoyerMooreSearch(String text, String pattern, int startIndex) throws Exception {
        SurvivalList sl = new SurvivalList(5, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);
        Method method = SearchThread.class.getDeclaredMethod(
                "boyerMooreSearch", String.class, String.class, int.class);
        method.setAccessible(true);
        return (int) method.invoke(thread, text, pattern, startIndex);
    }
}
