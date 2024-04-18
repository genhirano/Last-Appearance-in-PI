package controller;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import model.TargetRange;

public class StoreController {

    private static final int digitsLength = 3; // 3桁を対象。すなわち 001 - 999 桁を対象。
    private static final String digitsLengthFormat = "%0" + digitsLength + "d";
    private static final int maxDigit = Integer.valueOf(StringUtils.repeat("9", digitsLength));

    private static StoreController instance;

    /**
     * インスタンスの取得（シングルトンパターンっぽく）
     *
     * @return StoreControllerのインスタンス
     */
    public static StoreController getInstance() {
        if (null == instance) {
            instance = new StoreController();
        }
        return instance;
    }

    private StoreController() {
        instance = this;
    }

    /**
     * 一度に検索する対象データの定義情報取得.
     *
     * @param listSize いくつの数字を対象とするか
     * @return １回の検索対象（サバイバルリスト）開始から終了までの情報
     */
    public List<TargetRange> getNextList(Integer listSize) {

        // 結果保存先パスを取得
        String storeFilePath = Env.getInstance().getProp().getProperty(Env.PropKey.outputPath.getKeyName());

        List<TargetRange> retList = new ArrayList<>();

        for (int i = 1; i <= maxDigit; i++) {

            String filename = String.format(digitsLengthFormat, i) + ".txt";
            File file = new File(storeFilePath + "/" + filename);

            // 次に実行すべき情報を作る
            // （すでにある保存データ最後の、その次として実行する情報を作る）
            String nextMin = StringUtils.repeat("0", i); // スタート桁は0桁目
            String nextMax = String.format("%0" + nextMin.length() + "d", (0 + listSize - 1)); // 終了桁（スタート+指定の桁数）
            if (i < nextMax.length()) {
                nextMax = StringUtils.repeat("9", i);
            }

            // 保存されたファイルが有る場合は全部読んで、最後の行の次の情報を作る
            if (file.exists()) {

                // 保存用ファイルから全データ読み込み
                List<String> lines = null;
                try {
                    lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("fatal file read! " + file.getName(), e);
                }

                if (0 < lines.size()) {
                    // 保存ファイル最後行のその次として実行情報を作る

                    // 最終行のデータ取得
                    String readLine = lines.get(lines.size() - 1);

                    if (readLine.trim().isEmpty()) {
                        throw new RuntimeException("file is invalid(Last Line is empty!) " + filename);
                    }

                    // 保存データはCSVであるため、カンマで分割
                    String[] readLineArr = readLine.split(",");

                    // 最終行の検索範囲の最大値取得
                    String prevEndStr = readLineArr[1];

                    // 終了条件(対象桁数分だけ全部9だったらすでに終端に達している)
                    if (prevEndStr.equals(StringUtils.repeat("9", i))) {
                        retList.add(new TargetRange(i, "", ""));
                        continue;
                    }

                    // 次のスタート桁は、最終行のMAX + 1 桁目
                    Integer nextStart = Integer.valueOf(prevEndStr) + 1;
                    nextMin = String.format("%0" + prevEndStr.length() + "d", nextStart);

                    // 次の終了桁は スタート桁位置 + 指定の桁数
                    nextMax = String.format("%0" + prevEndStr.length() + "d", nextStart + listSize - 1);

                    // 最大検索文字列が指定桁数を超えてしまう場合は、全桁9とする
                    if (i < nextMax.length()) {
                        nextMax = StringUtils.repeat("9", i);
                    }

                }
            }

            retList.add(new TargetRange(i, nextMin, nextMax));
        }

        return retList;
    }

    public TargetRange getCurrentTargetStartEnd(Integer listSize) {
        // 次に実行すべき情報を作る
        // （すでにある保存データの一番最後の、その次として実行する情報を作る）

        // 結果保存ファイルの全ロード
        // (非効率ではあるがYCDファイルロードの度に毎回やる。このおかげで容易にリジュームできる)
        StoreController sc = StoreController.getInstance();
        List<TargetRange> storeDataList = sc.getNextList(listSize);

        // 対象とする結果保存ファイル（未終了）の決定
        TargetRange targetStartEnd = null;
        for (TargetRange se : storeDataList) {

            // 終わっていないこと（Startが空文字ならばすでに終了済みである）
            if ("".equals(se.getStart())) {
                continue;
            }

            // 範囲決定
            targetStartEnd = se;

            break;
        }

        // 次の実行情報が得られなければ終わり
        if (null == targetStartEnd) {
            return null;
        }

        return targetStartEnd;

    }

