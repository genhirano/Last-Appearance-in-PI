package model.pi;

import java.util.ArrayList;

public class SurvivalList extends ArrayList<String> {

    //一回戦の計画をする
    //二回戦はマージする

    public SurvivalList(Integer targetLength, Integer groupSize, Long gropuHeadNumber) {
        super();

        //ターゲットの長さチェック
        if(1 > targetLength){
            throw new RuntimeException("target length is not valid (under 1): " + targetLength);
        }

        //グループサイズチェック
        if(1 > groupSize){
            throw new RuntimeException("group size is not valid (under 1): " + groupSize);
        }

        //グループ先頭文字列の作成とチェック
        if(0L > gropuHeadNumber){
            throw new RuntimeException("group head number is fail (under zero): " + gropuHeadNumber);
        }





        //生き残りリストの作成。当初は全員生き残りとする


    }

}
