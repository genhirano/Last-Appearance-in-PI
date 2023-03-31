# Last-Appearance-in-PI
Find the number that appears the latest in pi by number of digits.  
桁数毎に円周率の中で一番遅く出現する数値を検索します。

## OVERVIEW
Pi is an irrational number. Find the latest occurrence of a specific number of digits among this infinite number of non-repeating magical numbers.  
円周率は無理数です。無限の循環しないこの不思議な数の中から、特定の桁数の数値において最も遅く出現する数を探します。

For example, among the digits of pi, "0" is the digit that appears the latest among the numbers 0 to 9. Surprisingly, the first appearance of "0" is at the 32nd decimal place. This is a remarkably late appearance.  
例えば円周率の中で「0」は0～9の数値のなかで最も遅く出現する数字です。「0」が初めて出現するのは少数以下32桁目です。これは驚くべき登場の遅さです。

3.
1415926535897932384626433832795 **(0)** 288419716......

----
In that case, what about two-digit numbers? The one that appears the latest is "68". It appears at the 605th decimal place.  
それでは、2桁の場合はどうでしょうか。最も遅く出現するのは「68」です。605桁目に出現します。

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
0005 **(68)** 1271 4526356082 7785771342 7


----
Next, how about three-digit numbers? What about four-digit numbers?  
次に、3桁の場合はどうでしょうか。4桁の場合は？

This project searches for one value that appears the latest among n-digit numbers in the digits of pi.  
このプロジェクトは、n桁の数値について、円周率の中で最も遅く出現する一つの値を検索します。

## WHY?
Why not?  
え?

## IMAGE
![web](https://user-images.githubusercontent.com/3538386/229020000-79905d0f-f9e7-4521-bd27-d94482eebae9.JPG)

## USAGE
* View Progress
your browser "http://localhost:8080"
* Create properties file "default.properties"
  * Please place the program in the location where it will be executed.
``` default.properties
# Web server port
port=8080

# result output path
outputPath=./

# max digit (何桁まで実行するか)
maxTargetLength=5

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
# (and more. Max:9999)
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