import controller.Env;
import controller.Main;
import org.junit.jupiter.api.TestInfo;

import java.io.File;

public class MainTest extends TestBase{

    @org.junit.jupiter.api.Test
    void 作りながら動かす用2(TestInfo testInfo)  {

        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");
        Main.main(new String[1]);

        try{
            this.wait(1000000);
        }catch(Exception e){

        }

    }
}
