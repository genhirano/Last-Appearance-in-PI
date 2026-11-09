package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SearchThread;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Searcher extends Thread {

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

        // 検索処理のメインループ
        while (true) {

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
                // YCDデータが尽きて探しきれなかった。検索スレッドだけ終了し、WEB画面は残す
                return;
            }

            // １サイクルの終了時間
            ZonedDateTime endTime = ZonedDateTime.now();

            // １サイクルの結果保存
            StoreController.getInstance().saveFile(targetRange.getLength(), targetRange.getStart(),
                    targetRange.getEnd(), sr.getTarget(), sr.getFindPos(), startTime, endTime);

            // Httpで強制アクセス（静的HTMLを作成するため）
            StoreController.getInstance().saveHTML();

        }

    }

    private SurvivalResult survive(TargetRange targetRange) {

        // プロセス内新記録の記録
        SurvivalResult processDeepest = new SurvivalResult("", -1L);

        // サバイバルリスト作成
        SurvivalList survivalList = new SurvivalList(targetRange.getLength(), Long.parseLong(targetRange.getStart()),
                Long.parseLong(targetRange.getEnd()));

        // 進捗情報用サバイバルリスト初期情報の登録
        synchronized (StoreController.survivalProgressMap) {
            StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_INFO", targetRange);
            StoreController.survivalProgressMap.put("SURVIVAL_LIST", survivalList);
            StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());
            StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());
            StoreController.survivalProgressMap.remove("NOW_SURVIVAL_DEPTH");
        }

        // YCDデータプロバイダ（シーケンシャルにYCDデータをブロックで提供）を作成
        int overWrapLength = targetRange.getLength(); // ユニットの境目対応として一つ前のケツの部分を今回の先頭に重ねる桁の長さ
        try (YCD_SeqProvider p = new YCD_SeqProvider(5, this.um_piFileList, overWrapLength, this.unitLength);) {

            // YCDファイルの全ヘッダー情報を記録
            StoreController.survivalProgressMap.put("YCD_FILE_INFO", p.getFileInfoMap());

            // 検索スレッドオブジェクト
            SearchThread searchThread = null;

            // 検索処理の開始時間
            long baseTime = System.currentTimeMillis();

            // 直前に検索スレッドへ渡したユニットの末尾桁（検索深さの表示用）
            long lastUnitEndDigit = 0L;

            // YCDデータが尽きるまで（または全て発見するまで）繰り返す
            while (p.hasNext()) {

                // YCDから次の対象文字列を読み込む（前のユニットの検索と並行して読む）
                final YCD_SeqProvider.Unit currentPi = p.next();

                // ひとつ前の検索処理の終了を待ち合わせる
                if (searchThread != null) {
                    searchThread.join();

                    // 現在の検索深さを記録
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", lastUnitEndDigit);

                    // 新記録なら結果を更新
                    if (0 < searchThread.getResult().compareTo(processDeepest)) {
                        processDeepest.update(searchThread.getResult());
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        searchThread = null;
                        break; // read YCD UNIT break
                    }

                }

                // デバッグ出力は、１秒より短く表示しない
                if ((null != searchThread) && (baseTime + 1000 < System.currentTimeMillis())) {

                    DecimalFormat decimalFormat = new DecimalFormat("#,###");
                    final String anowReadDepth = decimalFormat
                            .format((Long.parseLong(currentPi.getFileInfo().get(YCDFileUtil.FileInfo.END_DIGIT))));
                    final String nowReadDepth = decimalFormat
                            .format((currentPi.getStartDigit() + currentPi.getData().length()));

                    String output = "\r"
                            + "Items:" + survivalList.size()
                            + " " + nowReadDepth
                            + " / " + anowReadDepth
                            + " - \"" + survivalList.get(0) + "\""
                            + "-\"" + survivalList.get(survivalList.size() - 1) + "\"";
                    System.out.print(output);
                    System.out.print(" ".repeat(Math.max(0, 85 - output.length())) + "|"); // ターミナルの幅に応じて調整
                    System.out.flush();

                    baseTime = System.currentTimeMillis();
                }

                // 検索スレッドの作成と開始
                lastUnitEndDigit = currentPi.getStartDigit() + currentPi.getData().length();
                searchThread = new SearchThread(survivalList, currentPi);
                searchThread.start();
            }

            // 最後のユニットの検索（データが尽きた場合はここで待ち合わせる）
            if (searchThread != null) {
                searchThread.join();
                StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", lastUnitEndDigit);
                if (0 < searchThread.getResult().compareTo(processDeepest)) {
                    processDeepest.update(searchThread.getResult());
                }
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        // 検索終了。サバイバルリストが空になっているはず。
        // サバイバルリストが空でない場合は探しきれなかった、とする。
        if (!survivalList.isEmpty()) {
            stop("The file was too short to finalize the last one(発見できず。YCDファイルが短すぎました)"
                    + "  検索できなかったものの一例: " + survivalList.get(0)
                    + "  読み込み深さ: " + StoreController.survivalProgressMap.get("NOW_SURVIVAL_DEPTH"));
            return null;
        }

        return processDeepest;

    }

    /**
     * 検索を停止する（プロセスは終了せず、WEB画面は残す）.
     *
     * @param reason 停止理由
     */
    private void stop(String reason) {
        System.out.println();
        System.out.println("Search stopped: " + reason);
        StoreController.survivalProgressMap.put("SEARCH_STOPPED", reason);
        StoreController.survivalProgressMap.put("SEARCH_STOPPED_AT", ZonedDateTime.now());
        StoreController.getInstance().saveHTML();
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
