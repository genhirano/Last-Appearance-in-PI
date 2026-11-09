import model.pi.SearchThread;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 速度計測（実際の円周率データ：テスト用YCD 1,500万桁）.
 *
 * 時間がかかるため通常の mvn test では実行しない。
 * 実行方法: mvn test -Dtest=SearchBenchmarkTest -Dbenchmark=true
 *
 * 計測項目
 *   1. YCD 読み込み・変換のみ（検索なし）のスループット
 *   2. 桁数ごとの 1 ユニットあたり検索時間（本番設定 listSize=100,000 / unitLength=380,000）
 *   3. 5桁・6桁の 1 範囲（listSize=100,000）を読み込み込みで完了するまでの時間
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
public class SearchBenchmarkTest {

    private static final int UNIT_LENGTH = 380000;

    @Test
    void 速度計測() throws Exception {

        List<File> files = SearchThreadRealDataTest.ycdFileList(15);

        System.out.println();
        System.out.println("==================== 速度計測 ====================");

        // 1. 読み込みのみ
        for (int rep = 0; rep < 3; rep++) {
            long t0 = System.nanoTime();
            long digits = 0;
            try (YCD_SeqProvider p = new YCD_SeqProvider(1, files, 10, UNIT_LENGTH)) {
                while (p.hasNext()) {
                    digits += p.next().getData().length();
                }
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("YCD読み込みのみ: %,d桁 %,d ms (%,.0f 万桁/秒)%n", digits, ms, digits / 10000.0 / (ms / 1000.0));
        }

        // 2. 桁数ごとの 1 ユニットあたり検索時間
        for (int n : new int[]{5, 6, 7, 8, 10, 12}) {
            for (int rep = 0; rep < 2; rep++) {
                SurvivalList sl = new SurvivalList(n, 0, 99999);
                long search = 0;
                int units = 0;
                int before = sl.size();
                try (YCD_SeqProvider p = new YCD_SeqProvider(1, files, n, UNIT_LENGTH)) {
                    while (p.hasNext() && !sl.isEmpty()) {
                        YCD_SeqProvider.Unit u = p.next();
                        SearchThread t = new SearchThread(sl, u);
                        long t0 = System.nanoTime();
                        t.run();
                        search += System.nanoTime() - t0;
                        units++;
                    }
                }
                if (rep == 1) {
                    System.out.printf("%2d桁 検索のみ: 1ユニット %6.2f ms  発見 %,d件%n",
                            n, search / 1e6 / units, before - sl.size());
                }
            }
        }

        // 3. 読み込み込みの完了時間
        for (long[] c : new long[][]{{5, 0, 99999}, {6, 0, 99999}, {6, 500000, 599999}}) {
            long t0 = System.nanoTime();
            SurvivalResult r = SearchThreadRealDataTest.runSurvival((int) c[0], c[1], c[2], UNIT_LENGTH, files);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("%d桁 %d-%d 完了まで: %,d ms  最遅 %s @ %,d%n", c[0], c[1], c[2], ms, r.getTarget(), r.getFindPos());
            assertNotNull(r.getTarget());
        }
        System.out.println("==================================================");
    }
}
