package edu.caltech.ipac.firefly.visualize;

public enum Band {
    RED("Red", 0),
    GREEN("Green", 1),
    BLUE("Blue", 2),
    NO_BAND("NO_BAND", 0);
    private String _desc;
    private int _idx;

    Band() {
    }

    Band(String desc, int idx) {
        _desc = desc;
        _idx = idx;
    }

    public String toString() {
        return _desc;
    }

    public int getIdx() {
        return _idx;
    }


    public static Band parse(String s) {
        Band retval;
        try {
            retval= Enum.valueOf(Band.class,s.toUpperCase());
        } catch (Exception e) {
            retval= null;
        }

        return retval;
    }

}