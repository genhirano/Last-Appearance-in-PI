import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * YCD_SeqProvider の追加テスト.
 *
 * 1. remove() は UnsupportedOperationException を投げる
 * 2. Unit.indexOf(String) がパターンを正しく発見する
 * 3. Unit.indexOf(String) でパターンが存在しない場合は -1 を返す
 * 4. Unit.indexOf(String, int) で fromIndex 以降のみ検索される
 * 5. 全データ消費後に hasNext() が false を返す
 * 6. 連続する Unit の startDigit が単調増加する
 */
public class YCDSeqProviderAdditionalTest extends TestBase {

    private YCD_SeqProvider createProvider(int unitLength) throws Exception {
        URL url = YCDSeqProviderAdditionalTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));
        return new YCD_SeqProvider(1, fileList, 5, unitLength);
    }

    /** remove() は UnsupportedOperationException を投げる */
    @Test
    void remove_UnsupportedOperationExceptionが発生する() throws Exception {
        try (YCD_SeqProvider provider = createProvider(100)) {
            assertThrows(UnsupportedOperationException.class, provider::remove,
                    "Iterator.remove() はサポートされていないこと");
        }
    }

    /** Unit.indexOf(String) でπ先頭パターンが発見できる */
    @Test
    void unit_indexOf_パターンが存在する場合に正しい位置を返す() throws Exception {
        try (YCD_SeqProvider provider = createProvider(200)) {
            YCD_SeqProvider.Unit unit = provider.next();
            // TestBase コメントより π先頭: "14159265358979323846..."
            int pos = unit.indexOf("1415");
            assertTrue(pos >= 0, "π先頭パターン \"1415\" が発見されること");
        }
    }

    /** Unit.indexOf(String) でパターンが存在しない場合は -1 を返す */
    @Test
    void unit_indexOf_パターンが存在しない場合はマイナス1を返す() throws Exception {
        try (YCD_SeqProvider provider = createProvider(200)) {
            YCD_SeqProvider.Unit unit = provider.next();
            // "XXXX" は数字ではないのでπには存在しない
            int pos = unit.indexOf("XXXX");
            assertEquals(-1, pos, "存在しないパターンでは -1 を返すこと");
        }
    }

    /** Unit.indexOf(String, int) で fromIndex 以降だけを検索する */
    @Test
    void unit_indexOfWithFromIndex_指定位置以降のみ検索する() throws Exception {
        try (YCD_SeqProvider provider = createProvider(200)) {
            YCD_SeqProvider.Unit unit = provider.next();
            String data = unit.getData();

            // 先頭1文字を探してから、その次の位置以降を再検索
            String firstChar = String.valueOf(data.charAt(0));
            int firstPos  = unit.indexOf(firstChar, 0);
            int secondPos = unit.indexOf(firstChar, firstPos + 1);

            // 2回目は -1（以降に存在しない）か firstPos より後の位置
            assertTrue(secondPos == -1 || secondPos > firstPos,
                    "fromIndex 以降のみ検索されること");
        }
    }

    /** 全データ消費後は hasNext() が false を返す */
    @Test
    void hasNext_全データ消費後はfalseを返す() throws Exception {
        URL url = YCDSeqProviderAdditionalTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        List<File> fileList = new ArrayList<>();
        fileList.add(new File(dir, "Pi - Dec - Chudnovsky - 0.ycd"));

        // 大きな unitLength で少ないイテレーションで消費しきる
        try (YCD_SeqProvider provider = new YCD_SeqProvider(1, fileList, 5, 100000)) {
            while (provider.hasNext()) {
                provider.next();
            }
            assertFalse(provider.hasNext(), "全データ消費後は hasNext() が false を返すこと");
        }
    }

    /** 連続する Unit の startDigit は単調増加する */
    @Test
    void unit_startDigitが単調増加する() throws Exception {
        try (YCD_SeqProvider provider = createProvider(100)) {
            YCD_SeqProvider.Unit unit1 = provider.next();
            YCD_SeqProvider.Unit unit2 = provider.next();
            YCD_SeqProvider.Unit unit3 = provider.next();

            assertTrue(unit2.getStartDigit() > unit1.getStartDigit(),
                    "2番目のユニットの startDigit が1番目より大きいこと");
            assertTrue(unit3.getStartDigit() > unit2.getStartDigit(),
                    "3番目のユニットの startDigit が2番目より大きいこと");
        }
    }

    /** startDigit は 1 から始まる（π小数第1位が位置1） */
    @Test
    void unit_最初のunitのstartDigitは1() throws Exception {
        try (YCD_SeqProvider provider = createProvider(100)) {
            YCD_SeqProvider.Unit unit = provider.next();
            assertEquals(1L, unit.getStartDigit(), "最初のユニットの startDigit は 1 であること");
        }
    }
}
