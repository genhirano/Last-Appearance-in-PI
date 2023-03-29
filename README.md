# Last-Appearance-in-PI
Find the number that appears the latest in pi by number of digits.  
桁数毎に円周率の中で一番遅く出現する数値を検索します。

## OVERVIEW
Pi is an irrational number. Find the latest occurrence of a specific number of digits among this infinite number of non-repeating magical numbers.  
円周率は無理数です。無限の循環しないこの不思議な数の中から、特定の桁数の数値において最も遅く出現する数を探します。

## WHY?
Why not?  
え?

## FOR EXAMPLE
* Case : **One** digit  1桁 
  * 3.1415926535897932384626433832795 **(0)** 288419716......
    * The latest number to appear: 0, position: 32
    * 一番遅く出現する数：0, 少数以下32桁目
* Case : **Two** digit 2桁
  * 3.14159....48184676694051320005 **(68)** 1271452635......
    * The latest number to appear: 68, position: 605
    * 一番遅く出現する数：68, 少数以下605桁目
* Case : **Three** digit 3桁
  * 3.14159..............4697486762655165827658 **(483)** 58845......
    * The latest number to appear: 483, position: 8553
    * 一番遅く出現する数：483, 少数以下8553桁目
* Case : **Fou** digit 4桁 
  * ????
* Case : **Five** digit 5桁
  * ????
* Case : ..............
* Case : ..............
* Case : ..............
* Case : ..............

## USAGE
* Create properties file "default.properties"
``` default.properties
# Web server port
port=8080

# result output path
outputPath=src/test/

# max digit (何桁まで実行するか)
maxTargetLength=5

# searches per cycle (1サイクルあたりの検索数)
listSize=500

# read pi data per cycle (円周率データの１回の読み込み長さ)
unitLength=1900

# debug report span
reportSpan=50000

# Pi files (ycd Files)
ycd000=X:/ycdFile/Pi - Dec - Chudnovsky - 0.ycd
ycd001=X:/ycdFile/Pi - Dec - Chudnovsky - 1.ycd
ycd002=X:/ycdFile/Pi - Dec - Chudnovsky - 2.ycd
ycd003=X:/ycdFile/Pi - Dec - Chudnovsky - 3.ycd
# (and more. Max:9999)
```
* Exctute
  *  java -jar (jarname)
* View Progress 
  * your browser "http://localhost:8080" 



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