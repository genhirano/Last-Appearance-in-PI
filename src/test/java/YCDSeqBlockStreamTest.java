import model.ycd.YCDFileUtil;
import model.ycd.YCDHeaderInfoElem;
import model.ycd.YCDProcessUnit;
import model.ycd.YCD_SeqBlockStream;
import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * YCD_SeqBlockStream（高速化版）の正確性テスト.
 *
 * 旧実装と同じ方法（Long.toUnsignedString ＋ 左ゼロ埋め）で素直に変換した「参照データ」と比較する。
 *
 * 1. 1ブロックの変換（通常値・先頭ゼロ・2^63以上の符号なし値）
 * 2. 様々なユニット桁数で、全ユニットの連結が参照データと一致する（15ファイル）
 * 3. 最後以外のユニットは常にユニット桁数ちょうど、最後は端数
 * 4. startDigit がユニット桁数ずつ進み、2ファイル目以降は前ファイルの続きから始まる
 * 5. 読み終わりで hasNext() が false になり、さらに next() すると IOException
 * 6. YCD_SeqProvider 経由でも、各ユニットのデータが参照データの該当位置と一致する（ファイル境界含む）
 */
public class YCDSeqBlockStreamTest {

    private static File ycdFile(int index) throws Exception {
        URL url = YCDSeqBlockStreamTest.class.getClassLoader().getResource("1000000");
        return new File(new File(url.toURI()), "Pi - Dec - Chudnovsky - " + index + ".ycd");
    }

