/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.HREF;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Object representing tabular data.  For historic reason, DataObject represent a row and DataType represent a column.
 */
public class DataGroup implements Serializable, Cloneable, Iterable<DataObject> {

    public static final String ROW_IDX = "ROW_IDX";               // this contains the original row index of the table before any sorting or filtering
    public static final String ROW_NUM = "ROW_NUM";               // this is row number of the current dataset. (oracle's rownum)

    private ArrayList<DataObject> data = new ArrayList<>(200);
    private LinkedHashMap<String, DataType> columns = new LinkedHashMap<>();
    private TableMeta meta = new TableMeta();
    private String title;
    private transient DataType[] cachedColumnsAry = null;


    public DataGroup(String title, DataType[] dataDefs) {
        this(title, Arrays.asList(dataDefs));
    }

    public DataGroup(String title, List<DataType> dataDefs) {
        this.title = title;
        dataDefs.stream().forEach(this::addDataDefinition);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Append a column to this DataGroup.
     * @param dataType  the column to append
     */
    public void addDataDefinition(DataType dataType) {
        columns.put(dataType.getKeyName(), dataType);
        dataType.setColumnIdx(columns.size()-1);
    }

    /**
     * This ONLY removes the DataType.  Data will still be there.  DataType contains
     * the column_idx into the data, so it should be okay.
     * @param names
     */
    public void removeDataDefinition(String ...names) {
        if (names == null) return;
        Arrays.stream(names).forEach(cn -> columns.remove(cn));
    }

    /**
     * @return DataDataType[]  the all column information of this DataGroup.
     */
    public DataType[] getDataDefinitions() {
        if (cachedColumnsAry == null || cachedColumnsAry.length != columns.size()) {
            cachedColumnsAry = columns.values().toArray(new DataType[columns.size()]);
        }
        return cachedColumnsAry;
    }

    /**
     * @param key  column's name
     * @return  the column information for this given name
     */
    public DataType getDataDefintion(String key) {
        return columns.get(key);
    }

    /**
     * @param key
     * @param ignoreCase    true to ignore case
     * @return  the column information for this given name
     */
    public DataType getDataDefintion(String key, boolean ignoreCase) {
        if (ignoreCase) {
            for (DataType a : getDataDefinitions()) {
                if (a.getKeyName().equalsIgnoreCase(key)) {
                    return a;
                }
            }
            return null;
        } else return getDataDefintion(key);
    }

    /**
     * return the row data of this data group
     * @return
     */
    public List<DataObject> values() {
        return data;
    }

    /**
     * set the data to the given list.
     * @return
     */
    public void setData(List<DataObject> data) {
        this.data = new ArrayList<>(data);
    }

    public void clearData() { data.clear(); }

    public void add(DataObject s) {
        data.add(s);
        s.setRowNum(data.size()-1);
    }

    /**
     * @param key   if null, meta will be treated as a comment
     * @param value meta value
     */
    public void addAttribute(String key, String value) {
        this.meta.setAttribute(key, value);
    }

    /**
     * if there are duplicate keys, it will only return one.
     * @param key
     * @return
     */
    public String getAttribute(String key) {
        return meta.getAttribute(key);
    }

    /**
     * similar to getAttribute, but it returns just the value of the attribute.
     * @param key the attribute key
     * @param def default if attribute does not exists
     * @return
     */
    public String getAttribute(String key, String def) {
        String v = meta.getAttribute(key);
        return StringUtils.isEmpty(v) ? def : v;
    }

    /**
     * returns a set of unique keys
     * @return
     */
    public Set<String> getAttributeKeys() {
        return meta.getAttributes().keySet();
    }

    /**
     * this returns a map of attributes ignoring comments.
     *
     * @return
     */
    public Map<String, Attribute> getAttributes() {
        return meta.getAttributes();
    }

    /**
     * set the keywords to this new list
     */
    public void setKeywords(List<Attribute> attribList) {
        meta.setKeywords(attribList);
    }

    /**
     * Keyword is equivalent to attributes.  However, it mandates order and allow
     * for comments.
     * @return
     */
    public List<Attribute> getKeywords() {
        return meta.getKeywords();
    }

    public DataObject get(int i) {
        return data.get(i);
    }

    public boolean containsKey(String key) {
        return columns.containsKey(key);
    }

    public String[] getKeySet() {
        return columns.keySet().toArray(new String[columns.size()]);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DataGroup copy = new DataGroup(title, getDataDefinitions());
        copy.data = new ArrayList<>(data);
        copy.columns = new LinkedHashMap<>(columns);
        copy.meta = meta.clone();
        return copy;
    }

    public DataGroup cloneWithoutData() {
        DataGroup copy = new DataGroup(title, getDataDefinitions());
        copy.columns = new LinkedHashMap<>(columns);
        copy.meta = meta.clone();
        return copy;
    }

    public int size() {
        return data.size();
    }

    /**
     * this method will shrink the Column's width to fit the maximum's width of the data
     */
    public void shrinkToFitData() {
        for (DataType dt : columns.values()) {
            if (dt.getMaxDataWidth() <= 0) {
                String[] headers = {dt.getKeyName(), dt.getTypeDesc(), dt.getUnits(), dt.getNullString()};
                int hWidth = Arrays.stream(headers).mapToInt(s -> s == null ? 0 : s.length()).max().getAsInt();
                dt.ensureMaxDataWidth(hWidth);
                for (DataObject row : data) {
                    int vlength = row.getFormatedData(dt).length();
                    dt.ensureMaxDataWidth(vlength);
                }
            }
        }
    }

//======================================================================
//------------- Methods from TableConnectionList  ----------------------
//======================================================================

    /**
     * @param fromIndex from index
     * @param toIndex   to index
     * @return returns a subset of the datagroup between the specified fromIndex, inclusive, and toIndex, exclusive
     */
    public DataGroup subset(int fromIndex, int toIndex) {
        DataGroup retval = cloneWithoutData();
        if (fromIndex < size() && fromIndex < toIndex) {
            int endIdx = Math.min(size(), toIndex);
            retval.setData(values().subList(fromIndex, endIdx));
        }
        return retval;
    }

//====================================================================
//  static utility functions
//====================================================================

    public static DataType makeRowIdx() {
        DataType dt = new DataType(ROW_IDX, Integer.class);
        dt.setVisibility(DataType.Visibility.hidden);
        return dt;
    }

    public static DataType makeRowNum() {
        DataType dt = new DataType(ROW_NUM, Integer.class);
        dt.setVisibility(DataType.Visibility.hidden);
        return dt;
    }


    /**
     * This method convert data definition type from String to HREF, whenever all data elements with this data
     * definition can be converted to HREFs.
     *
     * @param dataGroup DataGroup
     */
    public static void convertHREFTypes(DataGroup dataGroup) {
        if (dataGroup.size() < 1) {
            return;
        }
        List<DataType> suspectHREFtypes = null;
        HREF href;
        int columnIdx = 0;
        for (Object obj : dataGroup.get(0).getData()) {
            if (obj instanceof String) {
                href = HREF.parseHREF((String) obj);
                if (href != null) {
                    if (suspectHREFtypes == null) {
                        suspectHREFtypes = new ArrayList<>();
                    }
                    suspectHREFtypes.add(dataGroup.getDataDefinitions()[columnIdx]);
                }
            }
            columnIdx++;
        }

        HREF[] hrefs;
        href = null;
        int rowIdx;
        if (suspectHREFtypes != null) {
            for (DataType dt : suspectHREFtypes) {
                hrefs = new HREF[dataGroup.size()];
                rowIdx = 0;
                for (DataObject o : dataGroup) {
                    href = HREF.parseHREF((String) o.getDataElement(dt));
                    if (href != null) {
                        hrefs[rowIdx] = href;
                    } else {
                        break;
                    }
                    rowIdx++;
                }
                if (href != null) {
                    // all fields are hrefs, can change data type definition
                    dt.setDataType(HREF.class);
                    rowIdx = 0;
                    for (DataObject o : dataGroup) {
                        o.setDataElement(dt, hrefs[rowIdx]);
                        rowIdx++;
                    }
                }
            }
        }
    }

    /**
     * merge the in coming attribute list with the current one.
     * All comments will be added.
     * Only attribute with new key will be added.
     */
    public void mergeAttributes(List<DataGroup.Attribute> attribList) {
        if (attribList == null) return;

        Set<String> curKeys = getAttributeKeys();
        for (DataGroup.Attribute at : attribList) {
            if (at.isComment()) {
                addAttribute(null, at.getValue());
            } else if (!curKeys.contains(at.getKey())) {
                addAttribute(at.getKey(), at.getValue());
            }
        }
    }

    public Iterator<DataObject> iterator() {
        return data.iterator();
    }


//===================================================================
//      public static classes
//===================================================================

    public static class Attribute implements Serializable, Cloneable {
        private static Random rnd = new Random();
        private String _key;
        private String _value;

        /**
         * create a comment attribute.
         * @param value
         */
        public Attribute(String value) {
            this(null, value);
        }

        public Attribute(String key, String value) {
            _key = key;
            _value = value;
        }

        public static Attribute parse(String s) {
            if (s == null || !s.startsWith("\\")) return null;
            String v = s.substring(1);  // remove '\'
            if (v.startsWith(" ")) {
                // this is a comment
                return new Attribute(v.trim());
            } else {
                String[] keyVal = v.split("=", 2);  // key/value separated by first '='
                if (keyVal.length == 2) {
                    return new Attribute(keyVal[0].trim(), keyVal[1].trim());
                } else {
                    return null;
                }
            }
        }

        public String getKey() {
            return _key;
        }

        public String getValue() {
            return _value;
        }

        public boolean isComment() {
            return _key == null ;
        }

        @Override
        public String toString() {
            String key = isComment() ? " " : _key + " = ";
            String value = _value == null ? "" : _value.replaceAll("\\p{Cntrl}", "");
            return "\\" + key + value;
        }
    }

}
