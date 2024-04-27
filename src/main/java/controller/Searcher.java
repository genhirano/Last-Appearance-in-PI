package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Searcher extends Thread {

    final Integer SURVAIVAL_LIST_DEFAULT_SIZE = 100; // サバイバルリストの初期サイズ

    /**
     * サバイバル結果を保持するクラス(メソッド戻り値として使用)
     */
    private class SurvivalResult {
        public final String target;
        public final Long findPos;

        public SurvivalResult(String target, Long findPos) {
            this.target = target;
            this.findPos = findPos;
        }
    }

    @Getter
    private final List<File> um_piFileList;// 不変

    @Getter
    private final Integer listSize;

    @Getter
    private final Integer unitLength;

    @Getter
    private final Integer reportSpan;

    public Searcher(List<File> piFileList, Integer listSize, Integer unitLength, Integer reportSpan)
            throws IOException {
        super();

        um_piFileList = Collections.unmodifiableList(piFileList); // 外部から参照されるための不変オブジェクト

        this.listSize = listSize;
        this.unitLength = unitLength;
        this.reportSpan = reportSpan;

        StoreController.setAllPiDataLength(YCDFileUtil.getMaxDepth(um_piFileList));

    }

    @Override
    public void run() {

        Integer survaivalListSize = SURVAIVAL_LIST_DEFAULT_SIZE; // サバイバルリストの初期サイズ

        Integer mokuhyouSeconds = 20; // 処理目標時間 秒（早くても遅くてもいまいち。この時間に近づける）

        // 検索処理のメインループ
        while (true) {

            // １サイクルの開始時間
            ZonedDateTime startTime = ZonedDateTime.now();
            StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", startTime);

            // 処理範囲の決定（次に記録する１行を定義）
            // この桁数の初めての時は、サバイバルリストの初期サイズをデフォルトに戻す
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(survaivalListSize);
            if (targetRange.getStart().replaceAll("0", "").isEmpty()) {

                // サバイバルリストを初期サイズとする
                survaivalListSize = SURVAIVAL_LIST_DEFAULT_SIZE;

                // 対象レンジをデフォルトのサイズで再計算
                targetRange = StoreController.getInstance().getCurrentTargetStartEnd(survaivalListSize);
            }

            StoreController.survivalProgressMap.put("SURVIVAL_DIGIT_LENGTH", String.valueOf(targetRange.getLength()));

            System.out.println("Current Digits: " + targetRange.getLength());

            // 1サイクルのサバイバル実行
            SurvivalResult sr = survive(targetRange);

            // １サイクルの終了時間
            ZonedDateTime endTime = ZonedDateTime.now();

            // １サイクルの結果保存
            StoreController.getInstance().saveFile(targetRange.getLength(), targetRange.getStart(),
                    targetRange.getEnd(), sr.target, sr.findPos, startTime, endTime);

            // Httpで強制アクセス（静的HTMLを作成するため）
            StoreController.getInstance().saveHTML();

            // 次回のサバイバルリストのサイズを計算
            survaivalListSize = calcSurvivalListSize(mokuhyouSeconds, survaivalListSize, startTime, endTime);

        }

    }

    private Integer calcSurvivalListSize(Integer mokuhyouSeconds, Integer currentSrvivalListSize,
            ZonedDateTime startTime, ZonedDateTime endTime) {

        // 処理開始と処理終了の時間差（実行時間）を計算
        long processSeconds = Duration.between(startTime, endTime).getSeconds();

        // 目標値に対してどのくらい差があるかを計算
        Long diff = mokuhyouSeconds - processSeconds;
        Double d = (double) diff / (double) mokuhyouSeconds;

        // 次回のサバイバルリストサイズを調整。
        // 処理目標時間より長くかかった場合はリストを短く、早く終わった場合はリストを長くする
        Integer nextListSize = currentSrvivalListSize + (int) (currentSrvivalListSize * d);

        // サバイバルリストのサイズが小さすぎる場合はデフォルトに戻す
        if (SURVAIVAL_LIST_DEFAULT_SIZE > nextListSize) {
            nextListSize = SURVAIVAL_LIST_DEFAULT_SIZE;
        }

        System.out.println("Current Survival List Size: " + currentSrvivalListSize + "  Next Survival List Size: "
                + nextListSize + "  Process Time: " + processSeconds + "  Diff: " + diff + "  rate: " + d);

        return nextListSize;

    }

    private SurvivalResult survive(TargetRange targetRange) {

        // YCDファイル読み込みに失敗することがある。その場合はリトライするがリトライ回数に制限を設ける
        Integer continueCount = 0;

        String lastFoundTarget = "";
        Long lastFoundPos = -1L;

        while (true) {

            if (5 < continueCount) {
                throw new RuntimeException("Fail!  Retry limit exceeded." + continueCount);
            }

            // 今回のサバイバルリストの作成
            SurvivalList survivalList = new SurvivalList(targetRange.getLength(),
                    Integer.valueOf(targetRange.getStart()), Integer.valueOf(targetRange.getEnd()));

            StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_LIST_SIZE", String.valueOf(survivalList.size()));

            // YCDデータプロバイダを作成
            int overWrapLength = targetRange.getLength(); // 一つ前のケツの部分を今回の先頭に重ねる桁の長さ
            try (YCD_SeqProvider p = new YCD_SeqProvider(this.um_piFileList, overWrapLength, this.unitLength);) {

                // YCDプロバイダの作成に成功したらリトライカウントをリセット
                continueCount = 0;

                // YCDプロバイダからパイユニットを順次取り出し（順次切り出したカレントパイループ）
                for (YCD_SeqProvider.Unit currentPi : p) {
                    System.out.print("|");
                    
                    Env.getInstance().getReportSpan();
                    
                    // カレントパイ文字列から、サバイバルリストのそれぞれを検索（サバイバルリストループ）
                    for (int i = survivalList.size() - 1; i >= 0; i--) {

                        String target = survivalList.get(i);

                        int pos = currentPi.indexOf(target);
                        if (0 <= pos) {
                            // ヒット
                            // ヒットしたら基本的にはサバイバルリストから削除する

                            // カレントパイ文字列の中での発見位置を、全体位置に変換
                            Long curFindPos = currentPi.getStartDigit() + pos;

                            // 発見位置が今までで一番後ろだったらメモ記録
                            if (lastFoundPos < curFindPos) {
                                lastFoundTarget = target; // 発見した対象
                                lastFoundPos = curFindPos; // 発見位置
                            }

                            // サバイバルリストからヒットした要素を削除
                            survivalList.remove(i);

                            System.out.print(" " + survivalList.size());

                        }
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        break;
                    }

                    StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_LIST_SIZE",
                            String.valueOf(survivalList.size()));

                }

            } catch (Throwable t) {

                // YCDファイルの読み込みなど、仮に何かしらのエラーが発生した場合でも、ちょっと時間をおいて再起動してみる
                continueCount++;
                RuntimeException ee = new RuntimeException("ERROR!  start over. count: " + continueCount, t);
                ee.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue; // 再起動
            }

            // 検索終了。サバイバルリストが空になっているはず。
            // サバイバルリストが空でない場合は探しきれなかった、とする。
            if (!survivalList.isEmpty()) {
                System.out.println(
                        "The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)"
                                + "  検索できなかったもの: " + survivalList.toString());
                Runtime.getRuntime().exit(-1);
            }

            System.out.println(" .end Last Appearance : [" + lastFoundTarget + "] Pos : " + lastFoundPos);
            return new SurvivalResult(lastFoundTarget, lastFoundPos);

        }

    }

}
