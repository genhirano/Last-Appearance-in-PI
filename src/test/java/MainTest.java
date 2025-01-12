import static org.junit.jupiter.api.Assertions.assertEquals;

import model.pi.SurvivalList;

public class MainTest extends TestBase {

    // @org.junit.jupiter.api.Test
    // void 一般テスト(TestInfo testInfo) {
    // }

    @org.junit.jupiter.api.Test
    void testMethod() {

        SurvivalList sl;
        
        sl = new SurvivalList(1, 1, 9);
        assertEquals("", sl.getCommonPrefix());

        sl = new SurvivalList(2, 0, 99);
        assertEquals("", sl.getCommonPrefix());

        sl = new SurvivalList(2, 10, 19);
        assertEquals("1", sl.getCommonPrefix());

        sl = new SurvivalList(10, 0, 1000);
        assertEquals("000000", sl.getCommonPrefix());

        sl = new SurvivalList(10, 0, 5000);
        assertEquals("000000", sl.getCommonPrefix());

        sl = new SurvivalList(5, 0, 9999);
        assertEquals("0", sl.getCommonPrefix());

        sl = new SurvivalList(5, 0, 99999);
        assertEquals("", sl.getCommonPrefix());

        sl = new SurvivalList(5, 0, 999);
        assertEquals("00", sl.getCommonPrefix());

        sl = new SurvivalList(5, 0, 123);
        assertEquals("00", sl.getCommonPrefix());

        sl = new SurvivalList(5, 0, 12);
        assertEquals("000", sl.getCommonPrefix());

    }


}
