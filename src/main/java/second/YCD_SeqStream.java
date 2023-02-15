package second;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * シーケンシャルアクセスYCDファイル
 */
public class YCD_SeqStream implements AutoCloseable{

    /**
     * 1回の検索処理で処理する桁数（ユニット桁数）
     */
    private Integer processUnitSize;

    /**
     * 対象ファイルパス
     */
    final private String filePath;

    /**
     * 対象ファイル読み込み用ストリーム.
     * OSのネイティブ呼び出しを少なくしてファイルから読み込む。
     */
    private BufferedInputStream fileStream = null;

    /**
     * 対象ファイルのスタート桁数
     */
    final private Long digit_Start;

    /**
     * 対象ファイルの桁数
     */
    final private Long digit_Length;

    /**
     * カレントプロセスユニット.
     *
     * next() で次のプロセスユニットを読み込んでカレントが遷移する.
     */
    private YCDProcessUnit currentProcessUnit = null; //現在

    /**
     * カレントプロセスユニットを読み込んだ時の余り文字列保持.
     */
    public String surplusDigitStr = "";

    /**
     * ブロックごと読み込み用の現在ブロック保持.
     */
    private Long currentBlock = 1L;


    private Boolean isClosed = true;
    public Boolean isClosed() {
        return this.isClosed;
    }

    public Integer getProcessUnitSize() {
        return this.processUnitSize;
    }
    public String getYCDFilePath() {
        return this.filePath;
    }

    public Long getDigit_Start() {
        return this.digit_Start;
    }
    public Long getDigit_Length() {
        return this.digit_Length;
    }

    private Long currentReadBlockSeq = 0L; //１ベース
    public Long getCurrentReadBlockSeq() {
        return this.currentReadBlockSeq;
    }

    /**
     * YCDファイルのシーケンシャルアクセスを構築する.
     *
     * @param fileName 対象YCDファイルパス.
     * @param unitSize １プロセスの処理桁数.
     */
    public YCD_SeqStream(String fileName, Integer unitSize) throws IOException {

        //用語定義
        // 　リードブロック　19桁づつ保存された8バイトのデータ。これがシーケンシャルにファイルに保存されている。
        // 　ユニット　１プロセスで処理するために整える指定される桁数
        // 　Digit 桁数

        //対象ファイル
        this.filePath = fileName;
        if (Files.notExists(Paths.get(filePath))) {
            throw new IOException("ファイルが存在しません:" + filePath);
        }

        //このファイルのスタート桁数と長さ取得
        Map<YCDHeaderInfoElem, String> map = YCDFileUtil.getYCDHeader(this.filePath);
        this.digit_Length = Long.valueOf(map.get(YCDHeaderInfoElem.BLOCK_SIZE));

        this.digit_Start = 1 + this.digit_Length * Integer.valueOf(map.get(YCDHeaderInfoElem.BLOCK_ID));

        //検索１プロセスのユニットの桁数
        if(0 < unitSize){
            this.processUnitSize = unitSize;
        }else{
            this.processUnitSize = 19000; //19で割り切れるから早いかも
        }
        if (19 > this.processUnitSize) {
            throw new IllegalArgumentException("ユニット桁数は19以上を指定してください : " + this.getProcessUnitSize());
        }


        //ブロックを複数読み込んで、ユニットを作った時の余りを保持する。
        // ユニットが19の倍数でないとき、19桁ずつ読むので余るので、その余りを保持。次のユニットの先頭に付加する。
        this.surplusDigitStr = "";

        //読み込み用ファイルのオープン。
        //（Closeも必要だが、AutoCloseableによるtry-with-resourcesが便利)
        this.open();

        //カレントブロックを最初のブロックにセット
        this.currentBlock = 1L;

        //最初の１プロセスの前に、仮想的に１回読んだことにする。スタート位置 ＝ ファイル先頭 - ユニットサイズ
        //このことにより、１回目の読み込みの際に next() するとちょうどよくなる。
        this.currentProcessUnit = new YCDProcessUnit(0L, this.digit_Start - this.processUnitSize, "");

    }

    /**
     * ファイルを開く.
     *
     * @throws IOException
     */
    private void open() throws IOException {
        try {

            //ヘッダー読み飛ばすため、ヘッダーサイズ取得
            Integer headerSize = YCDFileUtil.getHeaderSize(this.filePath);

            //対象ファイルアサイン
            FileInputStream fi = new FileInputStream(filePath);
            this.fileStream = new BufferedInputStream((InputStream) fi);

            //ヘッダー読み飛ばし
            this.fileStream.skip(headerSize+1);

            this.isClosed = false;

        } catch (IOException ex) {
            throw ex;
        }
    }

