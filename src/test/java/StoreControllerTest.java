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

import controller.StoreController;
import model.pi.SurvivalList;
import model.ycd.YCD_SeqProvider;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.TestInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("NonAsciiCharacters")
class StoreControllerTest extends TestBase {


    @org.junit.jupiter.api.Test
    void 作りながら動かす用(TestInfo testInfo) throws IOException {

        StoreController sc = StoreController.getInstance();
        List<StoreController.StartEndBean> l = sc.getNextList(1000, 4);

        for (StoreController.StartEndBean se : l) {

            System.out.println(se.getTargetLength() + " | " + se.getStart() + " |  " + se.getEnd());

        }


    }

    @org.junit.jupiter.api.Test
    void 作りながら動かす用2(TestInfo testInfo) throws IOException, InterruptedException {

        List<File> fileList = createFileList();
        String path = "./target/output";
        Integer maxLength = 7;

        Integer listSize = 100000;
        Integer unitLength = 1900;
        Integer reportSpan = 500;
/*
        Integer targetlength = 8;
        Integer listSize = 50000;
        Integer unitLength = 1900;
        Integer reportSpan = 5000;
*/

        //YCDフィアルの全体像をつかむ
        //全ファイルヘッダー情報取得 (targetLengthはこの処理では重要でないので、適当な値を入れている)
        Map<File, Map<YCD_SeqProvider.FileInfo, String>> ycdFileMap = YCD_SeqProvider.createFileInfo(fileList, 1);
        Integer fileCont = ycdFileMap.size();
        Long total = 0L;
        for (File f : ycdFileMap.keySet()) {
            total = Long.valueOf(ycdFileMap.get(f).get(YCD_SeqProvider.FileInfo.END_DIGIT));
        }
        System.out.println("FILE COUNT:" + fileCont + "  MAX DEPTH: " + total);

        //次に実行すべき情報を作る
        //（すでにある保存データの一番最後の、その次として実行する情報を作る）

        Integer continueCount = 0;
        while (true) {

            if (5 < continueCount) {
                System.out.println("コンティニューカウントが上限に達しました :" + continueCount);
                break;
            }

            //結果保存ファイルの全ロード
            //(リジュームのため、非効率ではあるが、ユニットロードの度に毎回やる。このおかげでいつでも途中終了できる)
            StoreController sc = StoreController.getInstance();
            List<StoreController.StartEndBean> storeDataList = sc.getNextList(listSize, maxLength);

            //今回の対象範囲の決定
            Integer targetLength = 0;
            StoreController.StartEndBean targetBean = null;
            for (StoreController.StartEndBean se : storeDataList) {

                //対象桁数
                targetLength++;

                //終わっていないこと（Startが空文字ならばすでに終了済みである）
                if ("".equals(se.getStart())) {
                    continue;
                }

                //範囲決定
                targetBean = se;

                break;
            }

            //次の実行情報が得られなければ終わり
            if (targetBean.getStart().isEmpty()) {
                break;
            }

            YCD_SeqProvider.Unit currentPi = null;
            ZonedDateTime startTime = null;
            ZonedDateTime endTime = null;

            SurvivalList sl = new SurvivalList(targetBean.getTargetLength(), Integer.valueOf(targetBean.getStart()), Integer.valueOf(targetBean.getEnd()));

            String lastData = "";
            Long lastFoundPos = -1L;

            try (YCD_SeqProvider p = new YCD_SeqProvider(fileList, targetBean.getTargetLength(), unitLength);) {

                startTime = ZonedDateTime.now();

                while (p.hasNext()) {

                    currentPi = p.getNext();

                    for (int i = sl.size() - 1; i >= 0; i--) {

                        Integer pos = currentPi.getData().indexOf(sl.get(i));

                        if (0 <= pos) {
                            Long curFindPos = currentPi.getStartDigit() + pos;
                            if (lastFoundPos < curFindPos) {
                                lastData = sl.get(i);
                                lastFoundPos = curFindPos;
                            }
                            sl.remove(i);

                        }
                    }

                    if (0 == (sl.size() % reportSpan)) {
                        System.out.println(
                                "targetLength: " + targetBean.getTargetLength()
                                        + " PiFILE_INDEX:" + currentPi.getFileInfo().get(YCD_SeqProvider.FileInfo.BLOCK_INDEX)
                                        + " SearchingDepth: " + currentPi.getStartDigit()
                                        + targetBean.getStart() + "-" + targetBean.getEnd()
                                        + " remaining: " + sl.size());
                    }


                    //今回の検索対象がなくなったら、このユニットの処理終了
                    if (0 >= sl.size()) {
                        break;
                    }

                }

                if (!sl.isEmpty()) {
                    throw new RuntimeException("The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)");
                }

                endTime = ZonedDateTime.now();

            } catch (Exception e) {
                continueCount++;
                RuntimeException ee = new RuntimeException("エラーが発生しましたので、最初からやりなおしてみます。コンティニューカウント : " + continueCount, e);
                ee.printStackTrace();
                Thread.sleep(3000);
                continue;
            }

            //結果保存
            sc.saveFile(targetBean.getTargetLength(), targetBean.getStart(), targetBean.getEnd(), lastData, lastFoundPos, startTime, endTime);

        }
    }


}
