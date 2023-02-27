package controller;

import model.ycd.YCD_SeqProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class StoreController {

    public class StartEndBean {

        public Integer getTargetLength() {
            return targetLength;
        }

        public void setTargetLength(Integer targetLength) {
            this.targetLength = targetLength;
        }

        Integer targetLength;
        String start;
        String end;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public StartEndBean() {
            this.start = "";
            this.end = "";
        }

        public StartEndBean(Integer targetLength, String start, String end) {
            this();
            this.targetLength = targetLength;
            this.start = start;
            this.end = end;
        }

    }


    private String storeFilePath = ".";


    private static StoreController instance;

    public static StoreController getInstance() {
        if (null == instance) {
            instance = new StoreController();
        }
        return instance;
    }

    private StoreController() {
        this.instance = this;
    }

    public List<StartEndBean> getNextList(Integer listSize, Integer MaxLength) {

        List<StartEndBean> retList = new ArrayList();

        for (int i = 1; i <= MaxLength; i++) {

            String filename = String.format("%02d", i) + ".txt";
            File file = new File(this.storeFilePath + "/" + filename);

            //次に実行すべき情報を作る
            //（すでにある保存データ最後の、その次として実行する情報を作る）
            String nextMin = StringUtils.repeat("0", i); //スタート桁は0桁目
            String nextMax = String.format("%0" + nextMin.length() + "d", (0 + listSize - 1)); //終了桁（スタート+指定の桁数）
            if (i < nextMax.length()) {
                nextMax = StringUtils.repeat("9", i);
            }

            //保存されたファイルが有る場合は全部読んで、最後の行の次の情報を作る
            if (file.exists()) {

                //保存用ファイルから全データ読み込み
                List<String> lines = null;
                try {
                    lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("fatal file read! " + file.getName(), e);
                }

                if (0 < lines.size()) {
                    //保存ファイル最後行のその次として実行情報を作る

                    //最終行のデータ取得
                    String readLine = lines.get(lines.size() - 1);

                    if (readLine.trim().isEmpty()) {
                        throw new RuntimeException("file is invalid(Last Line is empty!) " + filename);
                    }

                    //保存データはCSVであるため、カンマで分割
                    String[] readLineArr = readLine.split(",");

                    //最終行の検索範囲の最大値取得
                    String prevEndStr = readLineArr[1];

                    //終了条件(対象桁数分だけ全部9だったらすでに終端に達している)
                    if (prevEndStr.equals(StringUtils.repeat("9", i))) {
                        retList.add(new StartEndBean(i, "", ""));
                        continue;
                    }

                    //次のスタート桁は、最終行のMAX + 1 桁目
                    Integer nextStart = Integer.valueOf(prevEndStr) + 1;
                    nextMin = String.format("%0" + prevEndStr.length() + "d", nextStart);

                    //次の終了桁は スタート桁位置 + 指定の桁数
                    nextMax = String.format("%0" + prevEndStr.length() + "d", nextStart + listSize - 1);

                    //最大検索文字列が指定桁数を超えてしまう場合は、全桁9とする
                    if (i < nextMax.length()) {
                        nextMax = StringUtils.repeat("9", i);
                    }

                }
            }

            retList.add(new StartEndBean(i, nextMin, nextMax));
        }

        return retList;
    }

    public void saveFile(Integer targetLength, String start, String end, String lastData, Long
            lastFoundPos, ZonedDateTime startTime, ZonedDateTime endTime) {

        String filename = String.format("%02d", targetLength) + ".txt";

        // Fileオブジェクトの生成
        File file = new File(this.storeFilePath + "/" + filename);

        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println(filename + "create!");
            } catch (IOException e) {
                throw new RuntimeException(file.getPath(),e);
            }
        }

        try (
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file.getPath(), true)));) {

            //今回分追加
            Duration summerVacationDuration = Duration.between(startTime, endTime);

            //進捗状況
            Double allMax = Double.valueOf(StringUtils.repeat("9", targetLength));
            double d = (Double.valueOf(end) / allMax) * 100;
            d = ((double) Math.round(d * 1000)) / 1000;

            String recordStr =
                    start
                            + "," + end
                            + "," + lastData
                            + "," + lastFoundPos
                            + "," + summerVacationDuration.getSeconds();

            pw.println(recordStr);
            System.out.println(recordStr);

        }catch(IOException e){
            throw new RuntimeException("保存ファイル書き込み失敗",e);
        }

    }

    public List<String> getAll() {
        List<String> retList = new ArrayList<>();

        for (int i = 1; i <= Integer.MAX_VALUE; i++) {

            String filename = String.format("%02d", i) + ".txt";
            File file = new File(this.storeFilePath + "/" + filename);

            if (!file.exists()) {
                break;
            }
            //保存用ファイルから全データ読み込み
            List<String> lines = null;
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("fatal file read! " + file.getName(), e);
            }
            retList.add("-----" + filename + "-----");
            retList.addAll(lines);
        }

        return retList;

    }

    public List<String> getSummary() {
        List<String> retList = new ArrayList<>();

        for (int i = 1; i <= Integer.MAX_VALUE; i++) {

            String answer = "";

            String filename = String.format("%02d", i) + ".txt";
            File file = new File(this.storeFilePath + "/" + filename);

            if (!file.exists()) {
                break;
            }

            //タイトル
            answer = answer + "number of digits:" + String.format("%02d", i);

            //保存用ファイルから全データ読み込み
            List<String> lines = null;
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

                String maxDepthStr = "";
                Long maxDepth = Long.MIN_VALUE;
                Long allSec = 0L;
                for(String s : lines){

                    String[] splited = s.split(",");
                    Long depth = Long.valueOf(splited[3]);

                    if(depth > maxDepth){
                        maxDepth = depth;
                        maxDepthStr = splited[2];
                    }

                    //かかった時間(秒)の積算
                    allSec = allSec + Long.valueOf(splited[4]);

                }

                //一番最後の行を見る
                String lastLine = lines.get(lines.size()-1);
                if(lastLine.isEmpty()){
                    //ファイルに一行もない場合
                    answer = answer + " init progress...";
                    break;
                }

                //最終行の分析
                String[] lastSplited = lastLine.split(",");

                //進捗状況
                Integer allMax = Integer.valueOf(StringUtils.repeat("9", i));
                double progress = 0;
                if(allMax.equals(Integer.valueOf(lastSplited[1]))){
                    //最後まで到達していたら進捗100％とする
                    progress=100.0d;

                    answer = answer +  " the last appearing number: " + maxDepth;
                    answer = answer +  " appearing depth: " + maxDepthStr;
                    answer = answer +  " process time: " + allSec + "sec";

                }else{
                    //進捗取得
                    double d = (Double.valueOf(lastSplited[1]) / allMax) * 100;
                    progress = ((double) Math.round(d * 1000)) / 1000;

                    answer = answer +  " brute forced: " + maxDepthStr;
                    answer = answer + "  (remaining: " + (allMax - Integer.valueOf(lastSplited[2]) + " Progress: " + progress + "% " +  " processing time: " + allSec + "sec)");

                }

                retList.add(answer);

            } catch (IOException e) {
                throw new RuntimeException("fatal file read! " + file.getName(), e);
            }

        }

        return retList;


    }


}
