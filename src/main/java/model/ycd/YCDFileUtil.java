package model.ycd;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class YCDFileUtil {

    /**
     * ファイルヘッダー情報のそれぞれのラベル
     */
    public enum FileInfo {
        BLOCK_INDEX,
        FIRST_DATA,
        FIRST_DIGIT,
        END_DIGIT,
        FILE_SIZE,
        ;
    }

    /**
     * YCDファイルのヘッダー部分のサイズを返す.
     * <p>
     * YCDファイル先頭からここで得られたバイト数の次のバイトからが実データとなる。
     *
     * @param ycdFileName YCDファイル名
     * @return ファイルのヘッダー部分のサイズ(Byte)
     * @throws IOException ファイルアクセスエラー
     */
    public static Integer getHeaderSize(String ycdFileName) throws IOException {
        //最初の CRLFを探し、それに 1を加えた
        Integer headerSize = -1;
        try (final FileInputStream fi = new FileInputStream(ycdFileName);
             BufferedInputStream inputStream = new BufferedInputStream(fi);) {

            byte[] charArr = new byte[300];
            inputStream.read(charArr);
            String header = new String(charArr);

            final String CRLF = "" + (char) 0x0D + (char) 0x0A;
            headerSize = header.lastIndexOf(CRLF) + CRLF.length() - 1;

            //CRLFの次になんか、よくわからないが最後に1バイトついてるので、その分の「１」加える。
            headerSize++;

        } catch (IOException e) {
            throw e;
        }

        return headerSize;
    }

    /**
     * YCDファイルのファイルサイズを返す.
     *
     * @param fileName YCDファイル名
     * @return ファイルサイズ(Byte)
     * @throws FileNotFoundException 指定ファイルが存在しない場合
     */
    public static Long getFileSize(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        if (!f.isFile()) {
            throw new FileNotFoundException(fileName);
        }
        return f.length();
    }

    /**
     * YCDファイルのヘッダーを読み込んで返す.
     *
     * @param fileName YCDファイル名
     * @return ヘッダー情報
     * @throws IOException ファイル読み込みエラー
     */
    public static Map<YCDHeaderInfoElem, String> getYCDHeader(String fileName) throws IOException {

        Map<YCDHeaderInfoElem, String> map = new TreeMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line;
            while ((line = br.readLine()) != null) {

                if ("EndHeader".equals(line)) {
                    break;
                }

                if ("".equals(line)) {
                    continue;
                }

                String[] tmp = line.split(":");
                if (2 != tmp.length) {
                    continue;
                }

                if (tmp[0].trim().equals(YCDHeaderInfoElem.FILE_VERSION.toString())) {
                    map.put(YCDHeaderInfoElem.FILE_VERSION, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.BASE.toString())) {
                    map.put(YCDHeaderInfoElem.BASE, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.FIRST_DIGITS.toString())) {
                    map.put(YCDHeaderInfoElem.FIRST_DIGITS, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.TOTAL_DIGITS.toString())) {
                    map.put(YCDHeaderInfoElem.TOTAL_DIGITS, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.BLOCK_SIZE.toString())) {
                    map.put(YCDHeaderInfoElem.BLOCK_SIZE, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.BLOCK_ID.toString())) {
                    map.put(YCDHeaderInfoElem.BLOCK_ID, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.BASE.toString())) {
                    map.put(YCDHeaderInfoElem.BASE, tmp[1].trim());
                } else if (tmp[0].trim().equals(YCDHeaderInfoElem.BASE.toString())) {
                    map.put(YCDHeaderInfoElem.BASE, tmp[1].trim());
                }
            }
        }
        if (!(map.containsKey(YCDHeaderInfoElem.FILE_VERSION))
                && (!map.containsKey(YCDHeaderInfoElem.BASE))
                && (!map.containsKey(YCDHeaderInfoElem.FIRST_DIGITS))
                && (!map.containsKey(YCDHeaderInfoElem.TOTAL_DIGITS))
                && (!map.containsKey(YCDHeaderInfoElem.BLOCK_SIZE))
                && (!map.containsKey(YCDHeaderInfoElem.BLOCK_ID))
        ) {
            throw new IOException("ファイルヘッダーが不正です : " + fileName + "   " + map);
        }

        return map;
    }

    /**
     * 文字列が半角数字のみの文字列かどうかチェックする（マイナス記号は対象外）
     *
     * @param numberStr チェック対象数値の文字列
     * @return 文字列が正の整数として評価できる：trure できない:false
     */
    public static boolean isNumMatch(String numberStr) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[0-9]*$");
        java.util.regex.Matcher matcher = pattern.matcher(numberStr);
        return matcher.matches();
    }

    //指定ファイルの、指定桁数だけファイル先頭から読み込んで返す
    public static String getFirstData(String fileName, Integer disitCount) throws IOException {

        if (0 > disitCount) {
            throw new RuntimeException("ファイルの両端桁取得桁数はゼロ以上を指定してください: " + disitCount);
        }

        //ヘッダー読み飛ばすため、ヘッダーサイズ取得
        Integer headerSize = YCDFileUtil.getHeaderSize(fileName);

        //ブロック数算出
        Integer readBlockSize = (disitCount / 19) + 1;

        //読み込む全桁(19×ブロックまとめ数)文字列。
        StringBuilder allNum = new StringBuilder();

        //対象ファイルアサイン
        try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(fileName))) {
            //ヘッダー読み飛ばし
            fileStream.skip(headerSize + 1);


            for (Integer i = 0; i < readBlockSize; i++) {

                //ブロックサイズ × 8バイト読む。 64-bit(8 byte)に19桁の整数値で１ブロック。リトルエンディアン。
                // Base 10 .ycd files are stored as 64-bit integers words with 19 digits per word.
                // In both cases, each 8-byte word is little-endian.
                byte[] readBuffer = new byte[8];  //8バイト × ブロックまとめ数
                Integer readByteCount = fileStream.read(readBuffer);
                if (readByteCount != readBuffer.length) {
                    throw new IOException("正しく読み込めませんでした。 : " + fileName
                            + " 8バイトであるべき読み込みデータ長さ:" + readByteCount);
                }

                //1ブロック(8バイト、19桁)づつ、ブロックまとめ数分読む
                byte[] buff = Arrays.copyOfRange(readBuffer, 0, 8); //8バイト切り出す
                String numStr = Long.toUnsignedString(Long.reverseBytes(ByteBuffer.wrap(buff).getLong()));

                //先頭が０の場合はゼロの分だけ切られてしまうので、19桁になるように左ゼロ埋めする。
                if (19 > numStr.length()) {
                    numStr = String.format("%19s", numStr).replace(' ', '0');
                }

                //作成した文字列を積む
                allNum.append(numStr);
            }

        }

        return allNum.toString().substring(0, disitCount);

    }

    //ファイルリストの総桁数を返す
    public static Long getAllDigitsCount(List<File> fileList) throws IOException {

        Long maxDigit = 0L;
        for(File f : fileList){
            Map<YCDHeaderInfoElem, String> fileInfoMap = getYCDHeader(f.getPath());
            Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));
            maxDigit = maxDigit + blockSize;
        }

        return maxDigit;
    }


    //YCDファイルリストから、ファイル情報を構築する。オーバーラップさせる桁数を指定し、先頭桁も取得する
    public static Map<File, Map<YCDFileUtil.FileInfo, String>> createFileInfo(List<File> fileList, Integer overwrapLength) {

        //対象ファイル全ての情報事前取得
        try {

            Map<File, Map<YCDFileUtil.FileInfo, String>> fileMap = new LinkedHashMap<>();

            //全てのファイルが対象(小さい順に並んでいること)
            for (File f : fileList) {

                Map<YCDFileUtil.FileInfo, String> value = new HashMap<>();

                //ファイル情報取得
                Map<YCDHeaderInfoElem, String> fileInfoMap = YCDFileUtil.getYCDHeader(f.getPath());

                //ブロックID（ブロックIndexということにする）
                Integer blockID = Integer.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_ID));
                value.put(YCDFileUtil.FileInfo.BLOCK_INDEX, String.valueOf(blockID));

                //ブロックサイズ（そのファイルに格納されている総桁数）
                Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));

                //先頭桁と最終桁の取得
                value.put(YCDFileUtil.FileInfo.FIRST_DIGIT, String.valueOf((blockID * blockSize) + 1L));
                value.put(YCDFileUtil.FileInfo.END_DIGIT, String.valueOf((blockID + 1L) * blockSize));

                //ファイルサイズ(Byte)
                value.put(YCDFileUtil.FileInfo.FILE_SIZE, String.valueOf(YCDFileUtil.getFileSize(f.getPath())));

                //全ての円周率ファイルの先頭から、そのファイルの前のファイルの末尾に付加する桁数だけ切り出す
                value.put(YCDFileUtil.FileInfo.FIRST_DATA, YCDFileUtil.getFirstData(f.getPath(), overwrapLength));

                fileMap.put(f, value);

            }

            //ファイルがきちんと並んでいるかチェック
            Long firstDigit = Long.valueOf(fileMap.get(fileList.get(0)).get(YCDFileUtil.FileInfo.FIRST_DIGIT));
            if (!firstDigit.equals(1L)) {
                throw new RuntimeException("Illegal first digit in file : " + fileList.get(0).getName() + " is " + firstDigit);
            }

            Integer tmpIndex = Integer.valueOf(fileMap.get(fileList.get(0)).get(YCDFileUtil.FileInfo.BLOCK_INDEX));
            if (!tmpIndex.equals(0)) {
                throw new RuntimeException("Illegal first block id : " + fileList.get(0).getName() + " is " + tmpIndex);
            }

            //ファイルの順序と桁連結チェック
            //先頭ファイルの末尾桁番号を取得
            Integer previndex = tmpIndex;
            Long prevLastDigit = Long.valueOf(fileMap.get(fileList.get(0)).get(YCDFileUtil.FileInfo.END_DIGIT));
            for (File fi : fileMap.keySet()) {
                Map<YCDFileUtil.FileInfo, String> m = fileMap.get(fi);

                //最初のファイルはスキップ
                if (Long.valueOf(m.get(YCDFileUtil.FileInfo.FIRST_DIGIT)).equals(1L)) {
                    continue;
                }

                //このファイルの先頭桁番号取得
                //前のファイルのラスト桁番号 +1 がこのファイルの先頭桁番号でなければならない
                Long thisStart = Long.valueOf(m.get(YCDFileUtil.FileInfo.FIRST_DIGIT));
                if (!thisStart.equals(prevLastDigit + 1L)) {
                    throw new RuntimeException("Illegal start digit in file : " + fi.getName() + " - is Start : " + thisStart);
                }

                //このファイルのファイルインデックス取得
                //前のファイルのインデックス +1 がこのファイルのインデックスでなければならない
                Integer thisIndex = Integer.valueOf(m.get(YCDFileUtil.FileInfo.BLOCK_INDEX));
                if (!thisIndex.equals(previndex + 1)) {
                    throw new RuntimeException("Illegal index in file : " + fi.getName() + " - is index : " + thisIndex);
                }

                //このファイルのインデックスと最後尾桁番号を次の比較用に保持
                previndex = Integer.valueOf(m.get(YCDFileUtil.FileInfo.BLOCK_INDEX));
                prevLastDigit = Long.valueOf(m.get(YCDFileUtil.FileInfo.END_DIGIT));

            }

            return fileMap;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
