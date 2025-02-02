

import java.util.Random;

public class StringSearchBenchmark {
    private static final int ALPHABET_SIZE = 10; // 0-9 のみ

    // バッドキャラクターテーブルを作成
    private static int[] buildBadCharacterTable(String pattern) {
        int[] badCharTable = new int[ALPHABET_SIZE];
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            badCharTable[i] = -1;
        }
        for (int i = 0; i < pattern.length(); i++) {
            badCharTable[pattern.charAt(i) - '0'] = i;
        }
        return badCharTable;
    }

    // ボイヤー・ムーア検索
    public static int boyerMooreSearch(String text, String pattern) {
        int[] badCharTable = buildBadCharacterTable(pattern);
        int m = pattern.length();
        int n = text.length();
        int shift = 0;

        while (shift <= (n - m)) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }
            if (j < 0) {
                return shift;
            }
            int badCharIndex = text.charAt(shift + j) - '0';
            int badCharShift = j - badCharTable[badCharIndex];
            shift += Math.max(1, badCharShift);
        }
        return -1;
    }

    public static void main(String[] args) {
        // 100,000桁のランダムな数字列を作成
        Random rand = new Random();
        StringBuilder sb = new StringBuilder(100_000_000);
        for (int i = 0; i < 100_000_000; i++) {
            sb.append(rand.nextInt(10));
        }
        String text = sb.toString();

        // 検索するパターン
        String pattern = "0000345"; // 5桁の検索パターン

        // ======== indexOf() の計測 ========
        long startIndexOf = System.nanoTime();
        int index1 = text.indexOf(pattern);
        long endIndexOf = System.nanoTime();
        double timeIndexOf = (endIndexOf - startIndexOf) / 1_000_000.0;

        // ======== Boyer-Moore の計測 ========
        long startBM = System.nanoTime();
        int index2 = boyerMooreSearch(text, pattern);
        long endBM = System.nanoTime();
        double timeBM = (endBM - startBM) / 1_000_000.0;

        // 結果表示
        System.out.println("=== 検索結果 ===");
        System.out.println("String.indexOf() found at: " + index1);
        System.out.println("Boyer-Moore found at: " + index2);
        System.out.println();
        System.out.println("=== 実行時間 (ms) ===");
        System.out.println("String.indexOf() Time: " + timeIndexOf + " ms");
        System.out.println("Boyer-Moore Time: " + timeBM + " ms");

        // どちらが速いか
        if (timeIndexOf < timeBM) {
            System.out.println("\n➡ `String.indexOf()` の方が速い！");
        } else {
            System.out.println("\n➡ `Boyer-Moore` の方が速い！");
        }
    }
}