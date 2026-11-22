package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import model.ycd.YCDFileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Searcher extends Thread {
    private volatile boolean stopRequested;
    private volatile boolean stopped;

    public synchronized void requestStop() {
        if (stopped) {
            return;
        }
        stopRequested = true;
        StoreController.survivalProgressMap.put("SEARCH_STOPPING", true);
        interrupt();
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    @Getter
    private final List<File> um_piFileList;// 検索用ファイルリスト（不変にする）

    @Getter
    private final Integer listSize; // サバイバルリストの現在サイズ（残り）

    @Getter
    private final Integer unitLength;// 一回の読み込み長さ

    public Searcher(List<File> piFileList, Integer listSize, Integer unitLength)
            throws IOException {
        super();

        um_piFileList = Collections.unmodifiableList(piFileList); // 外部から参照されるための不変オブジェクト

        this.listSize = listSize;
        this.unitLength = unitLength;

        StoreController.setAllPiDataLength(YCDFileUtil.getAllDigitsCount(um_piFileList));

        StoreController.setAllFileInfo(YCDFileUtil.createFileInfo(piFileList, 0));

    }

    @Override
    public void run() {
        System.out.printf("[準備] 円周率ファイル %,d 件を読み込みました (総桁数: %,d 桁)%n",
                um_piFileList.size(), StoreController.getAllPiDataLength());

        // 検索処理のメインループ
        while (true) {
            if (stopRequested || isInterrupted()) {
                stop("Search interrupted; unfinished range will be retried.");
                return;
            }

            // １サイクルの開始時間
            ZonedDateTime startTime = ZonedDateTime.now();

            // 処理範囲の決定（次の処理を定義をStoreControllerから得る）
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(this.listSize);
            if (null == targetRange) {
                // 対応桁数まで全て終了。検索スレッドだけ終了し、WEB画面は残す
                stop("All target lengths are finished (up to " + SurvivalList.MAX_LENGTH + " digits).");
                return;
            }

            // 1サイクルのサバイバル実行
            SurvivalResult sr = survive(targetRange);
            if (null == sr) {
                // YCDデータが尽きて探しきれなかった、または中断された。検索スレッドだけ終了し、WEB画面は残す
                return;
            }

            // １サイクルの終了時間
            ZonedDateTime endTime = ZonedDateTime.now();

            // １サイクルの結果保存（停止要求との競合・保存中の割り込みによる部分書き込みや停止状態の未確定を防ぐ）
            synchronized (this) {
                if (stopRequested || isInterrupted()) {
                    stop("Search interrupted; unfinished range will be retried.");
                    return;
                }
                boolean interrupted = Thread.interrupted();
                try {
                    StoreController.getInstance().saveFile(targetRange.getLength(), targetRange.getStart(),
                            targetRange.getEnd(), sr.getTarget(), sr.getFindPos(), startTime, endTime);
                } catch (RuntimeException e) {
                    if (stopRequested || isInterrupted() || hasInterruptedCause(e)) {
                        stop("Search interrupted; unfinished range will be retried.");
                        return;
                    }
                    throw e;
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            long elapsedSeconds = Duration.between(startTime, endTime).getSeconds();
            System.out.printf("  -> [完了] %d 桁 (%s ～ %s) | 最遅出現: \"%s\" (位置: %,d 桁目) [所要時間: %,d秒]%n",
                    targetRange.getLength(), targetRange.getStart(), targetRange.getEnd(),
                    sr.getTarget(), sr.getFindPos(), elapsedSeconds);

            if (stopRequested || isInterrupted()) {
                stop("Search interrupted; unfinished range will be retried.");
                return;
            }

            // Httpで強制アクセス（静的HTMLを作成するため）
            StoreController.getInstance().saveHTML();

        }

    }

    private SurvivalResult survive(TargetRange targetRange) {

        SurvivalList survivalList = new SurvivalList(targetRange.getLength(), Long.parseLong(targetRange.getStart()),
                Long.parseLong(targetRange.getEnd()));
        System.out.printf("%n[探索開始] %d 桁 (%s ～ %s / 候補数: %,d)%n",
                targetRange.getLength(), targetRange.getStart(), targetRange.getEnd(), survivalList.size());
        synchronized (StoreController.survivalProgressMap) {
            StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_INFO", targetRange);
            StoreController.survivalProgressMap.put("SURVIVAL_LIST", survivalList);
            StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());
            StoreController.survivalProgressMap.put("SEARCH_STAGE", 1);
            StoreController.survivalProgressMap.remove("NOW_SURVIVAL_DEPTH");
            StoreController.survivalProgressMap.remove("STAGE1_DEPTH");
            StoreController.survivalProgressMap.remove("STAGE1_BATCH");
            StoreController.survivalProgressMap.remove("STAGE1_BATCHES");
            StoreController.survivalProgressMap.remove("STAGE1_PROGRESS");
            StoreController.survivalProgressMap.remove("STAGE1_WORKERS");
            StoreController.survivalProgressMap.remove("STAGE1_WORKER_INITIALS");
            StoreController.survivalProgressMap.remove("STAGE1_PHASE");
            StoreController.survivalProgressMap.remove("STAGE1_SPEED");
        }
        java.util.Properties settings = Env.getInstance().getProp();
        try {
            model.pi.StagedSearch search = new model.pi.StagedSearch(um_piFileList, unitLength,
                    Integer.parseInt(settings.getProperty("stage1Threads", "3")),
                    Integer.parseInt(settings.getProperty("stage1BatchCount", "10")),
                    Double.parseDouble(settings.getProperty("stage1SurvivalRate", "5")),
                    this::isStopRequested);
            SurvivalResult result = search.search(targetRange.getLength(), Long.parseLong(targetRange.getStart()),
                    Long.parseLong(targetRange.getEnd()), survivalList);
            if (result == null) {
                String sample = survivalList.isEmpty() ? "-" : survivalList.get(0);
                System.out.println("----------------------------------------------------------------------");
                System.out.println("[停止] 円周率データが不足したため探索を停止しました");
                System.out.printf("  - 対象: %d 桁 (%s ～ %s)%n",
                        targetRange.getLength(), targetRange.getStart(), targetRange.getEnd());
                System.out.printf("  - 理由: 全 %,d ファイル (%,d 桁) を走査し終えましたが、%n",
                        um_piFileList.size(), StoreController.getAllPiDataLength());
                System.out.printf("          未確定の候補が %,d 件残っています (未発見例: \"%s\")%n",
                        survivalList.size(), sample);
                System.out.println("  - メモ: この桁数を確定するには、より長い円周率データが必要です。");
                System.out.println("----------------------------------------------------------------------");
                stop("The file was too short to finalize all candidates.");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop("Search interrupted; unfinished range will be retried.");
            return null;
        } catch (Exception e) {
            if (stopRequested || isInterrupted() || hasInterruptedCause(e)) {
                stop("Search interrupted; unfinished range will be retried.");
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    private static boolean hasInterruptedCause(Throwable t) {
        while (t != null) {
            if (t instanceof InterruptedException
                    || t instanceof InterruptedIOException
                    || t instanceof ClosedByInterruptException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 検索を停止する（プロセスは終了せず、WEB画面は残す）.
     *
     * @param reason 停止理由
     */
    private void stop(String reason) {
        System.out.println();
        System.out.println("Search stopped: " + reason);
        synchronized (this) {
            stopped = true;
            StoreController.survivalProgressMap.put("SEARCH_STOPPED", reason);
            StoreController.survivalProgressMap.put("SEARCH_STOPPED_AT", ZonedDateTime.now());
            StoreController.survivalProgressMap.remove("SEARCH_STOPPING");
        }
        boolean interrupted = Thread.interrupted();
        try {
            StoreController.getInstance().saveHTML();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 検索が停止しているかどうか（停止理由）を返す.
     *
     * @return 停止していなければ null
     */
    public static String getStoppedReason() {
        Object reason = StoreController.survivalProgressMap.get("SEARCH_STOPPED");
        return (null == reason) ? null : reason.toString();
    }
}
