package model.ycd;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YCD_SeqProvider implements AutoCloseable {

    @Override
    public void close() throws Exception {

        if (null != this.currentStream) {
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

        private Map<FileInfo, String> fileInfo;

        public Map<FileInfo, String> getFileInfo() {
            return this.fileInfo;
        }


        private Unit(Map<FileInfo, String> fileInfo, Long startDigit, String data) {
            this.fileInfo = fileInfo;
            this.startDigit = startDigit;
            this.data = data;
        }

    }

    private File currentFile = null;
    private File lastFile = null;

    private YCD_SeqBlockStream currentStream = null;

    String prevUnitData = "";
    Long unitStartPoint = 1L;


    public YCD_SeqProvider(List<File> fileList, Integer targetLength, Integer unitLength) throws IOException {

        this.targetLength = targetLength;
        this.unitLnegth = unitLength;

        //全ファイルヘッダー情報取得
        this.fileInfoMap = createFileInfo(fileList, targetLength);

        //カレントファイルを先頭ファイルにする
        this.currentFile = fileList.get(0);

        //終了判定用に最後のファイルを保持しておく
        this.lastFile = fileList.get(fileList.size() - 1);

        //初めての呼び出し
        this.currentStream = this.createStream(this.currentFile, this.unitLnegth);

    }

    private YCD_SeqBlockStream createStream(File file, Integer unitLength) throws IOException {
        return new YCD_SeqBlockStream(file.getPath(), unitLength);
    }

    public Boolean hasNext() {

        //カレントストリームにまだ次があれば「次あり」
        if (this.currentStream.hasNext()) {
            return true;
        }
        //カレントストリームに次がなくても、次のファイルがあれば「次あり」
        if (!this.currentFile.getName().equals(this.lastFile.getName())) {
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
            if (!findNextFile) {
                this.currentFile = null;
            }

            //新しいファイルに映ったときは、ひとつ前データをクリアする
            //（境目については前のファイルのケツ部分で解決済み）
            this.prevUnitData = "";

        }

        //カレントストリームを次に進めて
        YCDProcessUnit pdu = this.currentStream.next();

        //データを得る。そのとき、一つ前のケツの文字を先頭に挿入して読み込みユニット間の検索漏れを防ぐ
        String thisLine = new StringBuffer(this.prevUnitData).append(pdu.getValue()).toString();

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

        Unit u = new Unit(this.fileInfoMap.get(this.currentFile), thisStartPoint, thisLine);
        return u;

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


    public static Map<File, Map<FileInfo, String>> createFileInfo(List<File> fileList, Integer targetLength) {

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
