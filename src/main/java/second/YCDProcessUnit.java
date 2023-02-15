package second;

public class YCDProcessUnit {

    private Long processNo;
    private Long startDigit;
    private String value;

    private YCDProcessUnit(){
        this(-1L,-1L,"");
    }

    public YCDProcessUnit(Long processNo, Long startDigit, String value){
        super();
        this.processNo = processNo;
        this.startDigit = startDigit;
        this.value = value;
    }

    public Long getProcessNo() {
        return this.processNo;
    }
    public void setProcessNo(Long l) {
        this.processNo = l;
    }

    public Long getStartDigit() {
        return this.startDigit;
    }
    public void setStartDigit(Long l) {
        this.startDigit = l;
    }

    public String getValue() {
        return this.value;
    }
    public void setValue(String s) {
        this.value = s;
    }

}
