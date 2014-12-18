package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Date: Feb 11, 2009
 *
 * @author loi
 * @version $Id: SortInfo.java,v 1.5 2010/10/21 22:15:53 loi Exp $
 */
public class SortInfo implements Serializable, Comparable {

    public enum Direction {ASC, DESC}

    public static final String SORT_INFO_TAG = "SortInfo";

    private Direction direction;
    private ArrayList<String> sortColumns;

    public SortInfo() {}

    public SortInfo(String... sortColumns) {
        this(Direction.ASC, sortColumns);
    }

    public SortInfo(Direction direction, String... sortColumns) {
        this.direction = direction;
        this.sortColumns = new ArrayList<String>(Arrays.asList(sortColumns));
    }

    public Direction getDirection() {
        return direction;
    }

    public ArrayList<String> getSortColumns() {
        return sortColumns;
    }

    public String[] getSortColumnAry() {
        return sortColumns.toArray(new String[sortColumns.size()]);
    }

    public String getPrimarySortColumn() {
        if (sortColumns.size() > 0) {
            return sortColumns.get(0);
        } else {
            return null;
        }
    }

    public static SortInfo parse(String str) {
        if (StringUtils.isEmpty(str)) return null;
        String[] kv = str.split("=", 2);
        if (kv != null && kv.length == 2 && kv[0].equals(SORT_INFO_TAG)) {
            String[] values = kv[1].split(",");
            if (values.length > 1) {
                if (values[0] != null) {
                    Direction dir = values[0].equals(Direction.ASC.name()) ? Direction.ASC : Direction.DESC;
                    String[] cols = new String[values.length-1];
                    System.arraycopy(values, 1, cols, 0, values.length-1);
                    return new SortInfo(dir, cols);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return SORT_INFO_TAG + "=" + direction + "," + StringUtils.toString(sortColumns, ",");
    }

//====================================================================
//  Implements Comparable
//====================================================================

    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof SortInfo &&
                obj.toString().equals(toString());
    }
}
