**【リアルタイム進捗はこちら】**  
![image](https://github.com/genhirano/Last-Appearance-in-PI/assets/3538386/0bc97c88-770c-478b-abea-cdea4dd180f7)
 [https://genhirano.coresv.com/](https://genhirano.coresv.com/)

---

# Last-Appearance-in-PI
円周率の中で最も遅く出現する数値の探索

## プロローグ
「円周率の中には、この世の中のすべての整数列が含まれている」 と言われています。  
* 例えば
  * 静数列「999999」（ゼロが6個連続）は762桁目に出現します。  
  * 静数列「12345678」は円周率の少数以下186,557,266桁目に出現します。	

どうやら、このようなレアな数列も出現するようです。

**でも、「すべて」って本当でしょうか。**

> [NOTE]
> 静数列「999999」（ゼロが6個連続）が出現する762桁目は「ファインマン・ポイント」[(→Wikipedia)](https://ja.wikipedia.org/wiki/ファインマン・ポイント)と呼ばれています。

## 概要
「円周率の中には、この世の中のすべての整数列が含まれている」らしい。  
これは検索すれば確認できます。

円周率は予測不能ですので、すべての整数列が円周率に含まれていることを証明するためには、**ひとつづず総当たりで探す**しかありません。

* 1桁：[0,1,2,3,4,5,6,7,8,9]の10個
* 2桁：[00,01,02...97,98,99]の100個
* 3桁：[000,001,002...998,999]の1,000個
* :
* 8桁：[00000000...99999999]の100,000,000個
* :
* N桁：[0{N}...9{N}]の10のN乗個

「円周率の中には、この世の中のすべての整数列が含まれている」のであれば、すべて発見できるはずです。

**各桁ごとに、出現が一番遅かった静数列を特定することができれば、** その桁の静数列はすべて出現したと言えます。  
これは目視では（少なくとも私には）難しいので、プログラムを作成して実行します。
> [NOTE]
>そのプログラムソースコードがこのリポジトリに格納されています。

### プログラムの方針
「この世の中のあらゆるすべての整数列」を、**桁数ごとにひとつづつ円周率データの中から検索し、一番最後に出現した静数列を特定する。** 
* 円周率のデータ
  * 円周率データはネットで公開されているものをダウンロードして使用します。
  * [利用している円周率データ（Decimal）](https://drive.google.com/drive/folders/1L_HnNULhHSuDabD036H94pGdD-XbKhLy)
* アルゴリズム
  * 桁数毎にすべての整数列を検索し、その桁数の静数列で一番最後（一番深い位置）に出現した静数列を一つ特定する。
  * 整数列は左ゼロパディングする。
  * 1桁から開始し、制限なくN桁まで実行する。
* 膨大なデータに対応すること
  * 円周率データも整数列も（おそらく）無限であるため、富豪的なアルゴリズムを採用しない。
* 中断に寛容であること
  * 処理が中断されても最初からやり直しにならないように、途中経過は保存され、保存された時点から再開可能であること
* 検索状況は目視でいつでも確認できること
  * ただ悶々と検索処理をするのではなく、検索処理の途中経過をリアルタイムで目視確認できること
* 達成すべき事項とその優先順位
  1. 処理状況がリアルタイムで確認できること
  2. プログラムの中断による検索のやり直しが最小限であること
  3. 非力なコンピュータでも実行できること 
  4. 検索スピード 
> [!NOTE]
> [現在のプロジェクト稼働環境](https://genhirano.coresv.com/)では現在、円周率データ22,600,000,000,000桁(22兆桁)を対象に実行されています。このデータは約9TBです。

## 偉大なる先人たち
> [!NOTE]
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


## 具体例
### １桁の場合の例
1桁の"0"は、[0..9]の数値のうちで最も遅く出現する数字です。"0"が初めて出現するのは少数以下32桁目です。"0"以外の"1"-"9"は、13桁目までに1回以上出現するので、32桁目に初めて出現する"0"は驚くべき出現の遅さです。

3.
1415926535897932384626433832795 **( 0 )** 288419716......

よって、対象が1桁の場合は  
**「円周率の中で最も遅く出現する1桁の数字は"0"で、出現場所は円周率少数以下32桁目」**  
となります。

### 2桁の場合
それでは、2桁の場合はどうでしょうか。  
"00"からはじめて、"01","02","03"..."99"と、2桁で表せる数列100個をすべて検索すると、最も遅く出現するのは"68"で、605桁目に出現します。"68"以外はそれより前にすべて出現済みです。

3.
1415926535 8979323846 2643383279 5028841971 6939937510
5820974944 5923078164 0628620899 8628034825 3421170679
8214808651 3282306647 0938446095 5058223172 5359408128
4811174502 8410270193 8521105559 6446229489 5493038196
4428810975 6659334461 2847564823 3786783165 2712019091
4564856692 3460348610 4543266482 1339360726 0249141273
7245870066 0631558817 4881520920 9628292540 9171536436
7892590360 0113305305 4882046652 1384146951 9415116094
3305727036 5759591953 0921861173 8193261179 3105118548
0744623799 6274956735 1885752724 8912279381 8301194912
9833673362 4406566430 8602139494 6395224737 1907021798
6094370277 0539217176 2931767523 8467481846 7669405132
0005 **( 68 )** 1271 4526356082 7785771342 7

対象が2桁の場合、結果は  
**「円周率の中で、最も遅く出現する2桁の数列は"68"で、出現場所は円周率少数以下605桁目」**  
となります。

### 3桁の場合、4桁の場合... and more...
桁が増えても同様です。それぞれ同じ処理をします。


## IMAGE
![image](https://github.com/genhirano/Last-Appearance-in-PI/assets/3538386/bca89ca7-f0df-49ab-98df-bdd8591b3b27)

## 特徴
* Resumable（中断・再開が可能）
  * プログラムは途方も無い時間を必要とします。実行後に思いついたプログラム実装を適用したり、意図しないプログラムの中断やコンピュータのダウンなどが発生したとき、最初からやり直したくありません。このプログラムは、そういったプログラムの途中終了後に再度実行しても途中から再開できます。
* YCD file format reading is modularized for user-friendly use（YCDファイル読み込みを使いやすい形にモジュール化した）
  * 膨大な円周率が格納されている"YCD"形式ファイルは保管効率のために圧縮されています。保管には都合が良いこの形式ですが、プログラムから直接利用しようとしたときに不便なこともあります。このプロジェクトでは、[y-cruncher](http://www.numberworld.org/y-cruncher/)プログラムが生成する"YCD"形式ファイルをJavaで直接扱えるように、読み込み部分のプログラムを隔離して明確にモジュール化しました。YCD_SeqProviderクラスは、複数の連続YCDファイルから、ファイルをまたぐ場合も、連続して指定した桁数ずつ切り出し提供できる、拡張forループが利用可能なモジュールを作りました。
* An embedded web server is included.（組み込みWEBサーバー）
  * プログラムにはWEBサーバーが組み込まれています。プログラムを開始するだけでWEB画面により計算中の様子が確認できます。

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
* Create properties file "default.properties"
  * Please place the program in the location where it will be executed.
``` default.properties
# Web server port
port=8080

# result output path (結果ファイルの吐き出し先パス)
outputPath=./

# searches per cycle (1サイクルあたりの検索数)
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
