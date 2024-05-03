# Last-Appearance-in-PI
円周率の中で最も遅く出現する数値の探索

### 探索状況（リアルタイム）
[https://genhirano.coresv.com/](https://genhirano.coresv.com/)

## PROJECT OVERVIEW
「円周率の中には、この世の中のあらゆるすべての数列が含まれている」と言われていますが本当でしょうか。  
任意の桁の任意の数列について、必ず円周率の中に出現するのでしょうか。  

これは検索すれば確認できます。

* 方法と手順
  * 「円周率データ」を準備する
  * その円周率データの中から、1桁で表現できる[0,1,2,3,4,5,6,7,8,9]の10個の数をそれぞれ検索し、1桁の数のうち最も深い位置で最後に出現した数と出現位置を特定する
  * 次に、2桁で表現できる全ての数列[00,01,02,03...98.99]の100個についても同様に検索する
  * さらに、3桁で表現できる全ての数列[000,001,002...998.999]の1000個についても同様に実施し、続けて4桁,5桁,5桁...N桁を実施する
  * それぞれの桁において、最も深い位置で最後に出現した数列と場所が特定できたことをもって「その桁で表現できる数列はすべて出現した」を確認する
  * 対象桁数を増やしていき、「円周率の中には、この世の中のあらゆるすべての数列が含まれている」を確認する

総当たり検索は人間には（とてもとても）無理ですので、検索処理及びその処理状況や結果を表示をコンピュータで実施するためのプログラムを作成した。

* 現在進捗
  * [現在の処理進捗](https://genhirano.coresv.com/)

### １桁の場合の例
1桁の"0"は、[0..9]の数値のうちで最も遅く出現する数字です。"0"が初めて出現するのは少数以下32桁目です。"0"以外の"1"-"9"は、13桁目までに1回以上出現するので、32桁目に初めて出現する"0"は驚くべき出現の遅さです。

3.
1415926535897932384626433832795 **( 0 )** 288419716......

よって、対象が1桁の場合は  
**「円周率の中で最も遅く出現する1桁の数字は"0"で、出現桁数は32桁目」**  
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
**「円周率の中で、最も遅く出現する2桁の数列は"68"で、出現桁数は605桁目」**  
となります。

### 3桁の場合、4桁の場合... and more...
桁が増えても同様です。それぞれ同じ処理をします。

## 偉大なる先人たち
> [!NOTE]
> このプロジェクトと同様の探索は、偉大な先人によって既に実施されています。   
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

## WHY?
Why not?  
え?

## IMAGE
![web](https://user-images.githubusercontent.com/3538386/229020000-79905d0f-f9e7-4521-bd27-d94482eebae9.JPG)

## このプロジェクトのポイント
* Just as a hobby（あくまでも趣味）
  * このプロジェクトはあくまでも個人の趣味によって作成されており、結果の正しさはいかなる観点からも保証しません。
  * すでに10桁までの結果はネットで公開されています。
    *  [The On-Line Encyclopedia of Integer Sequences® (OEIS®)](https://oeis.org/A332262)
* Entertainment-oriented mindset（エンタメです）
  * このプロジェクトは探索結果を得るためのものでありますが、その探索過程、探索中の様子をリアルタイムで公開することを最重要視しています。したがって探索アルゴリズムについては合理性（リソースの利用方法、拡張性、検索スピード）への配慮は優先順位を落としています。 
  * 非力なコンピュータを使って実装していますので、実用的なスピードは期待できません。
* Resumable（中断・再開が可能）
  * プログラムは途方も無い時間を必要とします。実行後に思いついたプログラム実装を適用したり、意図しないプログラムの中断やコンピュータのダウンなどが発生したとき、最初からやり直したくありません。このプログラムは、そういったプログラムの途中終了後に再度実行しても途中から再開できます。
* YCD file format reading is modularized for user-friendly use（YCDファイル読み込みを使いやすい形にモジュール化した）
  * 膨大な円周率が格納されている"YCD"形式ファイルは保管効率のために圧縮されています。保管には都合が良いこの形式ですが、プログラムから直接利用しようとしたときに不便なこともあります。このプロジェクトでは、[y-cruncher](http://www.numberworld.org/y-cruncher/)プログラムが生成する"YCD"形式ファイルをJavaで直接扱えるように、読み込み部分のプログラムを隔離して明確にモジュール化しました。YCD_SeqProviderクラスは、複数の連続YCDファイルから、ファイルをまたぐ場合も、連続して指定した桁数ずつ切り出し提供できる、拡張forループが利用可能なモジュールを作りました。
* An embedded web server is included.（組み込みWEBサーバー）
  * プログラムにはWEBサーバーが組み込まれています。プログラムを開始するだけでWEB画面により計算中の様子が確認できます。

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
