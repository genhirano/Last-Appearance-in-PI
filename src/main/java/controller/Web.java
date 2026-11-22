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
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import model.ProgressReportBean;
import model.pi.SurvivalList;
import model.pi.SurvivalList.DiscoverdInfo;
import model.ycd.YCDFileUtil;
import model.ycd.YCDFileUtil.FileInfo;

import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class Web {
    private static final String STOP_TOKEN = java.util.UUID.randomUUID().toString();

    private static volatile Searcher searcher;
    public static void setSearcher(Searcher value) { searcher = value; }

    public static boolean isLoopbackAddress(String addr) {
        return "127.0.0.1".equals(addr) || "::1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr);
    }

    public static boolean isLoopbackRequest(String remoteAddr, String forwardedFor) {
        if (!isLoopbackAddress(remoteAddr)) {
            return false;
        }
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            for (String part : forwardedFor.split(",")) {
                if (!isLoopbackAddress(part.trim())) {
                    return false;
                }
            }
        }
        return true;
    }

    // ThymeleafTemplateEngine
    private static TemplateEngine templateEngine;

    // Thymeleafリゾルバの設定
    private static final String DEFAULT_PREFIX = "templates/";
    private static final String DEFAULT_SUFFIX = ".html";
    private static final long DEFAULT_CACHE_TTL_MS = 3600000L;
    private static final String CHARACTER_ENCODING = "UTF-8";

    public static void init() {
        ZonedDateTime startTime = ZonedDateTime.now();

        // デバッグ画面を有効にする
        enableDebugScreen();

        // Thymeleafリゾルバの設定
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(DEFAULT_PREFIX);
        resolver.setSuffix(DEFAULT_SUFFIX);
        resolver.setTemplateMode("HTML5");
        resolver.setCacheTTLMs(DEFAULT_CACHE_TTL_MS);
        resolver.setCharacterEncoding(CHARACTER_ENCODING);

        // HTMLテンプレートエンジン作成(Thymeleaf)
        templateEngine = new ThymeleafTemplateEngine(resolver);

        // WEB-------------------------------------
        port(Env.getInstance().getPortNo());
        staticFiles.location("/public/");
        post("/stop", (req, resp) -> {
            String remote = req.raw().getRemoteAddr();
            if (!isLoopbackRequest(remote, req.headers("X-Forwarded-For"))) {
                resp.status(403);
                return "Forbidden";
            }
            String token = req.queryParams("token");
            if (token == null || !java.security.MessageDigest.isEqual(
                    STOP_TOKEN.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                resp.status(403);
                return "Forbidden";
            }
            Searcher current = searcher;
            if (current != null && current.isAlive()) current.requestStop();
            resp.redirect("/");
            return "";
        });
        get("/api/status", (req, resp) -> {
            try {
                resp.type("application/json; charset=UTF-8");
                resp.header("Cache-Control", "no-store, no-cache, must-revalidate");
                Map<String, Object> model = buildTelemetryModel(startTime, false);
                return toJson(model);
            } catch (Exception e) {
                System.out.println();
                System.out.println("Web API status error:");
                e.printStackTrace();
                throw e;
            }
        });
        get("/", new TemplateViewRoute() {
            @Override
            public ModelAndView handle(Request req, Response resp) throws Exception {
                try {
                    boolean loopback = isLoopbackRequest(req.raw().getRemoteAddr(), req.headers("X-Forwarded-For"));
                    return buildPage(startTime, loopback);
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
    private static ModelAndView buildPage(ZonedDateTime startTime, boolean includeStopForm) throws Exception {
        Map<String, Object> model = buildTelemetryModel(startTime, includeStopForm);
        ModelAndView modelAndView = new ModelAndView(model, "index");

        // htmlを生成してファイルに保存（CSRFトークンや停止フォームは静的HTMLへ出力しない）
        Map<String, Object> exportModel = new HashMap<>(model);
        exportModel.remove("STOP_TOKEN");
        String html = templateEngine.render(new ModelAndView(exportModel, "index"));
        synchronized (Web.class) {
            try {
                FileUtils.writeStringToFile(new File("index.html"), html, CHARACTER_ENCODING);
            } catch (Exception e) {
                // Windowsでの一時的なファイルロック競合でHTTP 500にしない
                System.out.println("Warning: could not write static index.html: " + e.getMessage());
            }
        }

        return modelAndView;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildTelemetryModel(ZonedDateTime startTime, boolean includeStopForm) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // ビューに渡すデータ作成
        ProgressReportBean pr = StoreController.getProgressReport();
        Map<String, Object> model = new HashMap<>();

        // 進行中のサバイバル情報があるか
        final boolean inProgress = (null != pr.getCurrentTargetLength() && null != pr.getInitSurvivalInfo());
        final long allPiDigits = (null != pr.getAllPiDataLength()) ? pr.getAllPiDataLength() : 0L;

        // 完了済みの各桁データと対数スケール・グラフ用情報の構築
        List<Map<String, String>> tmpData = pr.getResult();
        long maxCompletedDepth = 1L;
        for (Map<String, String> map : tmpData) {
            long rawDepth = parseLongSafe(map.get("Depth"), 0L);
            if (rawDepth > maxCompletedDepth) {
                maxCompletedDepth = rawDepth;
            }
        }
        double maxLogDepth = Math.max(1.0, Math.log10(Math.max(10L, maxCompletedDepth)));
        Map<Integer, Map<String, String>> completedByDigit = new HashMap<>();
        for (Map<String, String> map : tmpData) {
            long rawDepth = parseLongSafe(map.get("Depth"), 0L);
            long rawSec = parseLongSafe(map.get("ProcessTimeSec"), 0L);
            int d = (int) parseLongSafe(map.get("digits"), 0L);
            map.put("RawDepth", String.valueOf(rawDepth));
            map.put("Depth", String.format("%,d", rawDepth));
            map.put("FormattedTime", formatDurationHms(rawSec));
            double logVal = (rawDepth > 0) ? Math.log10(rawDepth) : 0.0;
            double logPct = Math.min(100.0, Math.max(4.0, (logVal / maxLogDepth) * 100.0));
            double linearPct = (maxCompletedDepth > 0)
                    ? Math.min(100.0, Math.max(1.0, (rawDepth * 100.0) / maxCompletedDepth))
                    : 0.0;
            map.put("LogPct", String.format(Locale.US, "%.2f", logPct));
            map.put("LinearPct", String.format(Locale.US, "%.2f", linearPct));
            completedByDigit.put(d, map);
        }
        model.put("DATA", tmpData); // 検索終了

        model.put("YCD_MAX_DEPTH", String.format("%,d", allPiDigits));
        model.put("YCD_MAX_DEPTH_RAW", allPiDigits);
        model.put("SYSTEMSTART", startTime.format(formatter));
        String stoppedReason = Searcher.getStoppedReason();
        Object stoppingObj = StoreController.survivalProgressMap.get("SEARCH_STOPPING");
        model.put("SEARCH_STOPPED", stoppedReason);
        model.put("SEARCH_STOPPING", stoppingObj);
        if (includeStopForm) {
            model.put("STOP_TOKEN", STOP_TOKEN);
        }

        Object searchStageObj = StoreController.survivalProgressMap.get("SEARCH_STAGE");
        int searchStage = (searchStageObj instanceof Number) ? ((Number) searchStageObj).intValue() : 0;
        model.put("SEARCH_STAGE", searchStageObj);

        Object stage1BatchObj = StoreController.survivalProgressMap.get("STAGE1_BATCH");
        Object stage1BatchesObj = StoreController.survivalProgressMap.get("STAGE1_BATCHES");
        model.put("STAGE1_BATCH", stage1BatchObj);
        model.put("STAGE1_BATCHES", stage1BatchesObj);

        Object stage1DepthObj = StoreController.survivalProgressMap.get("STAGE1_DEPTH");
        long stage1DepthRaw = (stage1DepthObj instanceof Number) ? ((Number) stage1DepthObj).longValue() : 0L;
        model.put("STAGE1_DEPTH", (stage1DepthObj instanceof Number) ? String.format("%,d", stage1DepthRaw) : stage1DepthObj);
        model.put("STAGE1_DEPTH_RAW", stage1DepthRaw);

        Object stage1ProgressObj = StoreController.survivalProgressMap.get("STAGE1_PROGRESS");
        double stage1ProgressRaw = (stage1ProgressObj instanceof Number) ? ((Number) stage1ProgressObj).doubleValue() : 0.0;
        model.put("STAGE1_PROGRESS", (stage1ProgressObj instanceof Number)
                ? String.format(Locale.US, "%.2f", stage1ProgressRaw) : stage1ProgressObj);
        model.put("STAGE1_PROGRESS_PCT", clampPct(stage1ProgressRaw));

        Object workersObj = StoreController.survivalProgressMap.get("STAGE1_WORKERS");
        Object workerInitialsObj = StoreController.survivalProgressMap.get("STAGE1_WORKER_INITIALS");
        model.put("STAGE1_WORKERS", workersObj);

        Object stage1PositionObj = StoreController.survivalProgressMap.get("NOW_SURVIVAL_DEPTH");
        long currentScanPosRaw = (stage1PositionObj instanceof Number) ? ((Number) stage1PositionObj).longValue() : 0L;
        model.put("STAGE1_POSITION", (stage1PositionObj instanceof Number) ? String.format("%,d", currentScanPosRaw) : stage1PositionObj);
        model.put("STAGE1_POSITION_RAW", currentScanPosRaw);

        Object stage1SpeedObj = StoreController.survivalProgressMap.get("STAGE1_SPEED");
        double stage1SpeedRaw = (stage1SpeedObj instanceof Number) ? ((Number) stage1SpeedObj).doubleValue() : 0.0;
        model.put("STAGE1_SPEED", (stage1SpeedObj instanceof Number) ? String.format("%,.0f", stage1SpeedRaw) : stage1SpeedObj);
        model.put("STAGE1_SPEED_HUMAN", formatDigitsSpeed(stage1SpeedRaw));
        // 対数スケールのスピードメーター（10^3 〜 10^9 桁/秒 を 0〜100% にマッピング）
        double speedGaugePct = (stage1SpeedRaw <= 0.0) ? 0.0
                : clampPct(((Math.log10(Math.max(1.0, stage1SpeedRaw)) - 3.0) / 6.0) * 100.0);
        model.put("STAGE1_SPEED_GAUGE_PCT", String.format(Locale.US, "%.1f", speedGaugePct));

        // 探索フェーズ詳細
        Object phaseObj = StoreController.survivalProgressMap.get("STAGE1_PHASE");
        String phaseCode;
        String phaseTitle;
        String phaseSubtitle;
        if (stoppedReason != null) {
            phaseCode = "STOPPED";
            phaseTitle = "HALTED // 探索停止";
            phaseSubtitle = stoppedReason;
        } else if (stoppingObj != null) {
            phaseCode = "STOPPING";
            phaseTitle = "STOPPING // 停止処理中";
            phaseSubtitle = "安全にスレッドを停止しています...";
        } else if (searchStage == 2 || "STAGE2".equals(phaseObj)) {
            phaseCode = "STAGE2";
            phaseTitle = "STAGE 2 // 最終確定ディープスイープ";
            phaseSubtitle = "Stage 1 を生き残った少数候補を基準深さ以降から最終走査中";
        } else if ("BATCH".equals(phaseObj) || stage1BatchObj != null) {
            phaseCode = "BATCH";
            phaseTitle = "STAGE 1 // 並列バッチ高速ふるい落とし";
            phaseSubtitle = "基準深さまでの円周率を並列走査し、95%以上の候補を一気に消去中";
        } else if (searchStage == 1 || "PILOT".equals(phaseObj)) {
            phaseCode = "PILOT";
            phaseTitle = "STAGE 1 // パイロット基準深さ測定";
            phaseSubtitle = "先頭グループで生存率が閾値に達する基準深さ（カットオフ位置）を計測中";
        } else {
            phaseCode = "IDLE";
            phaseTitle = "STANDBY // 準備中";
            phaseSubtitle = "次の探索サイクルを初期化しています";
        }
        model.put("STAGE_PHASE_CODE", phaseCode);
        model.put("STAGE_PHASE_TITLE", phaseTitle);
        model.put("STAGE_PHASE_SUBTITLE", phaseSubtitle);

        // スキャン深さレーダー進捗率
        double scanTargetDepth = ("BATCH".equals(phaseCode) && stage1DepthRaw > 0)
                ? (double) stage1DepthRaw
                : (double) Math.max(1L, allPiDigits);
        double scanRadarPct = (scanTargetDepth > 0) ? clampPct((currentScanPosRaw / scanTargetDepth) * 100.0) : 0.0;
        double piUsagePct = (allPiDigits > 0) ? clampPct((currentScanPosRaw * 100.0) / allPiDigits) : 0.0;
        model.put("SCAN_RADAR_PCT", String.format(Locale.US, "%.2f", scanRadarPct));
        model.put("PI_SCAN_USAGE_PCT", String.format(Locale.US, "%.3f", piUsagePct));

        // ワーカースレッド別テレメトリ & バッチ分割マトリクス
        Object threadsCountObj = StoreController.survivalProgressMap.get("STAGE1_THREADS_COUNT");
        int stage1ThreadsCount = (threadsCountObj instanceof Number) ? ((Number) threadsCountObj).intValue() : 3;
        Object groupsCountObj = StoreController.survivalProgressMap.get("STAGE1_GROUPS_COUNT");
        int stage1GroupsCount = (groupsCountObj instanceof Number) ? ((Number) groupsCountObj).intValue() : 0;
        Object survivalRateObj = StoreController.survivalProgressMap.get("STAGE1_SURVIVAL_RATE");
        double stage1SurvivalRate = (survivalRateObj instanceof Number) ? ((Number) survivalRateObj).doubleValue() : 5.0;
        double stage1TargetElimRate = Math.max(0.0, 100.0 - stage1SurvivalRate);
        model.put("STAGE1_THREADS_COUNT", stage1ThreadsCount);
        model.put("STAGE1_GROUPS_COUNT", stage1GroupsCount);
        model.put("STAGE1_SURVIVAL_RATE", String.format(Locale.US, "%.1f", stage1SurvivalRate));
        model.put("STAGE1_TARGET_ELIM_RATE", String.format(Locale.US, "%.1f", stage1TargetElimRate));

        Object batchMatrixObj = StoreController.survivalProgressMap.get("STAGE1_BATCH_MATRIX");
        List<Map<String, Object>> batchMatrix = (batchMatrixObj instanceof List<?>)
                ? (List<Map<String, Object>>) batchMatrixObj : Collections.emptyList();
        model.put("STAGE1_BATCH_MATRIX", batchMatrix);

        Object workerDetailsObj = StoreController.survivalProgressMap.get("STAGE1_WORKER_DETAILS");
        List<Map<String, Object>> workerCards = new ArrayList<>();
        if (workerDetailsObj instanceof List<?>) {
            workerCards = (List<Map<String, Object>>) workerDetailsObj;
        } else if (workersObj instanceof List<?>) {
            List<?> remList = (List<?>) workersObj;
            List<?> initList = (workerInitialsObj instanceof List<?>) ? (List<?>) workerInitialsObj : Collections.emptyList();
            for (int i = 0; i < remList.size(); i++) {
                long rem = (remList.get(i) instanceof Number) ? ((Number) remList.get(i)).longValue() : 0L;
                long init = (i < initList.size() && initList.get(i) instanceof Number)
                        ? ((Number) initList.get(i)).longValue() : Math.max(rem, 1L);
                long elim = Math.max(0L, init - rem);
                double elimPct = (init > 0) ? clampPct((elim * 100.0) / init) : 0.0;
                double remPct = (init > 0) ? clampPct((rem * 100.0) / init) : 0.0;
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("index", i + 1);
                w.put("name", (searchStage == 2) ? "Stage 2 Final Sweep Thread" : ("Parallel Thread #" + (i + 1)));
                w.put("roleBadge", (searchStage == 2) ? "FINAL SWEEP" : "PARALLEL WORKER");
                w.put("stateClass", "is-hunting");
                w.put("rangeStart", "-");
                w.put("rangeEnd", "-");
                w.put("initial", String.format("%,d", init));
                w.put("remaining", String.format("%,d", rem));
                w.put("eliminated", String.format("%,d", elim));
                w.put("threshold", "0");
                w.put("eliminatedPct", String.format(Locale.US, "%.1f", elimPct));
                w.put("remainingPct", String.format(Locale.US, "%.1f", remPct));
                w.put("targetElimPct", String.format(Locale.US, "%.1f", stage1TargetElimRate));
                w.put("threshProgressPct", String.format(Locale.US, "%.1f", elimPct));
                w.put("thresholdReached", false);
                w.put("commonPrefix", "-");
                w.put("commonPrefixLen", 0);
                w.put("deepestTarget", "-");
                w.put("deepestPos", "-");
                w.put("scanPos", (stage1PositionObj instanceof Number) ? String.format("%,d", currentScanPosRaw) : "0");
                w.put("scanPct", "0.0");
                workerCards.add(w);
            }
        }
        model.put("WORKER_CARDS", workerCards);

        final long elapsedSec = (null != pr.getCurenntElapsedTimeInSeconds()) ? pr.getCurenntElapsedTimeInSeconds() : 0L;
        model.put("RUNNING_TIME", elapsedSec);

        final int currentLength = inProgress ? pr.getCurrentTargetLength() : 0;
        model.put("CURRENT_DIGITS", currentLength);
        model.put("CURRENT_DIGITS_MIN", StringUtils.repeat("0", currentLength));
        model.put("CURRENT_DIGITS_MAX", StringUtils.repeat("9", currentLength));

        Object pilotDiscoveredObj = StoreController.survivalProgressMap.get("STAGE1_PILOT_DISCOVERED");
        long pilotDiscovered = ("PILOT".equals(phaseCode) && pilotDiscoveredObj instanceof Number)
                ? Math.max(0L, ((Number) pilotDiscoveredObj).longValue()) : 0L;

        final long baseDiscoveredCount = (null != pr.getCurrentDiscoveredCount()) ? pr.getCurrentDiscoveredCount() : 0L;
        final long discoveredCount = baseDiscoveredCount + pilotDiscovered;
        model.put("CURRENT_DISCOVERD_COUNT", String.format("%,d", discoveredCount));
        model.put("CURRENT_ELAPSED_TIME", String.format("%,d", elapsedSec));
        model.put("CURRENT_ELAPSED_HMS", formatDurationHms(elapsedSec));

        // 現在のサイクルの SurvivalList からリアルタイム最深記録・共通プレフィックス・直近発見・生存サンプルを取得
        SurvivalList activeList = (SurvivalList) StoreController.survivalProgressMap.get("SURVIVAL_LIST");
        String cycleDeepestTarget = "";
        long cycleDeepestPos = -1L;
        String commonPrefix = "";
        List<String> survivorHeadSamples = Collections.emptyList();
        List<String> survivorTailSamples = Collections.emptyList();
        List<Map<String, String>> recentDiscoveries = new ArrayList<>();

        if (activeList != null) {
            cycleDeepestTarget = activeList.getDeepestTarget();
            cycleDeepestPos = activeList.getDeepestPos();
            commonPrefix = activeList.getCommonPrefix();
            List<String>[] samples = activeList.getSurvivorSamples(6, 6);
            survivorHeadSamples = samples[0];
            survivorTailSamples = samples[1];

            List<DiscoverdInfo> recentInfo = activeList.getDiscoverdInfo();
            long maxRecentPos = Math.max(1L, cycleDeepestPos);
            int limit = Math.min(12, recentInfo.size());
            for (int i = recentInfo.size() - 1; i >= recentInfo.size() - limit; i--) {
                DiscoverdInfo info = recentInfo.get(i);
                Map<String, String> item = new LinkedHashMap<>();
                long pos = (info.getFindPos() != null) ? info.getFindPos() : 0L;
                double barPct = clampPct((pos * 100.0) / maxRecentPos);
                item.put("target", info.getTarget());
                item.put("findPos", String.format("%,d", pos));
                item.put("time", (info.getFindDateTime() != null) ? info.getFindDateTime().format(timeOnlyFormatter) : "--:--:--");
                item.put("barPct", String.format(Locale.US, "%.1f", Math.max(3.0, barPct)));
                recentDiscoveries.add(item);
            }
        }

        // 保存済みサイクルの最深と現在サイクルの最深を比較して、全体の暫定チャンピオンを決定
        String savedDeepestFind = (null != pr.getCurrentDeepestFind()) ? pr.getCurrentDeepestFind() : "";
        Long savedDeepestPos = pr.getCurrentDeepestFindPosition();
        String effectiveDeepestFind = savedDeepestFind;
        Long effectiveDeepestPos = savedDeepestPos;
        String deepestSource = "SAVED";
        if ((effectiveDeepestPos == null || cycleDeepestPos > effectiveDeepestPos) && cycleDeepestPos >= 0) {
            effectiveDeepestFind = cycleDeepestTarget;
            effectiveDeepestPos = cycleDeepestPos;
            deepestSource = "LIVE_CYCLE";
        }

        model.put("CURRENT_DEEPEST_FIND", (effectiveDeepestFind != null) ? effectiveDeepestFind : "");
        model.put("CURRENT_DEEPEST_FIND_POSITION",
                (null != effectiveDeepestPos && effectiveDeepestPos >= 0) ? String.format("%,d", effectiveDeepestPos) : "");
        model.put("DEEPEST_SOURCE", deepestSource);
        model.put("CYCLE_DEEPEST_FIND", cycleDeepestTarget);
        model.put("CYCLE_DEEPEST_FIND_POSITION", (cycleDeepestPos >= 0) ? String.format("%,d", cycleDeepestPos) : "");
        double deepestVsPiPct = (allPiDigits > 0 && effectiveDeepestPos != null && effectiveDeepestPos > 0)
                ? clampPct((effectiveDeepestPos * 100.0) / allPiDigits) : 0.0;
        model.put("DEEPEST_VS_PI_PCT", String.format(Locale.US, "%.2f", deepestVsPiPct));

        model.put("CURRENT_COMMON_PREFIX", commonPrefix);
        model.put("CURRENT_COMMON_PREFIX_LEN", commonPrefix.length());
        double prefixPct = (currentLength > 0) ? clampPct((commonPrefix.length() * 100.0) / currentLength) : 0.0;
        model.put("CURRENT_COMMON_PREFIX_PCT", String.format(Locale.US, "%.1f", prefixPct));
        model.put("SURVIVOR_HEAD_SAMPLES", survivorHeadSamples);
        model.put("SURVIVOR_TAIL_SAMPLES", survivorTailSamples);
        model.put("RECENT_DISCOVERIES", recentDiscoveries);

        model.put("SERVER_TIME", pr.getServerTime().format(formatter));

        // 対象桁の全個数（10^N）。10桁以上は int に収まらないため long
        final long allCount = inProgress ? StoreController.pow10(currentLength) : 0L;
        final long undiscoveredCount = Math.max(0L, allCount - discoveredCount);
        model.put("CURRENT_ALL_COUNT", String.format("%,d", allCount));
        model.put("CURRENT_UNDISCOVERD_COUNT", String.format("%,d", undiscoveredCount));

        double progress = 0.0;
        if (0L < allCount) {
            double d = (discoveredCount / (double) allCount) * 100;
            progress = ((double) Math.round(d * 100000)) / 100000;
        }
        model.put("CURRENT_PROGRESS_RATE", progress);
        model.put("CURRENT_PROGRESS_RATE_FMT", String.format(Locale.US, "%.5f", progress));
        model.put("CURRENT_PROGRESS_PCT_STR", String.format(Locale.US, "%.2f", clampPct(progress)));

        double speed = (0L < elapsedSec) ? (double) discoveredCount / (double) elapsedSec : 0.0;
        double roundedSpeed = ((double) Math.round(speed * 1000)) / 1000;
        model.put("CURRENT_SPEED", roundedSpeed);
        if (speed > 0.0 && undiscoveredCount > 0L) {
            long etaSec = (long) Math.ceil(undiscoveredCount / speed);
            model.put("CURRENT_ETA_TEXT", formatDurationHms(etaSec));
        } else if (undiscoveredCount == 0L && allCount > 0L) {
            model.put("CURRENT_ETA_TEXT", "完了");
        } else {
            model.put("CURRENT_ETA_TEXT", "計測中...");
        }

        // 1〜18桁のロードマップ情報
        List<Map<String, Object>> digitRoadmap = new ArrayList<>();
        for (int d = 1; d <= SurvivalList.MAX_LENGTH; d++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("digits", d);
            Map<String, String> comp = completedByDigit.get(d);
            if (comp != null) {
                item.put("status", "completed");
                item.put("statusLabel", "DONE");
                item.put("appearing", comp.get("Appearing"));
                item.put("depth", comp.get("Depth"));
                item.put("progressPct", "100.0");
            } else if (d == currentLength && inProgress) {
                item.put("status", (stoppedReason != null) ? "stopped" : "active");
                item.put("statusLabel", (stoppedReason != null) ? "HALT" : "LIVE");
                item.put("appearing", (effectiveDeepestFind != null && !effectiveDeepestFind.isEmpty()) ? effectiveDeepestFind : "-");
                item.put("depth", (effectiveDeepestPos != null && effectiveDeepestPos >= 0) ? String.format("%,d", effectiveDeepestPos) : "-");
                item.put("progressPct", String.format(Locale.US, "%.2f", clampPct(progress)));
            } else {
                item.put("status", "pending");
                item.put("statusLabel", "WAIT");
                item.put("appearing", "-");
                item.put("depth", "-");
                item.put("progressPct", "0.0");
            }
            digitRoadmap.add(item);
        }
        model.put("DIGIT_ROADMAP", digitRoadmap);

        // 発見位置から出現分布を作る（巨大配列のコピー・ソートを避けて O(1) メモリで集計）
        LinkedHashMap<Long, Long> discoverdPosMap = (activeList != null)
                ? activeList.getDistributionSnapshot()
                : distribution((null != pr.getDiscoverdPosList()) ? pr.getDiscoverdPosList() : new long[0]);
        LinkedHashMap<String, String> formatedDiscoverdPosMap = new LinkedHashMap<>();

        long maxBucketCount = 1L;
        long totalDistCount = 0L;
        for (Long val : discoverdPosMap.values()) {
            if (val != null) {
                if (val > maxBucketCount) maxBucketCount = val;
                totalDistCount += val;
            }
        }

        List<Map<String, Object>> distributionBars = new ArrayList<>();
        Long from = 0L;
        int bucketIdx = 1;
        for (Long key : discoverdPosMap.keySet()) {
            long countVal = discoverdPosMap.get(key);
            String fromKey = String.format("%,d", from + 1);
            String toKey = String.format("%,d", key);
            String rangeKey = fromKey + " - " + toKey;
            String countStr = String.format("%,d", countVal);
            formatedDiscoverdPosMap.put(rangeKey, countStr);

            double barPct = (maxBucketCount > 0) ? clampPct((countVal * 100.0) / maxBucketCount) : 0.0;
            double sharePct = (totalDistCount > 0) ? clampPct((countVal * 100.0) / totalDistCount) : 0.0;

            Map<String, Object> bar = new LinkedHashMap<>();
            bar.put("bucket", bucketIdx);
            bar.put("label", (bucketIdx * 10) + "%");
            bar.put("range", rangeKey);
            bar.put("fromStr", fromKey);
            bar.put("toStr", toKey);
            bar.put("shortTo", formatCompactDigits(key));
            bar.put("count", countStr);
            bar.put("rawCount", countVal);
            bar.put("barPct", String.format(Locale.US, "%.1f", barPct));
            bar.put("sharePct", String.format(Locale.US, "%.1f", sharePct));
            distributionBars.add(bar);

            from = key;
            bucketIdx++;
        }
        model.put("SURVIVAL_DISCOVERD_POS_MAP", formatedDiscoverdPosMap);
        model.put("DISTRIBUTION_BARS", distributionBars);

        // サバイバルリストの初期サイズ（10桁以上は int に収まらないため long）
        long survivalListInitialSize = 0L;
        long survivalStartVal = 0L;
        long survivalEndVal = 0L;
        if (inProgress) {
            survivalStartVal = parseLongSafe(pr.getInitSurvivalInfo().getStart(), 0L);
            survivalEndVal = parseLongSafe(pr.getInitSurvivalInfo().getEnd(), 0L);
            survivalListInitialSize = 1L + survivalEndVal - survivalStartVal;
        }
        model.put("CURRENT_SURVIVAL_INITIAL_COUNT", String.format("%,d", survivalListInitialSize));

        // サバイバルリストの現在発見数と残存数
        final long baseSurvivalDiscovered = (null != pr.getCurenntSurvivalDiscoveredCount())
                ? pr.getCurenntSurvivalDiscoveredCount() : 0L;
        final long survivalListDiscoverdCount = Math.min(survivalListInitialSize, baseSurvivalDiscovered + pilotDiscovered);
        final long survivalListRemainingCount = Math.max(0L, survivalListInitialSize - survivalListDiscoverdCount);
        model.put("CURRENT_SURVIVAL_DISCOVERD_COUNT", String.format("%,d", survivalListDiscoverdCount));
        model.put("CURRENT_SURVIVAL_REMAINING_COUNT", String.format("%,d", survivalListRemainingCount));

        // サバイバルリストの進捗率
        double survivalProcessRate = (0L < survivalListInitialSize)
                ? (double) survivalListDiscoverdCount / (double) survivalListInitialSize : 0.0;
        double survivalDiscoveredPct = ((double) Math.round(survivalProcessRate * 1000000)) / 1000000 * 100;
        double survivalRemainingPct = Math.max(0.0, 100.0 - survivalDiscoveredPct);
        model.put("CURRENT_SURVIVAL_DISCOVERD_RATE", survivalDiscoveredPct);
        model.put("CURRENT_SURVIVAL_DISCOVERD_RATE_FMT", String.format(Locale.US, "%.4f", survivalDiscoveredPct));
        model.put("CURRENT_SURVIVAL_DISCOVERD_PCT_STR", String.format(Locale.US, "%.2f", clampPct(survivalDiscoveredPct)));
        model.put("CURRENT_SURVIVAL_REMAINING_PCT_STR", String.format(Locale.US, "%.2f", clampPct(survivalRemainingPct)));

        // 全候補空間 10^N の中での現在サイクルの位置
        double cycleRangeStartPct = (allCount > 0) ? clampPct((survivalStartVal * 100.0) / allCount) : 0.0;
        double cycleRangeWidthPct = (allCount > 0) ? clampPct((survivalListInitialSize * 100.0) / allCount) : 0.0;
        long cycleSize = Math.max(1L, survivalListInitialSize);
        long currentCycleNum = (survivalStartVal / cycleSize) + 1L;
        long totalCycles = (allCount > 0) ? ((allCount + cycleSize - 1L) / cycleSize) : 1L;
        model.put("CYCLE_RANGE_START_PCT", String.format(Locale.US, "%.2f", cycleRangeStartPct));
        model.put("CYCLE_RANGE_WIDTH_PCT", String.format(Locale.US, "%.2f", Math.max(1.5, cycleRangeWidthPct)));
        model.put("CYCLE_CURRENT_NUMBER", String.format("%,d", currentCycleNum));
        model.put("CYCLE_TOTAL_COUNT", String.format("%,d", totalCycles));

        // カレントサバイバルリスト情報
        model.put("CURRENT_SURVIVAL_START", inProgress ? pr.getInitSurvivalInfo().getStart() : "");
        model.put("CURRENT_SURVIVAL_END", inProgress ? pr.getInitSurvivalInfo().getEnd() : "");

        String[] datetime = {"", ""};
        if (null != pr.getCurenntSurvivalStartTime()) {
            datetime = pr.getCurenntSurvivalStartTime().format(formatter).split(" ");
        }
        model.put("CURRENT_SURVIVAL_START_DATE", datetime[0]);
        model.put("CURRENT_SURVIVAL_START_TIME", datetime[1]);

        long cycleElapsedSec = (null != pr.getCurrentSurvivalElapsedSeconds()) ? pr.getCurrentSurvivalElapsedSeconds() : 0L;
        model.put("CURRENT_SURVIVAL_ELAPSED_TIME", cycleElapsedSec);
        model.put("CURRENT_SURVIVAL_ELAPSED_HMS", formatDurationHms(cycleElapsedSec));

        model.put("CURRENT_SURVIVAL_DEPTH",
                (null != pr.getCurenntSurvivalDepth()) ? String.format("%,d", pr.getCurenntSurvivalDepth()) : "");

        // YCDファイル情報（現在の探索深さがどのファイルを走査中かも付与）
        List<Map<String, String>> fileInfo = new ArrayList<>();
        Map<File, Map<YCDFileUtil.FileInfo, String>> allFileInfo =
                (null != pr.getAllFileInfo()) ? pr.getAllFileInfo() : Collections.emptyMap();
        for (File f : allFileInfo.keySet()) {
            Map<YCDFileUtil.FileInfo, String> info = allFileInfo.get(f);
            Map<String, String> map = new HashMap<>();

            long firstDigitRaw = parseLongSafe(info.get(FileInfo.FIRST_DIGIT), 0L);
            long endDigitRaw = parseLongSafe(info.get(FileInfo.END_DIGIT), 0L);

            // カンマ
            for (YCDFileUtil.FileInfo key : info.keySet()) {
                String value = info.get(key);
                if (value != null && value.matches("^[1-9]\\d*$")) {
                    value = String.format("%,d", Long.valueOf(value));
                }
                map.put(key.toString(), value);
            }

            // ファイルサイズのByte→GB変換
            Long fileSize = parseLongSafe(info.get(FileInfo.FILE_SIZE), 0L);
            String unitString;
            double printFileSize;
            if (1024L * 1024L * 1024L > fileSize) {
                printFileSize = fileSize / 1024d / 1024d;
                unitString = "MB";
            } else {
                printFileSize = fileSize / 1024d / 1024d / 1024d;
                unitString = "GB";
            }
            map.put("PRINT_FILE_SIZE", String.format("%.3f", printFileSize) + unitString);

            String scanStatus;
            double fileScanPct;
            if (currentScanPosRaw <= 0 || currentScanPosRaw < firstDigitRaw) {
                scanStatus = "WAIT";
                fileScanPct = 0.0;
            } else if (currentScanPosRaw >= endDigitRaw && endDigitRaw > 0) {
                scanStatus = "DONE";
                fileScanPct = 100.0;
            } else {
                scanStatus = "ACTIVE";
                long span = Math.max(1L, endDigitRaw - firstDigitRaw + 1L);
                fileScanPct = clampPct(((currentScanPosRaw - firstDigitRaw + 1L) * 100.0) / span);
            }
            map.put("SCAN_STATUS", scanStatus);
            map.put("SCAN_PCT", String.format(Locale.US, "%.1f", fileScanPct));

            fileInfo.add(map);
        }

        model.put("YCD_FILES_INFO", fileInfo);
        model.put("YCD_FILES_COUNT", fileInfo.size());

        return model;
    }

    private static long parseLongSafe(String s, long fallback) {
        if (s == null || s.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double clampPct(double pct) {
        if (!Double.isFinite(pct) || pct < 0.0) {
            return 0.0;
        }
        return Math.min(100.0, pct);
    }

    private static String formatDurationHms(long totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private static String formatDigitsSpeed(double speed) {
        if (!Double.isFinite(speed) || speed <= 0.0) {
            return "0 桁/s";
        }
        if (speed >= 1_000_000_000.0) {
            return String.format(Locale.US, "%.2f G桁/s", speed / 1_000_000_000.0);
        }
        if (speed >= 1_000_000.0) {
            return String.format(Locale.US, "%.2f M桁/s", speed / 1_000_000.0);
        }
        if (speed >= 1_000.0) {
            return String.format(Locale.US, "%.1f K桁/s", speed / 1_000.0);
        }
        return String.format(Locale.US, "%.0f 桁/s", speed);
    }

    private static String formatCompactDigits(long v) {
        if (v >= 1_000_000_000_000L) {
            return String.format(Locale.US, "%.2fT", v / 1_000_000_000_000.0);
        }
        if (v >= 1_000_000_000L) {
            return String.format(Locale.US, "%.2fB", v / 1_000_000_000.0);
        }
        if (v >= 1_000_000L) {
            return String.format(Locale.US, "%.2fM", v / 1_000_000.0);
        }
        if (v >= 1_000L) {
            return String.format(Locale.US, "%.1fK", v / 1_000.0);
        }
        return String.valueOf(v);
    }

    static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        appendJson(sb, obj);
        return sb.toString();
    }

    private static void appendJson(StringBuilder sb, Object obj) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof Boolean || obj instanceof Number) {
            if (obj instanceof Double && (!Double.isFinite((Double) obj))) {
                sb.append("0");
            } else if (obj instanceof Float && (!Float.isFinite((Float) obj))) {
                sb.append("0");
            } else {
                sb.append(obj.toString());
            }
        } else if (obj instanceof Map<?, ?>) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (entry.getKey() == null) continue;
                if (!first) sb.append(',');
                first = false;
                appendJsonString(sb, entry.getKey().toString());
                sb.append(':');
                appendJson(sb, entry.getValue());
            }
            sb.append('}');
        } else if (obj instanceof Iterable<?>) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) obj) {
                if (!first) sb.append(',');
                first = false;
                appendJson(sb, item);
            }
            sb.append(']');
        } else {
            appendJsonString(sb, obj.toString());
        }
    }

    private static void appendJsonString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
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
