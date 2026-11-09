package model.ycd;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * シーケンシャルアクセスYCDファイル.
 *
 * <p>
 * YCDファイル（10進）は、19桁を 8 バイト（64bit 符号なし整数、リトルエンディアン）に格納したブロックの連続で、
 * このクラスはそれを指定桁数（ユニット）ずつ文字列として順に提供する。
 * </p>
 *
 * <p>
 * 実装方針（省メモリ・高速）：
 * ユニット1つ分のバイト列をまとめて読み、固定の char 配列に 19 桁ずつ直接書き出す。
 * 作業メモリはユニット1つ分（バイト配列＋文字配列）のみで、ユニット桁数に関わらず一定。
 * ユニット桁数が 19 の倍数でないときの余りは、文字配列の先頭に繰り越して次のユニットに付加する。
 * </p>
 */
public class YCD_SeqBlockStream implements AutoCloseable {

    /** 1ブロックの桁数（10進 YCD は 64bit に 19 桁） */
    private static final int DIGITS_PER_BLOCK = 19;

    /** 1ブロックのバイト数 */
    private static final int BYTES_PER_BLOCK = 8;

    /**
     * 1回の検索処理で処理する桁数（ユニット桁数）
     */
    private final int processUnitSize;

    /**
     * 対象ファイルパス
     */
    private final String filePath;

    /**
     * 対象ファイル読み込み用ストリーム.
     */
    private DataInputStream fileStream = null;

    /**
     * 対象ファイルのスタート桁数
     */
    private final long digit_Start;

    /**
     * 対象ファイルの桁数
     */
    private final long digit_Length;

    /**
     * 対象ファイルの総ブロック数（切り上げ）
     */
    private final long totalBlocks;

    /**
     * 読み込み済みブロック数
     */
    private long readBlocks = 0L;

    /**
     * ブロック読み込み用バイトバッファ（ユニット1つ分）
     */
    private final byte[] byteBuffer;

    /**
     * 変換後文字バッファ（繰越分＋ユニット1つ分）
     */
    private final char[] charBuffer;

    /**
     * 文字バッファ先頭にある、前回の余り（繰越）文字数
     */
    private int carryLength = 0;

    /**
     * 次に返すユニットの開始桁
     */
    private long nextStartDigit;

    /**
     * 返したユニットの通し番号
     */
    private long processNo = 0L;

    private boolean isClosed = true;

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

    /**
     * 読み込み済みブロック数（1ブロック＝19桁）を返す.
     */
    public Long getCurrentReadBlockSeq() {
        return this.readBlocks;
    }

    /**
     * YCDファイルのシーケンシャルアクセスを構築する.
     *
     * @param fileName 対象YCDファイル名（フルパス).
     * @param unitSize １プロセスの処理桁数（19以上）. 0以下の場合は 19000.
     * @throws IOException ファイル読み込みエラー
     */
    public YCD_SeqBlockStream(String fileName, Integer unitSize) throws IOException {

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
        this.digit_Length = Long.parseLong(map.get(YCDHeaderInfoElem.BLOCK_SIZE));
        this.digit_Start = 1 + this.digit_Length * Long.parseLong(map.get(YCDHeaderInfoElem.BLOCK_ID));
        this.totalBlocks = (this.digit_Length + DIGITS_PER_BLOCK - 1) / DIGITS_PER_BLOCK;

        //検索１プロセスのユニットの桁数
        if (null != unitSize && 0 < unitSize) {
            this.processUnitSize = unitSize;
        } else {
            this.processUnitSize = 19000;
        }
        if (DIGITS_PER_BLOCK > this.processUnitSize) {
            throw new IllegalArgumentException("ユニット桁数は19以上を指定してください : " + this.getProcessUnitSize());
        }

        //バッファ確保（ユニット1つ分。繰越は最大18桁）
        int blocksPerUnit = (this.processUnitSize + DIGITS_PER_BLOCK - 1) / DIGITS_PER_BLOCK;
        this.byteBuffer = new byte[blocksPerUnit * BYTES_PER_BLOCK];
        this.charBuffer = new char[(DIGITS_PER_BLOCK - 1) + blocksPerUnit * DIGITS_PER_BLOCK];

        //読み込み用ファイルのオープン。
        //（Closeも必要だが、AutoCloseableによるtry-with-resourcesが便利)
        this.open();

        //最初のユニットの開始桁はファイルの先頭桁
        this.nextStartDigit = this.digit_Start;
    }

