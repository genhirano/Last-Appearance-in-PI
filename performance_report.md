# Last-Appearance-in-PI 高速化検討レポート

## はじめに

本レポートは「Last-Appearance-in-PI」プロジェクトの処理高速化について、現行実装の詳細分析をもとに
徹底的に検証・考察したものである。4つのアプローチ（並列処理・アルゴリズム改善・パラメータ調整・
根本改善）それぞれについて、現状の課題と具体的な改善案を日本語で記述する。

---

## 現行実装の概要と性能上の課題

### 処理フロー概要

```
[Searcher スレッド] (メインループ)
    │
    ├── YCD_SeqProvider から円周率データをユニット単位で読み込む
    │       └── YCD_SeqBlockStream → BufferedInputStream → 8バイト×19桁/ブロック
    │
    ├── SearchThread を生成・起動（1スレッド）
    │       ├── LC アルゴリズム（左共通文字があるとき）
    │       │       └── Boyer-Moore 検索 → サバイバルリスト照合
    │       └── SL アルゴリズム（左共通文字がないとき）
    │               └── 全サバイバルリストに対して String.indexOf 検索
    │
    └── 前回の SearchThread 完了待ち (join) → 結果マージ → 次ユニット読み込み
```

### 現行実装の主要な課題

| 課題 | 該当クラス | 影響度 |
|------|-----------|--------|
| `CopyOnWriteArrayList` による `remove` の O(n) コスト | `SurvivalList` | 大 |
| `survivalList.indexOf()` による O(n) 線形探索 | `SearchThread` | 大 |
| Boyer-Moore のバッドキャラクタテーブルを毎回再生成 | `SearchThread` | 中 |
| SL アルゴリズムで全サバイバルリストを毎回スキャン | `SearchThread` | 大 |
| SearchThread が 1 つのみ（擬似パイプライン止まり） | `Searcher` | 大 |
| 円周率文字列が `String` → ヒープに大量確保・GC 圧力 | 全体 | 中 |
| BufferedInputStream のデフォルトバッファ (8 KB) | `YCD_SeqBlockStream` | 小〜中 |

---

## １．並列処理アプローチ

### 1-1. 現状の「擬似パイプライン」

`Searcher.survive()` では以下のような擬似パイプラインが組まれている。

```java
// 現行コード（Searcher.java 抜粋）
while (true) {
    final YCD_SeqProvider.Unit currentPi = p.next();   // IO：次ユニットを読む
    if (searchThread != null) {
        searchThread.join();                            // 前回の検索完了を待つ
        // 結果マージ
    }
    searchThread = new SearchThread(survivalList, currentPi);
    searchThread.start();                              // 検索開始
}
```

IO（読み込み）と CPU（検索）が交互に重なるだけであり、CPU 並列度は 1 に止まっている。

### 1-2. 改善案 A：BlockingQueue によるプロデューサー・コンシューマー

IO スレッドと検索スレッドを完全に分離し、複数の検索スレッドが連続して動けるようにする。

```
[IO スレッド]          BlockingQueue<Unit>     [検索スレッド × N]
  YCD_SeqProvider  →  ▢ ▢ ▢ ▢ ▢ ▢ ▢  →  SearchThread-1
  (先読み・バッファ)                        SearchThread-2
                                            SearchThread-3
```

**実装イメージ（Java 疑似コード）:**

```java
BlockingQueue<YCD_SeqProvider.Unit> queue = new LinkedBlockingQueue<>(8); // 先読みバッファ 8 ユニット

// IO スレッド（プロデューサー）
Thread ioThread = new Thread(() -> {
    while (p.hasNext()) {
        queue.put(p.next()); // 満杯なら自動的にブロック
    }
    queue.put(POISON_PILL); // 終了シグナル
});
ioThread.start();

// 検索スレッド群（コンシューマー）
ExecutorService executor = Executors.newFixedThreadPool(N_WORKERS);
while (true) {
    YCD_SeqProvider.Unit unit = queue.take();
    if (unit == POISON_PILL) break;
    executor.submit(new SearchTask(survivalList, unit));
}
```

