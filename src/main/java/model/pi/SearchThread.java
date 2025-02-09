package model.pi;

import controller.StoreController;
import lombok.Getter;
import model.ycd.YCD_SeqProvider;

public class SearchThread extends Thread {

    private final SurvivalList survivalList;
    private final YCD_SeqProvider.Unit pi;

    @Getter
    SurvivalResult result = null;

    @Getter
    String algorithm = "";

    /**
     * 検索ロジックスレッド.
     * @param survivalList サバイバルリスト
     * @param pi 文字列ユニット
     */
    public SearchThread(SurvivalList survivalList, final YCD_SeqProvider.Unit pi) {
        this.survivalList = survivalList;
        this.pi = pi;
    }

    @Override
    public void run() {

        // サバイバルリストに存在する値の左共通部分を取得
        String leftCommonStr = survivalList.getCommonPrefix();

        if (leftCommonStr.isEmpty()) {
            // 左共通部分が無い
            // サバイバルリストループ検索
            result = searchSurvivalLoopAlgorithm(survivalList, pi);
            algorithm = "SL";
        } else {
            // 左共通部分がある
            // 左共通部分を使ってPi文字列基準のシーク検索
            result = searchLeftCommonAlgorithm(survivalList, pi, leftCommonStr);
            algorithm = "LC";
        }

    }

    private SurvivalResult searchLeftCommonAlgorithm(SurvivalList survivalList,
            YCD_SeqProvider.Unit currentPi, String leftCommonStr) {

        int startPos = -1;
        try {
            if (leftCommonStr.isEmpty()) {
                throw new RuntimeException("leftCommonStr is empty.");
            }

            SurvivalResult survivalResult = new SurvivalResult("", -1L);

            // このユニットの検索スタート位置初期値（検索終了している部分をスキップするためのシーク位置）
            int commonSeekPos = 0;

            // カレントパイユニットが末尾に達するまで繰り返す
            while (true) {

                // スタート位置を確定（次の共通文字列を検索してシークする）
                // int startPos = currentPi.indexOf(leftCommonStr, commonSeekPos);
                startPos = boyerMooreSearch(currentPi.getData(), leftCommonStr, commonSeekPos);

                // 共通左文字でシークができなかった（共通文字が見つからなかった）場合は次のユニットへ
                if (startPos < 0) {
                    break;
                }

                // シーク位置を先頭にしたとき、データが足りない場合は、次のYCDユニットへ
                if (startPos + survivalList.get(0).length() > currentPi.getData().length()) {
                    break;
                }

                // カレントパイ文字列
                final String currentPiStr = currentPi.getData().substring(startPos,
                        startPos + survivalList.get(0).length());

                // サバイバルリストからターゲットを探す
                final int suvIndex = survivalList.indexOf(currentPiStr);

                if (-1 < suvIndex) {
                    // サバイバルリストヒット

                    // カレントパイ文字列の中での発見位置を、全体位置に変換
                    final Long curFindPos = currentPi.getStartDigit() + startPos;

                    // 発見位置が今までで一番後ろだったらメモ記録（最遅候補とする）
                    if (survivalResult.getFindPos() < curFindPos) {
                        survivalResult.setTarget(currentPiStr); // 発見した対象
                        survivalResult.setFindPos(curFindPos); // 発見位置
                    }

                    // ヒットした要素をサバイバルリストから削除
                    survivalList.discover(currentPiStr, curFindPos);

                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

                }

                // シーク位置を１文字ずらす。（共通部分発見位置の次の文字から再開する）
                commonSeekPos = startPos + 1;
            }

            return survivalResult;
        } catch (Exception e) {
            System.out.println(survivalList.get(0).length());
            System.out.println(currentPi.getData().substring(startPos));
            System.out.println(survivalList.get(0));

            e.printStackTrace();
        }

        return null;
    }

    private SurvivalResult searchSurvivalLoopAlgorithm(SurvivalList survivalList, YCD_SeqProvider.Unit currentPi) {

        SurvivalResult survivalResult = new SurvivalResult("", -1L);

        // カレントパイ文字列から、サバイバルリストのそれぞれを検索（サバイバルリストループ）
        for (int i = survivalList.size() - 1; i >= 0; i--) {

            String target = survivalList.get(i);

            int pos = currentPi.indexOf(target);
            if (0 <= pos) {
                // ヒット
                // ヒットしたら基本的にはサバイバルリストから削除する

                // カレントパイ文字列の中での発見位置を、全体位置に変換
                Long curFindPos = currentPi.getStartDigit() + pos;

                // 発見位置が今までで一番後ろだったらメモ記録（最遅候補とする）
                if (survivalResult.getFindPos() < curFindPos) {
                    survivalResult.setTarget(target); // 発見した対象
                    survivalResult.setFindPos(curFindPos); // 発見位置
                }

                // ヒットした要素をサバイバルリストから削除
                survivalList.discover(target, curFindPos);

                StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

            }
        }
        return survivalResult;
    }

    // バッドキャラクターテーブルを作成
    private int[] buildBadCharacterTable(String pattern) {
        int[] badCharTable = new int[10];
        for (int i = 0; i < 10; i++) {
            badCharTable[i] = -1;
        }
        for (int i = 0; i < pattern.length(); i++) {
            badCharTable[pattern.charAt(i) - '0'] = i;
        }
        return badCharTable;
    }

    // ボイヤー・ムーア検索
    private int boyerMooreSearch(String text, String pattern, int startIndex) {
        int[] badCharTable = buildBadCharacterTable(pattern);
        int m = pattern.length();
        int n = text.length();
        int shift = startIndex; // 開始位置を指定

        while (shift <= (n - m)) {
            int j = m - 1;

            // 右から左へ比較
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            // 一致した場合
            if (j < 0) {
                return shift;
            }

            // バッドキャラクタールールに従いシフト
            int badCharIndex = text.charAt(shift + j) - '0';
            int badCharShift = j - badCharTable[badCharIndex];
            shift += Math.max(1, badCharShift);
        }
        return -1; // 見つからなかった場合
    }

}
