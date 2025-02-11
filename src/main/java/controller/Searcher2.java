package controller;

import lombok.Getter;
import model.TargetRange;
import model.pi.SearchThread;
import model.pi.SurvivalList;
import model.pi.SurvivalResult;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Searcher2 extends Thread {

    @Getter
    private final List<File> um_piFileList;// 検索用ファイルリスト（不変にする）

    @Getter
    private final Integer listSize; // サバイバルリストの現在サイズ（残り）

    @Getter
    private final Integer unitLength;// 一回の読み込み長さ

    /**
     * コンストラクタ.
     * 
     * @param piFileList 検索用ファイルのリスト
     * @param listSize   サバイバルリストのサイズ
     * @param unitLength ターゲット桁数
     * @throws IOException
     */
    public Searcher2(List<File> piFileList, Integer listSize, Integer unitLength)
            throws IOException {
        super();

        um_piFileList = Collections.unmodifiableList(piFileList); // 外部から参照されるため、不変オブジェクトにする

        this.listSize = listSize;
        this.unitLength = unitLength;

    }

    @Override
    public void run() {

        // 検索処理のメインループ
        while (true) {

            // 処理範囲の決定（次の処理を定義をStoreControllerから得る）
            TargetRange targetRange = StoreController.getInstance().getCurrentTargetStartEnd(this.listSize);

            // ターゲットレンジを分割する（左共通部分を長く取るには10分割が良い）
            List<TargetRange> targetRangeList = divideTargetRange(10, targetRange);

            // 1サイクルのサバイバル実行
            SurvivalResult sr = survive(targetRangeList);

        }

    }

    public static List<TargetRange> divideTargetRange(int count, TargetRange targetRange) {

        // ターゲット要素の数
        int targetCount = Integer.parseInt(targetRange.getEnd()) - Integer.parseInt(targetRange.getStart()) + 1;

        // 分割した時の一つの基本数
        int oneCount = targetCount / count;

        // 分割した時のあまり
        int modCount = targetCount % count;

        // 分割リストを作成
        List<TargetRange> targetRangeList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int start = Integer.parseInt(targetRange.getStart()) + (oneCount * i);
            int end = start + oneCount - 1; // zero base
            targetRangeList.add(new TargetRange(targetRange.getLength(), String.valueOf(start), String.valueOf(end)));
        }

        // 分割に余りがあれば、最後の要素にあまり分を付け加える
        if (modCount != 0) {
            TargetRange lastTargetRange = targetRangeList.get(targetRangeList.size() - 1);

            Integer lastEnd = Integer.valueOf(lastTargetRange.getEnd());

            lastEnd = lastEnd + modCount;

            TargetRange repTargetRange = new TargetRange(targetRange.getLength(), lastTargetRange.getStart(),
                    String.valueOf(lastEnd));

            targetRangeList.set(targetRangeList.size() - 1, repTargetRange);

        }

        return targetRangeList;
    }

    private SurvivalResult survive(List<TargetRange> targetRangeList) {


        //とりあえず、最初の一つを始める  この検索が最後まで行く
        
        //1/分割数になったら、到達深さを記録



        //その他を先に回す

        
        //次のを始めて、1/分割数になったらマージ
        //リソースを捨てる

        //一巡するとn個になる。これを分割する。分割数は始めの分割数－１。分割数が1になったら終わり
        



        //軸と、その他に分ける
        TargetRange baserange = targetRangeList.get(0);

        List<TargetRange> otherRange = Collections.emptyList();
        if (targetRangeList.size() > 1) {
            otherRange = targetRangeList.subList(1, targetRangeList.size());
        }








        //軸サバイバルリスト作成
        SurvivalList survivalList = new SurvivalList(baserange.getLength(), Integer.valueOf(baserange.getStart()),
                Integer.valueOf(baserange.getEnd()));

        // YCDデータプロバイダ（シーケンシャルにYCDデータをブロックで提供）を作成
        int overWrapLength = baserange.getLength(); // ユニットの境目対応として一つ前のケツの部分を今回の先頭に重ねる桁の長さ
        try (YCD_SeqProvider p = new YCD_SeqProvider(this.um_piFileList, overWrapLength, this.unitLength);) {

            // プロセス内新記録の記録
            SurvivalResult processDeepest = new SurvivalResult("", -1L);



            //その他の検索



            // 検索スレッドオブジェクト
            SearchThread searchThread = null;
            while (true) {

                // YCDから次の対象文字列を読み込む
                final YCD_SeqProvider.Unit currentPi = p.next();

                // ひとつ前の検索処理の終了を待ち合わせる
                if (searchThread != null) {
                    searchThread.join();

                    // 新記録なら結果を更新
                    if (0 < searchThread.getResult().compareTo(processDeepest)) {
                        processDeepest.update(searchThread.getResult());
                    }

                    // サバイバルリストが空になったら終了
                    if (survivalList.isEmpty()) {
                        break; // read YCD UNIT break
                    }

                }

                // 検索スレッドの作成と開始
                searchThread = new SearchThread(survivalList, currentPi);
                searchThread.start();
            }

            // 検索終了。サバイバルリストが空になっているはず。
            // サバイバルリストが空でない場合は探しきれなかった、とする。
            if (!survivalList.isEmpty()) {
                System.out.println(
                        "The file was too short to finalize the last one(発見できず。YCDファイルが短すぎました)"
                                + "  検索できなかったものの一例: " + survivalList.get(0));

                System.out.println(StoreController.survivalProgressMap.get("NOW_SURVIVAL_DEPTH"));

                Runtime.getRuntime().exit(-1);
            }

            return processDeepest;

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }



}
