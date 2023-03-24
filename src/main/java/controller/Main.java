package controller;

import model.ycd.YCDFileUtil;
import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import static model.ycd.YCDFileUtil.createFileInfo;
import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Main {

    private static ZonedDateTime startTime = ZonedDateTime.now();

    private static Map<File, Map<YCDFileUtil.FileInfo, String>> YCD_FILE_MAP;

    //ThymeleafTemplateEngine
    private static final TemplateEngine templateEngine = new ThymeleafTemplateEngine();

    public static void main(String[] arg) {

        //デバッグ画面を有効にする
        enableDebugScreen();

        //プログラム引数がないときはデフォルト「./default.properties」を環境設定として読み込む
        //(TESTの時はプログラム引数に任意のデータをセットすると同時に、プロパティー名を以下の記載と同じようにテスト用のものをせっとしておくこと)
        if(1 > arg.length){
            String propPath = new File(".").getAbsoluteFile().getParent();
            Env.setPropFileName(propPath + "\\default.properties");
        }


        //TODO なおす
        //in Test!!!!!
        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");


        //読み込み円周率ファイルリスト作成
        List<File> piFileList = createFileListByProp();

        Integer maxTargetLength = Integer.valueOf(Env.getInstance().getProp().get(Env.PropKey.maxTargetLength.toString()).toString());
        Integer listSize = Integer.valueOf(Env.getInstance().getProp().getProperty(Env.PropKey.listSize.toString()));
        Integer unitLength = Integer.valueOf(Env.getInstance().getProp().getProperty(Env.PropKey.unitLength.toString()));
        Integer reportSpan = Integer.valueOf(Env.getInstance().getProp().getProperty(Env.PropKey.reportSpan.toString()));

        Integer portNo = Integer.valueOf(Env.getInstance().getProp().getProperty(Env.PropKey.port.toString()));


        //事前情報収集フェーズ-------------------------------------

        //検索対象のYCDフィアルの全体像をつかむ
        //全ファイルヘッダー情報取得 (targetLengthはこの処理では重要でないので、適当な値を入れている)
        YCD_FILE_MAP = createFileInfo(piFileList, 1);
        Long ycdMaxDepth = 0L;
        for (File f : YCD_FILE_MAP.keySet()) {
            ycdMaxDepth = Long.valueOf(YCD_FILE_MAP.get(f).get(YCDFileUtil.FileInfo.END_DIGIT));
        }
        System.out.println("FILE COUNT:" + YCD_FILE_MAP.size() + "  MAX DEPTH: " + ycdMaxDepth);

        //バックグラウンド処理開始-------------------------------------
        Searcher s = new Searcher(piFileList, maxTargetLength, listSize, unitLength, reportSpan);
        s.start();

        //WEB-------------------------------------
        port(portNo);
        staticFiles.location("/public");
        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {

                //現在時刻取得
                ZonedDateTime nowTime = ZonedDateTime.now();
                Duration summerVacationDuration = Duration.between(startTime, nowTime);

                //最大検索桁数取得
                Long ycdMaxDepth = 0L;
                for (File f : YCD_FILE_MAP.keySet()) {
                    ycdMaxDepth = Long.valueOf(YCD_FILE_MAP.get(f).get(YCDFileUtil.FileInfo.END_DIGIT));
                }

                //保存ファイルコントローラーを作成し、
                StoreController sc = StoreController.getInstance();
                List<String> sum = sc.getSummary();
                Collections.reverse(sum);

                //ビューに渡すデータ作成
                Map<String, Object> model = new HashMap<>();
                model.put("DATA", sum);
                model.put("YCD_MAX_DEPTH", ycdMaxDepth);
                model.put("SYSTEMSTART", startTime);
                model.put("RUNNING_TIME", summerVacationDuration.getSeconds());

                System.out.println(model);

                return new ModelAndView(model, "index");

            }
        }, templateEngine);


        while(s.isAlive()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("End.");

    }

    public static List<File> createFileListByProp() {
        List<File> fileList = new ArrayList<>();


        final String notfound = "NOTFOUND";
        for (int i = 0; i < 9999; i++) {

            String noStr = String.format("%03d", i);
            try {
                String fullPath = Env.getInstance().getProp().getProperty("ycd" + noStr, notfound);

                File f = new File(fullPath);
                if (!f.exists()) {
                    if (i == 0) {
                        throw new RuntimeException("piFile is not found: " + f);
                    } else {
                        break;
                    }

                }

                fileList.add(new File(fullPath));

            } catch (MissingResourceException e) {
                if (i == 0) {
                    throw new RuntimeException("piFile is not define by property file: " + noStr);
                } else {
                    break;
                }
            }
        }

        return fileList;
    }

}
