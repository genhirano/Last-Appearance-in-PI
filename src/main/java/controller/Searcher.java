package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
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
            SurvivalResult sr = survive2(targetRange);

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

        // このサバイバルのスタート時間を記録
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

        while (true) {

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
            goSurvivalListRemake = true; // サバイバルリストの再作成フラグをON

            // YCDデータプロバイダを作成
            int overWrapLength = targetRange.getLength(); // 一つ前のケツの部分を今回の先頭に重ねる桁の長さ
            try (YCD_SeqProvider p = new YCD_SeqProvider(this.um_piFileList, overWrapLength, this.unitLength);) {

                // YCDプロバイダの作成に成功したらリトライカウントをリセット
                continueCount = 0;

                // YCDファイルの全ヘッダー情報を記録
                StoreController.survivalProgressMap.put("YCD_FILE_INFO", p.getFileInfoMap());

                // このサバイバルのスタート時間を記録
                StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

                // YCDプロバイダからパイユニットを順次取り出し（順次切り出したカレントパイループ）
                for (YCD_SeqProvider.Unit currentPi : p) {

                    // 現在の検索深さを記録
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH",
                            currentPi.getStartDigit() + currentPi.getData().length());

                    // このユニットの検索スタート位置
                    int startPos = 0;

                    // カレントパイ文字列から、サバイバルリストのそれぞれを検索（サバイバルリストループ）
                    for (int i = survivalList.size() - 1; i >= 0; i--) {
                        String target = survivalList.get(i);

                        int pos = currentPi.indexOf(target, startPos);
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
                            survivalList.discover(i, curFindPos);

                            StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

                        }
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        break;
                    }

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
                        "The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)"
                                + "  検索できなかったものの一例: " + survivalList.get(0));
                Runtime.getRuntime().exit(-1);
            }

            return new SurvivalResult(lastFoundTarget, lastFoundPos);

        }

    }

    private SurvivalResult survive2(TargetRange targetRange) {

        // YCDファイル読み込みに失敗することがある。その場合はリトライするがリトライ回数に制限を設ける
        Integer continueCount = 0;

        String lastFoundTarget = "";
        Long lastFoundPos = -1L;

        // サバイバルリスト
        SurvivalList survivalList = null;

        // サバイバルリストの(再)作成フラグ
        Boolean goSurvivalListRemake = true;

        // このサバイバルのスタート時間を記録
        StoreController.survivalProgressMap.put("SURVIVAL_CURRENT_START_TIME", ZonedDateTime.now());

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
                // 取り出したデータは、前のブロックのケツを先頭に負荷していて、区切り目の検索漏れを防いでいる
                for (YCD_SeqProvider.Unit currentPi : p) {

                    // 現在の検索深さを記録
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH",
                            currentPi.getStartDigit() + currentPi.getData().length());

                    // このユニットの検索スタート位置
                    int commonSeekPos = 0;

                    String leftCommonStr = survivalList.getCommonPrefix();

                    if (leftCommonStr.isEmpty()) {
                        // 共通部分がない場合は、共通部分なしを示す適当な文字をセット（indexOFは空文字検索でゼロを返すのでその対策）
                        leftCommonStr = "@@@@";
                    }

                    while (true) {

                        // 共通部分検索
                        commonSeekPos = currentPi.indexOf(leftCommonStr, commonSeekPos);

                        String currentPiStr = "";

                        int suvFindIndex = -1;
                        String findPiStr = "";

                        if (0 <= commonSeekPos) {
                            // 共通部分発見 の場合は、その場所から桁数分切り取って検索対象とする

                            // 共通部分が見つかった場合その位置から文字列長さを切り取る
                            currentPiStr = currentPi.getData().substring(commonSeekPos,
                                    commonSeekPos + targetRange.getLength() - 1);

                            // サバイバルリストから探す
                            if (survivalList.contains(currentPiStr)) {
                                suvFindIndex = commonSeekPos;
                                findPiStr =  currentPiStr;
                            }

                        } else {
                            // 共通部分発見できず の場合は、YCD読み取りユニット全体を検索対象とする
                            currentPiStr = currentPi.getData();

                            // サバイバルリストから探す
                            suvFindIndex = -1;
                            for (int i = 0; i < survivalList.size(); i++) {
                                int findIndex = currentPiStr.indexOf(survivalList.get(i));
                                if (findIndex >= 0) {
                                    System.out.println("find: " + survivalList.get(i));
                                    suvFindIndex = findIndex;
                                    findPiStr =  survivalList.get(i);
                                    break;
                                }
                            }

                            if(-1 == suvFindIndex){
                                break; //対象文字列の中にはもうない
                            }

                        }

                        if (0 <= suvFindIndex) {

                            // YCDカレント文字列の中での発見位置を全体位置に変換
                            Long curFindPos = currentPi.getStartDigit() + suvFindIndex;

                            // 発見位置が今までで一番後ろだったらメモ記録
                            if (lastFoundPos < curFindPos) {
                                lastFoundTarget = currentPiStr; // 発見した対象
                                lastFoundPos = curFindPos; // 発見位置
                            }

                            // サバイバルリストからヒットした要素を削除
                            survivalList.discover(findPiStr, curFindPos);
                            StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

                        }

                        // サバイバルリストが空になったら終了
                        if (survivalList.isEmpty()) {
                            break; // read YCD UNIT break
                        }

                        //共通検索位置を１文字進める
                        commonSeekPos++;

                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        break; // read YCD break
                    }

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
                        "The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)"
                                + "  検索できなかったものの一例: " + survivalList.get(0));
                Runtime.getRuntime().exit(-1);
            }

            return new SurvivalResult(lastFoundTarget, lastFoundPos);

        }

    }

}
