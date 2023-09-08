import controller.Env;
import controller.Main;
import controller.Searcher;
import org.junit.jupiter.api.TestInfo;

import java.io.File;

public class SearcherTest extends TestBase{

    @org.junit.jupiter.api.Test
    void 一般テスト(TestInfo testInfo)  {

        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "/src/test/resources/test.properties");

        Env.getInstance().setListSize(10);
        Env.getInstance().setUnitLength(1900);
        Env.getInstance().setReportSpan(100);

        Searcher searcher = new Searcher(Env.getInstance().createFileListByProp(), Env.getInstance().getListSize(), Env.getInstance().getUnitLength(), Env.getInstance().getReportSpan());
        searcher.start();

        //検索スレッドの終了まち。なくても良いが、テストしやすい。
        while(searcher.isAlive()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("End.");

    }
}
