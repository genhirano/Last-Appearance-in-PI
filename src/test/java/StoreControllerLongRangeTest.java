import controller.Env;
import controller.StoreController;
import model.TargetRange;
import model.pi.SurvivalList;
import model.ProgressReportBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StoreController の範囲計算・サマリが 10 桁以上（int の範囲を超える値）でも正しく動作することのテスト.
 *
 * 保存ファイル（NNN.txt）の形式: start,end,lastData,lastFoundPos,seconds
 *
 * 1. 10桁の保存ファイル末尾が int 最大値のとき、次の範囲が 2147483648 から始まる
 * 2. 12桁の保存ファイルから、次の範囲が正しく作られ、末尾は全桁9で打ち切られる
 * 3. 末尾が全桁9なら「終了済み」になる
 * 4. getSummary が 12 桁の深さ・件数を long で扱える
 * 5. getCurrentTargetStartEnd が最初の未終了桁を返す
 * 6. pow10
 */
public class StoreControllerLongRangeTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void セットアップ() throws Exception {
        Path prop = tempDir.resolve("test.properties");
        Files.write(prop, (
                "outputPath=" + tempDir.toString().replace("\\", "/") + "\n"
                + "port=8080\n"
                + "listSize=100000\n"
                + "unitLength=19000\n").getBytes(StandardCharsets.UTF_8));
        resetEnv();
        Env.setPropFileName(prop.toString());
    }

    @AfterEach
    void クリーンアップ() throws Exception {
        StoreController.survivalProgressMap.clear();
        resetEnv();
        Env.setPropFileName(null);
    }

    @Test
    void 進捗は最初のサイクルと保存直後に正しい() throws Exception {
        TargetRange range = new TargetRange(1, "0", "4");
        SurvivalList list = new SurvivalList(1, 0, 4);
        list.discover("0", 5L);
        StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_INFO", range);
        StoreController.survivalProgressMap.put("SURVIVAL_LIST", list);
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

        ProgressReportBean before = StoreController.getProgressReport();
        assertEquals(1, before.getCurrentTargetLength());
        assertEquals(1L, before.getCurrentDiscoveredCount());
        assertEquals(9L, before.getCurrentUndiscoveredCount());

        for (int i = 1; i <= 4; i++) list.discover(String.valueOf(i), 5L + i);
        writeStore(1, "0,4,4,9,3");
        ProgressReportBean saved = StoreController.getProgressReport();
        assertEquals(5L, saved.getCurrentDiscoveredCount());
        assertEquals(5L, saved.getCurrentUndiscoveredCount());
        assertEquals(3L, saved.getCurenntElapsedTimeInSeconds());
    }

    /** Env はシングルトンなので、テストごとに作り直す */
    private static void resetEnv() throws Exception {
        Field f = Env.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    private void writeStore(int length, String... lines) throws IOException {
        Path file = tempDir.resolve(String.format("%03d", length) + ".txt");
        Files.write(file, String.join("\n", lines).concat("\n").getBytes(StandardCharsets.UTF_8));
    }

    private static TargetRange rangeOf(List<TargetRange> list, int length) {
        for (TargetRange tr : list) {
            if (tr.getLength().equals(length)) {
                return tr;
            }
        }
        fail("桁数 " + length + " の範囲が無い");
        return null;
    }

    @Test
    void 十桁_int最大値の次の範囲が正しい() throws Exception {
        // 1〜9桁は終了済み、10桁は 2147483647 まで終わっている
        for (int i = 1; i <= 9; i++) {
            String nines = "9".repeat(i);
            writeStore(i, "0".repeat(i) + "," + nines + ",1,1,1");
        }
        writeStore(10, "2147383648,2147483647,2147400000,123456789012,100");

        List<TargetRange> list = StoreController.getInstance().getNextList(100000);
        TargetRange tr = rangeOf(list, 10);
        assertEquals("2147483648", tr.getStart(), "int 最大値 + 1 から始まること");
        assertEquals("2147583647", tr.getEnd());

        TargetRange current = StoreController.getInstance().getCurrentTargetStartEnd(100000);
        assertEquals(10, current.getLength());
        assertEquals("2147483648", current.getStart());
    }

    @Test
    void 十二桁_次の範囲と全桁9での打ち切り() throws Exception {
        writeStore(12, "999999700000,999999799999,999999750000,22000000000000,100",
                       "999999800000,999999899999,999999850000,22100000000000,100");

        List<TargetRange> list = StoreController.getInstance().getNextList(100000);
        TargetRange tr = rangeOf(list, 12);
        assertEquals("999999900000", tr.getStart());
        assertEquals("999999999999", tr.getEnd());

        // listSize が残りより大きくても全桁9で打ち切られる
        TargetRange tr2 = rangeOf(StoreController.getInstance().getNextList(10_000_000), 12);
        assertEquals("999999900000", tr2.getStart());
        assertEquals("999999999999", tr2.getEnd());
    }

    @Test
    void 末尾が全桁9なら終了済み() throws Exception {
        writeStore(12, "999999900000,999999999999,999999950000,22200000000000,100");

        TargetRange tr = rangeOf(StoreController.getInstance().getNextList(100000), 12);
        assertEquals("", tr.getStart(), "終了済みは start が空文字");
        assertEquals("", tr.getEnd());

        // 13桁は未着手なので先頭から
        TargetRange tr13 = rangeOf(StoreController.getInstance().getNextList(100000), 13);
        assertEquals("0000000000000", tr13.getStart());
        assertEquals("0000000099999", tr13.getEnd());
    }

    @Test
    void 保存ファイルが無い桁は先頭からlistSize分() throws Exception {
        List<TargetRange> list = StoreController.getInstance().getNextList(500);
        assertEquals("0", rangeOf(list, 1).getStart());
        assertEquals("9", rangeOf(list, 1).getEnd(), "listSize が全体より大きければ全桁9まで");
        assertEquals("000", rangeOf(list, 3).getStart());
        assertEquals("499", rangeOf(list, 3).getEnd());
        assertEquals("000000000000", rangeOf(list, 12).getStart());
        assertEquals("000000000499", rangeOf(list, 12).getEnd());
        assertEquals(18, list.size(), "対応桁数は 18 まで");

        TargetRange current = StoreController.getInstance().getCurrentTargetStartEnd(500);
        assertEquals(1, current.getLength());
    }

    @Test
    void getSummaryが十二桁の深さと件数をlongで扱える() throws Exception {
        writeStore(1, "0,9,0,32,1");
        writeStore(2, "00,99,68,605,2");
        // 3〜11桁は終了済み（getSummary は欠番のファイルで打ち切る仕様のため連続して置く）
        for (int i = 3; i <= 11; i++) {
            writeStore(i, "0".repeat(i) + "," + "9".repeat(i) + "," + "1".repeat(i) + ",1000,1");
        }
        writeStore(12, "000000000000,000000099999,000000012345,22000000000000,100",
                       "000000100000,000000199999,000000150000,22100000000000,200");

        List<Map<String, String>> summary = StoreController.getSummary();
        assertEquals(12, summary.size(), "1〜12桁");

        Map<String, String> d1 = summary.get(0);
        assertEquals("true", d1.get("Finished"));
        assertEquals("0", d1.get("Appearing"));
        assertEquals("32", d1.get("Depth"));
        assertEquals("10", d1.get("DiscoveredCount"), "0〜9 の 10 個");

        Map<String, String> d10 = summary.get(9);
        assertEquals("true", d10.get("Finished"), "10桁（全桁9 = 9999999999 は int を超える）が終了済みと判定されること");
        assertEquals("10000000000", d10.get("DiscoveredCount"));

        Map<String, String> d12 = summary.get(11);
        assertEquals("12", d12.get("digits"));
        assertEquals("false", d12.get("Finished"));
        assertEquals("000000150000", d12.get("Appearing"));
        assertEquals("22100000000000", d12.get("Depth"));
        assertEquals("200000", d12.get("DiscoveredCount"), "0〜199999 の 200,000 個");
        assertEquals("300", d12.get("ProcessTimeSec"));
    }

    @Test
    void pow10が正しい() {
        assertEquals(1L, StoreController.pow10(0));
        assertEquals(10L, StoreController.pow10(1));
        assertEquals(10_000_000_000L, StoreController.pow10(10));
        assertEquals(1_000_000_000_000_000_000L, StoreController.pow10(18));
    }
}
