package model.pi;

import java.util.ArrayList;
import java.util.List;

public class FlagFolder {

    public static void setTargetLength(Integer targetLength) {
        TARGET_LENGTH = targetLength;
    }

    private FlagFolder parent;
    private FlagFolder[] childlen;

    public FlagFolder[] getChildlen() {
        return this.childlen;
    }

    ;

    private FlagFolder getParent() {
        return this.parent;
    }

    private byte depth;

    private static Integer TARGET_LENGTH = -1;

    private byte myChar;

    public byte getMyChar() {
        return this.myChar;
    }

    private boolean wasFinded = false;

    public boolean wasFinded() {
        return this.wasFinded;
    }

    private byte getDepth() {
        byte depth = 0;
        if (null != parent) {
            depth = (byte) (depth + this.getParent().getDepth() + 1);
        }
        return depth;
    }


    public List<List<FlagFolder>> getFlat() {

        List<List<FlagFolder>> grandRet = new ArrayList<>();

        for (FlagFolder child : this.childlen) {

            if (null == child.getChildlen()) {
                List<FlagFolder> ret = new ArrayList<>();
                ret.add(this);
                ret.add(child);
                grandRet.add(ret);
                continue;
            }

            List<List<FlagFolder>> childNumlList = child.getFlat();

            for (List<FlagFolder> s : childNumlList) {
                List<FlagFolder> ret = new ArrayList<>();
                if (s.isEmpty()) {
                    ret.add(this);
                } else {
                    ret.add(this);
                    ret.addAll(s);
                }

                grandRet.add(ret);

            }

        }

        return grandRet;

    }

    public static int count = 0;

    public static FlagFolder createFlagFolderTree(Integer targetLength) {
        TARGET_LENGTH = targetLength;

        if (1 > TARGET_LENGTH) {
            throw new RuntimeException("TARGET LENGTH is :" + TARGET_LENGTH + " Please set valid Target Length");
        }

        return new FlagFolder(null, (byte) -1);
    }

    private FlagFolder(FlagFolder parent, byte num) {
        super();

        count++;
        this.parent = parent;
        this.myChar = num;

        if (TARGET_LENGTH > this.getDepth()) {
            FlagFolder[] flagFolders = new FlagFolder[10];
            for (byte i = 0; i < 10; i++) {
                flagFolders[i] = new FlagFolder(this, i);
            }
            this.childlen = flagFolders;
        }

    }

}
