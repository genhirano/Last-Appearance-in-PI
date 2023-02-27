package controller;

import model.pi.SurvivalList;
import model.ycd.YCD_SeqProvider;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class Searcher extends Thread{

    private List<File> piFileList;
    private String savePath;
    private Integer maxLength;
    private Integer listSize;
    private Integer unitLength;
    private Integer reportSpan;

    public Searcher(List<File> piFileList, String savePath, Integer maxLength, Integer listSize, Integer unitLength, Integer reportSpan){
        super();

        this.piFileList = piFileList;
        this.savePath = savePath;
        this.maxLength = maxLength;
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
                System.out.println("コンティニューカウントが上限に達しました :" + continueCount);
                break;
            }

            //結果保存ファイルの全ロード
            //(リジュームのため、非効率ではあるが、ユニットロードの度に毎回やる。このおかげでいつでも途中終了できる)
            StoreController sc = StoreController.getInstance();
            List<StoreController.StartEndBean> storeDataList = sc.getNextList(listSize, maxLength);

            //今回の対象範囲の決定
            Integer targetLength = 0;
            StoreController.StartEndBean targetBean = null;
            for (StoreController.StartEndBean se : storeDataList) {

                //対象桁数
                targetLength++;

                //終わっていないこと（Startが空文字ならばすでに終了済みである）
                if ("".equals(se.getStart())) {
                    continue;
                }

                //範囲決定
                targetBean = se;

                break;
            }

            //次の実行情報が得られなければ終わり
            if (targetBean.getStart().isEmpty()) {
                break;
            }

            YCD_SeqProvider.Unit currentPi = null;
            ZonedDateTime startTime = null;
            ZonedDateTime endTime = null;

            SurvivalList sl = new SurvivalList(targetBean.getTargetLength(), Integer.valueOf(targetBean.getStart()), Integer.valueOf(targetBean.getEnd()));

            String lastData = "";
            Long lastFoundPos = -1L;

            try (YCD_SeqProvider p = new YCD_SeqProvider(piFileList, targetBean.getTargetLength(), unitLength);) {

                startTime = ZonedDateTime.now();

                while (p.hasNext()) {

                    currentPi = p.getNext();

                    for (int i = sl.size() - 1; i >= 0; i--) {

                        Integer pos = currentPi.getData().indexOf(sl.get(i));

                        if (0 <= pos) {
                            Long curFindPos = currentPi.getStartDigit() + pos;
                            if (lastFoundPos < curFindPos) {
                                lastData = sl.get(i);
                                lastFoundPos = curFindPos;
                            }
                            sl.remove(i);

                        }
                    }

                    if (0 == (sl.size() % reportSpan)) {
                        System.out.println(
                                "targetLength: " + targetBean.getTargetLength()
                                        + " PiFILE_INDEX:" + currentPi.getFileInfo().get(YCD_SeqProvider.FileInfo.BLOCK_INDEX)
                                        + " SearchingDepth: " + currentPi.getStartDigit()
                                        + targetBean.getStart() + "-" + targetBean.getEnd()
                                        + " remaining: " + sl.size());
                    }


                    //今回の検索対象がなくなったら、このユニットの処理終了
                    if (0 >= sl.size()) {
                        break;
                    }

                }

                if (!sl.isEmpty()) {
                    throw new RuntimeException("The file was too short to finalize the last one(最後の一つを確定するにはYCDファイルが短すぎました)");
                }

                endTime = ZonedDateTime.now();

            } catch (Exception e) {
                continueCount++;
                RuntimeException ee = new RuntimeException("エラーが発生しましたので、最初からやりなおしてみます。コンティニューカウント : " + continueCount, e);
                ee.printStackTrace();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                continue;
            }

            //結果保存
            try {
                sc.saveFile(targetBean.getTargetLength(), targetBean.getStart(), targetBean.getEnd(), lastData, lastFoundPos, startTime, endTime);
            } catch (IOException e) {
                throw new RuntimeException("結果保存に失敗", e);
            }

        }
    }

}
