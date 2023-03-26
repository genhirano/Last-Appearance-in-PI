import controller.Env;
import controller.Main;
import org.junit.jupiter.api.TestInfo;

import java.io.File;

public class MainTest extends TestBase{

    @org.junit.jupiter.api.Test
    void 一般テスト(TestInfo testInfo)  {

        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");
        Main.main(new String[1]);

    }
}
