**【リアルタイム進捗はこちら】**  
![image](https://github.com/genhirano/Last-Appearance-in-PI/assets/3538386/0bc97c88-770c-478b-abea-cdea4dd180f7)
 [https://genhirano.coresv.com/](https://genhirano.coresv.com/)

---

# Last-Appearance-in-PI
円周率の中で最も遅く出現する自然数の探索

## プロローグ
「円周率の中には、この世の中のすべての自然数が含まれている」 と言われています。  
* 例えば
  * 「999999」（ゼロが6個連続）は762桁目に出現します。  
  * 「12345678」は186,557,266桁目に出現します。	

このような特徴的なもの、その他のレアな数列もどうやらすべて出現するようです。

でも、**「すべて」って本当でしょうか。**

確認してみたくなりました。

> [!NOTE]
> 数列「999999」（ゼロが6個連続）が出現する762桁目は「ファインマン・ポイント」[(→Wikipedia)](https://ja.wikipedia.org/wiki/ファインマン・ポイント)と呼ばれています。

## アプローチ
円周率は予測不能ですので、すべての自然数が円周率に含まれていることを証明するためには、**ひとつづず総当たりで探す**しかないようです。  
自然数は無限にありますので、**桁数毎に**検索します。

まずは、1桁。

3.141592653589793238462643383279502884......

| 対象 | 出現する場所<br>少数以下 |
| ---- | ------------------------ |
| "0"  | 32 桁目 ※最遅出現        |
| "1"  | 1 桁目                   |
| "2"  | 6 桁目                   |
| "3"  | 9 桁目                   |
| "4"  | 2 桁目                   |
| "5"  | 4 桁目                   |
| "6"  | 7 桁目                   |
| "7"  | 13 桁目                  |
| "8"  | 11 桁目                  |
| "9"  | 5 桁目                   |

見事に、全部出現しました。  
ということで、「1桁の自然数は全て出現する」が確認できました。

次に、2桁でやってみましょう。

| 対象 | 出現する場所<br>少数以下 |
| ---- | ------------------------ |
| "00" | 307 桁目                 |
| "01" | 167 桁目                 |
| "02" | 32 桁目                  |
| "03" | 85 桁目                  |
| "04" | 270 桁目                 |
| :    | :                        |
| "68" | 605 桁目 ※最遅出現       |  |
| :    | :                        |
| "95" | 30 桁目                  |
| "96" | 180 桁目                 |
| "97" | 12 桁目                  |
| "98" | 80 桁目                  |
| "99" | 44 桁目                  |

2桁の場合も見事に全部出現します。    

さあ、3桁、4桁、5桁...N桁の自然数を対象にやってみましょう！！
目視では、もう無理そうですね、、

> [!NOTE]
>検索対象自然数の数  
>  1桁：[0,1,2,3,4,5,6,7,8,9]の10個  
>  2桁：[00,01,02...97,98,99]の100個  
>  3桁：[000,001,002...998,999]の1,000個  
>  :  
>  8桁：[00000000...99999999]の100,000,000個  
>  :  
>  N桁：[0{N}...9{N}]の10のN乗個  

数桁であっても膨大な検索量ですが、「円周率の中には、この世の中のすべての自然数が含まれている」のであれば、すべての桁で全て発見できるはずです。。。  
でも目視ではとても難しそうです。。。

**対象桁において、「一番遅く出現する自然数」が特定できれば、その対象桁の自然数はすべて出現したと言えます。このアプローチで証明できそうです。**

> [!NOTE]
>このプロジェクトにおいてN桁の自然数とは、N桁に左ゼロ埋めされた自然数をいいます。  
> 例：4桁の自然数での「6」は、「0006」とします。

## プログラム「Last-Appearance-in-PI」
円周率の中で一番遅く出現する自然数の検索
### プログラムの方針
**円周率データから、１桁から始めて２桁、３桁と次々にすべての自然数を検索し、桁数ごとに一番最後に出現する自然数を特定する。** 
* 円周率のデータ
  * 円周率データはネットで公開されているものをダウンロードして使用します。
  * [利用している円周率データ（Decimal）](https://drive.google.com/drive/folders/1L_HnNULhHSuDabD036H94pGdD-XbKhLy)
* アルゴリズム
  * 1桁、2桁、3桁と、桁数毎にすべての自然数を検索し、その桁数の自然数で一番最後（一番深い位置）に出現した自然数を一つ特定する。
  * 自然数は対象桁で左ゼロパディングする。
  * 1桁から開始し、制限なくN桁まで実行する。
* 膨大なデータに対応する
  * 円周率データも自然数も（おそらく）無限であるため、ストレージやメモリの富豪的アルゴリズムを採用しない。
* 中断に寛容であること
  * 処理が中断されても最初からやり直しにならないように、途中経過は随時保存され、保存された時点から再開可能であること
* 検索状況は目視でいつでも確認できること
  * ただ悶々と検索処理をするのではなく、検索処理の途中経過をリアルタイムで目視確認できること
* 達成すべき事項とその優先順位
  1. 処理状況がリアルタイムで目視確認できること
  2. プログラムの中断による検索のやり直しコストが最小限であること
  3. 大きな桁においても、非力なコンピュータで実行できること 
  4. 検索スピードは問わない（遅くてよい） 

> [!NOTE]
> [現在のプロジェクト稼働環境](https://genhirano.coresv.com/)では現在、円周率データ(10進数)22,600,000,000,000桁(22兆桁)を対象に実行されています。このデータ量は合計約9TBです。

## 実行中の画面イメージ
![Screenshot_20240511-144810.png](https://github.com/genhirano/Last-Appearance-in-PI/assets/3538386/be791931-3a28-4d90-9ad5-d51bfd7e57a7)

### 特徴
* 中断・再開が可能
  * プログラムは途方も無い時間を必要とします。実行後に思いついたプログラム実装を適用したり、意図しないプログラムの中断やコンピュータのダウンなどが発生したとき、最初からやり直したくありません。このプログラムは、そういったプログラムの途中終了後に再度実行しても途中から再開します。

* 非富豪的なアプローチ
  * 非力なコンピュータでも実行可能なように設計しています。例えば、円周率の中に自然数を発見するたびにその自然数をすべてメモしておくようなことはできません。このプログラムは、桁数が大きくなっても（検索効率と引き換えに）非力なコンピュータで処理できるリソース範囲で実行できます。
  *  [現在の稼働環境](https://genhirano.coresv.com/) - [Raspberry Pi 4](https://www.raspberrypi.com/products/raspberry-pi-4-model-b/)
  *  ![PXL_20240507_111731293](https://github.com/genhirano/Last-Appearance-in-PI/assets/3538386/37cc4fbb-8a0b-4370-bb22-b8822d4774fa)

* 円周率データ（YCDファイル）読み込みをモジュール化した
  * 膨大な円周率が格納されている"YCD"形式ファイルは保管効率のためにバイナリ保存されています。このプロジェクトでは、[y-cruncher](http://www.numberworld.org/y-cruncher/)プログラムが生成する **"YCD形式ファイル"** をJavaで直接扱えるように、読み込み部分のプログラムを隔離して明確にモジュール化しました。YCD_SeqProviderクラスは、YCDファイルから（ファイルをまたぐ場合でも）連続して指定した桁数ずつ切り出し提供できる、拡張forループが利用可能なモジュールを作りました。
  * [YCD File Format](http://www.numberworld.org/y-cruncher/internals/representation.html)

* 組み込みWEBサーバーと静的HTMLファイルコンパイル
  * プログラムにはWEBサーバーが組み込まれています。プログラムを開始するだけでWEBサーバーが起動し、WEBブラウザで検索中の様子がリアルタイムに確認できます。
  * 平行してHTMLファイルを静的コンパイルして作成します。このHTMLファイルを使って、例えばFTPで公開用レンタルサーバーへアップロードするなど活用できます。 

## 注意
* Just as a hobby（あくまでも趣味）
  * このプロジェクトはあくまでも個人の趣味によって作成されており、結果の正しさはいかなる観点からも保証しません。
  * このプロジェクトが算出して確認すべき結果データは、すでに偉大な先人によって10桁までネットで公開されています。（結果確認のため、プログラムテスト時に参照させていただいています）
    *  [The On-Line Encyclopedia of Integer Sequences® (OEIS®)](https://oeis.org/A332262)
* Entertainment-oriented mindset（エンタメです）
  * このプロジェクトは探索結果を得るためのものでありますが、その探索過程、探索中の様子をリアルタイムで公開することを最重要視しています。したがって探索アルゴリズムについては合理性（リソースの利用方法、拡張性、検索スピード）への配慮は優先順位を落としています。 
  * 原始的な総当り検索アルゴリズムで実装されていますので、実用的（笑）なスピードは期待できません。

## USAGE
* Entry Point
  * controller.Main (Java version 11 or higher)
* View Progress
  * your browser "http://localhost:8080"
* Maven
  * Please install external packages that are dependent on using the .pom file of Maven.
  ``` 
  mvn install
  ``` 
* Create properties file "default.properties"
  * Please place the program in the location where it will be executed.
``` default.properties
# Web server port
port=8080

# output path (経過保存用ファイルの吐き出し先パス)
outputPath=./

# searches per cycle (1ユニットあたりの対象数)
listSize=500

# read pi data per cycle (円周率データの１回の読み込み長さ)
# Multiples of 19 are desirable (19の倍数が望ましい)
unitLength=1900

# Pi files (ycd Files)
ycd000=X:/ycdFile/Pi - Dec - Chudnovsky - 0.ycd
ycd001=X:/ycdFile/Pi - Dec - Chudnovsky - 1.ycd
ycd002=X:/ycdFile/Pi - Dec - Chudnovsky - 2.ycd
ycd003=X:/ycdFile/Pi - Dec - Chudnovsky - 3.ycd
# (and more. Max:999 files)
```

## PI DATA SOURCE
In this project, we are using the output of the pi calculation program "[y-cruncher](http://www.numberworld.org/y-cruncher/)" as the target for the search.  
このプロジェクトでは、円周率計算プログラム「[y-cruncher](http://www.numberworld.org/y-cruncher/)」が出力した計算結果を検索対象としています。

Data Download
https://drive.google.com/drive/folders/1L_HnNULhHSuDabD036H94pGdD-XbKhLy


## SPECIAL THANKS!
* [y-cruncher - A Multi-Threaded Pi-Program](http://www.numberworld.org/y-cruncher/)
  * Alexander J. Yee  - y-cruncher is a program that can compute Pi and other constants to trillions of digits. 
* [OpenJDK](https://openjdk.org/)
  * The place to collaborate on an open-source implementation of the Java Platform, Standard Edition, and related projects.
* [Spark Framework](https://sparkjava.com/)
  * micro framework for creating web applications in Kotlin and Java 8 with minimal effort
* [Bluma](https://bulma.io/)
  * CSS Framework 
* [Octicons](https://github.com/primer/octicons)
  * Octicons are a set of SVG icons
* [Apatch Maven](https://maven.apache.org/)
  * software project management and comprehension tool. 
* [Intellij idea](https://www.jetbrains.com/idea/)
  * The IDE that makes development a more productive and enjoyable experience
* [Git Hub](https://github.co.jp/)
  * development platform
* [Git](https://git-scm.com/)
  * free and open source distributed version control system designed to handle everything from small to very large projects with speed and efficiency.
* [Jenkins](https://www.jenkins.io/)
  * The leading open source automation server,
* ~~[Gitlab, Gitlab Pages]~~ 
  * ~~https://docs.gitlab.com/ee/user/project/pages/~~
  * ~~https://www.kageori.com/2023/06/gitlabgitlab-pages2023.html~~
  * ~~（結果公開用にGitLabPagesを利用していましたが、400分/月のパイプラインの利用制限により要件と見合わなくなったため現在未使用）~~
* [Real-World Data Sets](https://introcs.cs.princeton.edu/java/data/)
  * list of real-world data sets collected from the web. 

> [!NOTE]
> **偉大なる先人たち**
> 同様の探索は、偉大な先人によって既に実施されています。   
>
>**[オンライン整数列大辞典](https://oeis.org/)**  
>The On-Line Encyclopedia of Integer Sequences® (OEIS®)  
>[A032510](https://oeis.org/A032510) 桁数毎の最後に出現する整数  
>[A036903](https://oeis.org/A036903) 桁数毎に全てが出現する事を確認するためにスキャンする必要のある桁数  
>
>**[Sequence Machine](https://sequencedb.net/)**  
>Mathematical conjectures on top of 1301509 machine generated integer and decimal sequences.  
>[A032510](https://sequencedb.net/s/A032510)  
>0, 68, 483, 6716, 33394, 569540, 1075656, 36432643, 172484538, 5918289042, 56377726040,...  
>[A036903](https://sequencedb.net/s/A036903)  
>32, 606, 8555, 99849, 1369564, 14118312, 166100506, 1816743912, 22445207406, 241641121048, 2512258603207,...  
