/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.jhu.util.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This class is the data class for any set of objects that we show on plots.  <i>This class need more
 * documentation.</i>
 *
 * @author Trey Roby
 * @version $Id: DataGroup.java,v 1.26 2012/10/23 05:39:53 loi Exp $
 * @see edu.caltech.ipac.visualize.draw.FixedObject
 */
public class DataGroup implements Serializable,
                                    Iterable<DataObject>,
                                    Cloneable {

//======================================================================
//----------------------- Private / Protected variables ----------------
//======================================================================

    //TODO: take this out!!!
    public static final String ROWID_NAME = "ROWID";         // all row ids and indexes start from 0
    public static final DataType ROWID = new DataType(ROWID_NAME, Integer.class);


    private String _title;
    private final ArrayList<DataObject> _objects = new ArrayList<DataObject>(200);
    private final ArrayList<DataType> _dataDefinitions = new ArrayList<DataType>(30);
    private ArrayList<Attribute> _attributes = new ArrayList<Attribute>();
    private HashMap<String, Attribute> _cachedAttributesMap = null;
    private DataType _cachedDataDefinitionsAry[] = null;

    private int rowIdxOffset = 0;

    public DataGroup(String title, DataType dataDefs[]) {
        _title = title;
        for (int i = 0; i < dataDefs.length; i++) {
            dataDefs[i].setColumnIdx(i);
            _dataDefinitions.add(dataDefs[i]);
        }
        validateDataDefs(dataDefs);
    }

    public DataGroup(String title, List<DataType> dataDefs) {
        this(title, dataDefs.toArray(new DataType[dataDefs.size()]));
    }

    public void setRowIdxOffset(int rowIdxOffset) {
        this.rowIdxOffset = rowIdxOffset;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }


    public void addDataDefinition(DataType dataType) {
        _cachedDataDefinitionsAry = null;
        _dataDefinitions.add(dataType);
        int idx = _dataDefinitions.indexOf(dataType);
        dataType.setColumnIdx(idx);
        try {
            validateDataDefs(getDataDefinitions());
        } catch (IllegalArgumentException e) {
            _dataDefinitions.remove(dataType);
            _cachedDataDefinitionsAry = null;
            throw e;
        }
    }

    /**
     * Return the extra data types defined for this group.
     *
     * @return DataDataType[]  the extra data types.
     */
    public DataType[] getDataDefinitions() {
        if (_cachedDataDefinitionsAry == null) {
            _cachedDataDefinitionsAry = _dataDefinitions.toArray(
                    new DataType[_dataDefinitions.size()]);
        }
        return _cachedDataDefinitionsAry;
    }

    public DataType getDataDefintion(String key) {
        getDataDefinitions();
        for (DataType a : _cachedDataDefinitionsAry) {
            if (a.getKeyName().equals(key)) {
                return a;
            }
        }
        return null;
    }


    public void ensureCapacity(int size) {
        _objects.ensureCapacity(size);
    }


    public void setDataType(int typeIdx, Class type) {
        (_dataDefinitions.get(typeIdx)).setDataType(type);
    }

    /**
     * Return an iterator for all the objects in this group
     *
     * @return Iterator  the iterator
     */
    public Iterator<DataObject> iterator() {
        return _objects.iterator();
    }

    public void shrinkToFitData() {
        shrinkToFitData(false);
    }

    /**
     * this method will shrink the Column's width to fit the maximum's width of the data
     */
    public void shrinkToFitData(boolean force) {
        for (DataType dt : getDataDefinitions()) {
            int maxDataWidth = dt.getMaxDataWidth();
            if (force || maxDataWidth == 0) {
                for (DataObject row : this) {
                    int vlength = Number.class.isAssignableFrom(dt.getDataType()) ? row.getFormatedData(dt).length() : String.valueOf(row.getDataElement(dt)).length();
                    maxDataWidth = Math.max(maxDataWidth, vlength);
                }
            }
            Integer[] vals = {6, maxDataWidth, StringUtils.length(dt.getKeyName()), StringUtils.length(dt.getDataUnit())};
            int w = Collections.max(Arrays.asList(vals));
            dt.getFormatInfo().setWidth(w);
        }
    }

    /**
     * returns a snapshot/view of the objects in this DataGroup.
     */
    public List<DataObject> values() {
        return Collections.unmodifiableList(_objects);
    }

    /**
     * return the row data of this data group.. this is accessible to package classes only.
     *
     * @return
     */
    List<DataObject> getValues() {
        return _objects;
    }

    public void add(DataObject s) {
        s.setRowIdx(_objects.size() + rowIdxOffset);
        _objects.add(s);
    }

    public void remove(DataObject s) {
        Assert.tst(_objects.contains(s));
        _objects.remove(s);
    }

    /**
     *
     * @param key
     * @param value
     */
    public void addAttribute(String key, String value) {
        _attributes.add(new Attribute(key, value));
    }

    /**
     * set the attributes to this new list
     */
    public void setAttributes(List<Attribute> attribList) {
        _attributes = new ArrayList<Attribute>(attribList);
        _cachedAttributesMap = null;
    }

    /**
     * merge the in coming attribute list with the current one.
     * All comments will be added.
     * Only attribute with new key will be added.
     */
    public void mergeAttributes(List<Attribute> attribList) {
        if (attribList == null) return;

        Set<String> curKeys = getAttributeKeys();
        for (Attribute at : attribList) {
            if (StringUtils.isEmpty(at.getKey())) {
                _attributes.add(at);
            } else if (!curKeys.contains(at.getKey())) {
                _attributes.add(at);
            }
        }
        _cachedAttributesMap = null;
    }

    /**
     * if there are duplicate keys, it will only return one.
     * @param key
     * @return
     */
    public Attribute getAttribute(String key) {
        return getAttributes().get(key);
    }

    /**
     * returns a set of unique keys
     * @return
     */
    public Set<String> getAttributeKeys() {
        return Collections.unmodifiableSet(getAttributes().keySet());
    }

    /**
     * this returns a map of attributes.  It will ignore comments and duplicate keywords.
     *
     * @return
     */
    public Map<String, Attribute> getAttributes() {
        if (_cachedAttributesMap == null) {
            _cachedAttributesMap = new HashMap<String, Attribute>();
            for (Attribute a : _attributes) {
                if (!StringUtils.isEmpty(a.getKey())) {
                    _cachedAttributesMap.put(a.getKey(), a);
                }
            }
        }
        return _cachedAttributesMap;
    }

    /**
     * Keyword is equivalent to attributes.  However, it mandates order and allow
     * for comments and duplicate keywords.
     * @return
     */
    public List<Attribute> getKeywords() {
        return Collections.unmodifiableList(_attributes);
    }


    public DataObject get(int i) {
        return _objects.get(i);
    }


    public static boolean containsKey(DataType[] dataTypes, String key) {
        boolean found = false;
        for (int i = 0; (i < dataTypes.length && !found); i++) {
            found = dataTypes[i].getKeyName().equals(key);
        }
        return found;
    }

    public boolean containsKey(String key) {
        return containsKey(this.getDataDefinitions(), key);
    }

    public String[] getKeySet() {
        DataType dataTypes[] = getDataDefinitions();
        String retval[] = new String[dataTypes.length];
        for (int i = 0; (i < dataTypes.length); i++) {
            retval[i] = dataTypes[i].getKeyName();
        }
        return retval;
    }

    /**
     * Returns a subset of the datagroup between the specified fromIndex, inclusive, and toIndex, exclusive
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public DataGroup subset(int fromIndex, int toIndex) {

        try {
            DataGroup dg = (DataGroup) clone();
            dg._objects.clear();
            if (fromIndex < size() && fromIndex < toIndex) {
                int endIdx = Math.min(size(), toIndex);
                dg._objects.addAll(_objects.subList(fromIndex, endIdx));
            }
            return dg;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DataGroup copy = new DataGroup(_title, (ArrayList<DataType>) _dataDefinitions.clone());
        copy._objects.addAll((ArrayList<DataObject>) _objects.clone());
        copy._attributes = (ArrayList<Attribute>) _attributes.clone();

        return copy;
    }

//======================================================================
//------------- Methods from TableConnectionList  ----------------------
//======================================================================

    public int size() {
        return _objects.size();
    }

    public int indexOf(Object o) {
        return indexOf((DataObject) o);
    }

    public int indexOf(DataObject fixedObj) {
        return _objects.indexOf(fixedObj);
    }

//======================================================================
//------------------ Private / Protected / Package Methods --------------
//======================================================================

    String availableKeys() {
        DataType dataTypes[] = getDataDefinitions();
        StringBuffer buff = new StringBuffer(dataTypes.length + 10);
        buff.append("Available Keys: ");
        for (int i = 0; (i < dataTypes.length); i++) {
            buff.append(dataTypes[i].getKeyName());
            if (i < dataTypes.length - 1) buff.append(", ");
        }
        return buff.toString();
    }

    private static void validateDataDefs(DataType dataDef[]) {
        int tstCol;
        for (int i = 0; i < dataDef.length; i++) {
            tstCol = dataDef[i].getColumnIdx();
            for (int j = i + 1; j < dataDef.length; j++) {
                Assert.argTst((tstCol != dataDef[j].getColumnIdx()),
                        "data[" + i + "] and data[" + j +
                                "have the same column idx and, each must be unique");

            }
        }
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
                        suspectHREFtypes = new ArrayList<DataType>();
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

//===================================================================
//      public static classes
//===================================================================

    public static class Attribute implements Serializable, Cloneable {
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
            _key = StringUtils.isEmpty(key) ? " " : key;
            _value = value;
        }

        public String getKey() {
            return _key;
        }

        public String getValue() {
            return _value;
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

        public boolean isComment() {
            return _key != null && _key.startsWith(" ");
        }

        @Override
        public String toString() {
            if (StringUtils.isEmpty(_key)) {
                return "\\ " + _value;
            } else {
                return "\\" + _key + " = " + _value;
            }
        }
    }

}