    /**
     * ファイルを閉じる.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

        if (this.isClosed) {
            throw new IOException("閉じられています : " + this.filePath);
        }

        if (null == this.fileStream) {
            throw new IOException("ファイルが設定されていません" + this.filePath);
        }

        this.fileStream.close();
        this.isClosed = true;

    }

    /**
     * 次のプロセスユニットを取得して返す.
     * <p>
     * プロセスユニットは、指定されたユニットサイズ分の長さのデータが格納される。
     * 読み終わり時にプロセスユニット桁数に満たない桁しか読めなかった場合は短い桁数が格納されたプロセスユニットとなる。
     *
     * @return 次のプロセスユニット
     */
    public YCDProcessUnit next() throws IOException {

        if (!this.hasNext()) {
            throw new IOException("終わりを超えて読み込もうとしました : " + this.filePath + " " + this.currentProcessUnit);
        }

        //ブロック読み込み用バッファの初期化。初期値は前回の余りをセットしておく
        StringBuffer sb = new StringBuffer(this.surplusDigitStr);

        String data = "";
        while (true) {

            //読んでいて、プロセスユニット桁数に達する前にファイルが終わった場合、桁数足りないままで読み終わりとして、中途半端なプロセスユニットのまま返す。
            if (!this.hasNextReadBlock()) {

                //短いが、そのまま格納する。
                data = sb.toString();

                //余りは、なし。
                this.surplusDigitStr = "";

                break;
            }

            //YCDから１ブロック読む。(YCDファイルは10進で19桁で１ブロック) ＝ 19文字読む
            sb.append(nextBlockRead());

            //プロセスユニット桁数に達したら終わり
            if (this.processUnitSize < sb.toString().length()) {

                //プロセスユニットで必要な桁数を切り出す
                data = sb.toString().substring(0, this.processUnitSize);

                //余った文字列を保存
                this.surplusDigitStr = sb.toString().substring(this.processUnitSize);

                break;
            }

        }

        // 現在のプロセスユニットを、次のプロセスユニットで上書き更新し、新しいプロセスユニットを返す
        // （最初はスタート位置 -1 にあるため、同じロジックで最初の１回も処理する）
        Long start = this.currentProcessUnit.getStartDigit() + this.processUnitSize;
        this.currentProcessUnit.setProcessNo(this.currentProcessUnit.getProcessNo() + 1);
        this.currentProcessUnit.setStartDigit(start);
        this.currentProcessUnit.setValue(data);

        return this.currentProcessUnit;
    }


    /**
     * 次のプロセスユニットがあるかどうか（まだ次に読み込むべきものがあるかどうか）を返す.
     *
     * @return まだある:True, もうない:False
     */
    public Boolean hasNext() {

        if (this.hasNextReadBlock()) {
            return true; //まだ読めるなら次あり
        } else {
            if (!"".equals(this.surplusDigitStr)) {
                return true; //もう次に読むものがなくても余りがあれば次ありとする
            } else {
                return false; //もう次に読むものがなくて、あまりもない
            }
        }
    }

    /**
     * 次のブロックを読み込めるかを返す.
     *
     * @return 読み込める:True, 読み込めない:False
     */
    private Boolean hasNextReadBlock() {
        return this.digit_Length + 19 >= this.currentBlock * 19;
    }




    /**
     * 次のブロックを読んで返す.
     *
     * @return 次のブロック
     * @throws IOException
     */
    private String nextBlockRead() throws IOException {

        //ファイルがオープンされているかチェック
        if (null == this.fileStream) {
            throw new IOException("ファイルがオープンされていません : " + this.filePath);
        }

        //まだ読めるかチェック
        if (!this.hasNextReadBlock()) {
            throw new IOException("有効桁以上を読み込もうとしました : " + this.filePath
                    + " 読み込もうとした先頭桁:" + this.currentBlock * 19
                    + " Block:" + this.currentBlock);
        }

        //ブロックサイズ × 8バイト読む。 64-bit(8 byte)に19桁の整数値で１ブロック。リトルエンディアン。
        // Base 10 .ycd files are stored as 64-bit integers words with 19 digits per word.
        // In both cases, each 8-byte word is little-endian.
        byte[] readBuffer = new byte[8];  //8バイト × ブロックまとめ数
        Integer readByteCount = this.fileStream.read(readBuffer);

        if (readByteCount != readBuffer.length) {
            throw new IOException("正しく読み込めませんでした。 : " + this.filePath
                    + " 読み込もうとした先頭桁:" + this.currentBlock * 19
                    + " Block:" + this.currentBlock
                    + " 8バイトであるべき読み込みデータ長さ:" + readByteCount);
        }

        //リードブロックインデックスインクリメント
        this.currentReadBlockSeq++;

        //1ブロック(8バイト、19桁)づつ、ブロックまとめ数分読む
        byte[] buff = Arrays.copyOfRange(readBuffer, 0, 8); //8バイト切り出す
        String numStr = Long.toUnsignedString(Long.reverseBytes(ByteBuffer.wrap(buff).getLong()));

        //先頭が０の場合はゼロの分だけ切られてしまうので、19桁になるように左ゼロ埋めする。
        if (19 > numStr.length()) {
            numStr = String.format("%19s", numStr).replace(' ', '0');
        }

        //8バイト（19桁）読むと、ファイルの有効データ末端を超えて読むため、このファイルの有効桁より後の桁は切る
        Boolean isOver = false;
        Long readEndDigit = this.currentBlock * 19;
        if (readEndDigit > this.digit_Length) {
            //このファイルに格納されている桁数よりオーバーして読み込んだ場合はオーバー分を切り捨てる
            Long over = readEndDigit - this.digit_Length;
            numStr = numStr.substring(0, (int) (19 - over));  //左を残し、右からオーバー分を切り捨てる
            isOver = true;
        }

        //カレントブロックを一つ進める
        this.currentBlock++;

        return numStr;
    }

}
