/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

public enum Band {
    RED("Red", 0),
    GREEN("Green", 1),
    BLUE("Blue", 2),
    NO_BAND("NO_BAND", 0);
    private final String desc;
    private final int idx;

    Band(String desc, int idx) {
        this.desc = desc;
        this.idx = idx;
    }

    public String toString() { return desc; }
    public int getIdx() { return idx; }

    public static Band parse(String s) {
        try {
            return Enum.valueOf(Band.class,s.toUpperCase());
        } catch (Exception ignore) {
            return null;
        }
    }
}