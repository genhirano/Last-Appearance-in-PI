package controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvTest {

    @AfterEach
    void tearDown() throws Exception {
        resetEnvSingleton();
        Env.setPropFileName("");
    }

    @Test
    void createFileListByProp_相対パスはプロパティーファイル基準で解決される() throws Exception {
        Path tempDir = Files.createTempDirectory("env-relative-path-test");
        Path ycdDir = Files.createDirectories(tempDir.resolve("data"));
        Path ycdFile = Files.createFile(ycdDir.resolve("Pi - Dec - Chudnovsky - 0.ycd"));

        Path propFile = tempDir.resolve("default.properties");
        String propertyBody = String.join("\n",
                "outputPath=.",
                "port=8080",
                "listSize=10",
                "unitLength=100",
                "ycd000=data/Pi - Dec - Chudnovsky - 0.ycd");
        Files.write(propFile, propertyBody.getBytes(StandardCharsets.UTF_8));

        Env.setPropFileName(propFile.toAbsolutePath().toString());
        resetEnvSingleton();

        List<File> files = Env.getInstance().createFileListByProp();

        assertEquals(1, files.size());
        assertEquals(ycdFile.toFile().getCanonicalPath(), files.get(0).getCanonicalPath());
    }

    private static void resetEnvSingleton() throws Exception {
        Field instanceField = Env.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
