package controller;

import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Web {

    private static Long ycdMaxDepth = 0l;

    //ThymeleafTemplateEngine
    private static TemplateEngine templateEngine;

    //TymeLeafリゾルバの設定
    private static final String DEFAULT_PREFIX = "templates/";
    private static final String DEFAULT_SUFFIX = ".html";
    private static final long DEFAULT_CACHE_TTL_MS = 3600000L;
    private static final String CHARACTER_ENCODING = "UTF-8";
    

    public static void init(){
        ZonedDateTime startTime = ZonedDateTime.now();
        
        //デバッグ画面を有効にする
        enableDebugScreen();

        //Tymeleafリゾルバの設定
        ClassLoaderTemplateResolver  resolver = new ClassLoaderTemplateResolver ();
        resolver.setPrefix(DEFAULT_PREFIX);
        resolver.setSuffix(DEFAULT_SUFFIX);
        resolver.setTemplateMode("HTML5");
        resolver.setCacheTTLMs(DEFAULT_CACHE_TTL_MS);
        resolver.setCharacterEncoding(CHARACTER_ENCODING);

        //HTMLテンプレートエンジン作成(ThymeLeaf)
        templateEngine = new ThymeleafTemplateEngine(resolver);


        ycdMaxDepth = StoreController.getInstance().getPidataMaxDepth();;


        //WEB-------------------------------------
        port(Env.getInstance().getPortNo());
        staticFiles.location("/public/");
        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {

                //現在時刻取得
                ZonedDateTime nowTime = ZonedDateTime.now();
                Duration summerVacationDuration = Duration.between(startTime, nowTime);

                //保存ファイルコントローラーからサマリーを取得
                StoreController sc = StoreController.getInstance();
                List<String> sum = sc.getSummary();
                Collections.reverse(sum);

               

                //ビューに渡すデータ作成
                Map<String, Object> model = new HashMap<>();
                model.put("DATA", sum);
                model.put("YCD_MAX_DEPTH", ycdMaxDepth);
                model.put("SYSTEMSTART", startTime);
                model.put("RUNNING_TIME", summerVacationDuration.getSeconds());

                ModelAndView modelAndView = new ModelAndView(model, "index");

                //htmlを生成してファイルに保存
                String html = templateEngine.render(modelAndView);
                FileUtils.writeStringToFile(new File("index.html"), html,CHARACTER_ENCODING);

                return modelAndView;

            }
        }, templateEngine);

    }



}
