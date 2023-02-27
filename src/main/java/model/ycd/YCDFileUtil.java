package model.ycd;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class YCDFileUtil {

    /**
     * YCDファイルのヘッダー部分のサイズを返す.
     * <p>
     * YCDファイル先頭からここで得られたバイト数の次のバイトからが実データとなる。
     *
     * @return ファイルのヘッダー部分のサイズ(Byte)
     * @throws IOException ファイルアクセスエラー
     */
    public static Integer getHeaderSize(String ycdFileName) throws IOException {
        //最初の CRLFを探し、それに 1を加えた
        Integer headerSize = -1;
        try (final FileInputStream fi = new FileInputStream(ycdFileName);
             BufferedInputStream inputStream = new BufferedInputStream(fi);) {

            byte[] charArr = new byte[300];
            int check = inputStream.read(charArr);
            String header = new String(charArr);

            final String CRLF = "" + (char) 0x0D + (char) 0x0A;
            headerSize = header.lastIndexOf(CRLF) + CRLF.length() - 1;

            //CRLFの次になんか、最後に1バイトついてるのでそれを加える。
            headerSize++;

        } catch (IOException e) {
            throw e;
        }

        return headerSize;
    }

    /**
     * YCDファイルのファイルサイズを返す.
     *
     * @return ファイルサイズ(Byte)
     * @throws IOException
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
     * @return ヘッダー情報
     * @throws IOException
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
     * @param number
     * @return 文字列が正の整数として評価できる：trure できない:false
     */
    public static boolean isNumMatch(String number) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[0-9]*$");
        java.util.regex.Matcher matcher = pattern.matcher(number);
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

    //ファイルリストの最終桁を返す
    public static Long getMaxDepth(List<File> fileList) throws IOException {

        Long maxDepth = 0L;
        for(File f : fileList){
            Map<YCDHeaderInfoElem, String> fileInfoMap = getYCDHeader(f.getPath());

            Integer blockID = Integer.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_ID));
            Long blockSize = Long.valueOf(fileInfoMap.get(YCDHeaderInfoElem.BLOCK_SIZE));
            maxDepth = maxDepth + ((blockID + 1L) * blockSize);
        }

        return maxDepth;
    }




}
