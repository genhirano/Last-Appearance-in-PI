import controller.Env;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestBase {
/*
1415926535 8979323846 2643383279 5028841971 6939937510  :  50
5820974944 5923078164 0628620899 8628034825 3421170679  :  100
8214808651 3282306647 0938446095 5058223172 5359408128  :  150
4811174502 8410270193 8521105559 6446229489 5493038196  :  200
4428810975 6659334461 2847564823 3786783165 2712019091  :  250
4564856692 3460348610 4543266482 1339360726 0249141273  :  300
7245870066 0631558817 4881520920 9628292540 9171536436  :  350
7892590360 0113305305 4882046652 1384146951 9415116094  :  400
3305727036 5759591953 0921861173 8193261179 3105118548  :  450
0744623799 6274956735 1885752724 8912279381 8301194912  :  500
9833673362 4406566430 8602139494 6395224737 1907021798  :  550
6094370277 0539217176 2931767523 8467481846 7669405132  :  600
0005681271 4526356082 7785771342 7577896091 7363717872  :  650
1468440901 2249534301 4654958537 1050792279 6892589235  :  700
4201995611 2129021960 8640344181 5981362977 4771309960  :  750
5187072113 4999999837 2978049951 0597317328 1609631859  :  800
5024459455 3469083026 4252230825 3344685035 2619311881  :  850
7101000313 7838752886 5875332083 8142061717 7669147303  :  900
5982534904 2875546873 1159562863 8823537875 9375195778  :  950
1857780532 1712268066 1300192787 6611195909 2164201989  :  1,000
3809525720 1065485863 2788659361 5338182796 8230301952  :  1,050
0353018529 6899577362 2599413891 2497217752 8347913151  :  1,100
5574857242 4541506959 5082953311 6861727855 8890750983  :  1,150
8175463746 4939319255 0604009277 0167113900 9848824012  :  1,200


1568261622 8622318810 9672974760 6013028331 1937161140  :  999,500
8747270676 2558567775 1199566674 8615196491 2970193318  :  999,550
0849941096 1813929649 2789360902 1253544332 7375064260  :  999,600
6242994120 3273625582 4417498345 0947309453 4366159072  :  999,650
8416319368 3075719798 0682315357 3715557181 6122156787  :  999,700
9364250138 8711702327 5555779302 2667858031 9993081083  :  999,750
0576307652 3320507400 1393909580 7901637717 6292592837  :  999,800
6487479017 7274125678 1905555621 8050487674 6991140839  :  999,850
9779193765 4232062337 4717324703 3697633579 2589151526  :  999,900
0315614033 3212728491 9441843715 0696552087 5424505989  :  999,950
5678796130 3311646283 9963464604 2209010610 5779458151  :  1,000,000
--
3092756283 2084531584 6520010277 9723561292 3012605863  :  1,000,050
5360116492 0990258745 5521403969 7911534022 4158981324  :  1,000,100
5057229232 3321310453 9246998366 1127509161 3418779722  :  1,000,150
7187025774 4736124388 5140834659 3203124872 8192989830  :  1,000,200
7568923994 9381959832 1885103228 7546801765 3126534895  :  1,000,250
9379946441 0392567162 7137376567 0976190037 8241779526  :  1,000,300
4140347759 2636637423 9012488538 1837859047 2910469319  :  1,000,350
8892156107 9247143327 5600029017 1862854251 3963400457  :  1,000,400
5898174665 5521146070 7993228399 6193649114 7312126919  :  1,000,450
4530146775 5623261882 2547421749 2593988176 6761512788  :  1,000,500
8878297907 7553435112 1708399018 8345223321 9690347465  :  1,000,550




    static Long fileDigitCount = 200000000000l;
    //1415926535 8979323846 2643383279 5028841971 6939937510  :  50
    //5820974944 5923078164 0628620899 8628034825 3421170679  :  100
    //8214808651 3282306647 0938446095 5058223172 5359408128  :  150
    //4811174502 8410270193 8521105559 6446229489 5493038196  :  200

    //8899805359 4193619500 1030900622 7967575294 4036446738  :  199,999,999,900
    //9210366471 8253920158 5279099580 3609723371 5037170599  :  199,999,999,950
    //4305017750 7056695457 1580141586 9082820114 5623831042  :  200,000,000,000

    //0719003966 6177300581 1050342554 4359078194 5552422286  :  200,000,000,050
    //4618185109 7896727243 6233890369 9935947050 3330555981  :  200,000,000,100

    //0727660983 8249943254 9512493900 6650307906 5116490784  :  399,999,999,900
    //0060997174 9928403612 6728243112 6799519349 2408065838  :  399,999,999,950

    //4232472530 5584721833 6635696622 7418760375 3353406089  :  400,000,000,000
    //5153428885 2449501852 7058643345 3453965221 9840456444  :  400,000,000,050
    //4895849832 2987070902 2612045337 8854120901 8121603437  :  400,000,000,100


*/


    //1415926535 8979323846 2643383279 5028841971 6939937510  :  50
    //5820974944 5923078164 0628620899 8628034825 3421170679  :  100
    //8214808651 3282306647 0938446095 5058223172 5359408128  :  150
    //4811174502 8410270193 8521105559 6446229489 5493038196  :  200

    //5776575226 6711062777 3613221750 2579354811 5114950578  :  49,999,800
    //3951597844 7161098822 1888334044 8694584689 0750787802  :  49,999,850
    //8969881518 9523934649 3930044802 9988256830 8048318581  :  49,999,900
    //4127897300 0153683630 8346732220 0943329365 1632962502  :  49,999,950
    //5130045796 0464561703 2424263071 4554183801 7945652654  :  50,000,000
    //5645211683 3617131843 9896395742 7122077935 8831702756  :  50,000,050
    //7245293000 4977483983 0473876890 9185988619 6598406678  :  50,000,100
    //9169080497 5877271736 2123592806 1519874311 2112745274  :  50,000,150

    //8899805359 4193619500 1030900622 7967575294 4036446738  :  199,999,999,900
    //9210366471 8253920158 5279099580 3609723371 5037170599  :  199,999,999,950
    //4305017750 7056695457 1580141586 9082820114 5623831042  :  200,000,000,000
    //0719003966 6177300581 1050342554 4359078194 5552422286  :  200,000,000,050
    //4618185109 7896727243 6233890369 9935947050 3330555981  :  200,000,000,100

    //0727660983 8249943254 9512493900 6650307906 5116490784  :  399,999,999,900
    //0060997174 9928403612 6728243112 6799519349 2408065838  :  399,999,999,950
    //4232472530 5584721833 6635696622 7418760375 3353406089  :  400,000,000,000
    //5153428885 2449501852 7058643345 3453965221 9840456444  :  400,000,000,050
    //4895849832 2987070902 2612045337 8854120901 8121603437  :  400,000,000,100


/*
https://oeis.org/search?q=2512258603197
# A332262 (b-file synthesized from sequence entry)
 1                32
 2               605
 3             8,553
 4            99,846
 5         1,369,560
 6        14,118,307
 7       166,100,500
 8     1,816,743,905
 9    22,445,207,398
10   241,641,121,039
11 2,512,258,603,197

  19,000,000,000,000


100 trillion digits of pi ( (caution: you'll need 82 TB of storage space))
https://storage.googleapis.com/pi100t/index.html


今日の数学
https://www2.hamajima.co.jp/kyoto-math/pdf/kyomath202211.pdf


 */

    //対象ファイルリスト作成
    public static List<File> createFileList(){
        List<File> fileList = new ArrayList<>();

        //String path = new File(".").getAbsoluteFile().getParent();
        //path = path + "\\src\\test\\resources\\1000000";
        String path = "H:\\Pi";

        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 0.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 1.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 2.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 3.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 4.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 5.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 6.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 7.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 8.ycd"));
        fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 9.ycd"));
        //fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 10.ycd"));
        //fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 11.ycd"));
        //fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 12.ycd"));
        //fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 13.ycd"));
        //fileList.add(new File(path+"//Pi - Dec - Chudnovsky - 14.ycd"));

        return fileList;
    }

    public TestBase(){

        String path = new File(".").getAbsoluteFile().getParent();
        path = path + "\\src\\test\\resources\\test.properties";

        Env.setPropFileName(path);

        try(FileInputStream fis = new FileInputStream(path)){
            Env.getInstance().getProp().load(fis);

            Env.getInstance().getProp().load(fis);



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
