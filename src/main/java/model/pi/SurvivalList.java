package model.pi;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * サバイバルリスト.
 *
 * 基本的には{@code ArrayList<String>} と同じだが、StartからEndまでの連番ゼロ埋めを自動生成した状態を初期値とする.
 * 総当たり検索用。
 */
public class SurvivalList extends ArrayList<String> {

    /**
     * コンストラクタ.
     *
     * 指定長さの、スタートからエンドまでのゼロ埋め連続リストを作成する.
     *
     * @param length 桁数
     * @param start 開始数
     * @param end 終了数
     *
     *
     */
    public SurvivalList(Integer length, Integer start, Integer end) {
        super();

        //生き残り（全員生き残っているとする）を作成
        //リストサイズの指定が大きい場合、素直に追加するとターゲット桁長さを超えてしまうので、その際はターゲット長のマックスで打ち切り
        for(int i = start; i <= end; i++){
            String s = String.format("%0" + length + "d", i);

            if(length < s.length() ){
                break;
            }

            this.add(s);

            //全桁 9 であればそれがMAXであり、それ以上は不要なので、追加は終わり
            if(s.equals(StringUtils.repeat( "9", length))){
                break;
            }
        }
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList() throws IllegalAccessException {
        super();
        throw new IllegalAccessException("no args");
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList(int initialCapacity) throws IllegalAccessException {
        super();
        throw new IllegalAccessException("no args");
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList(Collection<? extends String> c) throws IllegalAccessException {
        super();
        throw new IllegalAccessException("no args");
    }


}
