/*
-- Pi - Dec - Chudnovsky - 0.ycd
1415926535 8979323846 2643383279 5028841971 6939937510  :  50
5820974944 5923078164 0628620899 8628034825 3421170679  :  100
8214808651 3282306647 0938446095 5058223172 5359408128  :  150

0315614033 3212728491 9441843715 0696552087 5424505989  :  999,950
5678796130 3311646283 9963464604 2209010610 5779458151  :  1,000,000

-- Pi - Dec - Chudnovsky - 2.ycd
3092756283 2084531584 6520010277 9723561292 3012605863  :  1,000,050
5360116492 0990258745 5521403969 7911534022 4158981324  :  1,000,100

5367596380 1909194175 8655931287 3960279125 1059654044  :  1,999,900
9621215177 0209578971 0665525923 6971933822 8226749132  :  1,999,950
2907174473 5892565046 1663735632 3687106519 1457297909  :  2,000,000

-- Pi - Dec - Chudnovsky - 3.ycd
6121731251 1797846672 6688273303 9105348653 0738901742  :  2,000,050
8273437618 9978918962 3531076701 5851424532 8542510122  :  2,000,100
7669694946 5807727822 8726883325 8449796975 2213606776  :  2,000,150
*/

import model.pi.SurvivalList;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("NonAsciiCharacters")
class SuvivalListTest extends TestBase {

    @org.junit.jupiter.api.Test
    void 作りながら動かす用(TestInfo testInfo) {

        SurvivalList sl = new SurvivalList(5, "00002", 10);


    }


    @org.junit.jupiter.api.Test
    void コンストラクタTest(TestInfo testInfo) {
        assertThrows(RuntimeException.class, () -> new SurvivalList(-1, "1", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(1, "a", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(1, "", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(1, null, 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(1, "00", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(1, "00", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(100, "000", 1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(100, "000", -1));
        assertThrows(RuntimeException.class, () -> new SurvivalList(100, "000", 0));
        new SurvivalList(1, "1", 1);
        new SurvivalList(2, "11", 1);
    }


}
