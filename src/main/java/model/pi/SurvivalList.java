package model.pi;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SurvivalList extends ArrayList<String> {

    public SurvivalList(Integer targetLength, Integer start, Integer end) {

        super();

        //生き残り（全員生き残っているとする）を作成
        //リストサイズの指定が大きい場合、素直に追加するとターゲット桁長さを超えてしまうので、その際はターゲット長のマックスで打ち切り
        for(int i = start; i <= end; i++){
            String s = String.format("%0" + targetLength + "d", i);

            if(targetLength < s.length() ){
                break;
            }

            this.add(s);

            //全桁 9 であればもうそれ以上は不要なので、追加は終わり
            if(s.equals(StringUtils.repeat( "9", targetLength))){
                break;
            }

        }

    }

}
