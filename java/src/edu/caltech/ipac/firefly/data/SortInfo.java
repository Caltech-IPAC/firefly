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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
