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

    public enum FileInfo {
        FIRST_DATA,
        FIRST_DIGIT,
        END_DIGIT,
        ;
    }

    /**
     * 検索対象ファイルの情報保持Map.
     */
    private final Map<File, Map<FileInfo, String>> fileInfoMap;
    public Map<File, Map<FileInfo, String>> getFileInfoMap(){
        return this.fileInfoMap;
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


    private Integer targetLength;
    private Integer unitLnegth;
    private Long startDigit;
    private Long endDigit;

    public YCD_Provider(List<File> fileList, Integer targetLength, Integer unitLength, Long startDigit, Long endDigit) {

        //全ファイルヘッダー情報取得
        this.fileInfoMap = this.createFileInfo(fileList,targetLength);

        this.targetLength = targetLength;
        this.unitLnegth = unitLength;
        this.startDigit = startDigit;
        this.endDigit = endDigit;

        //処理ステータスを READY とする
        this.status = Status.READY;

    }


    @Override
    public void run() {

        //処理ステータスを EXECUTING とする
        this.status = Status.EXECUTING;

        try {

            //ひとつ前の読み込みデータを作成
            String prevUnitData = "";
            Long unitStartPoint = 1L;

            //提供開始位置までシーク
            for(File f : this.fileInfoMap.keySet()){
                Map<FileInfo, String> m = this.fileInfoMap.get(f);

                try (YCD_SeqStream stream = new YCD_SeqStream(f.getPath(), 30);) {

                    //ユニット単位での読み込みループ
                    int i = 0;
                    while (stream.hasNext()) {

                        YCDProcessUnit pdu = stream.next();
                        String readData = pdu.getValue();

                        //検索実行文字列の作成
                        String thisLine = prevUnitData + readData;

                        if( (0 == i) ||(1 == i) ){
                            System.out.println(unitStartPoint + " : " +  thisLine);
                        }
                        i++;

                        if(!stream.hasNext()){
                            System.out.println(readData);
                        }


                        unitStartPoint = unitStartPoint + (prevUnitData.length());


                        prevUnitData = readData;



                    }


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }

/*

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
                }

            }

            this.status = Status.FINISHED;
*/

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


    private Map<File, Map<FileInfo, String>> createFileInfo(List<File> fileList, Integer targetLength){

        //対象ファイル全ての情報事前取得
        try {

            Map<File, Map<FileInfo, String>> fileMap = new LinkedHashMap<>();

            //全てのファイルが対象(小さい順に並んでいること)
            for (File f : fileList) {

                Map<FileInfo, String> value = new HashMap<>();

                //ファイル情報取得
                Map<YCDHeaderInfoElem, String> fileInfoMap = YCDFileUtil.getYCDHeader(f.getPath());

                //先頭桁と最終桁の取得
                Integer blockID = Integer.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_ID));
                Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));
                value.put(FileInfo.FIRST_DIGIT, String.valueOf((blockID * blockSize) + 1L));
                value.put(FileInfo.END_DIGIT, String.valueOf((blockID + 1L) * blockSize));

                //全ての円周率ファイルの先頭から、そのファイルの前のファイルの末尾に付加する桁数だけ切り出す
                value.put(FileInfo.FIRST_DATA, YCDFileUtil.getFirstData(f.getPath(), targetLength));

                fileMap.put(f, value);

            }

            //ファイルがきちんと並んでいるかチェック
            if(!Long.valueOf(fileMap.get(fileList.get(0)).get(FileInfo.FIRST_DIGIT)).equals(1L)){
                throw new RuntimeException("Illegal first digit in file : " + fileList.get(0).getName());
            }

            //ファイルの順序と桁連結チェック
            //先頭ファイルの末尾桁番号を取得
            Long prevLastDigit = Long.valueOf(fileMap.get(fileList.get(0)).get(FileInfo.END_DIGIT));
            for(File fi : fileMap.keySet()){
                Map<FileInfo, String> m = fileMap.get(fi);

                //最初のファイルはスキップ
                if(Long.valueOf(m.get(FileInfo.FIRST_DIGIT)).equals(1L)){
                    continue;
                }

                //このファイルの先頭桁番号取得
                Long thisStart = Long.valueOf(m.get(FileInfo.FIRST_DIGIT));

                //前のファイルのラスト桁番号 +1 がこのファイルの先頭桁番号でなければならない
                if( !thisStart.equals(prevLastDigit + 1L) ) {
                    throw new RuntimeException("Illegal start digit in file : " + fi.getName() + " - is Start : " + thisStart);
                }

                //このファイルの最後尾桁番号を保持
                prevLastDigit = Long.valueOf(m.get(FileInfo.END_DIGIT));

            }

            return fileMap;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
