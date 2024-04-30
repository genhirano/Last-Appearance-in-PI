package controller;

import lombok.Getter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;

public class Env {

    enum PropKey {

        outputPath(),
        listSize(),
        unitLength(),
        port(),
        ;

        private String name = "";

        PropKey() {
            this.name = this.toString();
        }

        public String getKeyName() {
            return this.name;
        }

    }


    private static String PROP_FILE_NAME = ""; // = "./project.properties";

    private static Env instance = null;

    private final Properties prop;

    @Getter
    private final static ZonedDateTime startTime = ZonedDateTime.now();

    public Integer getListSize(){
        return Integer.valueOf(prop.getProperty(Env.PropKey.listSize.toString()));
    }
    public void setListSize(Integer value){
        this.prop.setProperty(Env.PropKey.listSize.toString(), value.toString());
    }

    public Integer getUnitLength(){
        return Integer.valueOf(prop.getProperty(Env.PropKey.unitLength.toString()));
    }
    public void setUnitLength(Integer value){
        this.prop.setProperty(Env.PropKey.unitLength.toString(), value.toString());
    }

    public Integer getPortNo(){
        return  Integer.valueOf(prop.getProperty(Env.PropKey.port.toString()));
    }
    public void setPortNo(Integer value){
        this.prop.setProperty(Env.PropKey.port.toString(), value.toString());
    }

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

        if(null != propFileName){
            PROP_FILE_NAME = propFileName;
        }else{
            PROP_FILE_NAME = "";
        }
    }

    /**
     * プロパティーファイル名を取得.
     * @return プロパティーファイル名
     */
    public static String getPropFileName() {
        return PROP_FILE_NAME;
    }

    public static Env getInstance() {

        if (null == instance) {
            instance = new Env();
        }

        return instance;
    }

    public Properties getProp() {
        return this.prop;
    }


    /**
     * プロパティ―ファイルに定義されている対象YCDファイル情報から、ファイルオブジェクトの一覧を作成して返す.
     *
     * @return ファイルリスト
     * @throws FileNotFoundException 
     */
    public  List<File> createFileListByProp() throws FileNotFoundException {
        List<File> fileList = new ArrayList<>();

        final String notfound = "NOTFOUND";

        //ファイルID 0 から 最大 9999 まで。
        for (int i = 0; i < 9999; i++) {

            String noStr = String.format("%03d", i);
            try {

                //ファイル名取得。 "ycd" につなげて id
                String fullPath = Env.getInstance().getProp().getProperty("ycd" + noStr, notfound);

                File f = new File(fullPath);
                if (!f.exists()) {
                    if (i == 0) {
                        throw new FileNotFoundException("piFile is not found: " + f);
                    } else {
                        break;
                    }
                }

                fileList.add(new File(fullPath));

            } catch (MissingResourceException e) {
                if (i == 0) {
                    throw new RuntimeException("piFile is not define by property file: " + noStr);
                } else {
                    break;
                }
            }
        }

        return fileList;
    }


    /**
     * プロパティーファイルの内容をコンソール出力.
     *
     */
    public void printProp(){
        System.out.println("--- Property File : " + PROP_FILE_NAME + " ");
        for (Object key : this.getProp().keySet()) {
            if(0 <= key.toString().indexOf("ycd")){
                continue;
            }
            String value = this.getProp().getProperty(key.toString());
            System.out.println(key + ": " + value);
        }

        int i = 0;
        while(true) {
            String value = this.getProp().getProperty("ycd" + String.format("%03d", i));
            if(null == value){
                break;
            }
            System.out.println("ycd" + String.format("%03d", i) + ": " + value);
            i++;
        }

        System.out.println("--- end");

    }


}