**注意点:**  
複数の `SearchThread` が同時に `survivalList.discover()` を呼ぶため、スレッドセーフな実装が必要。
`CopyOnWriteArrayList` はスレッドセーフだが、後述のアルゴリズム改善（HashSet/ConcurrentHashMap 等）と
組み合わせる必要がある。また検索の順序依存性（「どのユニットで見つかったか」）に注意が必要。

**期待効果:** CPU コア数に比例した線形スケーリング（理想値）。Raspberry Pi 4 の 4 コアでは
最大 4 倍の速度向上が期待できる。

### 1-3. 改善案 B：サバイバルリスト分割並列検索

1 つの円周率ユニットに対して、サバイバルリストを N 分割し、それぞれを別スレッドで検索する。

```
円周率ユニット（文字列）
        │
        ├──→ SearchThread-1（サバイバルリスト 0〜24999）
        ├──→ SearchThread-2（サバイバルリスト 25000〜49999）
        ├──→ SearchThread-3（サバイバルリスト 50000〜74999）
        └──→ SearchThread-4（サバイバルリスト 75000〜99999）
```

SL アルゴリズムの段階（左共通部分なし、各要素を個別に検索する段階）では、
サバイバルリストは独立した検索対象の集合であるため分割並列化が比較的容易。

**期待効果:** SL アルゴリズムのフェーズで CPU 並列利用率が向上。LC アルゴリズムのフェーズでは
実装が複雑になるため効果は限定的。

### 1-4. 改善案 C：非同期先読み（Prefetch）

現行では `p.next()` が同期的に IO を行っている。非同期先読みで IO 待機時間を隠蔽できる。

```java
// CompletableFuture による先読み例
CompletableFuture<Unit> prefetch = CompletableFuture.supplyAsync(() -> p.next());

while (true) {
    Unit currentUnit = prefetch.join(); // 前回先読み済みを取得
    prefetch = CompletableFuture.supplyAsync(() -> p.next()); // 次を先読み開始
    // currentUnit を使って検索
}
```

**期待効果:** Raspberry Pi の SD カード / HDD の IO レイテンシが長い環境では特に有効。
IO レイテンシが検索時間より長い場合、ほぼ IO 待ちゼロで処理できる。

### 1-5. 並列処理アプローチのまとめ

| 案 | 実装難度 | 効果 | 推奨度 |
|----|---------|------|-------|
| A: BlockingQueue プロデューサー・コンシューマー | 中 | 大（CPU コア数倍） | ◎ |
| B: サバイバルリスト分割並列 | 中 | 中（SL フェーズのみ） | ○ |
| C: 非同期先読み | 低 | 中（IO バウンド環境で有効） | ◎ |

---

## ２．アルゴリズム改善アプローチ

### 2-1. SurvivalList のデータ構造改善

**現行の問題点：**

`SurvivalList` は `CopyOnWriteArrayList<String>` を継承している。

- `remove(index)` → 内部配列を毎回コピー：**O(n)**
- `indexOf(String)` → 線形スキャン：**O(n)**
- `get(i)` → O(1) だが検索に使えない

`listSize=100000` のとき、1 要素を削除（発見）するたびに 100,000 要素の配列コピーが走る。
これが処理全体の大きなオーバーヘッドとなっている。

**改善案：LinkedHashSet + LinkedHashMap への置き換え**

```java
// 改善案イメージ
// LinkedHashSet: O(1) contains / O(1) remove、挿入順を保持
// LinkedHashMap: 値→インデックスマッピングで高速 indexOf

private final LinkedHashSet<String> survivalSet = new LinkedHashSet<>();
// または
private final ConcurrentHashMap<String, Long> survivalMap = new ConcurrentHashMap<>();
// キー: 候補数字文字列、値: 発見位置（未発見なら -1）
```

