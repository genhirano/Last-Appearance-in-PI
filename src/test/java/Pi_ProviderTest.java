import model.ycd.YCD_SeqProvider;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//-- Pi - Dec - Chudnovsky - 1.ycd
//1415926535 8979323846 2643383279 5028841971 6939937510  :  50
//5820974944 5923078164 0628620899 8628034825 3421170679  :  100
//8214808651 3282306647 0938446095 5058223172 5359408128  :  150
//
//0315614033 3212728491 9441843715 0696552087 5424505989  :  999,950
//5678796130 3311646283 9963464604 2209010610 5779458151  :  1,000,000
//
//-- Pi - Dec - Chudnovsky - 2.ycd
//3092756283 2084531584 6520010277 9723561292 3012605863  :  1,000,050
//5360116492 0990258745 5521403969 7911534022 4158981324  :  1,000,100
//
//5367596380 1909194175 8655931287 3960279125 1059654044  :  1,999,900
//9621215177 0209578971 0665525923 6971933822 8226749132  :  1,999,950
//2907174473 5892565046 1663735632 3687106519 1457297909  :  2,000,000
//
//-- Pi - Dec - Chudnovsky - 3.ycd
//6121731251 1797846672 6688273303 9105348653 0738901742  :  2,000,050
//8273437618 9978918962 3531076701 5851424532 8542510122  :  2,000,100
//7669694946 5807727822 8726883325 8449796975 2213606776  :  2,000,150

@SuppressWarnings("NonAsciiCharacters")
class Pi_ProviderTest extends TestBase {

    @org.junit.jupiter.api.Test
    void YCDデータ読み込みテスト(TestInfo testInfo) throws IOException {
        long startTime = new Date().getTime();

        System.out.printf("TEST:[%s]%n", testInfo.getDisplayName());

        List<File> fileList = createFileList();

        try (YCD_SeqProvider p = new YCD_SeqProvider(fileList, 10, 30);) {

            YCD_SeqProvider.Unit u = null;

            //最初の30桁
            u = p.getNext();
            assertEquals(1, u.getStartDigit());
            assertEquals("141592653589793238462643383279", u.getData());

            //次の30桁。
            //この文字列の先頭に前のデータから１０桁(対象桁）だけ借りてきてくっついている
            u = p.getNext();
            assertEquals(21, u.getStartDigit());
            assertEquals("2643383279502884197169399375105820974944", u.getData());

            //------------------------------
            //ファイル境界テスト
            //目視確認したデータメモ
            // 999921 : 9441843715069655208754245059895678796130
            // 999951 : 5678796130331164628399634646042209010610
            // 999981 : 220901061057794581513092756283
            // 1000001 : 3092756283309275628320845315846520010277
            // 1000031 : 6520010277972356129230126058635360116492

            Boolean checked01 = false;
            Boolean checked02 = false;
            Boolean checked03 = false;
            Boolean checked04 = false;
            Boolean checked05 = false;

            while (p.hasNext()) {
                u = p.getNext();

                //最初のファイル ラスト前
                if ((999951 == u.getStartDigit())) {
                    assertEquals("5678796130331164628399634646042209010610", u.getData());
                    checked01 = true;
                }

                //最初のファイル ラスト。桁が足りずに短い桁になっている
                //次のファイルの先頭10桁「3092756283」が、ケツに付与されている
                if ((999981 == u.getStartDigit())) {
                    assertEquals("220901061057794581513092756283", u.getData());
                    checked02 = true;
                }

                //-----------
                //次のファイルの先頭。
                if ((1000001 == u.getStartDigit())) {
                    assertEquals("309275628320845315846520010277", u.getData());
                    checked03 = true;
                }

                //次のファイルの２ユニット目
                if ((1000021 == u.getStartDigit())) {
                    assertEquals("6520010277972356129230126058635360116492", u.getData());
                    checked04 = true;
                }

                //任意テスト
                if ((1369531 == u.getStartDigit())) {
                    assertEquals("9075136182741967948495297639533394766485", u.getData());
                    checked05 = true;
                }

            }

            assertTrue(checked01 && checked02 && checked03 && checked04 && checked05);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }



        System.out.println("実行時間: " + (new Date().getTime() - startTime) + "ms");
    }


}
