package controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Env {

    private static String PROP_FILE_NAME = ""; // = "./project.properties";

    private static Env instance = null;
    private final Properties prop;

    private Env() {

        try {
            if (PROP_FILE_NAME.isEmpty()) {
                throw new FileNotFoundException("プロパティーファイル名がセットされていません。");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        this.prop = new Properties();

        try (FileInputStream fis = new FileInputStream(PROP_FILE_NAME)) {
            this.prop.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * プロパティーファイル名をセット.
     * <p>
     * 使用前にかならずファイル名をセットすること。標準では「"./project.properties"」
     *
     * @param propFileName プロパティーファイル名
     */
    public static void setPropFileName(String propFileName) {
        PROP_FILE_NAME = propFileName;
    }


    public static Env getInstance() {

        if(null == instance){
            instance = new Env();
        }

        return instance;
    }

    public Properties getProp() {
        return this.prop;
    }

}