    /**
     * ファイルを開く.
     *
     * @throws IOException ファイル読み込みエラー
     */
    private void open() throws IOException {

        //ヘッダー読み飛ばすため、ヘッダーサイズ取得
        int headerSize = YCDFileUtil.getHeaderSize(this.filePath);

        //対象ファイルアサイン
        this.fileStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath), 1 << 16));

        //ヘッダー読み飛ばし
        int toSkip = headerSize + 1;
        while (0 < toSkip) {
            int skipped = this.fileStream.skipBytes(toSkip);
            if (skipped <= 0) {
                throw new IOException("ヘッダーを読み飛ばせませんでした : " + this.filePath);
            }
            toSkip -= skipped;
        }

        this.isClosed = false;
    }

    /**
     * ファイルを閉じる.
     *
     * @throws IOException ファイルクローズエラー
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
     * @throws  IOException ファイル読み込みエラー
     */
    public YCDProcessUnit next() throws IOException {

        if (!this.hasNext()) {
            throw new IOException("終わりを超えて読み込もうとしました : " + this.filePath
                    + " 読み込み済みブロック:" + this.readBlocks);
        }
        if (null == this.fileStream || this.isClosed) {
            throw new IOException("ファイルがオープンされていません : " + this.filePath);
        }

        // 繰越分で足りない桁数を埋めるのに必要なブロック数（ファイルに残っている分まで）
        int needDigits = this.processUnitSize - this.carryLength;
        long needBlocks = (needDigits <= 0) ? 0 : (needDigits + DIGITS_PER_BLOCK - 1) / DIGITS_PER_BLOCK;
        int blocks = (int) Math.min(needBlocks, this.totalBlocks - this.readBlocks);

        // まとめて読む
        this.fileStream.readFully(this.byteBuffer, 0, blocks * BYTES_PER_BLOCK);

        // 繰越分の後ろに 19 桁ずつ変換して書き出す
        int total = this.carryLength;
        for (int b = 0; b < blocks; b++) {
            decodeBlock(this.byteBuffer, b * BYTES_PER_BLOCK, this.charBuffer, total);
            total += DIGITS_PER_BLOCK;
        }
        this.readBlocks += blocks;

        // ファイル末尾のブロックは有効桁を超えて読んでいるので、最後のブロックを読んだこの回に限り超過分を切り捨てる
        // （繰越だけで作る最終ユニットの回に再度切り捨ててはいけない）
        if (0 < blocks && this.readBlocks == this.totalBlocks) {
            long decodedDigits = this.totalBlocks * DIGITS_PER_BLOCK;
            total -= (int) (decodedDigits - this.digit_Length);
        }

        // ユニット分を切り出し、余りは先頭に繰り越す
        int unitLength = Math.min(this.processUnitSize, total);
        String data = new String(this.charBuffer, 0, unitLength);
        this.carryLength = total - unitLength;
        System.arraycopy(this.charBuffer, unitLength, this.charBuffer, 0, this.carryLength);

        // ユニット情報（開始桁はユニット桁数ずつ進む）
        long startDigit = this.nextStartDigit;
        this.nextStartDigit += this.processUnitSize;
        this.processNo++;

        return new YCDProcessUnit(this.processNo, startDigit, data);
    }

    /**
     * 次のプロセスユニットがあるかどうか（まだ次に読み込むべきものがあるかどうか）を返す.
     *
     * @return まだある:True, もうない:False
     */
    public Boolean hasNext() {
        // まだ読めるブロックがあるか、余りが残っていれば次あり
        return this.readBlocks < this.totalBlocks || 0 < this.carryLength;
    }

    /**
     * 1ブロック（8バイト、リトルエンディアン、符号なし64bit）を 19 桁の文字に変換して書き出す.
     *
     * @param src    バイト列
     * @param srcPos ブロック先頭位置
     * @param dst    書き出し先
     * @param dstPos 書き出し先頭位置（ここから 19 文字書く）
     */
    static void decodeBlock(byte[] src, int srcPos, char[] dst, int dstPos) {
        long v = (src[srcPos] & 0xFFL)
                | (src[srcPos + 1] & 0xFFL) << 8
                | (src[srcPos + 2] & 0xFFL) << 16
                | (src[srcPos + 3] & 0xFFL) << 24
                | (src[srcPos + 4] & 0xFFL) << 32
                | (src[srcPos + 5] & 0xFFL) << 40
                | (src[srcPos + 6] & 0xFFL) << 48
                | (src[srcPos + 7] & 0xFFL) << 56;

        int i = dstPos + DIGITS_PER_BLOCK - 1;

        // 符号なしで 2^63 以上の値は、最初の1桁だけ符号なし演算で処理すれば以降は正の値になる
        if (v < 0) {
            dst[i--] = (char) ('0' + Long.remainderUnsigned(v, 10L));
            v = Long.divideUnsigned(v, 10L);
        }
        while (dstPos <= i) {
            dst[i--] = (char) ('0' + (v % 10L));
            v /= 10L;
        }
    }

}
