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
        D getValue(int colIdx);
        void setValue(int colIdx, D value);
        D getValue(String colName);
        void setValue(String colName, D value);
        Map<String, D> getValues();
        int size();
    }
}


/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