    /**
     * 参照デコーダ（旧実装と同じ手順）: ファイル全桁を文字列で返す.
     */
    static String referenceDecode(File f) throws IOException {
        Map<YCDHeaderInfoElem, String> h = YCDFileUtil.getYCDHeader(f.getPath());
        long digits = Long.parseLong(h.get(YCDHeaderInfoElem.BLOCK_SIZE));
        int headerSize = YCDFileUtil.getHeaderSize(f.getPath());
        StringBuilder sb = new StringBuilder((int) digits + 19);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            long skipped = 0;
            while (skipped < headerSize + 1) {
                skipped += in.skip(headerSize + 1 - skipped);
            }
            byte[] b = new byte[8];
            while (sb.length() < digits) {
                int n = in.read(b);
                assertEquals(8, n, "8バイト読めること");
                String s = Long.toUnsignedString(ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getLong());
                if (s.length() < 19) {
                    s = String.format("%19s", s).replace(' ', '0');
                }
                sb.append(s);
            }
        }
        return sb.substring(0, (int) digits);
    }

    // =========================================================
    // 1ブロック変換
    // =========================================================

    private static String decode(long v) throws Exception {
        Method m = YCD_SeqBlockStream.class.getDeclaredMethod("decodeBlock", byte[].class, int.class, char[].class, int.class);
        m.setAccessible(true);
        byte[] src = new byte[16];
        ByteBuffer.wrap(src, 8, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(v);
        char[] dst = new char[40];
        m.invoke(null, src, 8, dst, 10);
        return new String(dst, 10, 19);
    }

    @Test
    void decodeBlock_通常値と先頭ゼロと符号なし最大値() throws Exception {
        assertEquals("0000000000000000000", decode(0L));
        assertEquals("0000000000000000123", decode(123L));
        assertEquals("1415926535897932384", decode(1415926535897932384L));
        assertEquals("9223372036854775807", decode(Long.MAX_VALUE));
        // 2^63 以上（符号付きでは負）の値。10^19 - 1 = 9999999999999999999
        assertEquals("9999999999999999999", decode(Long.parseUnsignedLong("9999999999999999999")));
        assertEquals("9223372036854775808", decode(Long.MIN_VALUE));
    }

    // =========================================================
    // 連結一致・ユニット長・startDigit
    // =========================================================

    @ParameterizedTest
    @ValueSource(ints = {19, 23, 100, 1000, 1900, 19000, 380000, 999983, 1000000, 1234567})
    void 全ユニットの連結が参照データと一致する(int unitSize) throws Exception {
        File f = ycdFile(0);
        String ref = referenceDecode(f);
        assertEquals(1_000_000, ref.length());

        StringBuilder got = new StringBuilder();
        List<Integer> lengths = new ArrayList<>();
        long expectStart = 1L;
        try (YCD_SeqBlockStream s = new YCD_SeqBlockStream(f.getPath(), unitSize)) {
            assertEquals(1L, s.getDigit_Start());
            assertEquals(1_000_000L, s.getDigit_Length());
            while (s.hasNext()) {
                YCDProcessUnit u = s.next();
                assertEquals(expectStart, u.getStartDigit(), "startDigit がユニット桁数ずつ進むこと");
                expectStart += unitSize;
                lengths.add(u.getValue().length());
                got.append(u.getValue());
            }
            assertFalse(s.hasNext());
            assertThrows(IOException.class, s::next, "読み終わり後の next() は IOException");
        }

        assertEquals(ref, got.toString(), "unitSize=" + unitSize + " で連結結果が参照データと一致すること");

        // 最後以外はユニット桁数ちょうど
        for (int i = 0; i < lengths.size() - 1; i++) {
            assertEquals(unitSize, lengths.get(i), "途中のユニットはユニット桁数ちょうどであること");
        }
        int lastLen = lengths.get(lengths.size() - 1);
        assertTrue(0 < lastLen && lastLen <= unitSize);
        assertEquals(1_000_000 % unitSize == 0 ? unitSize : 1_000_000 % unitSize, lastLen,
                "最後のユニットは端数であること");
    }

    @Test
    void 全15ファイルが参照データと一致しstartDigitが連続する() throws Exception {
        for (int i = 0; i < 15; i++) {
            File f = ycdFile(i);
            String ref = referenceDecode(f);
            StringBuilder got = new StringBuilder();
            try (YCD_SeqBlockStream s = new YCD_SeqBlockStream(f.getPath(), 380000)) {
                assertEquals(1L + 1_000_000L * i, s.getDigit_Start(), "ファイル " + i + " の開始桁");
                while (s.hasNext()) {
                    got.append(s.next().getValue());
                }
            }
            assertEquals(ref, got.toString(), "ファイル " + i + " が一致すること");
        }
    }

    @Test
    void 既知のπ先頭と末尾が一致する() throws Exception {
        // TestBase コメントより: 1〜50桁、999,951〜1,000,000桁
        String ref = referenceDecode(ycdFile(0));
        assertEquals("14159265358979323846264338327950288419716939937510", ref.substring(0, 50));
        assertEquals("56787961303311646283996346460422090106105779458151", ref.substring(999_950, 1_000_000));
        // 2ファイル目の先頭 50 桁（1,000,001〜1,000,050）
        String ref1 = referenceDecode(ycdFile(1));
        assertEquals("30927562832084531584652001027797235612923012605863", ref1.substring(0, 50));
    }

    @Test
    void ユニット桁数が19未満は例外() throws Exception {
        File f = ycdFile(0);
        assertThrows(IllegalArgumentException.class, () -> new YCD_SeqBlockStream(f.getPath(), 18));
    }

    @Test
    void 存在しないファイルは例外() {
        assertThrows(IOException.class, () -> new YCD_SeqBlockStream("not-exist.ycd", 100));
    }

    // =========================================================
    // YCD_SeqProvider 経由（オーバーラップ・ファイル境界）
    // =========================================================

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 380000})
    void provider経由の各ユニットが参照データの該当位置と一致する(int unitLength) throws Exception {
        // 3ファイル分の参照データ（300万桁）
        StringBuilder refAll = new StringBuilder();
        List<File> files = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            files.add(ycdFile(i));
            refAll.append(referenceDecode(ycdFile(i)));
        }
        String ref = refAll.toString();

        int overlap = 7;
        long maxEnd = 0;
        int units = 0;
        try (YCD_SeqProvider p = new YCD_SeqProvider(1, files, overlap, unitLength)) {
            while (p.hasNext()) {
                YCD_SeqProvider.Unit u = p.next();
                int start = (int) (u.getStartDigit() - 1);
                String data = u.getData();
                assertEquals(ref.substring(start, start + data.length()), data,
                        "unit#" + units + " startDigit=" + u.getStartDigit() + " のデータが参照と一致すること");
                maxEnd = Math.max(maxEnd, start + data.length());
                units++;
            }
        }
        assertEquals(3_000_000L, maxEnd, "3ファイルの末尾まで読み切ること");
    }
}
