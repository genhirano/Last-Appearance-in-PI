package model.ycd;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YCDFileUtilTest {

    @Test
    void getFirstData_zeroDigits_returnsEmpty() throws Exception {
        String path = getTestYcdFilePath();
        assertEquals("", YCDFileUtil.getFirstData(path, 0));
    }

    @Test
    void getFirstData_19Digits_returns19DigitString() throws Exception {
        String path = getTestYcdFilePath();
        String first19 = YCDFileUtil.getFirstData(path, 19);
        assertEquals(19, first19.length());
        assertTrue(first19.matches("\\d{19}"));
    }

    @Test
    void dataOffset_matchesHeaderSizePlusOne() throws Exception {
        String path = getTestYcdFilePath();
        long dataOffset = YCDFileUtil.getDataStartOffset(path);
        assertEquals(YCDFileUtil.getHeaderSize(path) + 1L, dataOffset);
        assertTrue(dataOffset > 0L);
    }

    private String getTestYcdFilePath() throws Exception {
        URL url = YCDFileUtilTest.class.getClassLoader().getResource("1000000");
        File dir = new File(url.toURI());
        return new File(dir, "Pi - Dec - Chudnovsky - 0.ycd").getPath();
    }
}
