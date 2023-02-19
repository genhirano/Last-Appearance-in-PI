package model.pi;

import model.ycd.YCDFileUtil;
import model.ycd.YCDHeaderInfoElem;
import model.ycd.YCDProcessUnit;
import model.ycd.YCD_SeqStream;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YCD_Provider extends Thread implements AutoCloseable {

    @Override
    public void close() throws Exception {

        if(null != this.currentStream){
            this.currentStream.close();
        }

    }

    public enum FileInfo {
        BLOCK_INDEX,
        FIRST_DATA,
        FIRST_DIGIT,
        END_DIGIT,
        ;
    }

    /**
     * 検索対象ファイルの情報保持Map.
     */
    private final Map<File, Map<FileInfo, String>> fileInfoMap;

    private Integer targetLength;
    private Integer unitLnegth;

    public class Unit {

        private Long startDigit;

        public Long getStartDigit() {
            return this.startDigit;
        }

        private String data;

        public String getData() {
            return data;
        }


        private Unit(Long startDigit, String data) {
            this.startDigit = startDigit;
            this.data = data;
        }

    }

    private File currentFile = null;
    private File lastFile = null;

    private YCD_SeqStream currentStream = null;

    String prevUnitData = "";
    Long unitStartPoint = 1L;


    public YCD_Provider(List<File> fileList, Integer targetLength, Integer unitLength) throws IOException {

        this.targetLength = targetLength;
        this.unitLnegth = unitLength;

        //全ファイルヘッダー情報取得
        this.fileInfoMap = this.createFileInfo(fileList, targetLength);

        //カレントファイルを先頭ファイルにする
        this.currentFile = fileList.get(0);

        //終了判定用に最後のファイルを保持しておく
        this.lastFile = fileList.get(fileList.size()-1);

        //初めての呼び出し
        this.currentStream = this.createStream(this.currentFile, this.unitLnegth);

    }

    private YCD_SeqStream createStream(File file, Integer unitLength) throws IOException {
        return new YCD_SeqStream(file.getPath(), unitLength);
    }

    public Boolean hasNext(){

        //カレントストリームにまだ次があれば「次あり」
        if(this.currentStream.hasNext()){
            return true;
        }
        //カレントストリームに次がなくても、次のファイルがあれば「次あり」
        if(!this.currentFile.getName().equals(this.lastFile.getName())){
            return true;
        }

        //カレントストリームに次がなく、かつ、次のファイルもない場合は「次なし」
        return false;

    }


    public Unit getNext() throws IOException {


        //すでにカレントストリーム末尾に達していたら次のファイルへ
        if (!this.currentStream.hasNext()) {
            this.currentStream.close();
            this.currentStream = null;

            Integer thisFileIndex = Integer.valueOf(this.fileInfoMap.get(this.currentFile).get(FileInfo.BLOCK_INDEX));

            Boolean findNextFile = false;
            for (File f2 : this.fileInfoMap.keySet()) {
                Integer theFileIndex = Integer.valueOf(this.fileInfoMap.get(f2).get(FileInfo.BLOCK_INDEX));
                if (theFileIndex.equals(thisFileIndex + 1)) {
                    this.currentFile = f2;
                    this.currentStream = createStream(this.currentFile, this.unitLnegth);
                    findNextFile = true;
                    break;
                }
            }
            if(!findNextFile){
                this.currentFile = null;
            }

        }


        YCDProcessUnit pdu = this.currentStream.next();
        String readData = pdu.getValue();

        //検索実行文字列の作成
        String thisLine = this.prevUnitData + readData;


        //読んだ後、ファイル末尾に達していたら次のファイルの先頭をもってきてここのケツにくっつける
        if (!this.currentStream.hasNext()) {
            Integer thisFileIndex = Integer.valueOf(this.fileInfoMap.get(this.currentFile).get(FileInfo.BLOCK_INDEX));
            for (File f2 : this.fileInfoMap.keySet()) {
                Integer theFileIndex = Integer.valueOf(this.fileInfoMap.get(f2).get(FileInfo.BLOCK_INDEX));
                if (theFileIndex.equals(thisFileIndex + 1)) {
                    thisLine = thisLine + this.fileInfoMap.get(f2).get(FileInfo.FIRST_DATA);
                    break;
                }

            }
        }

        this.prevUnitData = thisLine.substring(thisLine.length() - this.targetLength);


        Long thisStartPoint = this.unitStartPoint;
        this.unitStartPoint = this.unitStartPoint + (thisLine.length()) - (prevUnitData.length());

        Unit u = new Unit(thisStartPoint, thisLine);
        return u;

    }


    @Override
    public void run() {

        try {

            Long unitStartPoint = 1L;

            for (File f : this.fileInfoMap.keySet()) {

                Map<FileInfo, String> m = this.fileInfoMap.get(f);

                try (YCD_SeqStream stream = new YCD_SeqStream(f.getPath(), this.unitLnegth);) {

                    //ひとつ前の読み込みデータを作成
                    String prevUnitData = "";

                    //ユニット単位での読み込みループ
                    int i = 0;
                    while (stream.hasNext()) {

                        YCDProcessUnit pdu = stream.next();
                        String readData = pdu.getValue();

                        //検索実行文字列の作成
                        String thisLine = prevUnitData + readData;


                        //ファイル末尾に達したら次のファイルの先頭をケツにくっつける
                        if (!stream.hasNext()) {
                            Integer thisFileIndex = Integer.valueOf(this.fileInfoMap.get(f).get(FileInfo.BLOCK_INDEX));

                            for (File f2 : this.fileInfoMap.keySet()) {
                                Integer theFileIndex = Integer.valueOf(this.fileInfoMap.get(f2).get(FileInfo.BLOCK_INDEX));
                                if (theFileIndex.equals(thisFileIndex + 1)) {
                                    thisLine = thisLine + this.fileInfoMap.get(f2).get(FileInfo.FIRST_DATA);

                                    //System.out.println("thisdata : " + thisLine);
                                    //System.out.println("nextdata : " + this.fileInfoMap.get(f2).get(FileInfo.FIRST_DATA));


                                    break;
                                }

                            }
                        }


                        //if ((0 == i) || (1 == i)|| (2 == i)|| (43477 == i)|| (43478 == i)|| (43479 == i)) {
                        //    System.out.println(i + "  " + String.format("%,d", unitStartPoint) + " unit : " + thisLine);
                        //}

                        //if ((0 == i % 100000)) {
                        //    System.out.println(i + "  " + String.format("%,d", unitStartPoint) + " unit : " + thisLine);
                        //}

                        i++;

                        prevUnitData = thisLine.substring(thisLine.length() - this.targetLength);

                        unitStartPoint = unitStartPoint + (thisLine.length()) - (prevUnitData.length());


                    }


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    public Long getMaxDepth() {
        Long maxDepth = -1L;
        for (File f : this.fileInfoMap.keySet()) {
            Long d = Long.valueOf(this.fileInfoMap.get(f).get(FileInfo.END_DIGIT));
            if (maxDepth < d) {
                maxDepth = d;
            }
        }
        return maxDepth;

    }


    private Map<File, Map<FileInfo, String>> createFileInfo(List<File> fileList, Integer targetLength) {

        //対象ファイル全ての情報事前取得
        try {

            Map<File, Map<FileInfo, String>> fileMap = new LinkedHashMap<>();

            //全てのファイルが対象(小さい順に並んでいること)
            for (File f : fileList) {

                Map<FileInfo, String> value = new HashMap<>();

                //ファイル情報取得
                Map<YCDHeaderInfoElem, String> fileInfoMap = YCDFileUtil.getYCDHeader(f.getPath());

                //ブロックID（ブロックIndexということにする）
                Integer blockID = Integer.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_ID));
                value.put(FileInfo.BLOCK_INDEX, String.valueOf(blockID));

                //ブロックサイズ（そのファイルに格納されている総桁数）
                Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));

                //先頭桁と最終桁の取得
                value.put(FileInfo.FIRST_DIGIT, String.valueOf((blockID * blockSize) + 1L));
                value.put(FileInfo.END_DIGIT, String.valueOf((blockID + 1L) * blockSize));

                //全ての円周率ファイルの先頭から、そのファイルの前のファイルの末尾に付加する桁数だけ切り出す
                value.put(FileInfo.FIRST_DATA, YCDFileUtil.getFirstData(f.getPath(), targetLength));

                fileMap.put(f, value);

            }

            //ファイルがきちんと並んでいるかチェック
            Long firstDigit = Long.valueOf(fileMap.get(fileList.get(0)).get(FileInfo.FIRST_DIGIT));
            if (!firstDigit.equals(1L)) {
                throw new RuntimeException("Illegal first digit in file : " + fileList.get(0).getName() + " is " + firstDigit);
            }

            Integer tmpIndex = Integer.valueOf(fileMap.get(fileList.get(0)).get(FileInfo.BLOCK_INDEX));
            if (!tmpIndex.equals(0)) {
                throw new RuntimeException("Illegal first block id : " + fileList.get(0).getName() + " is " + tmpIndex);
            }

            //ファイルの順序と桁連結チェック
            //先頭ファイルの末尾桁番号を取得
            Integer previndex = tmpIndex;
            Long prevLastDigit = Long.valueOf(fileMap.get(fileList.get(0)).get(FileInfo.END_DIGIT));
            for (File fi : fileMap.keySet()) {
                Map<FileInfo, String> m = fileMap.get(fi);

                //最初のファイルはスキップ
                if (Long.valueOf(m.get(FileInfo.FIRST_DIGIT)).equals(1L)) {
                    continue;
                }

                //このファイルの先頭桁番号取得
                //前のファイルのラスト桁番号 +1 がこのファイルの先頭桁番号でなければならない
                Long thisStart = Long.valueOf(m.get(FileInfo.FIRST_DIGIT));
                if (!thisStart.equals(prevLastDigit + 1L)) {
                    throw new RuntimeException("Illegal start digit in file : " + fi.getName() + " - is Start : " + thisStart);
                }

                //このファイルのファイルインデックス取得
                //前のファイルのインデックス +1 がこのファイルのインデックスでなければならない
                Integer thisIndex = Integer.valueOf(m.get(FileInfo.BLOCK_INDEX));
                if (!thisIndex.equals(previndex + 1)) {
                    throw new RuntimeException("Illegal index in file : " + fi.getName() + " - is index : " + thisIndex);
                }

                //このファイルのインデックスと最後尾桁番号を次の比較用に保持
                previndex = Integer.valueOf(m.get(FileInfo.BLOCK_INDEX));
                prevLastDigit = Long.valueOf(m.get(FileInfo.END_DIGIT));

            }

            return fileMap;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
