package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SurvivalList;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Searcher extends Thread {

    private final List<File> piFileList;
    @Getter
    private final List<File> um_piFileList;// 不変

    @Getter
    private final Integer listSize;

    @Getter
    private final Integer unitLength;

    @Getter
    private final Integer reportSpan;

    public Searcher(List<File> piFileList, Integer listSize, Integer unitLength, Integer reportSpan) {
        super();

        this.piFileList = piFileList;
        um_piFileList = Collections.unmodifiableList(piFileList); // 外部から参照されるための不変オブジェクト

        this.listSize = listSize;
        this.unitLength = unitLength;
        this.reportSpan = reportSpan;

    }

    @Override
    public void run() {

        Integer continueCount = 0;
        while (true) {

            if (5 < continueCount) {
                throw new RuntimeException("Fail!  Retry limit exceeded." + continueCount);
            }

            // 1サイクルの検索範囲の決定（サバイバルリストの範囲）
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(this.listSize);

            // 今回のサバイバルリストの作成
            // SurvivalList survivalList = new SurvivalList(targetRange.getLength(),
            // Integer.valueOf(targetRange.getStart()),
            // Integer.valueOf(targetRange.getEnd()));
            SurvivalList survivalSet = new SurvivalList(targetRange.getLength(),
                    Integer.valueOf(targetRange.getStart()), Integer.valueOf(targetRange.getEnd()));
            int survivalSetInitialSize = survivalSet.size();

            ZonedDateTime startTime = null;
            ZonedDateTime endTime = null;

            String lastData = "";
            Long lastFoundPos = -1L;

            // YCDデータプロバイダを作成
            int overWrapLength = targetRange.getLength(); // 一つ前のケツの部分を今回の先頭に重ねる桁の長さ
            try (YCD_SeqProvider p = new YCD_SeqProvider(piFileList, overWrapLength, unitLength);) {

                startTime = ZonedDateTime.now();

                // YCDプロバイダからパイユニットを順次取り出し
                for (YCD_SeqProvider.Unit currentPi : p) {
                    System.out.println("次のユニット" + currentPi.getStartDigit());

                    if (survivalSet.size() * -2 >= survivalSetInitialSize) {
                        System.out.println("アルゴリズム：パイの断片からサバイバルリストを検索する contains");
                        int currentPiStrPos = 0;
                        String currentPiStr = StringUtils.repeat("0", targetRange.getLength());

                        // カレントPiデータから、対象桁長さで文字列を順次取り出し
                        while (currentPiStr.length() >= targetRange.getLength()) {

                            int endIndex = currentPiStrPos + targetRange.getLength();

                            if (endIndex > currentPi.getData().length()) {
                                break;
                            }

                            // 読み込んだ長いパイデータから、ターゲットの長さ分の文字列として切り取る
                            currentPiStr = currentPi.getData().substring(currentPiStrPos,
                                    currentPiStrPos + targetRange.getLength());

                            // サバイバルセットから検索
                            // 切り取ったパイ文字列がサバイバルSetにあるかどうか
                            if (survivalSet.contains(currentPiStr)) {

                                //System.out.println((currentPiStrPos + currentPi.getStartDigit()) + "桁にて" + currentPiStr
                                //       + "を発見。リスト残り" + survivalSet.size());

                                // もしかしたら、これが最後の生き残りなのかもしれないので、ヒットした要素と、発見位置をバックアップする
                                if (1 == survivalSet.size()) {
                                    Long curFindPos = currentPi.getStartDigit() + currentPiStrPos;
                                    if (lastFoundPos < curFindPos) {
                                        lastData = currentPiStr;
                                        lastFoundPos = curFindPos;
                                    }
                                }
                                survivalSet.remove(currentPiStr);
                            }

                            if (survivalSet.isEmpty()) {
                                break;
                            }
                            currentPiStrPos++;
                        }
                    } else {
                        System.out.println("アルゴリズム：サバイバルリストからパイに検索をかける indexOf");

                        // サバイバルリストループ
                        for (int i = survivalSet.size() - 1; i >= 0; i--) {

                            // カレントパイユニットから検索
                            int pos = currentPi.indexOf(survivalSet.get(i));

                            // ヒットしたら
                            if (0 <= pos) {
                                //System.out.println((pos + currentPi.getStartDigit()) + " リスト残り" + survivalSet.size());

                                // もしかしたら、これが最後の生き残りなのかもしれないので、ヒットした要素と、発見位置をバックアップする
                                if (1 == survivalSet.size()) {
                                    Long curFindPos = currentPi.getStartDigit() + pos;
                                    if (lastFoundPos < curFindPos) {
                                        lastData = survivalSet.get(i);
                                        lastFoundPos = curFindPos;
                                    }
                                }

                                // サバイバルリストからヒットした要素を削除
                                survivalSet.remove(i);

                            }
                        }
                    }
                    
                    if (survivalSet.isEmpty()) {
                        break;
                    }

                }

            } catch (Throwable t) {

                // 仮に何かしらのエラーが発生した場合でも、ちょっと時間をおいて再起動してみる
                continueCount++;
                RuntimeException ee = new RuntimeException("ERROR!  start over. count: " + continueCount, t);
                ee.printStackTrace();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue; // 再起動
            }

            // 検索終了。サバイバルリストが空になっているはず。

            // サバイバルリストが空でない場合は探しきれなかった、とする。
            if (!survivalSet.isEmpty()) {
                throw new RuntimeException(
                        "The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)");
            }

            endTime = ZonedDateTime.now();

            // 結果保存
            StoreController.getInstance().saveFile(targetRange.getLength(), targetRange.getStart(),
                    targetRange.getEnd(), lastData, lastFoundPos, startTime, endTime);

        }

    }

}
