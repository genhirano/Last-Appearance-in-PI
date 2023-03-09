package model.pi;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * サバイバルリスト.
 *
 * 基本的にはArrayList<String>と同じだが、StartからEndまでの連番ゼロ埋めを自動生成した状態を初期値とする.
 */
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
