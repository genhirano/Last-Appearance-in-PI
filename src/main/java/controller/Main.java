package controller;

import model.ycd.YCD_SeqProvider;
import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Main {

    private static ZonedDateTime startTime = ZonedDateTime.now();

    //ThymeleafTemplateEngine
    private static final TemplateEngine templateEngine = new ThymeleafTemplateEngine();

    public static void main(String[] arg) {

        //デバッグ画面を有効にする
        enableDebugScreen();

        port(8080); // Spark will run on port 8080

        staticFiles.location("/public"); //document root

        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {

                ZonedDateTime nowTime = ZonedDateTime.now();
                Duration summerVacationDuration = Duration.between(startTime, nowTime);

                StoreController sc =  StoreController.getInstance();
                List<String> sum = sc.getSummary();
                Collections.reverse(sum);


                Map<String, Object> model = new HashMap<>();

                model.put("DATA", sum);
                model.put("SYSTEMSTART", startTime);
                model.put("RUNNING_TIME", summerVacationDuration.getSeconds());

                return new ModelAndView(model, "index");

            }
        }, templateEngine);

        ResourceBundle rb = ResourceBundle.getBundle("default");
        createFileListByProp(rb);

        System.out.println(rb.toString());

        List<File> piFileList = createFileListByProp(rb);
        String path = rb.getString("outputPath");
        Integer maxLength = Integer.valueOf(rb.getString("maxLength"));
        Integer listSize = Integer.valueOf(rb.getString("listSize"));
        Integer unitLength = Integer.valueOf(rb.getString("unitLength"));
        Integer reportSpan = Integer.valueOf(rb.getString("reportSpan"));

        //YCDフィアルの全体像をつかむ
        //全ファイルヘッダー情報取得 (targetLengthはこの処理では重要でないので、適当な値を入れている)
        Map<File, Map<YCD_SeqProvider.FileInfo, String>> ycdFileMap = YCD_SeqProvider.createFileInfo(piFileList, 1);
        Integer fileCont = ycdFileMap.size();
        Long total = 0L;
        for (File f : ycdFileMap.keySet()) {
            total = Long.valueOf(ycdFileMap.get(f).get(YCD_SeqProvider.FileInfo.END_DIGIT));
        }
        System.out.println("FILE COUNT:" + fileCont + "  MAX DEPTH: " + total);

        Searcher s = new Searcher(piFileList,path,7,100000,1900,500);
        s.start();

    }

    public static List<File> createFileListByProp(ResourceBundle rb) {
        List<File> fileList = new ArrayList<>();

        for(int i = 0 ; i < 9999; i++ ){
            String noStr = String.format("%03d", i);

            try{
                String s = rb.getString("ycd" + noStr);
                File f = new File(s);
                if(!f.exists()){
                    throw new RuntimeException("piFile is not found: " + s);
                }

                fileList.add(new File(s));

            }catch(MissingResourceException e){
                if(i == 0){
                    throw new RuntimeException("piFile is not define by property file: " + noStr);
                }else{
                    break;
                }
            }
        }

        return fileList;
    }

}