    /**
     * 検索結果をファイル保存する.
     *
     * @param targetLength 検索中桁数
     * @param start        検索範囲の開始(最小)数字
     * @param end          検索範囲の終了(最大)数字
     * @param lastData     一番最後までサバイブした数字
     * @param lastFoundPos lastDataの発見桁数
     * @param startTime    処理開示時間
     * @param endTime      処理終了時間
     */
    public void saveFile(Integer targetLength, String start, String end, String lastData, Long lastFoundPos,
            ZonedDateTime startTime, ZonedDateTime endTime) {

        // 結果保存先パスを取得
        String storeFilePath = Env.getInstance().getProp().getProperty(Env.PropKey.outputPath.getKeyName());

        // 記録するファイル名を生成
        String filename = String.format(digitsLengthFormat, targetLength) + ".txt";

        // 保存用Fileオブジェクトの生成
        File file = new File(storeFilePath + "/" + filename);

        // 保存対象ファイルがなければ作る
        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println(filename + "create!");
            } catch (IOException e) {
                throw new RuntimeException(file.getPath(), e);
            }
        }

        // 保存対象ファイルをアペンドモードで
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file.getPath(), true)));) {

            // 処理開始と処理終了の時間差（実行時間）を計算
            Duration summerVacationDuration = Duration.between(startTime, endTime);

            // 今回分データ作成
            String recordStr = start
                    + "," + end
                    + "," + lastData
                    + "," + lastFoundPos
                    + "," + summerVacationDuration.getSeconds();

            // 書き込み（アペンドモード）
            pw.println(recordStr);

        } catch (IOException e) {
            throw new RuntimeException("File Write Error", e);
        }

    }

    /**
     * 全ての結果保存ファイルからサマリーを取得.
     *
     * @return サマリー表現文字列リスト(画面表示用にフォーマットされた文字列リスト)
     */
    public List<String> getSummary() {

        // 結果保存先パスを取得
        String storeFilePath = Env.getInstance().getProp().getProperty(Env.PropKey.outputPath.getKeyName());

        List<String> retList = new ArrayList<>();

        // 全保存ファイルを対象。小さい順に処理。
        for (int i = 1; i <= Integer.MAX_VALUE; i++) {

            String answer = "";

            String filename = String.format(digitsLengthFormat, i) + ".txt";
            File file = new File(storeFilePath + "/" + filename);

            // 対象ファイルがなければそこで終了
            if (!file.exists()) {
                break;
            }

            // サマリ文字列作成開始
            // タイトル
            answer = answer + "digits:" + i;

            // 保存用ファイルから全データ読み込み
            List<String> lines = null;
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

                String maxDepthStr = "";
                Long maxDepth = Long.MIN_VALUE;
                Long allSec = 0L;

                // 保存レコードループ
                for (String s : lines) {

                    String[] splited = s.split(",");
                    Long depth = Long.valueOf(splited[3]);

                    // かかった時間(秒)の積算
                    allSec = allSec + Long.valueOf(splited[4]);

                    // これまでで最大深度であれば一番遅い可能性あり。メモ更新
                    if (depth > maxDepth) {
                        maxDepth = depth;
                        maxDepthStr = splited[2];
                    }

                }

                // 最終行を取得（終わっているかの判断 および 現在進捗情報を取得する）
                String lastLine = lines.get(lines.size() - 1);

                if (lastLine.isEmpty()) {
                    // ファイルに一行もない場合は、単純に「初期化中」としておこう。。
                    answer = answer + " initializeing progress...";
                    retList.add(answer);
                    break;
                }

                // 最終行の分析
                String[] lastSplited = lastLine.split(",");

                // 進捗状況
                Integer allMax = Integer.valueOf(StringUtils.repeat("9", i));
                if (allMax.equals(Integer.valueOf(lastSplited[1]))) {
                    // 最後まで到達していたら進捗100％とする
                    answer = answer + "     the last appearing: \"" + maxDepthStr + "\".";
                    answer = answer + " depth: " + maxDepth + ".";
                    answer = answer + " process time: " + allSec + "sec.";
                } else {
                    // 処理中の進捗取得
                    double d = (Double.valueOf(lastSplited[1]) / allMax) * 100;
                    double progress = ((double) Math.round(d * 1000)) / 1000;

                    answer = answer + "  brute forced depth:" + maxDepthStr;
                    answer = answer + "  (not appear:" + (allMax - Integer.valueOf(lastSplited[2])) + ")";
                    answer = answer + " Progress: " + progress + "%";
                    answer = answer + " processing time: " + allSec + "sec.";
                }

                // このファイルのサマリ（画面表示などで使える文字列）をリストに追加
                retList.add(answer);

            } catch (IOException e) {
                throw new RuntimeException("fatal file read! " + file.getName(), e);
            }

        }

        return retList;

    }

}