| 操作 | CopyOnWriteArrayList | LinkedHashSet | 改善倍率（n=100000） |
|------|---------------------|---------------|---------------------|
| `contains(e)` | O(n) | O(1) | ~100,000 倍 |
| `remove(e)` | O(n) コピー | O(1) | ~100,000 倍 |
| `get(i)` | O(1) | O(n) | 低下（回避策あり） |

ただし `getCommonPrefix()` の実装が `get(0)` / `get(size-1)` に依存しているため、
先頭・末尾の取得を効率的にする工夫（`TreeSet` の使用など）が必要。

**推奨構成:**

```java
// TreeSet で自然順序を保持 → first()/last() が O(log n)
// contains/remove が O(log n)
private final TreeSet<String> survivalSet = new TreeSet<>();
```

`CopyOnWriteArrayList` の O(n) コピーと比べると、`TreeSet` の O(log n) は桁違いに速い。

### 2-2. Aho-Corasick アルゴリズム（多パターン同時検索）

**現行の問題点：**

SL アルゴリズムでは、サバイバルリストの全要素（最大 100,000 件）に対して
`currentPi.indexOf(target)` を呼んでいる。つまり：

- テキスト長さ `L` = `unitLength` ≒ 380,000 桁
- パターン数 `P` = サバイバルリストサイズ ≒ 100,000 件
- パターン長 `M` = 桁数（1〜N桁）

現行計算量：**O(L × P × M)**

Aho-Corasick は全パターンを同時にテキストを 1 回スキャンして検出できる：

- 構築：O(P × M)
- 検索：**O(L + マッチ数)**

