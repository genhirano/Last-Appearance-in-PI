package model.pi;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SurvivalList extends ArrayList<String> {

    private Long lastFindIndex = -1L;
    public Long getLastFindIndex(){
        if(1 != this.size()){
            throw new RuntimeException("まださいごではない");
        }

        return this.lastFindIndex;
    }


    public SurvivalList(Integer targetLength, Integer start, Integer end) {
        super();

        //生き残り（全員生き残っているとする）を作成
        //リストサイズの指定が大きすぎて、素直に追加するとターゲット長さを超えてしまうので、その際はターゲット長のマックスで打ち切り

        for(int i = start; i <= end; i++){
            String s = String.format("%0" + targetLength + "d", i);
            this.add(s);
        }

    }

    public void remove(String findValue, Long findPos){
        this.lastFindIndex = findPos;
        this.remove(this.indexOf(findValue)) ;
    }

/*
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
*/
}
