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


import model.pi.YCD_Provider;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        try (YCD_Provider p = new YCD_Provider(fileList, 10, 30);) {

            YCD_Provider.Unit u = null;

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

            while (p.hasNext()) {
                u = p.getNext();

                //最初のファイル ラスト前
                if ((999951 == u.getStartDigit())) {
                    assertEquals("5678796130331164628399634646042209010610", u.getData());
                }

                //最初のファイル ラスト。桁が足りずに短い桁になっている
                //次のファイルの先頭10桁「3092756283」が、ケツに付与されている
                if ((999981 == u.getStartDigit())) {
                    assertEquals("220901061057794581513092756283", u.getData());
                }

                //-----------
                //次のファイルの先頭。
                if ((1000001 == u.getStartDigit())) {
                    assertEquals("3092756283309275628320845315846520010277", u.getData());
                }

                //次のファイルの２ユニット目
                if ((1000031 == u.getStartDigit())) {
                    assertEquals("6520010277972356129230126058635360116492", u.getData());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("実行時間: " + (new Date().getTime() - startTime) + "ms");
    }


/*
    @org.junit.jupiter.api.Test
    void ファイルヘッダー情報取得(TestInfo testInfo) throws InterruptedException {

        List<File> fileList = createFileList();
        Map<String, Map<YCD_Provider.Container, String>> m = new HashMap<>();

        m.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1");
            }
        });

        YCD_Provider p = new YCD_Provider(fileList, m);

        //ファイルリストチェック
        Integer i = 0;
        for (File mm : p.getFileInfoMap().keySet()) {

            //ファイル名
            assertEquals("Pi - Dec - Chudnovsky - " + i + ".ycd", mm.getName());

            //先頭データ
            switch (i) {
                case 0:
                    assertEquals("141592", p.getFileInfoMap().get(mm).get(YCD_Provider.FileInfo.FIRST_DATA));
                    break;
                case 1:
                    assertEquals("309275", p.getFileInfoMap().get(mm).get(YCD_Provider.FileInfo.FIRST_DATA));
                    break;
                case 2:
                    assertEquals("612173", p.getFileInfoMap().get(mm).get(YCD_Provider.FileInfo.FIRST_DATA));
                    break;
                case 3:
                    assertEquals("697067", p.getFileInfoMap().get(mm).get(YCD_Provider.FileInfo.FIRST_DATA));
                    break;
                default:
                    //到達したら不合格
                    assertTrue(false);
            }

            i++;
        }

    }


    @org.junit.jupiter.api.Test
    void 検索一般(TestInfo testInfo) throws InterruptedException {
        System.out.printf("TEST:[%s]", testInfo.getDisplayName());

        //ファイル入出力の処理時間計測を開始
        long startTime = new Date().getTime();

        List<File> fileList = createFileList();

        Map<String, Map<YCD_Provider.Container, String>> targetMap = new HashMap<>();


        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1");
            }
        });
        targetMap.put("1415926", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "2");
            }
        });
        targetMap.put("612173125117978466726688273303", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "2");
            }
        });
        targetMap.put("99999999999999", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "2");
            }
        });
        targetMap.put("9096", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1999998");
            }
        });

        YCD_Provider ys = new YCD_Provider(fileList, targetMap);
        Thread t1 = new Thread(ys);
        t1.start();
        t1.join();

        assertEquals("1", targetMap.get("141592").get(YCD_Provider.Container.FIND_DIGIT));
        assertEquals("1", targetMap.get("141592").get(YCD_Provider.Container.START_DIGIT));
        assertEquals("1", targetMap.get("141592").get(YCD_Provider.Container.REACHED_DIGIT));

        assertFalse(targetMap.get("99999999999999").containsKey(YCD_Provider.Container.FIND_DIGIT));
        assertEquals("2", targetMap.get("99999999999999").get(YCD_Provider.Container.START_DIGIT));
        assertEquals("3999986", targetMap.get("99999999999999").get(YCD_Provider.Container.REACHED_DIGIT));

        assertEquals("1999998", targetMap.get("9096").get(YCD_Provider.Container.FIND_DIGIT));
        assertEquals("1999998", targetMap.get("9096").get(YCD_Provider.Container.START_DIGIT));
        assertEquals("1999998", targetMap.get("9096").get(YCD_Provider.Container.REACHED_DIGIT));

        assertEquals("2000001", targetMap.get("612173125117978466726688273303").get(YCD_Provider.Container.FIND_DIGIT));
        assertEquals("2", targetMap.get("612173125117978466726688273303").get(YCD_Provider.Container.START_DIGIT));
        assertEquals("2000001", targetMap.get("612173125117978466726688273303").get(YCD_Provider.Container.REACHED_DIGIT));

        assertEquals("1457055", targetMap.get("1415926").get(YCD_Provider.Container.FIND_DIGIT));
        assertEquals("2", targetMap.get("1415926").get(YCD_Provider.Container.START_DIGIT));
        assertEquals("1457055", targetMap.get("1415926").get(YCD_Provider.Container.REACHED_DIGIT));

        //ファイル入出力の処理時間計測結果を出力
        System.out.println("実行時間: " + (new Date().getTime() - startTime) + "ms");

    }

    @org.junit.jupiter.api.Test
    void 検索_ファイル末端と次のファイルの境界(TestInfo testInfo) throws InterruptedException {
        System.out.printf("TEST:[%s]", testInfo.getDisplayName());

        //-- Pi - Dec - Chudnovsky - 2.ycd
        //5367596380 1909194175 8655931287 3960279125 1059654044  :  1,999,900
        //9621215177 0209578971 0665525923 6971933822 8226749132  :  1,999,950
        //2907174473 5892565046 1663735632 3687106519 1457297909  :  2,000,000
        //
        //-- Pi - Dec - Chudnovsky - 3.ycd
        //6121731251 1797846672 6688273303 9105348653 0738901742  :  2,000,050
        //8273437618 9978918962 3531076701 5851424532 8542510122  :  2,000,100
        //7669694946 5807727822 8726883325 8449796975 2213606776  :  2,000,150

        //ファイル入出力の処理時間計測を開始
        long startTime = new Date().getTime();

        List<File> fileList = createFileList();

        Map<String, Map<YCD_Provider.Container, String>> targetMap = new TreeMap<>();

        String exp = "";

        //"61217"
        // ↑ 2,000,001
        targetMap.put("61217", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1999998");
            }
        });
        exp = exp + ":" + "61217=FIND_DIGIT=2000001";


        //"909"
        //   ↑ 2,000,000
        targetMap.put("909", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1999998");
            }
        });
        exp = exp + ":" + "909=FIND_DIGIT=1999998";

        //"9096"
        //   ↑ 2,000,000
        targetMap.put("9096", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1999998");
            }
        });
        exp = exp + ":" + "9096=FIND_DIGIT=1999998";

        //"961217"
        // ↑ 2,000,000
        targetMap.put("961217", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1999998");
            }
        });
        exp = exp + ":" + "961217=FIND_DIGIT=2000000";


        YCD_Provider ys = new YCD_Provider(fileList, targetMap);
        Thread t1 = new Thread(ys);
        t1.start();
        t1.join();

        String act = "";
        for (String s : targetMap.keySet()) {
            act = act + ":" + s + "=" + YCD_Provider.Container.FIND_DIGIT + "=" + targetMap.get(s).get(YCD_Provider.Container.FIND_DIGIT);
        }

        assertEquals(exp, act);

        //ファイル入出力の処理時間計測結果を出力
        System.out.println("実行時間: " + (new Date().getTime() - startTime) + "ms");

    }


    @org.junit.jupiter.api.Test
    void ターゲットマップチェック(TestInfo testInfo) {
        List<File> fileList = createFileList();
        Map<String, Map<YCD_Provider.Container, String>> targetMap = new HashMap<>();

        //空のMap
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));


        //スタート桁が空文字
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));


        //スタート桁が数値でない
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "qqqqqq");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));

        //スタート桁がゼロ
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "0");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));

        //スタート桁がマイナス
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "-1");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));

        //スタート桁が計算式
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1+2");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));

        //スタート桁がLONGキャストエラー
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> new YCD_Provider(fileList, targetMap));

        //正常
        targetMap.put("141592", new HashMap<>() {
            {
                put(YCD_Provider.Container.START_DIGIT, "1");
            }
        });
        assertDoesNotThrow(() -> new YCD_Provider(fileList, targetMap));

    }
 */


}