**例：6 桁の場合**
- P = 1,000,000（100 万件）、M = 6、L = 380,000
- 現行（SL）：O(380,000 × 1,000,000 × 6) = O(2.28 × 10¹²）
- Aho-Corasick：O(380,000 + マッチ数) ≒ O(380,000)
- **理論上約 600 万倍の差**

**実装方法（Java）：**

Java では Apache Commons Text の `AhoCorasickSearch` や、
`ahocorasick` ライブラリ（`org.ahocorasick:ahocorasick`）が利用できる。

```java
// ライブラリ例
// <dependency>
//   <groupId>org.ahocorasick</groupId>
//   <artifactId>ahocorasick</artifactId>
//   <version>0.6.3</version>
// </dependency>

Trie trie = Trie.builder()
    .onlyWholeWords()
    .addKeywords(survivalList)
    .build();

Collection<Emit> emits = trie.parseText(piUnitString);
for (Emit emit : emits) {
    String found = emit.getKeyword();
    long position = unitStartDigit + emit.getStart();
    survivalList.discover(found, position);
}
```

**注意点：**
- サバイバルリストが変化するたびに Trie の再構築が必要
- 再構築コスト O(P × M) が発生するため、バッチ削除（複数発見後にまとめて再構築）が有効
- LC アルゴリズムと統合することで、さらに高い効果が期待できる

### 2-3. Boyer-Moore の最適化

**現行の問題点：**

```java
// SearchThread.java - LC アルゴリズム内ループ
while (true) {
    startPos = boyerMooreSearch(currentPi.getData(), leftCommonStr, commonSeekPos);
    // ...
}

private int[] buildBadCharacterTable(String pattern) {
    // ← この処理が boyerMooreSearch() 内で毎回呼ばれている
}
```

`boyerMooreSearch()` が呼ばれるたびに `buildBadCharacterTable()` が再実行されている。
`leftCommonStr` は同じユニット処理中は変化しないため、一度だけ構築すれば十分。

**改善案：**

```java
// キャッシュ化
private String cachedPattern = null;
private int[] cachedBadCharTable = null;

private int boyerMooreSearch(String text, String pattern, int startIndex) {
    if (!pattern.equals(cachedPattern)) {
        cachedBadCharTable = buildBadCharacterTable(pattern);
        cachedPattern = pattern;
    }
    // ... 以降は cachedBadCharTable を使用
}
```

さらに、現行実装は「バッドキャラクタルール」のみで「グッドサフィックスルール」を使っていない。
グッドサフィックスルールを追加すると、最悪計算量が O(n/m) に改善される。

### 2-4. getCommonPrefix の最適化

**現行の問題点：**

```java
// SurvivalList.java
public String getCommonPrefix() {
    String max = this.get(this.size() - 1);  // O(1) だが get(0) との比較
    for (int i = 0; i < this.length; i++) {
        commonLeftStr = this.get(0).substring(0, i + 1);
        if (commonLeftStr.equals(max.substring(0, i + 1))) { continue; }
        return commonLeftStr.substring(0, commonLeftStr.length() - 1);
    }
    return commonLeftStr;
}
```

このメソッドは `SearchThread.run()` から毎回呼ばれる。`TreeSet` 化した場合は `first()` と
`last()` で O(log n) で取得できる。また、リストが変化したときだけ再計算する「キャッシュ付き
プロパティ」にすることで更なる高速化が可能。

### 2-5. アルゴリズム改善のまとめ

| 改善案 | 実装難度 | 効果 | 推奨度 |
|--------|---------|------|-------|
| SurvivalList → TreeSet 化 | 低〜中 | 大（O(n)→O(log n)） | ◎ |
| Aho-Corasick 多パターン検索 | 中 | 非常に大（SL フェーズで圧倒的） | ◎ |
| Boyer-Moore テーブルキャッシュ | 低 | 小〜中 | ○ |
| グッドサフィックスルール追加 | 中 | 中 | ○ |
| getCommonPrefix キャッシュ化 | 低 | 小 | △ |

---

## ３．パラメータ調整アプローチ

### 3-1. unitLength（1 回の読み込み桁数）

**現行値：** `unitLength=380000`（README のクイックスタートでは `1900`、`default.properties` では `380000`）

`unitLength` はファイルから 1 度に読み込む円周率の桁数であり、以下のトレードオフがある。

| unitLength | メモリ使用量 | IO 頻度 | 検索スレッドへの影響 |
|------------|------------|---------|------------------|
| 小（例：19,000） | 低 | 高（頻繁に IO） | 検索単位が小さく IO 待ちが多い |
| 大（例：3,800,000） | 高 | 低（まとめて IO） | 検索単位が大きく 1 スレッドの処理時間が長い |
| 最適（例：380,000〜1,900,000） | 中 | 中 | IO と CPU がバランス |

**最適値の算出方法：**

1. `unitLength / 19` = 1 回に読む 8 バイトブロック数
2. 1 回の IO で読むバイト数 = `(unitLength / 19) × 8`
3. `BufferedInputStream` のバッファサイズ（デフォルト 8,192 バイト）に対して
   1 ユニット分の IO が何回に分かれるかを考慮する

**推奨：**

`unitLength` は 19 の倍数かつ、JVM の String 処理で GC が多発しないサイズに設定する。
Raspberry Pi 4（4 GB RAM）では `1,900,000`〜`3,800,000` 程度が実用的と考えられる。
ただし `SearchThread` の処理時間が IO 時間を上回るとパイプラインが無効になるため
プロファイリング（`-Xss`、`-Xmx` フラグ付き実行と `jvisualvm` 等）で確認すること。

### 3-2. listSize（1 サイクルのサバイバルリスト件数）

**現行値：** `listSize=100000`

`listSize` は 1 回の `survive()` で対象とする候補数（検索対象の自然数の数）。

- **小さくすると：** チェックポイント（保存）頻度が増える。中断時のやり直しコストが小さい。
  各サイクルで LC アルゴリズムが有効な期間が長くなる（共通左部分が多くなりやすい）。
- **大きくすると：** チェックポイント頻度が減る。中断時のやり直しコストが大きい。
  各サイクルで SL アルゴリズムを使う期間が長くなる（サバイバルリストの多様性が高い）。

**特に高桁数（6 桁以上）での挙動：**

6 桁では候補数が 1,000,000 件（100 万件）に達する。
`listSize=100000` とすると 10 回に分割して検索が行われる。
LC アルゴリズムの効果は各バッチの先頭部分（共通プレフィックスが残る間）だけ発揮される。

**推奨：**

Aho-Corasick を採用する場合は `listSize` を大きくとるほど Trie の再構築頻度が下がり有利。
`listSize = 1,000,000`（全候補 6 桁分）をまとめて 1 サイクルとすることが理想的。
メモリが制約となる場合は Trie のメモリ使用量と相談して調整する。

### 3-3. BufferedInputStream のバッファサイズ

**現行実装：**

```java
// YCD_SeqBlockStream.java
this.fileStream = new BufferedInputStream((InputStream) fi);
// デフォルトバッファサイズ = 8,192 バイト
```

YCD ファイルは 8 バイトブロックの連続データであり、シーケンシャルアクセスのみ。
デフォルトの 8,192 バイト（= 1,024 ブロック分 = 19,456 桁分）は小さい可能性がある。

**改善案：**

```java
// バッファを 1 MB に拡大（シーケンシャル読み込みに有効）
this.fileStream = new BufferedInputStream(fi, 1024 * 1024);
```

`unitLength=380000` の場合、1 ユニット = 約 160,000 バイトの YCD データが必要。
バッファを 1 MB 以上にすれば OS システムコール回数を大幅に削減できる。

また、Java の NIO (`FileChannel` + `ByteBuffer`) を使ったメモリマップドファイルへの切り替えも
有効である（後述「根本改善アプローチ」参照）。

### 3-4. JVM パラメータ調整

高桁数処理では大量の `String` オブジェクトが生成される。JVM のヒープとGC設定が
パフォーマンスに直結する。

**推奨 JVM オプション例：**

```bash
java -Xmx2g -Xms1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -jar Last-Appearance-in-PI-1.0-SNAPSHOT-jar-with-dependencies.jar default.properties
```

- `-XX:+UseG1GC`：大ヒープに適した GC（停止時間の予測可能性が高い）
- `-XX:+UseStringDeduplication`：同一内容の `String` をヒープ上で共有（G1GC と併用）
- `-Xmx2g`：Raspberry Pi 4（4GB）では 2GB 程度が安全

### 3-5. パラメータ調整のまとめ

| パラメータ | 現行値 | 推奨範囲 | 主な効果 |
|-----------|--------|---------|---------|
| `unitLength` | 380,000 | 1,900,000〜3,800,000 | IO 回数削減、パイプライン効率向上 |
| `listSize` | 100,000 | 500,000〜1,000,000（Aho-Corasick 併用時） | Trie 再構築コスト削減 |
| BufferedInputStream バッファ | 8,192 B | 1,048,576 B (1 MB) | IO システムコール削減 |
| JVM ヒープ（`-Xmx`） | デフォルト | 2,048 MB | GC 頻度の削減 |

---

## ４．根本改善・変更アプローチ

### 4-1. 処理言語変更：C++ / Rust / Go

**現行の Java 実装の限界：**

| 要因 | Java | C++ / Rust |
|------|------|-----------|
| オブジェクト・文字列の GC オーバーヘッド | あり（停止が発生） | なし（手動 or RAII） |
| JIT コンパイル後の性能 | 高いがウォームアップが必要 | ネイティブコードで即座に最高性能 |
| SIMD 命令の利用 | JVM が内部的に使用、制御困難 | 直接記述可能（intrinsics） |
| メモリ配置の最適化 | JVM が管理、制御困難 | 完全に制御可能 |

**C++ による Boyer-Moore の SIMD 最適化例（概念）：**

```cpp
// AVX2 を使って 32 バイト同時比較
#include <immintrin.h>
__m256i pattern_vec = _mm256_set1_epi8(pattern[0]);
__m256i text_chunk  = _mm256_loadu_si256((__m256i*)(text + i));
__m256i match       = _mm256_cmpeq_epi8(pattern_vec, text_chunk);
int mask = _mm256_movemask_epi8(match);
// mask の各ビットがマッチ位置を示す
```

**Rust による実装のメリット：**
- メモリ安全性が保証されつつ C++ と同等の速度
- `rayon` クレートで簡単にデータ並列処理が実現できる
- `memchr` クレート：Boyer-Moore より高速な SIMD ベースのメモリ検索

**期待効果：** Java 比で単純な文字列検索処理において 3〜10 倍の速度向上。GC が発生しないため
レイテンシが安定する。

**移行コスト：** 大（全コードの書き直し）。YCD ファイルフォーマットの読み込みモジュール、
Web UI、状態保存・再開機能など、現行の全機能を再実装する必要がある。

### 4-2. GPU 活用（CUDA / OpenCL）

**適用範囲：**

GPU は大量の並列演算に特化している。円周率文字列から多数のパターンを同時に探索する
このタスクは、GPU 並列処理と非常に相性が良い。

**CUDA による多パターン検索の概念：**

```
GPU カーネル（1スレッド = 1 候補数字を担当）
    ├── スレッド 0: "000000" を π 文字列から検索
    ├── スレッド 1: "000001" を π 文字列から検索
    ├── スレッド 2: "000002" を π 文字列から検索
    ...
    └── スレッド 999999: "999999" を π 文字列から検索

→ 100 万スレッドが同時に 1 スレッド 1 桁ずつ比較
```

**NVIDIA GeForce RTX 3090 の場合：**
- CUDA コア数：10,496
- 理論演算性能：35.6 TFLOPS（FP32）
- メモリ帯域：936 GB/s

`unitLength=380,000` の π 文字列を GPU メモリに転送し、
CUDA カーネルで全サバイバルリストを並列検索すると、
**1 ユニットあたりの検索が数ミリ秒以下**になる可能性がある。

**Java からの CUDA 利用方法：**

1. **JCuda**（Java CUDA バインディング）を使用して Java から CUDA カーネルを呼び出す
2. カーネルの実装は CUDA C/C++ で記述し、`nvcc` でコンパイル
3. Java 側から JNI 経由でカーネルを起動

**課題：**
- Raspberry Pi 4 は CUDA 非対応（ARM GPU）
- 現行の稼働環境（Raspberry Pi 4）では適用不可
- 開発コストが高い
- GPU メモリ転送のオーバーヘッドが小さいユニットサイズでは逆に遅くなる可能性

**代替案：OpenCL（ARM GPU 対応）**

Raspberry Pi 4 の VideoCore VI GPU は OpenCL を部分的にサポートする。
ただし演算性能はデスクトップ GPU と比べて大幅に低く、現実的な高速化は限定的。

### 4-3. SIMD 命令による文字列検索の高速化

**x86_64 環境での適用（開発・テスト用サーバーに有効）：**

SIMD (Single Instruction Multiple Data) を使うと、CPU が一度に複数バイトを比較できる。

- SSE4.2：`_mm_cmpistrm` 命令で 16 バイトの文字列比較が 1 命令で可能
- AVX2：256 ビット幅で 32 バイトを同時処理
- AVX-512：512 ビット幅で 64 バイトを同時処理

**Java での SIMD 活用（Project Valhalla / Panama）：**

Java 19 以降で導入された `java.lang.foreign` API と Vector API（Project Panama）を使うと
Java から SIMD 命令を直接利用できる。

```java
// Java Vector API（インキュベータ段階、Java 19+）
import jdk.incubator.vector.*;

// 256 ビット幅で 32 文字を同時処理する例（概念）
VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_256;
ByteVector textVec    = ByteVector.fromArray(SPECIES, textBytes, offset);
ByteVector patternVec = ByteVector.broadcast(SPECIES, (byte)pattern.charAt(0));
VectorMask<Byte> mask = textVec.compare(VectorOperators.EQ, patternVec);
```

**期待効果：** 純粋な Java の `String.indexOf` と比べて 4〜16 倍の文字列検索速度。

### 4-4. メモリマップドファイル（MappedByteBuffer）

**現行の IO 実装：**

```java
// YCD_SeqBlockStream.java
this.fileStream = new BufferedInputStream(new FileInputStream(filePath));
```

`BufferedInputStream` はユーザー空間のバッファを介してデータを読む。
`MappedByteBuffer`（メモリマップド IO）を使うと OS のページキャッシュを直接 Java から参照でき、
ユーザー空間へのコピーが不要になる。

**実装例：**

```java
// NIO メモリマップドファイル
try (FileChannel channel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ)) {
    MappedByteBuffer buffer = channel.map(
        FileChannel.MapMode.READ_ONLY, headerOffset, channel.size() - headerOffset);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    while (buffer.hasRemaining()) {
        long block = buffer.getLong(); // 8 バイト = 19 桁
        String digits = Long.toUnsignedString(block);
        // ...
    }
}
```

**期待効果：**
- OS のページキャッシュと直接連携するため、IO スループットが向上
- 特に大きな `unitLength` 設定時や、YCD ファイルが SSD 上にある場合に有効
- Raspberry Pi で SD カードを使用している場合の効果は限定的

### 4-5. データ形式変換（YCD → プレーンテキスト前処理）

**現行の処理フロー：**

```
YCD バイナリ → (毎回) 8 バイト×ブロック → Long.reverseBytes() → toUnsignedString() → 文字列検索
```

YCD ファイルを 1 回だけデコードして、10 進テキスト形式（1 行 = 1 桁 or N 桁）として
保存しておけば、実行時の変換コストが完全になくなる。

**前処理スクリプト（Python イメージ）：**

```python
# pi_digits_0.txt に変換
with open("Pi - Dec - Chudnovsky - 0.ycd", "rb") as f:
    header = f.read(header_size)
    while True:
        block = f.read(8)
        if not block: break
        val = int.from_bytes(block, 'little')
        print(f"{val:019d}", end="", file=out)
```

**デメリット：**
- YCD 形式は 8 バイト（64 ビット）に 19 桁を格納しており、1 バイトあたり約 2.4 桁を表現している。
  これを 1 バイト = 1 桁のテキストに展開すると、ファイルサイズは約 2.4 倍に膨張する。
  9 TB の YCD データをデコードすると約 21 TB のテキストデータが生成される。
- 初期変換に数日〜数週間かかる可能性

**現実的な代替案：** YCD のデコード結果を LZ4 や Snappy で圧縮保存する。
これらは高速な圧縮・展開（GB/s オーダー）が可能で、ファイルサイズを削減しつつ
変換コストを排除できる。

### 4-6. 分散処理（Apache Spark / 独自クラスタ）

**考え方：**

```
各 Worker ノードに円周率データの一部を割り当て、並列検索する

Worker-1: π の  1 桁目 〜  5 兆桁目
Worker-2: π の  5 兆桁目 〜 10 兆桁目
Worker-3: π の 10 兆桁目 〜 15 兆桁目
Worker-4: π の 15 兆桁目 〜 22.6 兆桁目

→ 全 Worker の結果をマージして、最も深い位置を特定
```

**Apache Spark によるアプローチ：**

```scala
// Spark RDD による分散検索（概念）
val piRdd = sc.textFile("hdfs://pi_data/*.txt")  // 分散ストレージ
val survivalBC = sc.broadcast(survivalList)      // サバイバルリストをブロードキャスト

val results = piRdd.mapPartitions { partition =>
  val localSurvival = survivalBC.value
  partition.flatMap { piChunk =>
    searchAll(piChunk, localSurvival) // Aho-Corasick 等で検索
  }
}

val deepest = results.maxBy(_.findPos)
```

**課題：**
- π のデータが 9 TB あり、HDFS 等の分散ストレージが必要
- ネットワーク転送コストが高い
- 現行の Raspberry Pi 4 単体環境では適用不可
- クラスタ構築コストが高い

**小規模クラスタ（Raspberry Pi 複数台）での適用：**

複数の Raspberry Pi を所有する場合、REST API で各ノードが担当範囲を検索し
結果を集約するシンプルな実装でも、ノード数に比例した速度向上が期待できる。

### 4-7. 根本改善アプローチのまとめ

| 案 | 実装難度 | 効果 | 現稼働環境適用可否 |
|----|---------|------|-----------------|
| C++ / Rust への言語変更 | 非常に大 | 非常に大 | 可（要全面書き直し） |
| GPU 活用（CUDA） | 大 | 非常に大 | 不可（RPi4 非対応） |
| GPU 活用（OpenCL on RPi4） | 大 | 小〜中 | 可（部分的） |
| SIMD 命令（Java Vector API） | 中 | 中〜大 | 可（Java 19+ が必要） |
| メモリマップドファイル | 低〜中 | 中 | 可 |
| データ形式変換（事前デコード） | 低 | 中（変換後） | 可（ストレージ 2 倍必要） |
| 分散処理（Spark） | 非常に大 | 非常に大 | 不可（単体環境） |
| RPi クラスタ（独自実装） | 中〜大 | 大（台数倍） | 可（RPi 複数台が必要） |

---

## 総合評価と推奨アクション

### 実施優先度マトリクス

```
効果
大  │  [Aho-Corasick]        [並列処理 A+C]
    │  [SurvivalList→TreeSet] [C++/Rust 移行]
    │
中  │  [unitLength 拡大]     [listSize 拡大]
    │  [Boyer-Moore キャッシュ] [MappedByteBuffer]
    │
小  │  [BIS バッファ拡大]    [JVM チューニング]
    └─────────────────────────────────────────→ 実装難度
         低      中      高      非常に高
```

### 段階的改善ロードマップ

**Phase 1（短期・低リスク・現 Java/RPi4 環境で実施可能）:**

1. `BufferedInputStream` バッファを 1 MB に拡大
2. `boyerMooreSearch` のバッドキャラクタテーブルをキャッシュ化
3. `unitLength` を 1,900,000 に拡大してテスト
4. JVM オプション（`-Xmx2g -XX:+UseG1GC`）の最適化

**Phase 2（中期・中難度・現 Java/RPi4 環境で実施可能）:**

1. `SurvivalList` を `TreeSet<String>` ベースに書き直し
2. 非同期先読み（`CompletableFuture`）の導入
3. Aho-Corasick ライブラリの導入（SL アルゴリズムの置き換え）
4. BlockingQueue によるプロデューサー・コンシューマーパターンへの移行

**Phase 3（長期・高難度・環境変更を伴う）:**

1. Java 19+ への移行と Java Vector API (SIMD) の実験
2. C++ or Rust による検索コアの実装（JNI 経由で呼び出し）
3. 複数 Raspberry Pi を用いたクラスタ化（担当範囲分割）

### 期待される累積効果

| フェーズ | 主な改善 | 期待速度向上 |
|---------|---------|------------|
| Phase 1 のみ | パラメータ最適化 | 1.2〜1.5 倍 |
| Phase 1+2 | アルゴリズム改善 + 並列化 | 10〜50 倍 |
| Phase 1+2+3 | 言語変更 or クラスタ化 | 100〜1000 倍以上 |

---

## おわりに

本プロジェクトは「趣味・エンタメ」を最重要目的としており、README にも「検索スピードは問わない」
と明記されている。現状の Raspberry Pi 4 上での Java 実装は、非力なハードウェアでも長期稼働可能な
設計として合理的である。

高速化を追求する場合、最も費用対効果が高いのは以下の組み合わせと考えられる：

> **SurvivalList の TreeSet 化 ＋ Aho-Corasick による多パターン検索 ＋ 非同期先読み（CompletableFuture）**

これらはすべて現行の Java/RPi4 環境で実装可能であり、理論上 10〜50 倍の性能向上が期待できる。
ただし、どの改善も「探索過程のリアルタイム公開」「中断・再開可能」という設計原則を
損なわないよう慎重に実装することが重要である。

