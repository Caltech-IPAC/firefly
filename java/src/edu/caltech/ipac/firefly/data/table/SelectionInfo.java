/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Date: Jun 19, 2009
*
* @author loi
* @version $Id: SelectionInfo.java,v 1.5 2012/02/14 01:32:21 loi Exp $
*/
public class SelectionInfo implements Serializable {
    private boolean selectAll;
    private Set<Integer> exceptions = new HashSet<Integer>();
    private int rowCount;

    public SelectionInfo() {
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public void selectAll() {
        exceptions.clear();
        selectAll = true;
    }

    public void deselectAll() {
        exceptions.clear();
        selectAll = false;
    }

    public void select(int idx) {
        if (selectAll) {
            exceptions.remove(new Integer(idx));
        } else {
            exceptions.add(idx);
            if (exceptions.size() == rowCount) {
                selectAll();
            }
        }
    }

    public void deselect(int idx) {
        if (selectAll) {
            exceptions.add(idx);
        } else {
            exceptions.remove(new Integer(idx));
        }
    }

    public boolean isSelected(int idx) {
        if (selectAll) {
            return !exceptions.contains(idx);
        } else {
            return exceptions.contains(idx);
        }
    }

    /**
     * this when use on the client side can be very memory intensive
     * when it's a long list.
     * @return
     */
    public SortedSet<Integer> getSelected() {
        TreeSet<Integer> all = new TreeSet<Integer>();
        if (selectAll) {
            for(int i = 0; i < rowCount; i++) {
                if (!exceptions.contains(i)) {
                    all.add(i);
                }
            }
        } else {
            all.addAll(exceptions);
        }
        return all;
    }

    public int getFirstSelectedIdx() {
        if (selectAll) {
            for(int i = 0; i < rowCount; i++) {
                if (!exceptions.contains(i)) {
                    return i;
                }
            }
        } else if (exceptions.size() > 0){
            return exceptions.iterator().next();
        }
        return -1;
    }

    public int getSelectedCount() {
        if (rowCount < 1) return 0;
        if (selectAll) {
            return rowCount - exceptions.size();
        } else {
            return exceptions.size();
        }
    }

    public boolean isSelectAll() {
        return selectAll && (exceptions.size() == 0);
    }

    public String toString() {
        return selectAll + "-" + StringUtils.toString(exceptions, ",") + "-" + rowCount;
    }

    public static SelectionInfo parse(String s) {
        SelectionInfo si = new SelectionInfo();
        String[] parts = s.split("-");
        if (parts.length == 3) {
            si.selectAll = Boolean.valueOf(parts[0]).booleanValue();
            if (!StringUtils.isEmpty(parts[1])) {
                si.exceptions = new HashSet<Integer>(StringUtils.convertToListInteger(parts[1], ","));
            }
            si.rowCount = Integer.parseInt(parts[2]);
        }
        return si;
    }
}
