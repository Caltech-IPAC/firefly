package edu.caltech.ipac.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sparse table data indexed into a hash map for easy access to cell data.
 */
public class MappedData {
    private HashMap<String, Object> data = new HashMap<String, Object>();

    public void put(int idx, String colName, Object value) {
        this.data.put(makeKey(idx, colName), value);
    }

    public Object get(int idx, String colName) {
        return this.data.get(makeKey(idx, colName));
    }

    public Collection<Object> values() {
        return data.values();
    }

    /**
     * convenience method to return the values for the given column as a list of string
     * @param colName
     * @return
     */
    public List<String> getValues(String colName) {
        List<String> vals = new ArrayList<String>();
        for(Map.Entry<String, Object> s : data.entrySet()) {
            if (s.getKey().endsWith(colName)) {
                String v = s.getValue() == null ? "" : s.getValue().toString();
                vals.add(v);
            }
        }
        return vals;
    }

    /**
     * get a row of data.  Map is keyed by column name.
     * @param idx row index to get the data from
     * @return
     */
    public Map<String, Object> getRow(int idx) {
        Map<String, Object> retval = new HashMap<>();
        for(Map.Entry<String, Object> e : data.entrySet()) {
            if (e.getKey().startsWith(String.valueOf(idx))) {
                retval.put(getCnameFromKey(e.getKey()), e.getValue());
            }
        }
        return retval;
    }

    private String makeKey(int idx, String colName) {
        return idx + "-" + colName;
    }
    private String getCnameFromKey(String key) {
        try {
            String[] parts = String.valueOf(key).split("-", 2);
            if (parts.length > 1) {
                return parts[1];
            } else {
                throw new RuntimeException("Key missing column name:" + key);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
