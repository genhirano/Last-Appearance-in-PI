import controller.Env;
import controller.Main;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;

public class MainTest extends TestBase{

    @org.junit.jupiter.api.Test
    void 一般テスト(TestInfo testInfo)  {

        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");
        Main.main(new String[1]);
        //Main.main(new String[]{Env.getPropFileName()});

    }

    @org.junit.jupiter.api.Test
    void プログラム引数にNULLがセットされている場合正しくエラーになるか(TestInfo testInfo)  {

        assertThrows(RuntimeException.class, () -> {
            Main.main(null);
        });

        assertThrows(RuntimeException.class, () -> {
            Main.main(new String[1]);
        });

    }



}
