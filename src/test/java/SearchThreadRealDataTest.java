import model.pi.SearchThread;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchThread の実データ・参照実装比較テスト.
 *
 * 1. 実際の円周率データ（テスト用YCD）で 1〜5桁の最遅出現数と位置が OEIS A032510 / A036903 と一致する
 *    （本番と同じく listSize で範囲を分割し、範囲ごとの最遅の最大を取る）
 * 2. ランダムな数字列で、素直な参照実装（各対象を indexOf で探す）と
 *    最遅発見・発見位置・残りリストが完全に一致する（ユニット分割・オーバーラップあり、プレフィックス有無の両方）
 */
public class SearchThreadRealDataTest extends TestBase {

    private static YCD_SeqProvider testProvider;

    @BeforeAll
    static void セットアップ() throws Exception {
        testProvider = new YCD_SeqProvider(1, ycdFileList(1), 5, 100);
    }

    @AfterAll
    static void クリーンアップ() throws Exception {
        if (testProvider != null) {
            testProvider.close();
        }
    }

    /**
     * OEIS A032510（最遅出現数）/ A036903（その位置）: 0/32, 68/605, 483/8553, 6716/99846, 33394/1369560
     */
    @ParameterizedTest
    @CsvSource({
            // length, listSize, expectTarget, expectPos
            "1, 10, 0, 32",
            "2, 100, 68, 605",
            "2, 7, 68, 605",
            "3, 1000, 483, 8553",
            "3, 300, 483, 8553",
            "4, 10000, 6716, 99846",
            "4, 2500, 6716, 99846",
            "5, 100000, 33394, 1369560",
            "5, 30000, 33394, 1369560",
    })
    void 実円周率データで最遅出現数がOEISと一致する(int length, int listSize, String expectTarget, long expectPos)
            throws Exception {

        List<File> files = ycdFileList(15);
        long all = (long) Math.pow(10, length);

        // 本番と同じく listSize ごとの範囲に分けて検索し、範囲ごとの最遅の最大を取る
        SurvivalResult deepest = new SurvivalResult("", -1L);
        for (long start = 0; start < all; start += listSize) {
            long end = Math.min(all - 1, start + listSize - 1);
            SurvivalResult r = runSurvival(length, start, end, 19000, files);
            if (0 < r.compareTo(deepest)) {
                deepest.update(r);
            }
        }

        assertEquals(String.format("%0" + length + "d", Long.parseLong(expectTarget)), deepest.getTarget());
        assertEquals(expectPos, deepest.getFindPos());
    }

    /**
     * ランダムな数字列で、参照実装と結果が一致することを確認する.
     */
    @ParameterizedTest
    @CsvSource({
            // length, start, end, textLength, unitLength, seed
            "1, 0, 9, 50, 7, 1",
            "2, 0, 99, 300, 50, 2",            // プレフィックスなし
            "2, 30, 69, 300, 37, 3",           // プレフィックスなし（途中で付く）
            "3, 0, 999, 5000, 400, 4",
            "3, 100, 199, 2000, 333, 5",       // プレフィックス "1"
            "4, 0, 9999, 50000, 1900, 6",
            "4, 2000, 2999, 20000, 1000, 7",   // プレフィックス "2"
            "5, 12345, 23456, 100000, 19000, 8",
            "6, 0, 9999, 100000, 7919, 9",     // プレフィックス "00"
            "6, 123400, 123499, 300000, 19000, 10", // プレフィックス "1234"（テキストが短く全部は見つからない）
    })
    void ランダムテキストで参照実装と結果が一致する(int length, long start, long end, int textLength, int unitLength,
            long seed) throws Exception {

        String text = randomDigits(textLength, seed);

        // 参照実装：各対象の初出位置（1始まり）を indexOf で求める
        Map<String, Long> expectPos = new HashMap<>();
        List<String> expectSurvivors = new ArrayList<>();
        String expectTarget = "";
        long expectDeepest = -1L;
        for (long v = start; v <= end; v++) {
            String t = String.format("%0" + length + "d", v);
            int idx = text.indexOf(t);
            if (idx < 0) {
                expectSurvivors.add(t);
            } else {
                expectPos.put(t, 1L + idx);
                if (expectDeepest < 1L + idx) {
                    expectDeepest = 1L + idx;
                    expectTarget = t;
                }
            }
        }

        // 検査対象：ユニットに分割（オーバーラップ length 桁）して順に検索
        SurvivalList sl = new SurvivalList(length, start, end);
        SurvivalResult deepest = new SurvivalResult("", -1L);
        int pos = 0;
        while (pos < text.length() && !sl.isEmpty()) {
            int from = Math.max(0, pos - length);
            int to = Math.min(text.length(), pos + unitLength);
            SearchThread t = new SearchThread(sl, createUnit(1L + from, text.substring(from, to)));
            t.run();
            if (0 < t.getResult().compareTo(deepest)) {
                deepest.update(t.getResult());
            }
            pos = to;
        }

        assertEquals(expectTarget, deepest.getTarget(), "最遅発見が一致すること");
        assertEquals(expectDeepest, deepest.getFindPos(), "最遅発見位置が一致すること");
        assertEquals(expectSurvivors, new ArrayList<>(sl), "残りリストが一致すること");
        assertEquals(expectPos.size(), sl.getDiscoveredCount(), "発見数が一致すること");

        // 発見位置の集合が一致すること
        List<Long> expectPositions = new ArrayList<>(expectPos.values());
        Collections.sort(expectPositions);
        long[] got = sl.getDiscoveredPositions();
        assertEquals(expectPositions.size(), got.length);
        for (int i = 0; i < got.length; i++) {
            assertEquals(expectPositions.get(i).longValue(), got[i], "発見位置 " + i);
        }

        // 直近詳細の対象・位置が参照と一致すること
        for (SurvivalList.DiscoverdInfo d : sl.getDiscoverdInfo()) {
            assertEquals(expectPos.get(d.getTarget()), d.getFindPos(), d.getTarget() + " の発見位置");
        }
    }

    // =========================================================
    // ヘルパー
    // =========================================================

    /**
     * Searcher.survive と同じ流れで、YCDファイルを先頭から読みながらサバイバルリストが空になるまで検索する.
     */
    static SurvivalResult runSurvival(int length, long start, long end, int unitLength, List<File> files)
            throws Exception {
        SurvivalList sl = new SurvivalList(length, start, end);
        SurvivalResult deepest = new SurvivalResult("", -1L);
        try (YCD_SeqProvider p = new YCD_SeqProvider(1, files, length, unitLength)) {
            while (p.hasNext() && !sl.isEmpty()) {
                SearchThread t = new SearchThread(sl, p.next());
                t.run(); // スレッドを起こさず同期実行
                if (0 < t.getResult().compareTo(deepest)) {
                    deepest.update(t.getResult());
                }
            }
        }
        assertTrue(sl.isEmpty(), "テストデータ内で全て発見できること");
        return deepest;
    }

    static String randomDigits(int length, long seed) {
        Random r = new Random(seed);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('0' + r.nextInt(10)));
        }
        return sb.toString();
    }

    /** テスト用YCDファイル（0番から count 個）のリスト */
    static List<File> ycdFileList(int count) throws Exception {
        URL url = SearchThreadRealDataTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            fileList.add(new File(dir, "Pi - Dec - Chudnovsky - " + i + ".ycd"));
        }
        return fileList;
    }

    static YCD_SeqProvider.Unit createUnit(long startDigit, String data) throws Exception {
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
}
