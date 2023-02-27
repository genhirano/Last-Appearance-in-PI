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

@SuppressWarnings("NonAsciiCharacters")
class SurvivalMangerTest extends TestBase {


    @org.junit.jupiter.api.Test
    void 作りながら動かす用2(TestInfo testInfo) throws IOException, InterruptedException {

        //結果保存ファイルを検索し、あれば読み込み、なければ新規作成する
        //ファイルを読み込み、次のスタートエンドを決定する
        //次のリストをメモリにつくる
        //メモリリストの検索開始して、最後の一つになるまで検索する
        //その一つを記録する
        //もどる

        List<File> fileList = createFileList();

        Integer targetlength = 9;
        Integer listSize = 1000;
        Integer unitLength = 1900;
        Integer reportSpan = 500;
/*
        Integer targetlength = 8;
        Integer listSize = 50000;
        Integer unitLength = 1900;
        Integer reportSpan = 5000;
*/

        /*
        Integer targetlength = 6;
        Integer listSize = 51111;
        Integer unitLength = 1900;
        Integer reportSpan = 100;
*/
        String path = "./target/output";
        String filename = String.format("%02d", targetlength) + ".txt";

        //保存用ファイルの存在確認
        File dir = new File(path);
        File[] files = dir.listFiles();
        File targetFile = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().equals(filename)) {
                System.out.println( filename + "  found!");
                targetFile = file;
                break;
            }
        }

        //保存用ファイルがなかった場合は空ファイルを作成する（初めての実行）
        if (null == targetFile) {
            // Fileオブジェクトの生成
            File file = new File(path + "/" + filename);
            file.createNewFile();
            targetFile = file;
            System.out.println( filename + "create!");
        }

        //YCDフィアルの全体像をつかむ
        //全ファイルヘッダー情報取得
        Map<File, Map<YCD_SeqProvider.FileInfo, String>> ycdFileMap = YCD_SeqProvider.createFileInfo(fileList,targetlength);
        Integer fileCont = ycdFileMap.size();
        Long total = 0L;
        for(File f : ycdFileMap.keySet()){
            total = Long.valueOf(ycdFileMap.get(f).get(YCD_SeqProvider.FileInfo.END_DIGIT));
        }
        System.out.println("FILE COUNT:" + fileCont + "  MAX DEPTH: " + total);


        Integer continueCount = 0;
        while (true) {

            if(5 < continueCount){
                System.out.println("コンティニューカウントが上限に達しました :" + continueCount);
                break;
            }

            //保存用ファイルから全データ読み込み
            String fname = targetFile.getPath();
            List<String> list = Files.readAllLines(Paths.get(fname), StandardCharsets.UTF_8);

            //次に実行すべき情報を作る
            //（すでにある保存データの一番最後の、その次として実行する情報を作る）
            String nextMin = "";
            String nextMax = "";
            YCD_SeqProvider.Unit currentPi = null;
            ZonedDateTime startTime = null;
            ZonedDateTime endTime = null;


            if (0 == list.size()) {
                //保存ファイルが空っぽならば、最初としての実行情報を創作する
                nextMin = StringUtils.repeat("0", targetlength); //スタート桁は0桁目

                nextMax = String.format("%0" + nextMin.length() + "d", (0 + listSize - 1)); //終了桁（スタート+指定の桁数）
                if (targetlength < nextMax.length()) {
                    nextMax = StringUtils.repeat("9", targetlength);
                }

            } else {
                //保存ファイルの一番最後の行の次として実行情報を作る

                //最終行のデータ取得
                String readLine = list.get(list.size() - 1);

                if (readLine.trim().isEmpty()) {
                    throw new IOException("file is invalid(Last Line is empty!) " + filename);
                }

                String[] readLineArr = readLine.split(",");

                String prevEndStr = readLineArr[1];

                //終了条件
                if (prevEndStr.equals(StringUtils.repeat("9", targetlength))) {
                    break;
                }


                //次のスタート桁は、最終行のMAX + 1 桁目
                Integer nextStart = Integer.valueOf(prevEndStr) + 1;
                nextMin = String.format("%0" + prevEndStr.length() + "d", nextStart);

                //次の終了桁は スタート桁位置 + 指定の桁数
                nextMax = String.format("%0" + prevEndStr.length() + "d", nextStart + listSize - 1);

                if (targetlength < nextMax.length()) {
                    nextMax = StringUtils.repeat("9", targetlength);
                }

            }

            SurvivalList sl = new SurvivalList(targetlength, Integer.valueOf(nextMin), Integer.valueOf(nextMax));

            String lastData = "";
            Long lastFoundPos = -1L;


            int count = 0;
            try (YCD_SeqProvider p = new YCD_SeqProvider(fileList, targetlength, unitLength);) {

                startTime = ZonedDateTime.now();

                while (p.hasNext()) {

                    currentPi = p.getNext();

                    count++;

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

                    if ( (0 == count % reportSpan) || (sl.isEmpty())) {
                        System.out.println(
                                "FILE_INDEX:" + currentPi.getFileInfo().get(YCD_SeqProvider.FileInfo.BLOCK_INDEX)
                                        + " " + currentPi.getStartDigit() + " から "
                                        + nextMin + "-" + nextMax + " リスト残り:" + sl.size() + " count: " + count);
                    }


                    if (0 >= sl.size()) {
                        break;
                    }

                }

                if (!sl.isEmpty()) {
                    throw new RuntimeException("The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)");
                }

                endTime = ZonedDateTime.now();

            } catch (Exception e) {
                continueCount ++;
                RuntimeException ee = new RuntimeException("エラーが発生しましたので、最初からやりなおしてみます。コンティニューカウント : " + continueCount, e);
                ee.printStackTrace();
                Thread.sleep(3000);
                continue;
            }


            //出力先を作成する
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(targetFile.getPath(), true)));) {

                //今回分追加
                Duration summerVacationDuration = Duration.between(startTime, endTime);

                //進捗状況
                Double allMax =  Double.valueOf(StringUtils.repeat("9", targetlength));
                double d = (Double.valueOf(nextMax) / allMax) * 100;
                d = ((double)Math.round(d * 1000))/1000;

                String recordStr =
                        nextMin
                                + "," + nextMax
                                + "," + lastData
                                + "," + lastFoundPos
                                + "," + currentPi.getFileInfo().get(YCD_SeqProvider.FileInfo.BLOCK_INDEX)
                                + "," + d + "%"
                                + "," + summerVacationDuration.getSeconds() + "sec";

                pw.println(recordStr);
                System.out.println(recordStr);

            }

        }

    }

}
