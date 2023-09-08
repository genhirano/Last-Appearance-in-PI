package controller;

import lombok.Getter;
import model.pi.SurvivalList;
import model.ycd.YCDFileUtil;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class Searcher extends Thread{

    private final List<File> piFileList;
    @Getter
    private final List<File> um_piFileList;//不変

    @Getter
    private final Integer listSize;

    @Getter
    private final Integer unitLength;

    @Getter
    private final Integer reportSpan;

    public Searcher(List<File> piFileList, Integer listSize, Integer unitLength, Integer reportSpan){
        super();

        this.piFileList = piFileList;
        um_piFileList = Collections.unmodifiableList(piFileList); //外部から参照されるための不変オブジェクト

        this.listSize = listSize;
        this.unitLength = unitLength;
        this.reportSpan = reportSpan;

    }


    @Override
    public void run(){

        //次に実行すべき情報を作る
        //（すでにある保存データの一番最後の、その次として実行する情報を作る）

        Integer continueCount = 0;
        while (true) {

            if (5 < continueCount) {
                throw new RuntimeException("Fail!  Retry limit exceeded." + continueCount);
            }

            //結果保存ファイルの全ロード
            //(非効率ではあるがYCDファイルロードの度に毎回やる。このおかげで容易にリジュームできる)
            StoreController sc = StoreController.getInstance();
            List<StoreController.StartEndBean> storeDataList = sc.getNextList(listSize);

            //対象とする結果保存ファイル（未終了）の決定
            StoreController.StartEndBean targetBean = null;
            for (StoreController.StartEndBean se : storeDataList) {

                //終わっていないこと（Startが空文字ならばすでに終了済みである）
                if ("".equals(se.getStart())) {
                    continue;
                }

                //範囲決定
                targetBean = se;

                break;
            }

            //次の実行情報が得られなければ終わり
            if (null == targetBean) {
                break;
            }

            ZonedDateTime startTime = null;
            ZonedDateTime endTime = null;

            //今回のサバイバルリストの作成
            SurvivalList survivalList = new SurvivalList(targetBean.getTargetLength(), Integer.valueOf(targetBean.getStart()), Integer.valueOf(targetBean.getEnd()));

            String lastData = "";
            Long lastFoundPos = -1L;

            //YCDプロバイダを作成
            try (YCD_SeqProvider p = new YCD_SeqProvider(piFileList, targetBean.getTargetLength(), unitLength);) {

                startTime = ZonedDateTime.now();

                //-----
                // Let's ブルートフォース検索   ユニット ＶＳ サバイバルリスト
                //-----

                //YCDプロバイダからユニットを順次取り出して
                for(YCD_SeqProvider.Unit currentPi : p){

                    //サバイバルリストループ
                    for (int i = survivalList.size() - 1; i >= 0; i--) {

                        //カレントユニットから検索
                        Integer pos = currentPi.getData().indexOf(survivalList.get(i));

                        //ヒットしたら
                        if (0 <= pos) {

                            //もしかしたら、これが最後の生き残りなのかもしれないので、ヒットした要素と、発見位置をバックアップする
                            Long curFindPos = currentPi.getStartDigit() + pos;
                            if (lastFoundPos < curFindPos) {
                                lastData = survivalList.get(i);
                                lastFoundPos = curFindPos;
                            }

                            //サバイバルリストからヒットした要素を削除
                            survivalList.remove(i);

                        }
                    }

                    // コンソール実況
                    if (0 == (survivalList.size() % reportSpan)) {
                        System.out.println(
                                "targetLength: " + targetBean.getTargetLength()
                                        + " PiFILE_INDEX:" + currentPi.getFileInfo().get(YCDFileUtil.FileInfo.BLOCK_INDEX)
                                        + " SearchingDepth: " + currentPi.getStartDigit()
                                        + " targetLenge: " +targetBean.getStart() + "-" + targetBean.getEnd()
                                        + " remaining: " + survivalList.size());
                    }


                    //サバイバルリストが空になったら、このユニットの処理終了。次のユニットへ
                    if (0 >= survivalList.size()) {
                        break;
                    }

                }

                // 全てのファイル、全てのユニットが終わったのに、サバイバルリストが空出ない場合は探しきれなかった、とする。
                if (!survivalList.isEmpty()) {
                    throw new RuntimeException("The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)");
                }

                endTime = ZonedDateTime.now();

            } catch (Throwable t) {

                //仮に何かしらのエラーが発生した場合でも、ちょっと時間をおいて再起動してみる
                continueCount++;
                RuntimeException ee = new RuntimeException("ERROR!  start over. count: " + continueCount, t);
                ee.printStackTrace();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue; //再起動
            }

            //結果保存
            sc.saveFile(targetBean.getTargetLength(), targetBean.getStart(), targetBean.getEnd(), lastData, lastFoundPos, startTime, endTime);

        }
    }

}
