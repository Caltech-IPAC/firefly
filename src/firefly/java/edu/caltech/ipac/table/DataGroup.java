/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.HREF;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.*;

import static edu.caltech.ipac.table.TableUtil.fixDuplicates;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

/**
 * Object representing tabular data.  For historic reason, DataObject represent a row and DataType represent a column.
 */
public class DataGroup implements Serializable, Cloneable, Iterable<DataObject> {

    public static final String ROW_IDX = "ROW_IDX";               // this contains the original row index of the table before any sorting or filtering
    public static final String ROW_NUM = "ROW_NUM";               // this is row number of the current dataset. (oracle's rownum)

    private LinkedHashMap<String, DataType> columns = new LinkedHashMap<>();
    private HashMap<String, PrimitiveList> data = new HashMap<>();
    private TableMeta meta = new TableMeta();
    private String title;
    private int size;
    private List<GroupInfo> groups = new ArrayList<>();   // for <GROUP> under <TABLE> of VOTable
    private List<LinkInfo> links = new ArrayList<>();     // for <LINK> under <TABLE> of VOTable
    private List<ParamInfo> params = new ArrayList<>();  // for <PARAM> under <TABLE> of VOTABLE
    private List<ResourceInfo> resources = new ArrayList<>();  // for <RESOURCE> in VOTABLE
    private transient DataType[] cachedColumnsAry = null;
    private transient int highlightedRow = -1;
    private int initCapacity = 1000;

    public DataGroup() {}

    public DataGroup(String title, DataType[] dataDefs) {
        this(title, Arrays.asList(dataDefs));
    }

    public DataGroup(String title, List<DataType> dataDefs) {
        this.title = title;
        fixDuplicates(dataDefs);
        dataDefs.forEach(this::addDataDefinition);
    }

    public void setInitCapacity(int initCap) { this.initCapacity = initCap; }

    public String getTitle() {
        return title;
    }

    public void setTableMeta(TableMeta meta) { this.meta = meta; }
    public TableMeta getTableMeta() { return meta; }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Append a column to this DataGroup.
     * @param dataType  the column to append
     */
    public void addDataDefinition(DataType dataType) {
        columns.put(dataType.getKeyName(), dataType);
    }

    /**
     * This ONLY removes the DataType.  Data will still be there.  DataType contains
     * the column_idx into the data, so it should be okay.
     * @param names
     */
    public void removeDataDefinition(String ...names) {
        if (names == null) return;
        for(String cn : names) {
            columns.remove(cn);
            data.remove(cn);
        };
    }

