/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.firefly.data.HasAccessInfo;
import edu.caltech.ipac.firefly.data.HasAccessInfos;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Date: May 8, 2008
 *
 * @author loi
 * @version $Id: TableData.java,v 1.3 2010/05/28 23:06:59 loi Exp $
 */
public interface TableData<R extends TableData.Row> extends Serializable, HasAccessInfos {

    String getAttribute(String key);
    Map<String, String> getAttributes();
    void addColumn(int index, String name);
    boolean addRow(R row);
    boolean removeRow(int rowIdx);
    void clear();
    R getRow(int rowIdx);
    int indexOf(R row);
    List<R> getRows();
    List<String> getColumnNames();
    int getColumnIndex(String colName);
    int size();

    public interface Row<D> extends Serializable, HasAccessInfo {
        List<String> getColumnNames();
        D getValue(int colIdx);
        void setValue(int colIdx, D value);
        D getValue(String colName);
        void setValue(String colName, D value);
        Map<String, D> getValues();
        int size();
        int getRowIdx();
        void setRowIdx(int idx);
    }
}


