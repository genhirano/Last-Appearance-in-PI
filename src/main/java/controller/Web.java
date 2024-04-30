package controller;

import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import model.ProgressReportBean;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Web {

    // ThymeleafTemplateEngine
    private static TemplateEngine templateEngine;

    // TymeLeafリゾルバの設定
    private static final String DEFAULT_PREFIX = "templates/";
    private static final String DEFAULT_SUFFIX = ".html";
    private static final long DEFAULT_CACHE_TTL_MS = 3600000L;
    private static final String CHARACTER_ENCODING = "UTF-8";

    public static void init() {
        ZonedDateTime startTime = ZonedDateTime.now();

        // デバッグ画面を有効にする
        enableDebugScreen();

        // Tymeleafリゾルバの設定
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(DEFAULT_PREFIX);
        resolver.setSuffix(DEFAULT_SUFFIX);
        resolver.setTemplateMode("HTML5");
        resolver.setCacheTTLMs(DEFAULT_CACHE_TTL_MS);
        resolver.setCharacterEncoding(CHARACTER_ENCODING);

        // HTMLテンプレートエンジン作成(ThymeLeaf)
        templateEngine = new ThymeleafTemplateEngine(resolver);

        // WEB-------------------------------------
        port(Env.getInstance().getPortNo());
        staticFiles.location("/public/");
        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {

                // ビューに渡すデータ作成
                ProgressReportBean pr = StoreController.getProgressReport();
                Map<String, Object> model = new HashMap<>();
                //Collections.reverse(pr.getResult());
                
                
                model.put("DATA", pr.getResult());//検索終了
                


                
                
                model.put("YCD_MAX_DEPTH", String.format("%,d", pr.getAllPiDataLength()));
                model.put("SYSTEMSTART", startTime);
                model.put("RUNNING_TIME", pr.getCurenntElapsedTimeInSeconds());

                model.put("CURRENT_DIGITS", pr.getCurrentTargetLength());
                model.put("CURRENT_DISCOVERD_COUNT", pr.getCurrentDiscoveredCount());
                model.put("CURRENT_UNDISCOVERD_COUNT", pr.getCurrentUndiscoveredCount());
                model.put("CURRENT_ELAPSED_TIME", String.format("%,d", pr.getCurenntElapsedTimeInSeconds()));
                model.put("CURRENT_DEEPEST_FIND_POSITION", String.format("%,d", pr.getCurrentDeepestFindPosition()));
                model.put("CURRENT_DEEPEST_FIND", pr.getCurrentDeepestFind());
                

                model.put("SERVER_TIME", pr.getServerTime());


                Integer allMax = -1;
                if(null != pr.getCurrentTargetLength()){
                    allMax = Integer.valueOf(StringUtils.repeat("9", pr.getCurrentTargetLength()));
                }
                

                double progress = 0.0;
                if(null != pr.getCurrentDiscoveredCount()){
                    double d = (pr.getCurrentDiscoveredCount() / (double)allMax) * 100;
                    progress = ((double) Math.round(d * 100000)) / 100000;
                }
                model.put("CURRENT_PROGRESS_RATE", progress);


                ModelAndView modelAndView = new ModelAndView(model, "index");

                // htmlを生成してファイルに保存
                String html = templateEngine.render(modelAndView);
                FileUtils.writeStringToFile(new File("index.html"), html, CHARACTER_ENCODING);

                return modelAndView;

            }
        }, templateEngine);

    }

}
