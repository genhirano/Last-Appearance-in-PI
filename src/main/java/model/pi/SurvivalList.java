package model.pi;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SurvivalList extends ArrayList<String> {

    //一回戦の計画をする
    //二回戦はマージする

    private String myName;

    public SurvivalList(Integer targetLength, Integer start, Integer listsize) {
        super();

        //生き残りリスト。当初は全員生き残りとする

        //ターゲットの長さチェック
        if (1 > targetLength) {
            throw new RuntimeException("target length is not valid (under 1): " + targetLength);
        }
        if (100 <= targetLength) {
            throw new RuntimeException("target length is not valid (over 100): " + targetLength);
        }

        if (0 >start) {
            throw new RuntimeException("start is invalid: " + start);
        }

        //生き残り（全員生き残っているとする）を作成
        //リストサイズの指定が大きすぎて、素直に追加するとターゲット長さを超えてしまうので、その際はターゲット長のマックスで打ち切り
        String max = String.format("%0" + targetLength + "d", start);
        for (int i = 0; i < listsize; i++) {
            String s = String.format("%0" + targetLength + "d", i + start);
            if (s.length() > targetLength) {
                break;
            }
            this.add(s);
            max = s;
        }

        //基本ファイル名
        String targetLengthStr = String.format("%02d", targetLength);
        this.myName = targetLengthStr
                + "_" + String.format("%0" + targetLength + "d", start)
                + "_" + max;







        /*
        ターゲット桁数
        先頭文字列
        最終文字列
        検索終了位置 15桁とする (19,000,000,000,000)
         */


    }

    public void saveToFile(Integer ycdFileIndex) throws IOException {

        String writeFileName = "./target/output/";

        writeFileName = writeFileName + this.myName + "_" + ycdFileIndex + ".txt";

        try {

            //出力先を作成する
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(writeFileName)));) {
                for (String s : this) {
                    pw.println(s);
                }
            }

        } catch (IOException e) {

            //TODO 必要であればメッセージを追加する
            throw new IOException(e);
        }

    }

}
