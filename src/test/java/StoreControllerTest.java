
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.junit.jupiter.api.TestInfo;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


import controller.Env;
import controller.StoreController;

public class StoreControllerTest extends TestBase {

    @org.junit.jupiter.api.Test
    void 一般テスト(TestInfo testInfo) {

        String path = new File(".").getAbsoluteFile().getParent();
        Env.setPropFileName(path + "\\src\\test\\resources\\test.properties");

        StoreController sc = StoreController.getInstance();

        List<String> sum = sc.getSummary();

        for (String s : sum) {
            System.out.println(s);
        }


        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Map<String, String> systemProperties = runtimeMXBean.getSystemProperties();

            System.out.println("os.name" + " : " + systemProperties.get("os.name"));
            System.out.println("os.arch" + " : " + systemProperties.get("os.arch"));
            System.out.println("java.vendor" + " : " + systemProperties.get("java.vendor"));
            System.out.println("java.version" + " : " + systemProperties.get("java.version"));
            System.out.println("java.vendor.version" + " : " + systemProperties.get("java.vendor.version"));

        }


        MBeanServer server = MBeanServerFactory.createMBeanServer();
        
        try {
            RuntimeMXBean mbean = ManagementFactory.getRuntimeMXBean();
            server.registerMBean(mbean, new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME));
            
            System.out.println("Creating an HTML protocol adaptor..");
            HtmlAdaptorServer adaptor = new HtmlAdaptorServer();
            ObjectName adaptorName = new ObjectName("Adaptor:name=adaptor,port=8082");
            server.registerMBean(adaptor, adaptorName);
            adaptor.start();
        } catch (InstanceAlreadyExistsException ex) {
            ex.printStackTrace();
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (NotCompliantMBeanException ex) {
            ex.printStackTrace();
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
        }
        

}
