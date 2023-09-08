package controller;

import java.io.File;
import java.util.*;

public class Main {

    private final static String propFileName = "/default.properties";

    public static void main(String[] arg) {

        //バブルソートのスニペット



        //プロパティーファイルの読み込み。
        if(0 >= arg.length){
            //プログラム引数がない場合はデフォルトのプロパティーファイル名で読み込みを試行。
            String propPath = new File(".").getAbsoluteFile().getParent();
            Env.setPropFileName(propPath + propFileName);
        }else{
            //プログラム引数がある場合はarg[0]をファイル名として、それをプロパティーファイルとして読み込みを試行
            Env.setPropFileName(arg[0]);
            //Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");
        }

        //読み込んだEnvファイルの内容表示
        Env.getInstance().printProp();

        //プロパティーフィアルから読み込み円周率ファイルリスト作成
        List<File> piFileList = Env.getInstance().createFileListByProp();

        //バックグラウンド処理開始-------------------------------------
        Searcher searcher = new Searcher(piFileList, Env.getInstance().getListSize(), Env.getInstance().getUnitLength(), Env.getInstance().getReportSpan());
        searcher.start();

        //WEB　Start
        Web.init();

    }

}
