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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.TestInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("NonAsciiCharacters")
class SuvivalListTest extends TestBase {

    @org.junit.jupiter.api.Test
    void 作りながら動かす用2(TestInfo testInfo) throws IOException {

        //結果保存ファイルを検索し、あれば読み込み、なければ新規作成する
        //ファイルを読み込み、次のスタートエンドを決定する
        //次のリストをメモリにつくる
        //メモリリストの検索開始して、最後の一つになるまで検索する
        //その一つを記録する
        //もどる

        Integer targetlength = 3;
        String path = "./target/output";
        String filename = String.format("%02d", targetlength) + ".txt";
        Integer listSize = 10;

        //保存用ファイルの存在確認
        File dir = new File(path);
        File[] files = dir.listFiles();
        File targetFile = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.getName().equals(filename)) {
                System.out.println("found!");
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
            System.out.println("create!");
        }

        System.out.println(targetFile.getName());

        //保存用ファイルから全データ読み込み
        String fname = targetFile.getPath();
        List<String> list = Files.readAllLines(Paths.get(fname), StandardCharsets.UTF_8);


        //次に実行すべき情報を作る
        //（すでにある保存データの一番最後の、その次として実行する情報を作る）
        String nextMin = "";
        String nextMax = "";
        if (0 == list.size()) {
            //保存ファイルが空っぽならば、最初としての実行情報を創作する
            nextMin = StringUtils.repeat("0", targetlength); //スタート桁は0桁目
            nextMax = String.format("%0" + nextMin.length() + "d", (0 + listSize - 1)); //終了桁（スタート+指定の桁数）
        } else {
            //保存ファイルの一番最後の行の次として実行情報を作る

            //最終行のデータ取得
            String readLine = list.get(list.size() - 1);
            String[] readLineArr = readLine.split(",");

            //次のスタート桁は、最終行のMAX + 1 桁目
            Integer nextStart = Integer.valueOf(readLineArr[1]) + 1;
            nextMin = String.format("%0" + readLineArr[1].length() + "d", nextStart);

            //次の終了桁は スタート桁位置 + 指定の桁数
            nextMax = String.format("%0" + nextMin.length() + "d", nextStart + listSize - 1);

        }

        System.out.println(nextMin + "" + nextMax);

        SurvivalList sl = new SurvivalList(targetlength, Integer.valueOf(nextMin), Integer.valueOf(nextMax));

        //検索して、最後の一つにする
        //Random r = new Random();
        //Integer nokoriIndex = r.nextInt(sl.size());

        for (int i = sl.size() - 1; i >= 0; i--) {

            if (i == 2) {
                continue;
            }
            sl.remove(sl.get(i), 345L);

        }

        System.out.println(sl.get(0) + "," + sl.getLastFindIndex());


        //最後の一つになったとする

        try {

            //出力先を作成する
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(targetFile.getPath(),true)));) {

                //今回分追加
                pw.println(nextMin + ","+ nextMax + ","+  sl.get(0) + "," + sl.getLastFindIndex());

            }

        } catch (IOException e) {
            //TODO 必要であればメッセージを追加する
            throw new IOException(e);
        }


    }

    @org.junit.jupiter.api.Test
    void 作りながら動かす用(TestInfo testInfo) throws IOException {

        //結果保存ファイルを検索し、あれば読み込み、なければ新規作成する
        //ファイルを読み込み、次のスタートエンドを決定する
        //次のリストをメモリにつくる
        //メモリリストの検索開始して、最後の一つになるまで検索する
        //その一つを記録する
        //もどる


        Integer targetlength = 3;
        Integer max = Integer.valueOf(StringUtils.repeat("3", targetlength));

        Integer current = 0;

        Integer step = 100;
        while (current <= max) {

            SurvivalList sl = new SurvivalList(targetlength, current, step);

            current = current + step;
        }


        //resume
        //次のスタート位置を探す

        String path = "./target/output";
        File dir = new File(path);
        File[] files = dir.listFiles();
        List<List<String>> fileInfoList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            String fileName = file.getName().replace(".txt", "");
            List sarr = Arrays.asList(fileName.split("_"));
            fileInfoList.add(sarr);
        }


        //不純物の削除
        for (int i = fileInfoList.size() - 1; i >= 0; i--) {
            if (targetlength != Integer.valueOf(fileInfoList.get(i).get(0))) {
                fileInfoList.remove(i);
            }
        }

        //終了位置が一番小さいものを検索して、保持する
        Integer minStart = Integer.MAX_VALUE;
        List<String> min = null;
        for (List<String> l : fileInfoList) {
            Integer start = Integer.valueOf(l.get(1));
            if (minStart >= start) {
                min = l;
                minStart = start;
            }
        }
        System.out.println(min);

        //いちいちで、残り一つになるまでやっていく


    }


    @org.junit.jupiter.api.Test
    void コンストラクタTest(TestInfo testInfo) {
/*
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
*/
    }


}
