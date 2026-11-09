package controller;

import spark.*;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import model.ProgressReportBean;
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
                try {
                    return buildPage(startTime);
                } catch (Exception e) {
                    // ルート内の例外はログに出さないと原因が追えない（SLF4J は NOP）ため、ここで出力してから再スロー
                    System.out.println();
                    System.out.println("Web page rendering error:");
                    e.printStackTrace();
                    throw e;
                }
            }
        }, templateEngine);

    }

    /**
     * 進捗画面のモデルを組み立て、静的HTMLも保存する.
     *
     * 進行中のサバイバル情報が無い瞬間（起動直後・桁の切り替わり直後）でも描画できるように、null は既定値に置き換える。
     *
     * @param startTime システム開始時刻
     * @return モデルとビュー
     */
    private static ModelAndView buildPage(ZonedDateTime startTime) throws Exception {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // ビューに渡すデータ作成
        ProgressReportBean pr = StoreController.getProgressReport();
        Map<String, Object> model = new HashMap<>();

        // 進行中のサバイバル情報があるか
        final boolean inProgress = (null != pr.getCurrentTargetLength() && null != pr.getInitSurvivalInfo());

        List<Map<String, String>> tmpData = pr.getResult();
        for (Map<String, String> map : tmpData) {
            map.put("Depth", String.format("%,d", Long.valueOf(map.get("Depth"))));
        }
        model.put("DATA", tmpData);// 検索終了

        model.put("YCD_MAX_DEPTH", String.format("%,d", pr.getAllPiDataLength()));
        model.put("SYSTEMSTART", startTime);
        model.put("SEARCH_STOPPED", Searcher.getStoppedReason());

        final long elapsedSec = (null != pr.getCurenntElapsedTimeInSeconds()) ? pr.getCurenntElapsedTimeInSeconds() : 0L;
        model.put("RUNNING_TIME", elapsedSec);

        final int currentLength = inProgress ? pr.getCurrentTargetLength() : 0;
        model.put("CURRENT_DIGITS", currentLength);
        model.put("CURRENT_DIGITS_MIN", StringUtils.repeat("0", currentLength));
        model.put("CURRENT_DIGITS_MAX", StringUtils.repeat("9", currentLength));

        final long discoveredCount = (null != pr.getCurrentDiscoveredCount()) ? pr.getCurrentDiscoveredCount() : 0L;
        model.put("CURRENT_DISCOVERD_COUNT", String.format("%,d", discoveredCount));
        model.put("CURRENT_ELAPSED_TIME", String.format("%,d", elapsedSec));

        model.put("CURRENT_DEEPEST_FIND", (null != pr.getCurrentDeepestFind()) ? pr.getCurrentDeepestFind() : "");
        model.put("CURRENT_DEEPEST_FIND_POSITION",
                (null != pr.getCurrentDeepestFindPosition()) ? String.format("%,d", pr.getCurrentDeepestFindPosition()) : "");

        model.put("SERVER_TIME", pr.getServerTime().format(formatter));

        // 対象桁の全個数（10^N）。10桁以上は int に収まらないため long
        final long allCount = inProgress ? StoreController.pow10(currentLength) : 0L;

        model.put("CURRENT_UNDISCOVERD_COUNT", String.format("%,d", Math.max(0L, allCount - discoveredCount)));
        double progress = 0.0;
        if (0L < allCount) {
            double d = (discoveredCount / (double) allCount) * 100;
            progress = ((double) Math.round(d * 100000)) / 100000;
        }
        model.put("CURRENT_PROGRESS_RATE", progress);

        double speed = (0L < elapsedSec) ? (double) discoveredCount / (double) elapsedSec : 0.0;
        model.put("CURRENT_SPEED", ((double) Math.round(speed * 1000)) / 1000);

        // 発見位置（昇順）から出現分布を作る。まだ発見が無い場合は空の分布
        long[] discoverdInfoList = (null != pr.getDiscoverdPosList()) ? pr.getDiscoverdPosList() : new long[0];

        LinkedHashMap<Long, Long> discoverdPosMap = distribution(discoverdInfoList);

        LinkedHashMap<String, String> formatedDiscoverdPosMap = new LinkedHashMap<>();

        Long from = 0L;
        for (Long key : discoverdPosMap.keySet()) {
            String fromKey = String.format("%,d", from + 1);
            String toKey = String.format("%,d", key);
            formatedDiscoverdPosMap.put((fromKey + " - " + toKey), String.format("%,d", discoverdPosMap.get(key)));
            from = key;
        }

        model.put("SURVIVAL_DISCOVERD_POS_MAP", formatedDiscoverdPosMap);

        // サバイバルリストの初期サイズ（10桁以上は int に収まらないため long）
        long survivalListInitialSize = 0L;
        if (inProgress) {
            survivalListInitialSize = 1L + Long.parseLong(pr.getInitSurvivalInfo().getEnd())
                    - Long.parseLong(pr.getInitSurvivalInfo().getStart());
        }
        model.put("CURRENT_SURVIVAL_INITIAL_COUNT", String.format("%,d", survivalListInitialSize));
        // サバイバルリストの現在発見数
        final long survivalListDiscoverdCount = (null != pr.getCurenntSurvivalDiscoveredCount())
                ? pr.getCurenntSurvivalDiscoveredCount() : 0L;
        model.put("CURRENT_SURVIVAL_DISCOVERD_COUNT", String.format("%,d", survivalListDiscoverdCount));

        // サバイバルリストの進捗率
        double survivalProcessRate = (0L < survivalListInitialSize)
                ? (double) survivalListDiscoverdCount / (double) survivalListInitialSize : 0.0;
        model.put("CURRENT_SURVIVAL_DISCOVERD_RATE", ((double) Math.round(survivalProcessRate * 1000000)) / 1000000 * 100);

        // カレントサバイバルリスト情報
        model.put("CURRENT_SURVIVAL_START", inProgress ? pr.getInitSurvivalInfo().getStart() : "");
        model.put("CURRENT_SURVIVAL_END", inProgress ? pr.getInitSurvivalInfo().getEnd() : "");

        String[] datetime = {"", ""};
        if (null != pr.getCurenntSurvivalStartTime()) {
            datetime = pr.getCurenntSurvivalStartTime().format(formatter).split(" ");
        }
        model.put("CURRENT_SURVIVAL_START_DATE", datetime[0]);
        model.put("CURRENT_SURVIVAL_START_TIME", datetime[1]);

        model.put("CURRENT_SURVIVAL_ELAPSED_TIME",
                (null != pr.getCurrentSurvivalElapsedSeconds()) ? pr.getCurrentSurvivalElapsedSeconds() : 0L);

        model.put("CURRENT_SURVIVAL_DEPTH",
                (null != pr.getCurenntSurvivalDepth()) ? String.format("%,d", pr.getCurenntSurvivalDepth()) : "");

        // YCDファイル情報
        List<Map<String, String>> fileInfo = new ArrayList<>();
        Map<File, Map<YCDFileUtil.FileInfo, String>> allFileInfo =
                (null != pr.getAllFileInfo()) ? pr.getAllFileInfo() : Collections.emptyMap();
        for (File f : allFileInfo.keySet()) {
            Map<YCDFileUtil.FileInfo, String> info = allFileInfo.get(f);
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

        return modelAndView;
    }

    static LinkedHashMap<Long, Long> distribution(long[] positions) {
        LinkedHashMap<Long, Long> counts = new LinkedHashMap<>();
        long last = positions.length == 0 ? 0L : positions[positions.length - 1];
        long width = Math.max(1L, last / 10 + (last % 10 == 0 ? 0 : 1));
        long[] bounds = new long[10];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = width * (i + 1);
            counts.put(bounds[i], 0L);
        }
        int bucket = 0;
        for (long position : positions) {
            while (bucket < 9 && bounds[bucket] < position) {
                bucket++;
            }
            counts.put(bounds[bucket], counts.get(bounds[bucket]) + 1);
        }
        return counts;
    }
}
