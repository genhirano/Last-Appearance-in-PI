package model;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YCD_Provider extends Thread {

    public enum Status {
        READY,
        EXECUTING,
        FINISHED,
        ABORT,
        ;
    }

    public enum Container {
        SEARCH_ID,
        START_DIGIT,
        PREV_FIND_POS,
        FIND_DIGIT,
        REACHED_DIGIT,
        START_TIME,
        END_TIME,
        ;
    }

    public enum FileInfo {
        FIRST_DATA,
        FIRST_DIGIT,
        END_DIGIT,
        ;
    }


    /**
     * 検索実行ID.
     */
    private final String searchID = RandomStringUtils.randomAlphanumeric(5);

    public String getSearchID() {
        return this.searchID;
    }


    /**
     * 検索対象リスト.
     */
    private final Map<String, Map<Container, String>> targetMap;

    public Map<String, Map<Container, String>> getTargetMap() {
        return this.targetMap;
    }


    /**
     * 検索対象リストの中にある最長の桁数.
     */
    private final Integer longerTargetLength;

    /**
     * 検索対象ファイルリスト.
     */
    private final List<File> fileList;

    /**
     * 検索対象ファイルの情報保持Map.
     */
    private final Map<File, Map<FileInfo, String>> fileInfoMap = new HashMap<>();
    public Map<File, Map<FileInfo, String>> getFileInfoMap(){
        return this.fileInfoMap;
    }
    /**
     * 検索終了桁数（進捗）.
     */
    private Long processDigit = 0L;

    public Long getProcessDigit() {
        return this.processDigit;
    }

    /**
     * 検索中リスト.
     */
    private final List<String> processTarget = new ArrayList<>();

    /**
     * 処理ステータス.
     */
    private Status status;

    public Status getStatus() {
        return this.status;
    }

    /**
     * デバッグ用わざと遅くするミリセカンド.
     */
    private Long debugWait = 0L;

    public void setDebugwait(Long mills) {
        this.debugWait = mills;
    }





    public YCD_Provider(List<File> fileList, Map<String, Map<Container, String>> targetMap) {
        this.fileList = fileList;
        this.targetMap = targetMap;

        //検索対象Mapのチェック
        this.check_before_targetMap(targetMap);

        //検索対象の最大文字列長さを取得
        Integer longerLength = -1;
        for (String s : targetMap.keySet()) {
            if (longerLength < s.length()) {
                longerLength = s.length();
            }
        }
        this.longerTargetLength = longerLength;

        //検索続行リスト作成。当初は全て対象とする。
        for (String s : targetMap.keySet()) {
            this.processTarget.add(s);
        }

        //円周率ファイルの先頭から、連結に必要な桁数だけ切り出す
        try {
            for (File f : fileList) {

                Map<FileInfo, String> value = new HashMap<>();

                //先頭データをターゲットの一番長い桁数に合わせて取得
                String s = YCDFileUtil.getFirstData(f.getPath(), this.longerTargetLength);
                value.put(FileInfo.FIRST_DATA, s);

                //ファイルのスタート桁位置
                Map<YCDHeaderInfoElem, String> fileInfoMap = YCDFileUtil.getYCDHeader(f.getPath());
                Integer blockID = Integer.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_ID));
                Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));

                value.put(FileInfo.FIRST_DIGIT, String.valueOf((blockID * blockSize) + 1L));
                value.put(FileInfo.END_DIGIT, String.valueOf((blockID + 1L) * blockSize));
                this.fileInfoMap.put(f, value);

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.status = Status.READY;

    }

    private void check_before_targetMap(Map<String, Map<Container, String>> targetMap) {

        //空チェック
        if (targetMap.isEmpty()) {
            throw new IllegalArgumentException("target is not Set");
        }

        //検索スタート桁設定チェック
        for (String s : targetMap.keySet()) {

            //スタート位置が設定されていない場合は1桁目から検索開始とする
            if(!targetMap.get(s).containsKey(Container.START_DIGIT)){
                targetMap.get(s).put(Container.START_DIGIT, "1");
            }

            try {
                Long i = Long.valueOf(targetMap.get(s).get(Container.START_DIGIT));

                if (0L >= i) {
                    throw new IllegalArgumentException();
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("start digit is not valid :[" + targetMap.get(s).get(Container.START_DIGIT) + "]");
            }
        }
    }


    @Override
    public void run() {

        //処理開始時間
        final String startTimeStr = String.valueOf(new Date().getTime());

        this.status = Status.EXECUTING;

        //検索対象がない場合は、終了として終わる
        if(this.targetMap.isEmpty()){
            this.status = Status.FINISHED;
            return;
        }

        try {

            //ひとつ前のデータ保持用。初期値は検索最大長さの無効文字列
            String prevData = StringUtils.repeat("@", this.longerTargetLength);

            //すべての検索対象の中から一番小さい検索開始桁数を取得
            Long minStartPos = Long.MAX_VALUE;

            //全ての検索対象に対してスタート時刻などを一斉セット。
            for (String t : this.targetMap.keySet()) {
                targetMap.get(t).put(Container.SEARCH_ID, this.searchID);
                targetMap.get(t).put(Container.START_TIME, startTimeStr);
                targetMap.get(t).put(Container.FIND_DIGIT, "-1");

                //未発見とする
                targetMap.get(t).remove(Container.FIND_DIGIT);

                //終了時刻は最後にセット
                targetMap.get(t).remove(Container.END_TIME);

                targetMap.get(t).remove(Container.REACHED_DIGIT);

                //最小スタート位置取得用
                if (minStartPos > Long.valueOf(this.targetMap.get(t).get(Container.START_DIGIT))){
                    minStartPos = Long.valueOf(this.targetMap.get(t).get(Container.START_DIGIT));
                }

            }

            //検索対象ファイルループ
            for (Integer i = 0; i < fileList.size(); i++) {

                Long d = Long.valueOf(this.fileInfoMap.get(fileList.get(i)).get(FileInfo.END_DIGIT));
                if(d < minStartPos){
                    System.out.println("検索スタート桁がこのファイルより後にあるためスキップしました" + d + "  " + fileList.get(i).getName());
                    continue;
                }

                //次のファイルの先頭桁を取得(最後に末尾にアペンドして検索するために準備)
                String nextBuff = "";
                if (i < fileList.size() - 1) {
                    nextBuff = this.fileInfoMap.get(fileList.get(i+1)).get(FileInfo.FIRST_DATA);
                }

                try (YCD_SeqStream stream = new YCD_SeqStream(fileList.get(i).getPath(), 190);) {

                    //デバッグ時用 わざと遅くする
                    try {
                        Thread.sleep(this.debugWait);
                    } catch (InterruptedException e) {
                    }

                    //ユニット単位での読み込みループ
                    while (stream.hasNext()) {

                        //カレントユニット内で検索対象分ループ
                        synchronized (this.processTarget) {

                            YCDProcessUnit pdu = stream.next();
                            String readData = pdu.getValue();

                            //今回検索対象の先頭にくっつけるための、ひとつ前の読み込みデータを準備（検索対象桁の最大長さがあれば十分）
                            if (!prevData.isEmpty()) {
                                if (prevData.length() > this.longerTargetLength) {
                                    prevData = prevData.substring(prevData.length() - this.longerTargetLength);
                                }
                            }

                            //現在処理桁
                            this.processDigit = pdu.getStartDigit();

                            //検索実行文字列の作成
                            String thisLine = prevData + readData;

                            //最後の読み込みだった場合は、次のファイルの先頭データを検索対象の末尾に追加
                            if (!stream.hasNext()) {
                                thisLine = thisLine + nextBuff;
                            }

                            List<String> fixList = new ArrayList<>();
                            for (String t : this.processTarget) {

                                Long findIndex = -1L;
                                Integer unitFind = -1;
                                Integer pos = 0;
                                while (true) {

                                    //検索対象文字列から検索
                                    unitFind = thisLine.indexOf(t, pos);

                                    if (0 <= unitFind) { //ヒットしたら

                                        findIndex = pdu.getStartDigit() - prevData.length() + unitFind;

                                        if (targetMap.containsKey(t)) {
                                            if (findIndex >= Long.valueOf(this.targetMap.get(t).get(Container.START_DIGIT))) {
                                                this.targetMap.get(t).put(Container.FIND_DIGIT, findIndex.toString());
                                                this.targetMap.get(t).put(Container.END_TIME, String.valueOf(new Date().getTime()));
                                                fixList.add(t);
                                                break;
                                            }
                                        }

                                        //ヒットした場合は、検索開始位置をシークして同じ対象文字列でもう一度検索をかける
                                        pos = unitFind + 1;

                                    } else { //ヒットしなかったら
                                        break;
                                    }
                                }

                                if (targetMap.containsKey(t)) {
                                    targetMap.get(t).put(Container.REACHED_DIGIT, pdu.getStartDigit().toString());
                                }

                            }

                            //検索スタート桁以降で発見している場合は、検索対象からはずす
                            //その際に、検索到達桁も発見桁数にセット（見つかったから検索終了、の意味）
                            for (String t : fixList) {
                                this.processTarget.remove(t);
                                targetMap.get(t).put(Container.REACHED_DIGIT, targetMap.get(t).get(Container.FIND_DIGIT));
                            }

                            //検索対象がもうなければ終了する
                            if (0 >= this.processTarget.size()) {
                                break;
                            }
                            prevData = readData;

                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                //検索対象がもうなければ終了する
                if (0 >= this.processTarget.size()) {
                    break;
                }

            }

            //ギブアップ
            // 最後まで検索しても見つからなかった場合は、到達桁数に最後のユニット先頭桁をセット
            for (String t : targetMap.keySet()) {
                if (!targetMap.get(t).containsKey(Container.FIND_DIGIT)) {
                    targetMap.get(t).put(Container.REACHED_DIGIT, String.valueOf(this.getMaxDepth() - t.length()));
                    targetMap.get(t).put(Container.END_TIME, String.valueOf(new Date().getTime()));
                }

            }

            this.status = Status.FINISHED;

        } catch (Exception e) {
            this.status = Status.ABORT;
            throw new RuntimeException(e);
        }



    }


    public Long getMaxDepth(){
        Long maxDepth = -1L;
        for(File f : this.fileInfoMap.keySet()){
            Long d = Long.valueOf(this.fileInfoMap.get(f).get(FileInfo.END_DIGIT));
            if(maxDepth < d){
                maxDepth = d;
            }
        }
        return maxDepth;

    }


}
