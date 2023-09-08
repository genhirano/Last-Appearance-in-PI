package controller;

import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Web {

    private static Long ycdMaxDepth = 0l;

    //ThymeleafTemplateEngine
    private final static TemplateEngine templateEngine = new ThymeleafTemplateEngine();


    public static void init(){

        //デバッグ画面を有効にする
        enableDebugScreen();

        ZonedDateTime startTime = ZonedDateTime.now();

        //最大検索桁数取得
        //Long ycdMaxDepth = 0L;
        //for (File f : YCD_FILE_MAP.keySet()) {
        //    ycdMaxDepth = Long.valueOf(YCD_FILE_MAP.get(f).get(YCDFileUtil.FileInfo.END_DIGIT));
        //}



        //WEB-------------------------------------
        port(Env.getInstance().getPortNo());
        staticFiles.location("/public");
        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {

                //現在時刻取得
                ZonedDateTime nowTime = ZonedDateTime.now();
                Duration summerVacationDuration = Duration.between(startTime, nowTime);


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

    }

}
