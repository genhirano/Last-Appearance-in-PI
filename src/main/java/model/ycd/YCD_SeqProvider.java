package model.ycd;

import java.io.File;
import java.io.IOException;
import java.util.*;
import lombok.Getter;

import static model.ycd.YCDFileUtil.createFileInfo;

public class YCD_SeqProvider implements AutoCloseable, Iterable<YCD_SeqProvider.Unit>, Iterator<YCD_SeqProvider.Unit> {

    /**
     * 検索対象ファイルの情報保持Map.
     */
    @Getter
    private final Map<File, Map<YCDFileUtil.FileInfo, String>> fileInfoMap;

    private Integer overWrapLength;
    private Integer baseUnitLength;

    public class Unit {

        @Getter
        private final Long startDigit;

        @Getter
        private final String data;

        private Map<YCDFileUtil.FileInfo, String> fileInfo;

        public Map<YCDFileUtil.FileInfo, String> getFileInfo() {
            return this.fileInfo;
        }

        private Unit(Map<YCDFileUtil.FileInfo, String> fileInfo, Long startDigit, String data) {
            this.fileInfo = fileInfo;
            this.startDigit = startDigit;
            this.data = data;
        }

        public int indexOf(String targetString, int fromIndex) {
            return this.data.indexOf(targetString, fromIndex);
        }

    }

    private File currentFile;
    private File lastFile;
    private YCD_SeqBlockStream currentStream;

    String prevUnitData = "";
    Long unitStartPoint = 1L;

    /**
     * YCDから一定の桁数でデータを提供する
     * 
     * @param fileList       読み込み対象YCDファイルリスト。小さい順に並べてあること。
     * @param overWrapLength 重複して提供する桁長さ
     * @param unitLength     基本切り出し桁長さ
     * @throws IOException ファイル読み込みエラー
     */
    public YCD_SeqProvider(List<File> fileList, Integer overWrapLength, Integer unitLength) throws IOException {

        this.overWrapLength = overWrapLength;
        this.baseUnitLength = unitLength;

        // 全ファイルヘッダー情報取得
        this.fileInfoMap = Collections.unmodifiableMap(createFileInfo(fileList, overWrapLength));

        // カレントファイルを先頭ファイルにする
        this.currentFile = fileList.get(0);

        // 終了判定用に最後のファイルを保持しておく
        this.lastFile = fileList.get(fileList.size() - 1);

        // 初めての呼び出し
        this.currentStream = this.createStream(this.currentFile, this.baseUnitLength);

    }

    /**
     * 指定ファイルのストリームを新規作成する
     * 
     * @param ycdFile    対象のYCDファイル
     * @param unitLength ファイルの継ぎ目をオーバーラップさせる桁数
     * @return Unit
     * @throws IOException
     */
    private YCD_SeqBlockStream createStream(File ycdFile, Integer unitLength) throws IOException {
        System.out.println();
        System.out.println("Next file: " + this.currentFile.getName());
        return new YCD_SeqBlockStream(ycdFile.getPath(), unitLength);
    }

    @Override
    public boolean hasNext() {

        // カレントストリームにまだ次があれば「次あり」
        if (this.currentStream.hasNext()) {
            return true;
        }
        // カレントストリームに次がなくても、次のファイルがあれば「次あり」
        if (!this.currentFile.getName().equals(this.lastFile.getName())) {
            return true;
        }

        // カレントストリームに次がなく、かつ、次のファイルもない場合は「次なし」
        return false;

    }

    @Override
    public Unit next() {

        // すでにカレントストリーム末尾に達していたら次のファイルへ
        if (!this.currentStream.hasNext()) {
            this.nextFile();
        }

        try {
            // 現在ポジションの次のユニットを取得

            // カレントストリームを次に進めて
            YCDProcessUnit pdu = this.currentStream.next();

            // データを得る。そのとき、一つ前のケツの文字を先頭に挿入する。これで読み込みユニット間の検索漏れを防ぐ
            String thisLine = new StringBuffer(this.prevUnitData).append(pdu.getValue()).toString();

            // ファイル間重複提供処理
            // 読んだ後、ファイル末尾に達していたら次のファイルの先頭をもってきてここのケツにくっつける
            if (!this.currentStream.hasNext()) {
                Integer thisFileIndex = Integer
                        .valueOf(this.fileInfoMap.get(this.currentFile).get(YCDFileUtil.FileInfo.BLOCK_INDEX));
                for (File f2 : this.fileInfoMap.keySet()) {
                    Integer theFileIndex = Integer
                            .valueOf(this.fileInfoMap.get(f2).get(YCDFileUtil.FileInfo.BLOCK_INDEX));
                    if (theFileIndex.equals(thisFileIndex + 1)) {
                        thisLine = thisLine + this.fileInfoMap.get(f2).get(YCDFileUtil.FileInfo.FIRST_DATA);
                        break;
                    }
                }
            }

            // この次に読み込んだユニットの先頭に付与するデータとしてこのデータのケツのオーバーラップ分を保持しておく
            this.prevUnitData = thisLine.substring(thisLine.length() - this.overWrapLength);

            // ポジション更新
            Long thisStartPoint = this.unitStartPoint;
            this.unitStartPoint = this.unitStartPoint + (thisLine.length()) - (prevUnitData.length());

            // ユニットデータを作って返却
            return new Unit(this.fileInfoMap.get(this.currentFile), thisStartPoint, thisLine);

        } catch (IOException e) {
            throw new RuntimeException("YCD Data read error. " + this.currentFile.getPath(), e);
        }

    }

    /**
     * 次のファイルへ移行
     */
    private void nextFile() {

        // 次のファイルに移行する場合はまず、現在のストリームを閉じる
        try {
            this.close();
        } catch (Exception e) {
            throw new RuntimeException("CurrentStream close fail", e);
        }

        // 現在のファイルインデックス取得
        Integer thisFileIndex = Integer
                .valueOf(this.fileInfoMap.get(this.currentFile).get(YCDFileUtil.FileInfo.BLOCK_INDEX));

        Boolean findNextFile = false;
        for (File f2 : this.fileInfoMap.keySet()) {

            // 現在のファイルインデックスの次に出会ったらそれをカレントにする
            Integer theFileIndex = Integer.valueOf(this.fileInfoMap.get(f2).get(YCDFileUtil.FileInfo.BLOCK_INDEX));
            if (theFileIndex.equals(thisFileIndex + 1)) {
                this.currentFile = f2;
                try {
                    // 次のファイルのストリームを作成
                    this.currentStream = createStream(this.currentFile, this.baseUnitLength);
                } catch (IOException e) {
                    throw new RuntimeException(this.currentFile.getPath() + " Open file file ", e);
                }

                findNextFile = true;
                break;
            }
        }

        // 次のファイルがなかったら
        if (!findNextFile) {
            this.currentFile = null;
        }

        // 新しいファイルにカレントが移ったときは、ひとつ前データをクリアする
        // （境目について心配はない。前のファイルのケツ部分にこのファイルの先頭を付与して処理済みである）
        this.prevUnitData = "";

    }

    @Override
    public void remove() {
        // 削除はサポートしない
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws Exception {
        if (null != this.currentStream) {
            this.currentStream.close();
            this.currentStream = null;
        }
    }

    @Override
    public Iterator<Unit> iterator() {
        return this;
    }

}
