package model.pi;

import java.util.ArrayList;

public class SurvivalList extends ArrayList<String> {

    //一回戦の計画をする
    //二回戦はマージする

    public SurvivalList(Integer targetLength, String start, Integer listsize) {
        super();

        //生き残りリスト。当初は全員生き残りとする

        //ターゲットの長さチェック
        if (1 > targetLength) {
            throw new RuntimeException("target length is not valid (under 1): " + targetLength);
        }
        if (100 <= targetLength) {
            throw new RuntimeException("target length is not valid (over 100): " + targetLength);
        }

        if (start.isEmpty()) {
            throw new RuntimeException("start is empty: " + start);
        }
        if (start.length() != targetLength) {
            throw new RuntimeException("start is invalid length : " + start);
        }

        try {
            Integer i = Integer.parseInt(start);
            if (0 > i) {
                throw new RuntimeException("start is invalid: " + start);
            }
        } catch (Exception e) {
            throw new RuntimeException("start is invalid: " + start + " " + e.getLocalizedMessage());
        }

        Integer startInt = Integer.parseInt(start);

        //基本ファイル名
        String targetLengthStr = String.format("%02d", targetLength);
        System.out.println(targetLengthStr
                + "_" + String.format("%0" + targetLength + "d",startInt)
                + "_" + String.format("%0" + targetLength + "d", (listsize - 1) + startInt)
        );


        for (int i = 0; i < listsize; i++) {
            this.add(String.format("%0" + targetLength + "d", i + startInt));
        }


        System.out.println(this.toString());

        System.out.print("_" + this.size() + "_" + "456678765432");
        System.out.println("_" + "00022" + "_" + "456678765432");




        /*
        ターゲット桁数
        先頭文字列
        最終文字列
        検索終了位置 15桁とする (19,000,000,000,000)
         */




    }

}
