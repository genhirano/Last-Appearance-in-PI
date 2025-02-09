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
            StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", startTime);

            // 処理範囲の決定（次の処理を定義をStoreControllerから得る）
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(this.listSize);

            StoreController.survivalProgressMap.put("SURVIVAL_DIGIT_LENGTH", String.valueOf(targetRange.getLength()));

            // 1サイクルのサバイバル実行
            SurvivalResult sr = survive(targetRange);

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

        // このサバイバルのスタート時間を記録
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

        // プロセス内新記録の記録
        SurvivalResult processDeepest = new SurvivalResult("", -1L);

        // このサバイバルリスト消化のスタート時間を記録
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

        // サバイバルリスト作成
        SurvivalList survivalList = new SurvivalList(targetRange.getLength(), Integer.valueOf(targetRange.getStart()),
                Integer.valueOf(targetRange.getEnd()));

        // 進捗情報用サバイバルリスト初期情報の登録
        StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_INFO", targetRange);
        StoreController.survivalProgressMap.put("SURVIVAL_DISCOVERD_LIST", survivalList.getDiscoverdInfo());

        // YCDデータプロバイダ（シーケンシャルにYCDデータをブロックで提供）を作成
        int overWrapLength = targetRange.getLength(); // ユニットの境目対応として一つ前のケツの部分を今回の先頭に重ねる桁の長さ
        try (YCD_SeqProvider p = new YCD_SeqProvider(5, this.um_piFileList, overWrapLength, this.unitLength);) {

            // YCDファイルの全ヘッダー情報を記録
            StoreController.survivalProgressMap.put("YCD_FILE_INFO", p.getFileInfoMap());

            // 検索スレッドオブジェクト
            SearchThread searchThread = null;

            // 検索処理の開始時間
            long baseTime = System.currentTimeMillis();

            while (true) {

                // YCDから次の対象文字列を読み込む
                final YCD_SeqProvider.Unit currentPi = p.next();

                // ひとつ前の検索処理の終了を待ち合わせる
                if (searchThread != null) {
                    searchThread.join();

                    // 現在の検索深さを記録
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH",
                            currentPi.getStartDigit() + currentPi.getData().length());

                    // 新記録なら結果を更新
                    if (0 < searchThread.getResult().compareTo(processDeepest)) {
                        processDeepest.update(searchThread.getResult());
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
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
                            + searchThread.getAlgorithm()
                            + " Items:" + survivalList.size()
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
                searchThread = new SearchThread(survivalList, currentPi);
                searchThread.start();
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        // 検索終了。サバイバルリストが空になっているはず。
        // サバイバルリストが空でない場合は探しきれなかった、とする。
        if (!survivalList.isEmpty()) {
            System.out.println(
                    "The file was too short to finalize the last one(発見できず。YCDファイルが短すぎました)"
                            + "  検索できなかったものの一例: " + survivalList.get(0));

            System.out.println(StoreController.survivalProgressMap.get("NOW_SURVIVAL_DEPTH"));

            Runtime.getRuntime().exit(-1);
        }

        return processDeepest;

    }
}
