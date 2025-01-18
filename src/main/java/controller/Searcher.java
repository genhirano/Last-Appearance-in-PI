package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Searcher extends Thread {

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

            // 処理範囲の決定（次の処理を定義）
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(this.listSize);

            StoreController.survivalProgressMap.put("SURVIVAL_DIGIT_LENGTH", String.valueOf(targetRange.getLength()));

            // 1サイクルのサバイバル実行
            SurvivalResult sr = survive(targetRange);

            // １サイクルの終了時間
            ZonedDateTime endTime = ZonedDateTime.now();

            // １サイクルの結果保存
            StoreController.getInstance().saveFile(targetRange.getLength(), targetRange.getStart(),
                    targetRange.getEnd(), sr.target, sr.findPos, startTime, endTime);

            // Httpで強制アクセス（静的HTMLを作成するため）
            StoreController.getInstance().saveHTML();

        }

    }

    private SurvivalResult survive(TargetRange targetRange) {

        // YCDファイル読み込みに失敗することがある。その場合はリトライするがリトライ回数に制限を設ける
        Integer continueCount = 0;

        String lastFoundTarget = "";
        Long lastFoundPos = -1L;

        // サバイバルリスト
        SurvivalList survivalList = null;

        // サバイバルリストの(再)作成フラグ
        Boolean goSurvivalListRemake = true;

        // このサバイバルリスト消化のスタート時間を記録
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // 読み込み失敗時のリトライ処理用ループ
        while (true) {

            // YCDプロバイダの作成に失敗することがある。その場合はリトライするがリトライ回数に制限を設ける
            if (5 < continueCount) {
                throw new RuntimeException("Fail!  Retry limit exceeded." + continueCount);
            }

            // 今回のサバイバルリストの作成フラグがONの場合はサバイバルリストを作成
            // IOエラーなどで再度読み込みする場合は再作成しない
            if (goSurvivalListRemake) {

                // サバイバルリスト作成
                survivalList = new SurvivalList(targetRange.getLength(), Integer.valueOf(targetRange.getStart()),
                        Integer.valueOf(targetRange.getEnd()));

                // 進捗情報用サバイバルリスト初期情報の登録
                StoreController.survivalProgressMap.put("SURVIVAL_INITIAL_INFO", targetRange);
                StoreController.survivalProgressMap.put("SURVIVAL_DISCOVERD_LIST", survivalList.getDiscoverdInfo());

            }
            goSurvivalListRemake = true; // サバイバルリストの再作成フラグをON（リセット）

            // YCDデータプロバイダ（シーケンシャルにYCDデータをブロックで提供）を作成
            int overWrapLength = targetRange.getLength(); // 一つ前のケツの部分を今回の先頭に重ねる桁の長さ
            try (YCD_SeqProvider p = new YCD_SeqProvider(this.um_piFileList, overWrapLength, this.unitLength);) {

                // YCDプロバイダの作成に成功したらリトライカウントをリセット
                continueCount = 0;

                // YCDファイルの全ヘッダー情報を記録
                StoreController.survivalProgressMap.put("YCD_FILE_INFO", p.getFileInfoMap());

                // このサバイバルのスタート時間を記録
                StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

                // YCDプロバイダからパイユニットを順次取り出し（順次切り出したカレントパイループ）
                // 取り出したデータは、前のブロックのケツを先頭に付加していて、ユニット区切り目の検索漏れを防いでいる
                for (final YCD_SeqProvider.Unit currentPi : p) {

                    // 現在の検索深さを記録
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH",
                            currentPi.getStartDigit() + currentPi.getData().length());

                    // サバイバルリストの文字列左共通部分を取得（共通部分でまず検索シークするため）
                    String leftCommonStr = survivalList.getCommonPrefix();
                    if (leftCommonStr.isEmpty()) {
                        // 共通部分がない場合は、共通部分なしを示す適当な文字をセット（indexOFは空文字検索でゼロを返すのでその対策）
                        leftCommonStr = "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@";
                    }

                    // このユニットの検索スタート位置初期値（検索終了している部分をスキップするためのシーク位置）
                    int commonSeekPos = 0;

                    // カレントパイユニットが末尾に達するまで繰り返す
                    while (true) {

                        // スタート位置を確定（次の共通文字列を検索してシークする）
                        int startPos = currentPi.indexOf(leftCommonStr, commonSeekPos);

                        // 共通左文字によるシークができなかった場合は、１文字だけシークする（逐次）
                        if (startPos == -1) {
                            startPos = commonSeekPos;
                        }

                        // シーク位置を先頭にしたとき、データが足りない場合は、次のYCDユニットへ
                        if (startPos + targetRange.getLength() > currentPi.getData().length()) {
                            break;
                        }

                        // カレントパイ文字列
                        final String currentPiStr = currentPi.getData().substring(startPos,
                                startPos + targetRange.getLength());

                        // サバイバルリストからそれを探す
                        final int suvIndex = survivalList.indexOf(currentPiStr);

                        if (-1 < suvIndex) {
                            // サバイバルリストヒット

                            // カレントパイ文字列の中での発見位置を、全体位置に変換
                            final Long curFindPos = currentPi.getStartDigit() + startPos;

                            // 発見位置が今までで一番後ろだったらメモ記録（最遅候補とする）
                            if (lastFoundPos < curFindPos) {
                                lastFoundTarget = currentPiStr; // 発見した対象
                                lastFoundPos = curFindPos; // 発見位置
                            }

                            // ヒットした要素をサバイバルリストから削除
                            survivalList.discover(currentPiStr, curFindPos);

                            StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

                        }

                        // シーク位置を１文字ずらす。（共通部分発見位置の次の文字から再開する）
                        commonSeekPos = startPos + 1;
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        break; // read YCD UNIT break
                    }

                    final String anowReadDepth = decimalFormat
                            .format((Long.parseLong(currentPi.getFileInfo().get(YCDFileUtil.FileInfo.END_DIGIT))));
                    final String nowReadDepth = decimalFormat
                            .format((currentPi.getStartDigit() + currentPi.getData().length()));
                    System.out.print("\rRemainingItems: " + survivalList.size()
                            + " ReadDepth: " + nowReadDepth
                            + " / " + anowReadDepth
                            + " min:" + survivalList.get(0)
                            + " max:" + survivalList.get(survivalList.size() - 1)
                            + " leftCommon:" + leftCommonStr
                            + "                      ");

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
                goSurvivalListRemake = false; // サバイバルリストの再作成フラグをOFF
                continue; // 再起動
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

            return new SurvivalResult(lastFoundTarget, lastFoundPos);

        }

    }

}
