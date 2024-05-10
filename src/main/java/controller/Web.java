package controller;

import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import model.ProgressReportBean;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;
import model.ycd.YCDFileUtil.FileInfo;

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

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                // ビューに渡すデータ作成
                ProgressReportBean pr = StoreController.getProgressReport();
                Map<String, Object> model = new HashMap<>();

                model.put("DATA", pr.getResult());// 検索終了

                model.put("YCD_MAX_DEPTH", String.format("%,d", pr.getAllPiDataLength()));
                model.put("SYSTEMSTART", startTime);
                model.put("RUNNING_TIME", pr.getCurenntElapsedTimeInSeconds());

                model.put("CURRENT_DIGITS", pr.getCurrentTargetLength());

                model.put("CURRENT_DIGITS_MIN", StringUtils.repeat("0", pr.getCurrentTargetLength()));
                model.put("CURRENT_DIGITS_MAX", StringUtils.repeat("9", pr.getCurrentTargetLength()));

                model.put("CURRENT_UNDISCOVERD_COUNT", pr.getCurrentUndiscoveredCount());
                model.put("CURRENT_ELAPSED_TIME", String.format("%,d", pr.getCurenntElapsedTimeInSeconds()));

                model.put("CURRENT_DEEPEST_FIND", pr.getCurrentDeepestFind());
                model.put("CURRENT_DEEPEST_FIND_POSITION", String.format("%,d", pr.getCurrentDeepestFindPosition()));

                model.put("SERVER_TIME", pr.getServerTime().format(formatter));

                Integer allMax = -1;
                if (null != pr.getCurrentTargetLength()) {
                    allMax = Integer.valueOf(StringUtils.repeat("9", pr.getCurrentTargetLength()));
                }

                model.put("CURRENT_DISCOVERD_COUNT", pr.getCurrentDiscoveredCount());
                model.put("CURRENT_UNDISCOVERD_COUNT", (allMax + 1) - pr.getCurrentDiscoveredCount());
                double progress = 0.0;
                if (null != pr.getCurrentDiscoveredCount()) {
                    double d = (pr.getCurrentDiscoveredCount() / (double) allMax) * 100;
                    progress = ((double) Math.round(d * 100000)) / 100000;
                }
                model.put("CURRENT_PROGRESS_RATE", progress);

                Double speed = (double) pr.getCurrentDiscoveredCount() / (double) pr.getCurenntElapsedTimeInSeconds();

                model.put("CURRENT_SPEED", ((double) Math.round(speed * 1000)) / 1000);

                ArrayList<Long> discoverdInfoList = pr.getDiscoverdPosList();

                LinkedHashMap<Long, Long> discoverdPosMap = new LinkedHashMap<>();
                Long lastDiscoverPos = discoverdInfoList.get(discoverdInfoList.size() - 1);
                
                // 10分割
                Long splitPos = lastDiscoverPos / 10;
                for (Integer i = 1; i <= 10; i++) {
                    discoverdPosMap.put(splitPos * i, 0L);
                }
                discoverdPosMap.put((splitPos * 10 + splitPos), 0L);

                for (Long di : discoverdInfoList) {

                    for (Long dif : discoverdPosMap.keySet()) {
                        if (di <= dif) {
                            discoverdPosMap.put(dif, discoverdPosMap.get(dif) + 1);
                            break;
                        }
                    }

                }

                LinkedHashMap<String, String> formatedDiscoverdPosMap = new LinkedHashMap<>();

                Long from = 0L;
                for (Long key : discoverdPosMap.keySet()) {
                    String fromKey = String.format("%,d", from); 
                    String toKey = String.format("%,d", key);
                    
                    formatedDiscoverdPosMap.put((fromKey + " - " + toKey), String.format("%,d", discoverdPosMap.get(key)));
                    from = key;
                }

                model.put("SURVIVAL_DISCOVERD_POS_MAP", formatedDiscoverdPosMap);

                // サバイバルリストの初期サイズ
                Integer SurvivalListInitialSize = 1 + Integer.valueOf(pr.getInitSurvivalInfo().getEnd())
                        - Integer.valueOf(pr.getInitSurvivalInfo().getStart());
                model.put("CURRENT_SURVIVAL_INITIAL_COUNT", SurvivalListInitialSize);

                // サバイバルリストの現在発見数
                Integer SurvivalListDiscoverdCount = pr.getCurenntSurvivalDiscoveredCount();
                model.put("CURRENT_SURVIVAL_DISCOVERD_COUNT", SurvivalListDiscoverdCount);

                // サバイバルリストの進捗率
                Double survivalProcessRate = (double) SurvivalListDiscoverdCount / (double) SurvivalListInitialSize
                        * 100d;
                model.put("CURRENT_SURVIVAL_DISCOVERD_RATE", ((double) Math.round(survivalProcessRate * 100)) / 100);

                // カレントサバイバルリスト情報
                model.put("CURRENT_SURVIVAL_START", pr.getInitSurvivalInfo().getStart());
                model.put("CURRENT_SURVIVAL_END", pr.getInitSurvivalInfo().getEnd());

                String formattedString = pr.getCurenntSurvivalStartTime().format(formatter);
                String[] datetime = formattedString.split(" ");

                model.put("CURRENT_SURVIVAL_START_DATE", datetime[0]);
                model.put("CURRENT_SURVIVAL_START_TIME", datetime[1]);

                model.put("CURRENT_SURVIVAL_ELAPSED_TIME", pr.getCurrentSurvivalElapsedSeconds());

                model.put("CURRENT_SURVIVAL_DEPTH", String.format("%,d", pr.getCurenntSurvivalDepth()));

                // YCDファイル情報
                List<Map<String, String>> fileInfo = new ArrayList<>();
                for (File f : pr.getAllFileInfo().keySet()) {
                    Map<YCDFileUtil.FileInfo, String> info = pr.getAllFileInfo().get(f);
                    Map<String, String> map = new HashMap<>();

                    // カンマ
                    for (YCDFileUtil.FileInfo key : info.keySet()) {
                        String value = info.get(key);
                        if (value.matches("^[1-9]\\d*$")) {
                            value = String.format("%,d", Long.valueOf(value));
                        }
                        map.put(key.toString(), value);
                    }

                    // ファイルサイズのByte→GB変換
                    Long fileSize = Long.valueOf(info.get(FileInfo.FILE_SIZE));
                    String unitString = "";
                    double printFileSize = -1;
                    if (1024 * 1024 * 1024 > fileSize) {
                        printFileSize = fileSize / 1024d / 1024d;
                        unitString = "MB";
                    } else {
                        printFileSize = fileSize / 1024d / 1024d / 1024d;
                        unitString = "GB";
                    }

                    map.put("PRINT_FILE_SIZE", String.format("%.3f", printFileSize) + unitString);

                    fileInfo.add(map);
                }

                model.put("YCD_FILES_INFO", fileInfo);

                ModelAndView modelAndView = new ModelAndView(model, "index");

                // htmlを生成してファイルに保存
                String html = templateEngine.render(modelAndView);
                FileUtils.writeStringToFile(new File("index.html"), html, CHARACTER_ENCODING);

                String now = ZonedDateTime.now().format(formatter);

                System.out.println(now + " :  htmlを生成してファイルに保存しました。");

                return modelAndView;

            }
        }, templateEngine);

    }

}
