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
 * searchSurvivalWindowAlgorithm（スライディングウィンドウ＋HashMap方式）の正確性テスト.
 *
 * 旧 searchSurvivalLoopAlgorithm と同じ結果を返すことを検証する。
 */
public class SearchThreadWindowAlgorithmTest extends TestBase {

    private static YCD_SeqProvider testProvider;

    @BeforeAll
    static void セットアップ() throws Exception {
        URL url = SearchThreadWindowAlgorithmTest.class.getClassLoader().getResource("1000000");
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
    // 新旧アルゴリズムの結果一致テスト
    // =========================================================

    /**
     * 制御テキスト "120134" でウィンドウアルゴリズムが旧ループアルゴリズムと同じ結果を返す.
     *
     * SurvivalList(2, 12, 34)、startDigit=100。
     * 期待: target="34", findPos=104。
     */
    @Test
    void windowAlgorithm_制御テキストで旧アルゴリズムと同じ結果を返す() throws Exception {
        String piText = "120134";
        long startDigit = 100L;

        // 旧アルゴリズムで実行
        SurvivalList slOld = new SurvivalList(2, 12, 34);
        YCD_SeqProvider.Unit unitOld = createUnit(startDigit, piText);
        model.pi.SurvivalResult resultOld = invokeLoopAlgorithm(new SearchThread(slOld, unitOld), slOld, unitOld);

        // 新アルゴリズムで実行
        SurvivalList slNew = new SurvivalList(2, 12, 34);
        YCD_SeqProvider.Unit unitNew = createUnit(startDigit, piText);
        model.pi.SurvivalResult resultNew = invokeWindowAlgorithm(new SearchThread(slNew, unitNew), slNew, unitNew);

        assertEquals("34", resultNew.getTarget(), "ウィンドウアルゴリズムが正しいターゲットを返すこと");
        assertEquals(104L, resultNew.getFindPos(), "ウィンドウアルゴリズムが正しい発見位置を返すこと");
        assertEquals(resultOld.getTarget(), resultNew.getTarget(), "旧アルゴリズムと同じターゲットを返すこと");
    }

    /**
     * 実際のπ先頭データで2桁ターゲットを検索したとき、ウィンドウアルゴリズムが旧ループアルゴリズムと同じ結果を返す.
     */
    @Test
    void windowAlgorithm_πデータで旧アルゴリズムと同じ最遅ターゲットを返す() throws Exception {
        // π先頭: "14159265358979323846..."
        String piText = "14159265358979323846264338327950288419716939937510";
        long startDigit = 1L;

        // 2桁 "10"〜"99" → 共通プレフィックスなし → SLアルゴリズム対象
        SurvivalList slOld = new SurvivalList(2, 10, 99);
        SurvivalList slNew = new SurvivalList(2, 10, 99);
        assertEquals("", slOld.getCommonPrefix(), "前提: 共通プレフィックスなし");

        YCD_SeqProvider.Unit unitOld = createUnit(startDigit, piText);
        YCD_SeqProvider.Unit unitNew = createUnit(startDigit, piText);

        model.pi.SurvivalResult resultOld = invokeLoopAlgorithm(
                new SearchThread(slOld, unitOld), slOld, unitOld);
        model.pi.SurvivalResult resultNew = invokeWindowAlgorithm(
                new SearchThread(slNew, unitNew), slNew, unitNew);

        // 双方のサバイバルリストに残る要素も一致するはず
        assertEquals(slOld.size(), slNew.size(),
                "旧アルゴリズムと新アルゴリズムで発見件数（サバイバルリスト残数）が一致すること");
        assertEquals(resultOld.getTarget(), resultNew.getTarget(),
                "旧アルゴリズムと新アルゴリズムで最遅ターゲットが一致すること");
        assertEquals(resultOld.getFindPos(), resultNew.getFindPos(),
                "旧アルゴリズムと新アルゴリズムで最遅発見位置が一致すること");
    }

    /**
     * ターゲットが1件だけのとき、ウィンドウアルゴリズムが旧ループアルゴリズムと同じ結果を返す.
     */
    @Test
    void windowAlgorithm_ターゲット1件で旧アルゴリズムと同じ結果を返す() throws Exception {
        // "41" はπ先頭 "14159..." の位置 1 にある (startDigit=1 なら findPos=2)
        String piText = "14159265358979323846";
        long startDigit = 1L;

        SurvivalList slOld = new SurvivalList(2, 41, 41);
        SurvivalList slNew = new SurvivalList(2, 41, 41);

        YCD_SeqProvider.Unit unitOld = createUnit(startDigit, piText);
        YCD_SeqProvider.Unit unitNew = createUnit(startDigit, piText);

        model.pi.SurvivalResult resultOld = invokeLoopAlgorithm(
                new SearchThread(slOld, unitOld), slOld, unitOld);
        model.pi.SurvivalResult resultNew = invokeWindowAlgorithm(
                new SearchThread(slNew, unitNew), slNew, unitNew);

        assertTrue(slOld.isEmpty(), "前提: 旧アルゴリズムで \"41\" が発見されること");
        assertTrue(slNew.isEmpty(), "新アルゴリズムで \"41\" が発見されること");
        assertEquals(resultOld.getTarget(), resultNew.getTarget());
        assertEquals(resultOld.getFindPos(), resultNew.getFindPos());
    }

    /**
     * ターゲットがπデータ内に存在しないとき、ウィンドウアルゴリズムが旧ループアルゴリズムと同じ空結果を返す.
     */
    @Test
    void windowAlgorithm_ターゲット未発見のとき旧アルゴリズムと同じ空結果を返す() throws Exception {
        // "00" はπ先頭50桁には現れない
        String piText = "14159265358979323846264338327950288419716939937510";
        long startDigit = 1L;

        SurvivalList slOld = new SurvivalList(2, 0, 0);
        SurvivalList slNew = new SurvivalList(2, 0, 0);

        YCD_SeqProvider.Unit unitOld = createUnit(startDigit, piText);
        YCD_SeqProvider.Unit unitNew = createUnit(startDigit, piText);

        model.pi.SurvivalResult resultOld = invokeLoopAlgorithm(
                new SearchThread(slOld, unitOld), slOld, unitOld);
        model.pi.SurvivalResult resultNew = invokeWindowAlgorithm(
                new SearchThread(slNew, unitNew), slNew, unitNew);

        assertFalse(slOld.isEmpty(), "前提: 旧アルゴリズムで \"00\" は発見されないこと");
        assertFalse(slNew.isEmpty(), "新アルゴリズムで \"00\" は発見されないこと");
        // どちらも未発見のためデフォルト値 (-1L) のまま
        assertEquals(resultOld.getFindPos(), resultNew.getFindPos(),
                "どちらも未発見のとき発見位置が一致すること");
    }

    /**
     * ターゲットがpiデータの末尾付近にある場合、ウィンドウアルゴリズムが旧ループアルゴリズムと同じ結果を返す.
     */
    @Test
    void windowAlgorithm_ターゲットが末尾付近にある場合旧アルゴリズムと同じ結果を返す() throws Exception {
        // テキスト "12345678" でターゲット "78" は末尾位置 6
        String piText = "12345678";
        long startDigit = 0L;

        SurvivalList slOld = new SurvivalList(2, 78, 78);
        SurvivalList slNew = new SurvivalList(2, 78, 78);

        YCD_SeqProvider.Unit unitOld = createUnit(startDigit, piText);
        YCD_SeqProvider.Unit unitNew = createUnit(startDigit, piText);

        model.pi.SurvivalResult resultOld = invokeLoopAlgorithm(
                new SearchThread(slOld, unitOld), slOld, unitOld);
        model.pi.SurvivalResult resultNew = invokeWindowAlgorithm(
                new SearchThread(slNew, unitNew), slNew, unitNew);

        assertTrue(slOld.isEmpty(), "前提: 旧アルゴリズムで \"78\" が発見されること");
        assertTrue(slNew.isEmpty(), "新アルゴリズムで \"78\" が発見されること");
        assertEquals(resultOld.getTarget(), resultNew.getTarget());
        assertEquals(resultOld.getFindPos(), resultNew.getFindPos());
    }

    /**
     * ターゲットが複数ありそれぞれ異なる位置にある場合、ウィンドウアルゴリズムが最遅のものを返す.
     *
     * テキスト "120134"、SurvivalList(2, 12, 34)。
     * "12"=pos0→findPos100, "20"=pos1→101, "13"=pos3→103, "34"=pos4→104（最遅）。
     */
    @Test
    void windowAlgorithm_複数ターゲットのうち最遅のものが返される() throws Exception {
        SurvivalList sl = new SurvivalList(2, 12, 34);
        YCD_SeqProvider.Unit unit = createUnit(100L, "120134");

        model.pi.SurvivalResult result = invokeWindowAlgorithm(
                new SearchThread(sl, unit), sl, unit);

        assertEquals("34", result.getTarget(), "最も遅く初出したターゲット \"34\" が返されること");
        assertEquals(104L, result.getFindPos(), "\"34\" の発見位置が 104 であること");
    }

    /**
     * run() 経由で SL アルゴリズムが選ばれ、ウィンドウアルゴリズムが使われることを確認.
     */
    @Test
    void run_SLアルゴリズムでウィンドウアルゴリズムが使われ正しい結果を返す() throws Exception {
        SurvivalList sl = new SurvivalList(2, 12, 34);
        assertEquals("", sl.getCommonPrefix(), "前提: 共通プレフィックスなし");

        YCD_SeqProvider.Unit unit = createUnit(100L, "120134");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join();

        assertEquals("SL", thread.getAlgorithm(), "SLアルゴリズムが選ばれること");
        assertNotNull(thread.getResult());
        assertEquals("34", thread.getResult().getTarget());
        assertEquals(104L, thread.getResult().getFindPos());
    }

    // =========================================================
    // ヘルパーメソッド
    // =========================================================

    private static YCD_SeqProvider.Unit createUnit(long startDigit, String data) throws Exception {
        Constructor<?> ctor = null;
        for (Constructor<?> c : YCD_SeqProvider.Unit.class.getDeclaredConstructors()) {
            if (c.getParameterCount() == 4) {
                ctor = c;
                break;
            }
        }
        if (ctor == null) {
            throw new IllegalStateException("Unit の4引数コンストラクタが見つかりません");
        }
        ctor.setAccessible(true);
        return (YCD_SeqProvider.Unit) ctor.newInstance(testProvider, Collections.emptyMap(), startDigit, data);
    }

    /**
     * searchSurvivalWindowAlgorithm をリフレクション経由で呼び出す.
     */
    private model.pi.SurvivalResult invokeWindowAlgorithm(SearchThread thread,
            SurvivalList survivalList, YCD_SeqProvider.Unit unit) throws Exception {
        Method method = SearchThread.class.getDeclaredMethod(
                "searchSurvivalWindowAlgorithm", SurvivalList.class, YCD_SeqProvider.Unit.class);
        method.setAccessible(true);
        return (model.pi.SurvivalResult) method.invoke(thread, survivalList, unit);
    }

    /**
     * searchSurvivalLoopAlgorithm をリフレクション経由で呼び出す（旧アルゴリズムとの比較用）.
     */
    private model.pi.SurvivalResult invokeLoopAlgorithm(SearchThread thread,
            SurvivalList survivalList, YCD_SeqProvider.Unit unit) throws Exception {
        Method method = SearchThread.class.getDeclaredMethod(
                "searchSurvivalLoopAlgorithm", SurvivalList.class, YCD_SeqProvider.Unit.class);
        method.setAccessible(true);
        return (model.pi.SurvivalResult) method.invoke(thread, survivalList, unit);
    }
}
