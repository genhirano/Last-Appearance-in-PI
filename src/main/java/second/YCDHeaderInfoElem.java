package second;

public enum YCDHeaderInfoElem {

    FILE_VERSION("FileVersion")
    ,BASE("Base")
    ,FIRST_DIGITS("FirstDigits")
    ,TOTAL_DIGITS("TotalDigits")
    ,BLOCK_SIZE("Blocksize")
    ,BLOCK_ID("BlockID")
    ;

    private String name = "";
    private YCDHeaderInfoElem(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return this.name;
    }


}
