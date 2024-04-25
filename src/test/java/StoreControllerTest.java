
import java.io.File;
import java.io.IOException;
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
    void basic(TestInfo testInfo) {

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


        System.out.println("os.name" + " : " + SystemUtils.OS_NAME);

        for (String key : systemProperties.keySet()) {
            System.out.println(key + " : " + systemProperties.get(key));
        }


/* 
        try {
            JVMCPUUsage jvmCPUUsage = new JVMCPUUsage();
            jvmCPUUsage.openMBeanServerConnection();
            jvmCPUUsage.getMXBeanProxyConnections();
            Float f = jvmCPUUsage.getJvmCpuUsage();

            System.out.println(f);

        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }
}
