package controller;

import java.io.File;
import java.util.*;

public class Main {

    private final static String propFileName = "/default.properties";

    //CPUの使用状況を表示する


    public static void main(String[] arg) {

        //プロパティーファイルロード
        if(0 >= arg.length){
            //プログラム引数がない場合はデフォルトのプロパティーファイル名で読み込みを試行。
            String propPath = new File(".").getAbsoluteFile().getParent();
            Env.setPropFileName(propPath + propFileName);
        }else{
            //プログラム引数がある場合はarg[0]をファイル名として、それをプロパティーファイルとして読み込みを試行
            Env.setPropFileName(arg[0]);
            //Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");
        }

        //Envクラスのインスタンスを取得 (シングルトン)
        Env env = Env.getInstance();

        //Envの内容表示
        env.printProp();

        //検索対象の円周率ファイルリスト作成
        List<File> piFileList = env.createFileListByProp();

        //バックグラウンドで検索処理開始
        Searcher searcher = new Searcher(piFileList, env.getListSize(), env.getUnitLength(), env.getReportSpan());
        searcher.start();

        //WEB Start
        Web.init();

    }

}
