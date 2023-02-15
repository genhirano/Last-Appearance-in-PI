package controller;

import second.YCD_Searcher;

import java.io.File;
import java.util.*;

public class Main {

    //対象ファイルリスト作成
    public static List<File> createFileList() {
        List<File> fileList = new ArrayList<>();

        String path = new File(".").getAbsoluteFile().getParent();
        path = path + "\\src\\test\\resources\\1000000";

        fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 0.ycd"));
        fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 1.ycd"));
        fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 2.ycd"));
        fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 3.ycd"));

        return fileList;
    }

    public static List<File> createFileListBig() {
        List<File> fileList = new ArrayList<>();

        //String path = "H:\\Pi";
        String path = "c:";

        fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 0.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 1.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 2.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 3.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 4.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 5.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 6.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 7.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 8.ycd"));
        //fileList.add(new File(path + "//Pi - Dec - Chudnovsky - 9.ycd"));

        return fileList;
    }


    public static void main(String[] arg) {

        List < File > fileList = createFileList();

        Map<String, Map<YCD_Searcher.Container, String>> map = new HashMap<>();

        map.put("10", new HashMap<YCD_Searcher.Container, String>());
        map.put("11", new HashMap<YCD_Searcher.Container, String>());
        map.put("1419", new HashMap<YCD_Searcher.Container, String>());
        map.put("2213606776", new HashMap<YCD_Searcher.Container, String>());
        map.put("22136067760000000000", new HashMap<YCD_Searcher.Container, String>());

        YCD_Searcher y = new YCD_Searcher(fileList, map);
        y.start();
        try {
            y.join();
        } catch (InterruptedException e) {
            // 例外処理
            e.printStackTrace();
        }

        for(String s : y.getTargetMap().keySet()){
            System.out.println(s + " | " + y.getTargetMap().get(s));
        }

    }


}
