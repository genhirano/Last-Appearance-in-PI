package model.pi;

import controller.StoreController;
import lombok.Getter;
import model.ycd.YCD_SeqProvider;

/**
 * 検索ロジックスレッド.
 *
 * カレントパイ文字列（ユニット）1つ分を検索し、サバイバルリストに残っている数を発見したら削除する。
 *
 * <p>
 * アルゴリズム（左共通部分シーク）：
 * サバイバルリストは昇順の連番なので、残っている要素には左側に共通部分（例：「00012」）があることが多い。
 * その共通部分をボイヤー・ムーア法でシークし、見つかった位置の N 桁がサバイバルリストに含まれるかを確認する。
 * 共通部分が空の場合（リストが広い範囲に散らばっている序盤）は、全位置を順に確認する。
 * 発見のたびに共通部分を取り直すので、リストが減るほどシークは速くなる。
 * </p>
 */
public class SearchThread extends Thread {

    private final SurvivalList survivalList;
    private final YCD_SeqProvider.Unit pi;

    @Getter
    SurvivalResult result = null;

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
        result = search(survivalList, pi);
    }

    /**
     * ユニット1つ分を検索する.
     *
     * @param survivalList サバイバルリスト
     * @param currentPi カレントパイ文字列ユニット
     * @return このユニットで発見したもののうち最も深いもの（発見なしの場合は target="", findPos=-1）
     */
    private SurvivalResult search(SurvivalList survivalList, YCD_SeqProvider.Unit currentPi) {

        SurvivalResult survivalResult = new SurvivalResult("", -1L);

        if (survivalList.isEmpty()) {
            return survivalResult;
        }

        final int n = survivalList.getLength();
        final String data = currentPi.getData();
        final int len = data.length();
        if (len < n) {
            return survivalResult;
        }

        // サバイバルリストに存在する値の左共通部分
        String prefix = survivalList.getCommonPrefix();
        int[] badCharTable = prefix.isEmpty() ? null : buildBadCharacterTable(prefix);

        // このユニット内の検索開始位置（検索済み部分をスキップする）
        int seekPos = 0;

        while (true) {

            // 次の候補位置を決める（共通部分があればそれをシーク、無ければ次の位置）
            final int startPos = (badCharTable == null)
                    ? seekPos
                    : boyerMooreSearch(data, prefix, badCharTable, seekPos);

            // 候補が無い、または N 桁分のデータが足りない場合はこのユニットは終わり
            if (startPos < 0 || len < startPos + n) {
                break;
            }

            // 候補位置の N 桁がサバイバルリストに残っていれば発見
            final String candidate = data.substring(startPos, startPos + n);
            if (survivalList.contains(candidate)) {

                // カレントパイ文字列の中での発見位置を、全体位置に変換
                final Long curFindPos = currentPi.getStartDigit() + startPos;

                // 発見位置が今までで一番後ろだったらメモ記録（最遅候補とする）
                if (survivalResult.getFindPos() < curFindPos) {
                    survivalResult.setTarget(candidate);
                    survivalResult.setFindPos(curFindPos);
                }

                // ヒットした要素をサバイバルリストから削除
                survivalList.discover(candidate, curFindPos);

                StoreController.survivalProgressMap.put("NOW_SURVIVAL_LIST_SIZE", survivalList.size());

                if (survivalList.isEmpty()) {
                    break;
                }

                // リストが減ると共通部分が伸びることがあるので取り直す
                final String newPrefix = survivalList.getCommonPrefix();
                if (!newPrefix.equals(prefix)) {
                    prefix = newPrefix;
                    badCharTable = prefix.isEmpty() ? null : buildBadCharacterTable(prefix);
                }
            }

            // 候補位置の次の文字から再開する
            seekPos = startPos + 1;
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

    // ボイヤー・ムーア検索（テーブルを毎回作る版。テスト用に残す）
    private int boyerMooreSearch(String text, String pattern, int startIndex) {
        return boyerMooreSearch(text, pattern, buildBadCharacterTable(pattern), startIndex);
    }

    // ボイヤー・ムーア検索
    private int boyerMooreSearch(String text, String pattern, int[] badCharTable, int startIndex) {
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
