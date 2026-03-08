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
 * SearchThread のアルゴリズム分岐・Boyer-Moore 正確性・YCDファイル境界テスト.
 *
 * 1. 共通プレフィックスあり → LCアルゴリズムが選ばれる
 * 2. 共通プレフィックスなし → SLアルゴリズムが選ばれる
 * 3. Boyer-Moore: パターンが中間にある場合
 * 4. Boyer-Moore: パターンが見つからない場合
 * 5. Boyer-Moore: 全同一文字パターン ("0000000" 等)
 * 6. Boyer-Moore: テキスト末尾付近のパターン（チャンク境界シミュレーション）
 * 7. Boyer-Moore: startIndex パラメータが正しく機能する
 * 8. YCDファイル境界: 複数ファイルにまたがる場合にオーバーラップ機構が動作する
 */
public class SearchThreadAlgorithmTest extends TestBase {

    /** テスト用 YCD_SeqProvider（Unit インスタンス生成の外部クラス参照として使用） */
    private static YCD_SeqProvider testProvider;

    @BeforeAll
    static void セットアップ() throws Exception {
        URL url = SearchThreadAlgorithmTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));
        // overWrapLength=5, unitLength=100 で初期化
        testProvider = new YCD_SeqProvider(1, fileList, 5, 100);
    }

    @AfterAll
    static void クリーンアップ() throws Exception {
        if (testProvider != null) {
            testProvider.close();
        }
    }

    // =========================================================
    // アルゴリズム分岐テスト
    // =========================================================

    /**
     * 共通プレフィックスが存在する SurvivalList を使うと LC アルゴリズムが選ばれる.
     *
     * 例: "0000"-"0009" → 共通プレフィックス "000" → LC
     */
    @Test
    void run_共通プレフィックスあり_LCアルゴリズムが選ばれる() throws Exception {
        // "0000", "0001", ..., "0009" は共通プレフィックス "000" を持つ
        SurvivalList sl = new SurvivalList(4, 0, 9);
        assertEquals("000", sl.getCommonPrefix(), "前提: 共通プレフィックスが存在すること");

        // パイ数字データ（先頭部分）のユニットを生成
        // 実際のπ先頭: 14159265358979323846...
        YCD_SeqProvider.Unit unit = createUnit(1L,
                "1415926535897932384626433832795028841971693993751058209749445923078164");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join();

        assertEquals("LC", thread.getAlgorithm(),
                "共通プレフィックスがある場合は LC アルゴリズムが使われること");
    }

    /**
     * 共通プレフィックスが存在しない SurvivalList を使うと SL アルゴリズムが選ばれる.
     *
     * 例: "1"-"9" は共通プレフィックスなし → SL
     */
    @Test
    void run_共通プレフィックスなし_SLアルゴリズムが選ばれる() throws Exception {
        // "1", "2", ..., "9" は共通プレフィックスなし
        SurvivalList sl = new SurvivalList(1, 1, 9);
        assertEquals("", sl.getCommonPrefix(), "前提: 共通プレフィックスが存在しないこと");

        YCD_SeqProvider.Unit unit = createUnit(1L,
                "1415926535897932384626433832795028841971693993751058209749445923078164");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join();

        assertEquals("SL", thread.getAlgorithm(),
                "共通プレフィックスがない場合は SL アルゴリズムが使われること");
    }

    /**
     * SL アルゴリズムで制御済みテキストから最も遅く初出するパターンが返される.
     *
     * テキスト "120134"、SurvivalList(2, 12, 34)（共通プレフィックスなし → SL）、
     * startDigit=100 で検索したとき:
     *   - "12" → index 0 → findPos 100
     *   - "20" → index 1 → findPos 101
     *   - "13" → index 3 → findPos 103
     *   - "34" → index 4 → findPos 104 ← 最大
     * 結果: target="34", findPos=104 を期待する。
     */
    @Test
    void run_SLアルゴリズム_制御テキストで正しい発見結果を返す() throws Exception {
        // SurvivalList(2, 12, 34): ["12","13",...,"34"] → 共通プレフィックスなし → SL
        SurvivalList slSL = new SurvivalList(2, 12, 34);
        assertEquals("", slSL.getCommonPrefix(), "前提: SLアルゴリズムが選ばれること");

        // 制御済みテキスト: "120134"、startDigit=100
        YCD_SeqProvider.Unit unit = createUnit(100L, "120134");

        SearchThread thread = new SearchThread(slSL, unit);
        thread.start();
        thread.join();

        assertEquals("SL", thread.getAlgorithm());
        assertNotNull(thread.getResult());
        // findPos 最大の "34" が index 4 → 100+4 = 104
        assertEquals("34", thread.getResult().getTarget(),
                "最も遅く初出したパターン \"34\" が結果であること");
        assertEquals(104L, thread.getResult().getFindPos(),
                "\"34\" の発見位置が 104 であること");
    }

    /**
     * LC アルゴリズムで制御済みテキストから最も遅く初出するパターンが正しい位置で返される.
     *
     * テキスト "1230005000123456000987"、SurvivalList(4, 0, 9)（共通プレフィックス "000" → LC）、
     * startDigit=1 で検索したとき:
     *   - "0005" → index 3 → findPos 4
     *   - "0001" → index 7 → findPos 8
     *   - "0009" → index 16 → findPos 17 ← 最大
     * 結果: target="0009", findPos=17 を期待する。
     */
    @Test
    void run_LCアルゴリズム_制御テキストで正しい発見結果を返す() throws Exception {
        // SurvivalList(4, 0, 9) = ["0000",...,"0009"] → 共通プレフィックス "000" → LC
        SurvivalList sl = new SurvivalList(4, 0, 9);
        assertEquals("000", sl.getCommonPrefix(), "前提: LCアルゴリズムが選ばれること");

        // 制御済みテキスト: startDigit=1
        // "1230005000123456000987"
        //  idx 3: "0005" → findPos=4
        //  idx 7: "0001" → findPos=8
        //  idx16: "0009" → findPos=17 (最大)
        YCD_SeqProvider.Unit unit = createUnit(1L, "1230005000123456000987");

        SearchThread thread = new SearchThread(sl, unit);
        thread.start();
        thread.join();

        assertEquals("LC", thread.getAlgorithm());
        assertNotNull(thread.getResult());
        assertEquals("0009", thread.getResult().getTarget(),
                "最も遅く初出したパターン \"0009\" が結果であること");
        assertEquals(17L, thread.getResult().getFindPos(),
                "\"0009\" の発見位置が 17 であること");
    }

    /**
     * パターンがテキストの中間で発見される.
     */
    @Test
    void boyerMooreSearch_パターンが中間に存在する場合に発見できる() throws Exception {
        SurvivalList sl = new SurvivalList(5, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // "12345" が "0123456789" の位置1にある
        int result = invokeBoyerMooreSearch(thread, "0123456789", "12345", 0);

        assertEquals(1, result, "中間のパターンが正しい位置で発見されること");
    }

    /**
     * パターンが存在しない場合は -1 を返す.
     */
    @Test
    void boyerMooreSearch_パターンが存在しない場合はマイナス1を返す() throws Exception {
        SurvivalList sl = new SurvivalList(5, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        int result = invokeBoyerMooreSearch(thread, "1234567890", "99999", 0);

        assertEquals(-1, result, "パターンが存在しない場合は -1 を返すこと");
    }

    /**
     * 全同一文字パターン ("0000000" 等) が正しく発見される.
     * Boyer-Moore の bad character ルールのエッジケース.
     */
    @Test
    void boyerMooreSearch_全同一文字パターンを正しく発見できる() throws Exception {
        SurvivalList sl = new SurvivalList(7, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // "000" が "1110001111" の位置3にある
        int result = invokeBoyerMooreSearch(thread, "1110001111", "000", 0);

        assertEquals(3, result, "全同一文字パターンが正しい位置で発見されること");
    }

    /**
     * 全同一文字パターンの先頭マッチ.
     */
    @Test
    void boyerMooreSearch_全同一文字パターンがテキスト先頭にある() throws Exception {
        SurvivalList sl = new SurvivalList(5, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // "0000" が "0000123456" の位置0にある
        int result = invokeBoyerMooreSearch(thread, "0000123456", "0000", 0);

        assertEquals(0, result, "テキスト先頭の全同一文字パターンが発見されること");
    }

    /**
     * テキスト末尾付近のパターンが発見される（チャンク境界シミュレーション）.
     *
     * YCDユニット末尾付近にパターンが位置する状況を再現する.
     * 実運用では overWrapLength により次ユニット先頭と重なるデータが付与されるため
     * チャンク境界をまたぐパターンも発見可能。ここではその境界付近のケースを検証する。
     */
    @Test
    void boyerMooreSearch_テキスト末尾付近のパターンを発見できる() throws Exception {
        SurvivalList sl = new SurvivalList(3, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // "789" がテキスト末尾にある
        String text = "1234567890123456789";
        int result = invokeBoyerMooreSearch(thread, text, "789", 0);

        assertEquals(6, result, "テキスト末尾付近のパターンが正しい位置で発見されること");
    }

    /**
     * startIndex パラメータにより指定位置以降の検索が行われる.
     *
     * startIndex より前の出現はスキップされ、それ以降の最初の出現位置が返される.
     */
    @Test
    void boyerMooreSearch_startIndexパラメータが機能する() throws Exception {
        SurvivalList sl = new SurvivalList(3, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // "12" は "1212345678" の位置0と2に存在する
        // startIndex=1 で検索すると位置2が返される
        int result = invokeBoyerMooreSearch(thread, "1212345678", "12", 1);

        assertEquals(2, result, "startIndex 以降の最初の出現位置が返されること");
    }

    /**
     * 実際のπ先頭データで既知パターンを発見できる（Boyer-Moore の正確性確認）.
     *
     * TestBase コメントより: π先頭: "14159265358979323846..."
     * "926535" は位置4にある (1-based index での位置は 5)
     */
    @Test
    void boyerMooreSearch_実際のπデータで既知パターンを発見できる() throws Exception {
        SurvivalList sl = new SurvivalList(6, 0, 9);
        YCD_SeqProvider.Unit unit = createUnit(1L, "0000000000");
        SearchThread thread = new SearchThread(sl, unit);

        // π先頭: 1415926535897932384626433832795028841971693993751
        String piStart = "14159265358979323846264338327950288419716939937510";
        // "926535" は 0-based でposition 4 にある
        int result = invokeBoyerMooreSearch(thread, piStart, "926535", 0);

        assertEquals(4, result, "πデータの既知パターンが正しい位置で発見されること");
    }

    // =========================================================
    // YCDファイル境界テスト
    // =========================================================

    /**
     * ユニット境界をまたぐパターンはオーバーラップなしでは検出できない.
     *
     * π の先頭 "14159265358979323846264338..." において
     * "462" は位置19（0-based index 18, 19, 20）にある。
     * unitLength=20 で分割すると:
     *   - Unit 1: indices 0-19 = "14159265358979323846"  → "462" なし
     *   - Unit 2 (overlap=0): indices 20-39 = "26433832795028841971" → "462" なし
     * overWrapLength=0 のとき "462" は発見されない（境界にまたがるため）。
     */
    @Test
    void ycdファイル境界_オーバーラップなしではユニット境界パターンが検出されない() throws Exception {
        URL url = SearchThreadAlgorithmTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());

        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));

        // "462" はπ位置 19 (0-based index 18-20) にまたがる
        // overWrapLength=0 ではユニット境界をまたぐ "462" が検出できないこと
        SurvivalList sl = new SurvivalList(3, 462, 462); // ["462"] のみ
        int overWrapLength = 0;
        int unitLength = 20;

        try (YCD_SeqProvider provider = new YCD_SeqProvider(1, fileList, overWrapLength, unitLength)) {
            // 最初の3ユニットだけ確認すれば十分（位置19はunit 1か2にある）
            int unitCount = 0;
            while (provider.hasNext() && !sl.isEmpty() && unitCount < 3) {
                YCD_SeqProvider.Unit unit = provider.next();
                SearchThread thread = new SearchThread(sl, unit);
                thread.start();
                thread.join();
                unitCount++;
            }
        }

        // overWrapLength=0 では位置19の "462" がユニット境界で分断され発見されない
        assertFalse(sl.isEmpty(),
                "overWrapLength=0 のとき境界パターン \"462\" は発見されないこと");
        assertTrue(sl.contains("462"),
                "overWrapLength=0 のとき \"462\" はサバイバルリストに残ること");
    }

    /**
     * ユニット境界をまたぐパターンはオーバーラップありで正しい位置に発見できる.
     *
     * π の "462" は位置19にある（unit 1 の末尾 "46" + unit 2 の先頭 "2"）。
     * overWrapLength=3 のとき:
     *   - Unit 2 の先頭に前ユニット末尾3文字 "846" が付加される
     *   - Unit 2 データ: "84626433..." の index 1 に "462" が現れる
     *   - findPos = unit2.startDigit(18) + localIndex(1) = 19
     * overWrapLength >= patternLength-1 のとき境界パターンが正しく発見される。
     */
    @Test
    void ycdファイル境界_オーバーラップありではユニット境界パターンが正しい位置で発見される() throws Exception {
        URL url = SearchThreadAlgorithmTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());

        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));

        SurvivalList sl = new SurvivalList(3, 462, 462); // ["462"] のみ
        int overWrapLength = 3;
        int unitLength = 20;

        SearchThread lastThread = null;

        try (YCD_SeqProvider provider = new YCD_SeqProvider(1, fileList, overWrapLength, unitLength)) {
            int unitCount = 0;
            // 最初の3ユニット以内で "462" が発見されるはず
            while (provider.hasNext() && !sl.isEmpty() && unitCount < 3) {
                YCD_SeqProvider.Unit unit = provider.next();
                if (lastThread != null) lastThread.join();
                lastThread = new SearchThread(sl, unit);
                lastThread.start();
                unitCount++;
            }
            if (lastThread != null) lastThread.join();
        }

        assertTrue(sl.isEmpty(),
                "overWrapLength=3 のとき境界パターン \"462\" が発見されること");
        assertEquals(1, sl.getDiscoverdInfo().size());
        assertEquals("462", sl.getDiscoverdInfo().get(0).getTarget());
        // π位置 19 ("462" は 0-based index 18,19,20 → startDigit=1 の場合 findPos=19)
        assertEquals(19L, sl.getDiscoverdInfo().get(0).getFindPos(),
                "\"462\" の発見位置がπの正しい位置 19 であること");
    }

    /**
     * YCDファイル境界でオーバーラップ付きユニットが生成される.
     *
     * ファイル末尾のユニットには次ファイルの先頭 overWrapLength 文字が付加されるため
     * ユニットのデータ長が base unitLength より大きくなる場合がある。
     * この挙動を検証することで、境界またぎのパターン検索が可能であることを確認する。
     */
    @Test
    void ycdファイル境界_オーバーラップ付きユニットが生成される() throws Exception {
        URL url = SearchThreadAlgorithmTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());

        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 1.ycd"));

        int overWrapLength = 5;
        int unitLength = 500;

        boolean foundBoundaryUnit = false;

        try (YCD_SeqProvider provider = new YCD_SeqProvider(1, fileList, overWrapLength, unitLength)) {
            while (provider.hasNext()) {
                YCD_SeqProvider.Unit unit = provider.next();
                // ファイル境界ユニットはデータ長が unitLength + overWrapLength を超える
                if (unit.getData().length() > unitLength + overWrapLength) {
                    foundBoundaryUnit = true;
                    // ファイル境界ユニットのデータが数字のみで構成されることも確認
                    assertTrue(unit.getData().matches("[0-9]+"),
                            "ファイル境界ユニットのデータは数字のみであること");
                    break;
                }
            }
        }

        assertTrue(foundBoundaryUnit,
                "ファイル境界をまたぐオーバーラップ付きユニットが生成されること");
    }

    // =========================================================
    // ヘルパーメソッド
    // =========================================================

    /**
     * テスト用 YCD_SeqProvider.Unit を指定データで生成する.
     *
     * Unit は非静的内部クラスでコンストラクタが private のため、リフレクションを使用する。
     * コンパイラが生成する合成コンストラクタ（パラメータ数が多い）を除外し、
     * 引数4つの本来のプライベートコンストラクタを使用する。
     *
     * @param startDigit ユニット先頭のπ桁数位置
     * @param data       ユニットのπ桁データ文字列
     * @return 生成した Unit インスタンス
     */
    private static YCD_SeqProvider.Unit createUnit(long startDigit, String data) throws Exception {
        Constructor<?> ctor = null;
        for (Constructor<?> c : YCD_SeqProvider.Unit.class.getDeclaredConstructors()) {
            // 外部クラス + Map + Long + String = 4パラメータの本来のコンストラクタを選ぶ
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
     * SearchThread の private メソッド boyerMooreSearch をリフレクション経由で呼び出す.
     *
     * @param thread     SearchThread インスタンス
     * @param text       検索対象テキスト（数字のみ）
     * @param pattern    検索パターン（数字のみ）
     * @param startIndex 検索開始位置
     * @return パターン発見位置（見つからない場合は -1）
     */
    private int invokeBoyerMooreSearch(SearchThread thread, String text, String pattern, int startIndex)
            throws Exception {
        Method method = SearchThread.class.getDeclaredMethod(
                "boyerMooreSearch", String.class, String.class, int.class);
        method.setAccessible(true);
        return (int) method.invoke(thread, text, pattern, startIndex);
    }
}
