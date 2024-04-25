# Last-Appearance-in-PI
円周率の中で最も遅く出現する数値の探索

## OVERVIEW
このプロジェクトは、膨大な桁の「円周率データ」から、最も遅く出現する数を、桁数毎に探索します。


### １桁の場合
例えば”0”は0-9の数値のうちで最も遅く出現する数字です。”0”が初めて出現するのは少数以下32桁目です。”0”以外の”1”-”9”は、13桁目までに1回以上出現するので、32桁目に初めて出現する”0”は驚くべき出現の遅さです。

3.
1415926535897932384626433832795 **( 0 )** 288419716......

対象が1桁の場合、このプロジェクトの結果は  
「円周率の中で、最も遅く出現する1桁の数字は"0"で、出現桁数は32桁目」  
となります。

### 2桁の場合
それでは、2桁の場合はどうでしょうか。  
最も遅く出現するのは"68"でした。605桁目に出現します。2桁の数字で、"68"以外はそれより前にすべて出現済みです。

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

従って対象が2桁の場合、このプロジェクトの結果は  
「円周率の中で、最も遅く出現する2桁の数字は"68"で、出現桁数は68桁目」  
となります。

### 3桁の場合、4桁の場合... and more...
次に、3桁の場合はどうでしょうか。4桁の場合は？

このプロジェクトは、円周率の中で最も遅く出現する一つの数字を桁数毎にそれぞれ探索し、出現位置を特定します。

## Project Story
円周率は無理数（循環せず無限に続く数）であるため、奥深くまで検索すればすべての数字が含まれている、と言われています。

本当でしょうか。

例えば、"999999"という“9”が6個続く数字が円周率に含まれているかを見てみることにしましょう。
円周率を3.1415....とスキャンしていくと、小数点以下762桁目から6個の“9”の並びを見つけることができます。”9”が6個も並ぶのは特殊と言えますが、円周率の中には確かに存在しています。

> [!NOTE] 
> ファインマンポイント  
> ”9”が6個並ぶ円周率小数点以下762桁目は「Feynman point」（ファインマン・ポイント）と呼ばれており、お茶目で優秀な物理学者であるリチャード・P・ファインマンがこの桁まで円周率を暗唱したことで有名です。
> 
> Wikipedia : https://ja.wikipedia.org/wiki/ファインマン・ポイント

このように、特殊な数字（の並び）であっても円周率が無理数ならばどこかに存在しているはずです。

### 「特殊な数字」の定義

さて、ここで「特殊な数字」とは何でしょうか。

例えば、”111111”や、"1234567"はどうでしょう。  
円周率を検索してみると、  
”111111”は少数以下255,945桁目  
"1234567"は少数以下9,470,344桁目  
に存在しています。  
同じ数字が連続したり、順番に並んでいたりする特殊で珍しい数字も、円周率から検索深度を深めて探索すれば見つかるようです。
また、自分の生年月日、電話番号、マイナンバー、クレジットカード番号など、自分にとっての特別な番号というのも同様に円周率の中に見つかるでしょう。

再度、「特殊な数字」とは何でしょうか。  
私たちの感覚では、特殊な数字とは、同じ数字が続いたり、等差や等比、連続性があるなど、何らかのルールが認識できるものであったり、自分に偶然に割り当てられたような数字や、自分以外だれにも知られていない内緒の数字だったりします。

そこで、私たちの基準ではなく、円周率から見て、円周率の中で珍しい数字とはいったいどの数字なのかを考えます。円周率の中で最もレアな数字とは。

このプロジェクトは、円周率「3.1415...」をスキャンしたときに最も出現が遅い数字（数列）を円周率の中で最も珍しくて特殊な数字であると定義します。  

つまり、1桁であれば”0”、2桁であれば”68”が最も特殊な数字です。


### つまり、プロジェクトの目的は
さらには、4桁の場合、5桁の場合、6桁の...と探索の桁数を伸ばし、最も遅く出現する数字を探索する。

このプロジェクトは、つまり特殊な数字を桁数毎に発見することを目的としています。  

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
  * 非力なコンピュータを使って実装していますので、実用的なスピードは期待できません。
* Entertainment-oriented mindset（エンタメです）
  * このプロジェクトは探索結果を得るためのものでありますが、その探索過程、探索中の様子をリアルタイムで公開することを最重要視しています。したがって探索アルゴリズムについては合理性（リソースの利用方法、拡張性、検索スピード）への配慮は優先順位を落としています。 
* resumable（中断・再開が可能）
  * プログラムは途方も無い時間を必要とします。実行後に思いついたプログラム実装を適用したり、意図しないプログラムの中断やコンピュータのダウンなどが発生したとき、最初からやり直したくありません。このプログラムは、そういったプログラムの途中終了後に再度実行しても途中から再開できます。
* YCD file format reading is modularized for user-friendly use（YCDファイル読み込みが使いやすくモジュール化されている）
  * 膨大な円周率が格納されている”YCD”形式ファイルは保管効率のために圧縮されています。保管には都合が良いこの形式ですが、プログラムから直接利用しようとしたときに不便なこともあります。このプロジェクトでは、[y-cruncher](http://www.numberworld.org/y-cruncher/)プログラムが生成する”YCD”形式ファイルをJavaで直接扱えるように、読み込み部分を明確にモジュール化しています。YCD_SeqProviderクラスは、複数の連続したYCDファイルから指定した桁数ずつ連続して切り出し提供できるように設計され、拡張forループが利用可能で簡単に扱えるユーザーフレンドリーなモジュールです。
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

# result output path (結果ファイルの吐き出し先)
outputPath=./

# searches per cycle (1サイクルあたりの検索数)
listSize=500

# read pi data per cycle (円周率データの１回の読み込み長さ)
unitLength=1900

# debug console report span
reportSpan=50000

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