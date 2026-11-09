package model.pi;

import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * サバイバルリスト.
 *
 * StartからEndまでの連番ゼロ埋めを自動生成した状態を初期値とする、昇順の読み取り専用リスト.
 * 総当たり検索用。要素の削除は {@link #discover} でのみ行う。
 *
 * <p>
 * 内部構造（省メモリ設計）：
 * <ul>
 * <li>要素の文字列は保持しない。要素は「連番の何番目か（スロット）」で管理し、必要なときに番号から文字列を生成する。</li>
 * <li>削除しても配列は詰めず、生存フラグを落とすだけにする。
 * 「先頭から数えて何番目の生存者か」はフェニック木（Binary Indexed Tree）で数える。</li>
 * <li>発見済み情報は、画面の出現分布に必要な「発見位置」だけを long 配列で全件保持し、
 * 対象・日時を含む詳細は直近 {@link #RECENT_DISCOVERED_CAPACITY} 件のみ保持する。</li>
 * </ul>
 * これにより、1要素あたりのメモリは約 5 バイト（生存フラグ 1 バイト＋フェニック木 4 バイト）＋発見位置 8 バイトとなり、
 * get / indexOf / contains / discover はいずれもコピーなしの O(log n) で動作する。
 * </p>
 *
 * <p>
 * 対応桁数は 1〜{@link #MAX_LENGTH} 桁（値が long に収まる範囲）。
 * </p>
 */
public class SurvivalList extends AbstractList<String> implements RandomAccess {

    /** 対応する最大桁数（10^18 - 1 が long に収まる） */
    public static final int MAX_LENGTH = 18;

    /** 詳細（対象文字列・発見日時）を保持する直近発見件数 */
    public static final int RECENT_DISCOVERED_CAPACITY = 100;

    /**
     * 発見済情報保持用.
     */
    public static class DiscoverdInfo {

        @Getter
        private String target;

        @Getter
        private Long findPos;

        @Getter
        private ZonedDateTime findDateTime;

        public DiscoverdInfo(String target, Long findPos, ZonedDateTime findDateTime) {
            this.target = target;
            this.findPos = findPos;
            this.findDateTime = findDateTime;
        }

        @Override
        public String toString() {
            return "DiscoverdInfo{" +
                    "target='" + target + '\'' +
                    ", findPos=" + findPos +
                    ", findDateTime=" + findDateTime +
                    '}';
        }
    }

    // 桁数
    private final int length;

    /** スロット0の数値 */
    private final long startValue;

    /** スロット数（初期要素数） */
    private final int slotCount;

    /** 生存フラグ（スロット番号順） */
    private final boolean[] alive;

    /** 生存数カウント用フェニック木（1始まり） */
    private final int[] tree;

    /** 生存数 */
    private int aliveCount;

    /** 発見位置（発見順）。画面の出現分布用に全件保持する（最終的に全要素が発見されるので最初から要素数分を確保） */
    private long[] discoveredPos;

    /** 発見数 */
    private int discoveredCount = 0;

    /** 発見位置が昇順のまま追加されているか（昇順ならスナップショット時にソート不要） */
    private boolean discoveredPosSorted = true;

    /** 直近の発見詳細（古いものから捨てる） */
    private final ArrayDeque<DiscoverdInfo> recentDiscovered = new ArrayDeque<>();

    /**
     * コンストラクタ.
     *
     * 指定長さの、スタートからエンドまでのゼロ埋め連続リストを作成する.
     * エンドが桁数の最大値（全桁9）を超える場合は最大値で打ち切る.
     *
     * @param length 桁数（1〜{@link #MAX_LENGTH}）
     * @param start  開始数（0以上）
     * @param end    終了数（start以上）
     */
    public SurvivalList(int length, long start, long end) {
        super();
        if (length < 1 || MAX_LENGTH < length) {
            throw new IllegalArgumentException(
                    "length must be 1.." + MAX_LENGTH + ", but length=" + length);
        }
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0, but start=" + start);
        }
        if (start > end) {
            throw new IllegalArgumentException(
                    "start must be <= end, but start=" + start + ", end=" + end);
        }
        this.length = length;
        this.startValue = start;

        // 桁数の最大値（全桁9）で打ち切り
        long maxValue = pow10(length) - 1;
        long cappedEnd = Math.min(end, maxValue);
        long count = (start > maxValue) ? 0 : (cappedEnd - start + 1);
        if (Integer.MAX_VALUE - 8 < count) {
            throw new IllegalArgumentException("list is too large: " + count);
        }
        this.slotCount = (int) count;

        // 全スロット生存としてフェニック木を O(n) で構築
        this.alive = new boolean[slotCount];
        this.tree = new int[slotCount + 1];
        Arrays.fill(alive, true);
        for (int i = 1; i <= slotCount; i++) {
            tree[i] += 1;
            int parent = i + (i & -i);
            if (parent <= slotCount) {
                tree[parent] += tree[i];
            }
        }
        this.aliveCount = slotCount;
        this.discoveredPos = new long[slotCount];
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList() throws IllegalAccessException {
        throw new IllegalAccessException("no args");
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList(int initialCapacity) throws IllegalAccessException {
        throw new IllegalAccessException("no args");
    }

    /**
     * コンストラクタ（使用禁止).
     *
     * @throws IllegalAccessException このコンストラクタは使用できません。
     */
    public SurvivalList(Collection<? extends String> c) throws IllegalAccessException {
        throw new IllegalAccessException("no args");
    }

    /**
     * 桁数を返す.
     *
     * @return 桁数
     */
    public int getLength() {
        return this.length;
    }

    /**
     * remove（使用禁止).
     */
    @Override
    @Deprecated
    public String remove(int index) {
        throw new RuntimeException("Not available");
    }

    @Override
    public synchronized String get(int index) {
        if (index < 0 || aliveCount <= index) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + aliveCount);
        }
        return format(startValue + selectSlot(index));
    }

    @Override
    public synchronized int size() {
        return aliveCount;
    }

    @Override
    public synchronized int indexOf(Object o) {
        int slot = slotOf(o);
        if (slot < 0 || !alive[slot]) {
            return -1;
        }
        return prefixCount(slot);
    }

    @Override
    public int lastIndexOf(Object o) {
        // 要素は重複しない
        return indexOf(o);
    }

    @Override
    public synchronized boolean contains(Object o) {
        int slot = slotOf(o);
        return 0 <= slot && alive[slot];
    }

    /**
     * 生存要素を昇順にたどるイテレータ（読み取り専用）.
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int nextSlot = nextAliveSlot(0);

            @Override
            public boolean hasNext() {
                return nextSlot < slotCount;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String ret = format(startValue + nextSlot);
                nextSlot = nextAliveSlot(nextSlot + 1);
                return ret;
            }
        };
    }

    /**
     * インデックス指定で発見（削除）する.
     *
     * @param index   生存リスト内のインデックス（0始まり）
     * @param findPos 発見位置
     */
    public synchronized void discover(int index, Long findPos) {
        if (index < 0 || aliveCount <= index) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + aliveCount);
        }
        killSlot(selectSlot(index), findPos);
    }

    /**
     * 文字列指定で発見（削除）する.
     *
     * @param targetStr 発見した対象文字列
     * @param findPos   発見位置
     */
    public synchronized void discover(String targetStr, Long findPos) {

        //発見した要素のスロットを取得
        int slot = slotOf(targetStr);

        //発見した要素がない場合は例外
        if (slot < 0 || !alive[slot]) {
            throw new RuntimeException("Not found");
        }

        killSlot(slot, findPos);
    }

    /**
     * 発見数を返す.
     *
     * @return 発見数
     */
    public synchronized int getDiscoveredCount() {
        return this.discoveredCount;
    }

    /**
     * 全発見位置のスナップショット（昇順）を返す.
     *
     * @return 発見位置の配列（コピー）
     */
    public synchronized long[] getDiscoveredPositions() {
        long[] copy = Arrays.copyOf(discoveredPos, discoveredCount);
        if (!discoveredPosSorted) {
            Arrays.sort(copy);
        }
        return copy;
    }

    /**
     * 直近の発見詳細（古い順）のスナップショットを返す.
     *
     * 保持件数は最大 {@link #RECENT_DISCOVERED_CAPACITY} 件。
     * 全件の発見数は {@link #getDiscoveredCount()}、全件の発見位置は {@link #getDiscoveredPositions()} を使うこと。
     *
     * @return 直近の発見詳細（コピー）
     */
    public synchronized ArrayList<DiscoverdInfo> getDiscoverdInfo() {
        return new ArrayList<>(this.recentDiscovered);
    }

    /**
     * 保持中の要素文字列を左からみて全てに共通する部分を返す.
     *
     * 例：「000000」「000001」「000002」の場合、左共通部分の「00000」を返す
     *
     * @return 左共通部分文字列
     */
    public synchronized String getCommonPrefix() {

        if (this.aliveCount == 0) {
            return "";
        }

        // 昇順なので、最小要素と最大要素の共通部分が全体の共通部分
        String min = this.get(0);

        //要素が一つだけの場合はそのまま返す
        if (this.aliveCount == 1) {
            return min;
        }

        String max = this.get(this.aliveCount - 1);

        int i = 0;
        while (i < this.length && min.charAt(i) == max.charAt(i)) {
            i++;
        }
        return min.substring(0, i);
    }

    // =========================================================
    // 内部処理
    // =========================================================

    /** スロットを削除（生存フラグを落とし、フェニック木を更新し、発見済みに記録） */
    private void killSlot(int slot, Long findPos) {

        // 発見位置を全件保持（要素数分を確保済み。発見数は要素数を超えない）
        if (0 < discoveredCount && findPos < discoveredPos[discoveredCount - 1]) {
            discoveredPosSorted = false;
        }
        discoveredPos[discoveredCount++] = findPos;

        // 詳細は直近分のみ保持
        recentDiscovered.addLast(new DiscoverdInfo(format(startValue + slot), findPos, ZonedDateTime.now()));
        if (RECENT_DISCOVERED_CAPACITY < recentDiscovered.size()) {
            recentDiscovered.removeFirst();
        }

        alive[slot] = false;
        for (int i = slot + 1; i < tree.length; i += i & -i) {
            tree[i]--;
        }
        aliveCount--;
        modCount++;
    }

    /** 文字列からスロット番号を求める。範囲外・形式不正は -1 */
    private int slotOf(Object o) {
        if (!(o instanceof String)) {
            return -1;
        }
        String s = (String) o;
        if (s.length() != this.length) {
            return -1;
        }
        long v = 0L;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || '9' < c) {
                return -1;
            }
            v = v * 10L + (c - '0');
        }
        long slot = v - startValue;
        if (slot < 0 || slotCount <= slot) {
            return -1;
        }
        return (int) slot;
    }

    /** 数値を桁数分ゼロ埋めした文字列にする */
    private String format(long v) {
        char[] buf = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            buf[i] = (char) ('0' + (v % 10));
            v /= 10;
        }
        return new String(buf);
    }

    /** スロット [0, slot) にいる生存数 */
    private int prefixCount(int slot) {
        int sum = 0;
        for (int i = slot; i > 0; i -= i & -i) {
            sum += tree[i];
        }
        return sum;
    }

    /** index 番目（0始まり）の生存者のスロット番号 */
    private int selectSlot(int index) {
        int pos = 0;
        int rem = index + 1;
        for (int step = Integer.highestOneBit(Math.max(1, slotCount)); step > 0; step >>= 1) {
            int next = pos + step;
            if (next <= slotCount && tree[next] < rem) {
                pos = next;
                rem -= tree[next];
            }
        }
        return pos; // 1始まりの pos+1 が該当 → 0始まりのスロットは pos
    }

    /** from 以降で最初の生存スロット（無ければ slotCount） */
    private synchronized int nextAliveSlot(int from) {
        int i = from;
        while (i < slotCount && !alive[i]) {
            i++;
        }
        return i;
    }

    /** 10 の n 乗（n は 0〜18） */
    static long pow10(int n) {
        long v = 1L;
        for (int i = 0; i < n; i++) {
            v *= 10L;
        }
        return v;
    }

}