    /**
     * @return DataDataType[]  the all column information of this DataGroup.
     */
    public DataType[] getDataDefinitions() {
        if (cachedColumnsAry == null || cachedColumnsAry.length != columns.size()) {
            cachedColumnsAry = columns.values().toArray(new DataType[0]);
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
        ArrayList<DataObject> rval = new ArrayList<>(size());
        for (int i=0; i<size(); i++) {
            rval.add(DataObject.getDataAt(this, i));
        }
        return rval;
    }

    public void clearData() {
        data.clear();
        size = 0;
    }

    /**
     * add a row represented by the given DataObject into this DataGroup
     * @param s
     */
    public void add(DataObject s) {
        if (s != null) {
            if (data.size() == 0) size = 0;                    // reset size if there's no data
            for (DataType dt : getDataDefinitions()) {
                addData(dt.getKeyName(), s.getDataElement(dt.getKeyName()));
            }
            size++;
        }
    }

    /**
     * Add a full row of data based on the columns index.
     * @param row   the array of data based on the columns index.
     */
    public void add(Object[] row) {
        if (row != null) {
            if (data.size() == 0) size = 0;                    // reset size if there's no data
            DataType[] cols = getDataDefinitions();
            for (int i = 0; i < cols.length; i++) {
                addData(cols[i].getKeyName(), row[i]);
            }
            size++;
        }
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
     * this returns a list of attributes.
     * @return
     */
    public List<Attribute> getAttributeList() {
        return new ArrayList<>(meta.getAttributes().values());
    }

    public DataObject get(int rowIdx) {
        return DataObject.getDataAt(this, rowIdx);
    }

    public boolean containsKey(String key) {
        return columns.containsKey(key);
    }

    public String[] getKeySet() {
        return columns.keySet().toArray(new String[columns.size()]);
    }

    /**
     * @return  a shallow clone of this DataGroup
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        DataGroup copy = new DataGroup(title, getDataDefinitions());
        copy.addMetaFrom(this);
        copy.data = data;
        return copy;
    }

    /**
     * @return a clone of this DataGroup without the DATA.
     */
    public DataGroup cloneWithoutData() {
        ArrayList<DataType> copyCols = new ArrayList<>(columns.size());
        for (DataType dt: getDataDefinitions()) {
            copyCols.add(dt.newCopyOf());
        }
        DataGroup copy = new DataGroup(title, copyCols);
        copy.addMetaFrom(this);
        return copy;
    }

    public void addMetaFrom(DataGroup dg) {
        if (dg == null) return;
        applyIfNotEmpty(dg.getTitle(), v -> setTitle(v));
        dg.getTableMeta().getKeywords().forEach(kw -> getTableMeta().addKeyword(kw.getKey(), kw.getValue()));
        dg.getTableMeta().getAttributeList().forEach(at -> addAttribute(at.getKey(), at.getValue()));
        dg.getGroupInfos().forEach(g -> getGroupInfos().add(g));
        dg.getLinkInfos().forEach(l -> getLinkInfos().add(l));
        dg.getParamInfos().forEach(p -> getParamInfos().add(p));
        dg.getResourceInfos().forEach(r -> getResourceInfos().add(r));
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getHighlightedRow() {
        return highlightedRow;
    }

    public void setHighlightedRow(int highlightedRow) {
        this.highlightedRow = highlightedRow;
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
        toIndex = Math.min(toIndex, size);
        DataGroup retval = cloneWithoutData();
        for(int i=fromIndex; i<toIndex; i++) {
            retval.add(DataObject.getDataAt(this, i));
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
     * get GroupInfo list
     * @return list of GroupInfo object
     */
    public List<GroupInfo> getGroupInfos() {
        return groups;
    }
    public void setGroupInfos(List<GroupInfo> groupInfos) {
        groups.clear();
        if (groupInfos != null) groups.addAll(groupInfos);
    }

    /**
     * get LinkInfo list
     * @return a list of LinkInfo
     */
    public List<LinkInfo> getLinkInfos() {
        return links;
    }
    public void setLinkInfos(List<LinkInfo> linkInfos) {
        links.clear();
        links.addAll(linkInfos);
    }

    /**
     * get a list Params representing static columns (like PARAM in votable)
     * @return a list of static columns in form of Params objects
     */
    public List<ParamInfo> getParamInfos() {
        return params;
    }
    public void setParamInfos(List<ParamInfo> paramInfos) {
        this.params.clear();
        this.params.addAll(paramInfos);
    }
    public ParamInfo getParam(String name) {
        return params.stream()
                    .filter(p -> p.getKeyName().equals(name))
                    .findAny()
                    .orElse(null);
    }

    public List<ResourceInfo> getResourceInfos() { return resources; }
    public void setResourceInfos(List<ResourceInfo> resourceInfos) {
        this.resources.clear();
        if (resourceInfos != null) this.resources.addAll(resourceInfos);
    }

    /**
     * merge the in coming attribute list with the current one.
     * All comments will be added.
     * Only attribute with new key will be added.
     */
    public void mergeAttributes(List<DataGroup.Attribute> attribList) {
        if (attribList == null) return;

        for (DataGroup.Attribute at : attribList) {
            if ( !(at.isComment() || getTableMeta().contains(at.getKey())) ) {
                addAttribute(at.getKey(), at.getValue());
            }
        }
    }

    public Iterator<DataObject> iterator() {
        return values().iterator();
    }


//===================================================================
//      public static classes
//===================================================================

    public static class Attribute implements Serializable, Cloneable {
        private String key;
        private String value;
        private boolean isKeyword;

        /**
         * create a comment attribute.
         * @param value
         */
        public Attribute(String value) {
            this(null, value);
        }

        public Attribute(String key, String value) {
            this(key, value, false);
        }
        public Attribute(String key, String value, boolean isKeyword) {
            this.key = key;
            this.value = value;
            this.isKeyword = isKeyword;
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
                    String val = keyVal[1].trim();
                    if (val.matches("^\".+\"$|^'.+'$")) val = val.substring(1, val.length()-1);
                    return new Attribute(keyVal[0].trim(), val);
                } else {
                    return null;
                }
            }
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public boolean isComment() {
            return key == null ;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isKeyword() { return isKeyword; }

        @Override
        public String toString() {
            String key = isComment() ? " " : this.key + " = ";
            String value = this.value == null ? "" : this.value.replaceAll("\\p{Cntrl}", "");
            return "\\" + key + value;
        }
    }

    public Object getData(String cname, int rowIdx) {
        PrimitiveList data = getDataList(cname);
        return data == null ? null : data.get(rowIdx);
    }

    public void setData(String cname, int rowIdx, Object val) {
        PrimitiveList data = getDataList(cname);
        if (data != null) {
            data.set(rowIdx, val);
        }
    }

    public void trimToSize() {
        for (PrimitiveList plist : data.values()) {
            plist.trimToSize();
        }
    }

//====================================================================
// data retrieval methods  .. protect this to keep DataGroup consistent
//====================================================================

    private void addData(String cname, Object val) {
        PrimitiveList data = getDataList(cname);
        if (data != null) {
            data.add(val);
        }
    }

    private PrimitiveList getDataList(String cname) {
        PrimitiveList dataList = data.get(cname);
        if (dataList == null) {
            DataType dt = getDataDefintion(cname);
            if (dt != null) {
                if (dt.getArraySize() != null) {
                    dataList =  new PrimitiveList.Objects(initCapacity);
                } else {
                    Class clz = dt.getDataType();
                    if (clz == Double.class) {
                        dataList = new PrimitiveList.Doubles(initCapacity);
                    } else if (clz == Float.class) {
                        dataList = new PrimitiveList.Floats(initCapacity);
                    } else if (clz == Long.class) {
                        dataList = new PrimitiveList.Longs(initCapacity);
                    } else if (clz == Integer.class) {
                        dataList = new PrimitiveList.Integers(initCapacity);
                    } else if (clz == Short.class) {
                        dataList = new PrimitiveList.Shorts(initCapacity);
                    } else if (clz == Byte.class) {
                        dataList = new PrimitiveList.Bytes(initCapacity);
                    } else if (clz == Boolean.class) {
                        dataList = new PrimitiveList.Booleans(initCapacity);
                    } else {
                        dataList = new PrimitiveList.Objects(initCapacity);
                    }
                }
                data.put(cname, dataList);
            }
        }
        return dataList;
    }
}
