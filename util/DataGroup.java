package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableReader;

import javax.swing.event.ChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This class is the data class for any set of objects that we show on
 * plots.  <i>This class need more documentation.</i>
 *
 * @see edu.caltech.ipac.visualize.draw.FixedObject
 *
 * @author Trey Roby
 * @version $Id: DataGroup.java,v 1.26 2012/10/23 05:39:53 loi Exp $
 *
 */
public class DataGroup implements TableConnectionList,
                                  Serializable,
                                  Iterable<DataObject>,
                                  Cloneable {

//======================================================================
//----------------------- Private / Protected variables ----------------
//======================================================================
    public static final String ADD_ATTRIBUTE                = "add_attribute";
    public static final String REMOVE_ATTRIBUTE             = "remove_attribute";
    public static final String UPDATE_ATTRIBUTE             = "remove_attribute";
    static public final String ROW_SELECTION = "RowSelection";

    //TODO: take this out!!!
    public final static long  serialVersionUID = 2020572073287448356L;
    public static final String ROWID_NAME = "ROWID";         // all row ids and indexes start from 0
    public static final DataType ROWID = new DataType(ROWID_NAME, Integer.class);


    private String               _title;
    private DataObject           _current;
    private final ArrayList<DataObject> _objects= new ArrayList<DataObject>(200);
    private boolean              _doingBulkUpdates  = false;
    private final ArrayList<DataType> _dataDefinitions= new ArrayList<DataType>(30);
    private final PropertyChangeSupport _propChange=
                                       new PropertyChangeSupport(this);
    private int _importantLength;
    private int _lastIdx= 0;
    private DataType _cachedDataDefinitionsAry[]= null;
    private LinkedHashMap<String,Attribute> _attributes;  // lazy initialize.. use getAttributes() to access

    private ArrayList<Integer> _selected = null;
    ChangeEvent _event;// = new ChangeEvent(_selected);
    private int rowIdxOffset = 0;

   public DataGroup(String title, DataType dataDefs[] ) {
       _title           = title;
       reinit(dataDefs);
   }

    public DataGroup(String title, List<DataType> dataDefs ) {
        this(title, dataDefs.toArray(new DataType[dataDefs.size()]));
    }

    public void setRowIdxOffset(int rowIdxOffset) {
        this.rowIdxOffset = rowIdxOffset;
    }

    public void reinit(DataType dataDefs[]) {
        _importantLength= getExtraImportantLength(dataDefs);
        for(int i=0; i<dataDefs.length; i++) {
            dataDefs[i].setColumnIdx(i);
            _dataDefinitions.add(dataDefs[i]);
        }
        validateDataDefs(dataDefs);
        clear();
    }

    public String getTitle() { return _title;  }
    public void setTitle(String title) { _title= title;  }


    public void addDataDefinition(DataType dataType) {
        _cachedDataDefinitionsAry= null;
        _dataDefinitions.add(dataType);
        int idx= _dataDefinitions.indexOf(dataType);
        dataType.setColumnIdx(idx);
        try {
            validateDataDefs(getDataDefinitions());
        } catch (IllegalArgumentException e) {
            _dataDefinitions.remove(dataType);
            _cachedDataDefinitionsAry= null;
            throw e;
        }
    }



    public void ensureCapacity(int size) {
        _objects.ensureCapacity(size);
    }


    public void setDataType(int typeIdx, Class type) {
        (_dataDefinitions.get(typeIdx)).setDataType(type);
    }

    /**
     * Return an iterator for all the objects in this group
     * @return Iterator  the iterator
     */
    public Iterator<DataObject> iterator() { return _objects.iterator(); }

    public void beginBulkUpdate() { _doingBulkUpdates = true; }

    public void endBulkUpdate()   {
        if (_doingBulkUpdates) {
            _propChange.firePropertyChange ( BULK_UPDATE, null, this);
        }
        _doingBulkUpdates= false;
    }

    /**
     * Set the current object
     * @param current  the new current object
     */
    public void setCurrent(DataObject current) { _current= current; }

    /**
     * Return the current object
     * @return DataObject  the current object
     */
    public DataObject getCurrent() { return _current; }

    /**
     * Return the extra data types defined for this group.
     * @return DataDataType[]  the extra data types.
     */
    public DataType[] getDataDefinitions() {
        if (_cachedDataDefinitionsAry==null) {
            _cachedDataDefinitionsAry= _dataDefinitions.toArray(
                                    new DataType[_dataDefinitions.size()]);
        }
        return _cachedDataDefinitionsAry;
    }

    public void shrinkToFitData() {
        shrinkToFitData(false);
    }

    /**
     * this method will shrink the Column's width to fit the maximum's width of the data
     */
    public void shrinkToFitData(boolean force) {
        for (DataType dt: getDataDefinitions()) {
            int maxDataWidth = dt.getMaxDataWidth();
            if (force || maxDataWidth == 0) {
                for(DataObject row : this) {
                    int vlength = Number.class.isAssignableFrom(dt.getDataType()) ? row.getFormatedData(dt).length() : String.valueOf(row.getDataElement(dt)).length();
                    maxDataWidth = Math.max(maxDataWidth, vlength);
                }
            }
            Integer[] vals = {6, maxDataWidth, StringUtils.length(dt.getKeyName()), StringUtils.length(dt.getDataUnit())};
            int w = Collections.max(Arrays.asList(vals));
            dt.getFormatInfo().setWidth(w);
        }
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

    /**
     * returns a snapshot/view of the objects in this DataGroup.
     */
    public List<DataObject> values() {
        return Collections.unmodifiableList(_objects);
    }

    /**
     * return the row data of this data group.. this is accessible to package classes only.
     * @return
     */
    List<DataObject> getValues() {
        return _objects;
    }

    public void add(DataObject s) {
        s.setRowIdx(_objects.size() + rowIdxOffset);
       _objects.add(s);
       if (!_doingBulkUpdates) {
           _propChange.firePropertyChange ( ADD, null, this);
       }
    }

    public void remove(DataObject s) {
       Assert.tst(_objects.contains(s));
       _objects.remove(s);
       if (!_doingBulkUpdates) {
           _propChange.firePropertyChange ( REMOVE, null, this);
       }
    }

    /**
     * Add an attribute to this data group
     * @param attrib
     * @return previous _value associated with specified _key, or null if there
     * was no mapping for _key. A null return can also indicate that the map
     * previously associated null with the specified _key, if the implementation
     * supports null values.
     */
    public Object addAttributes(Attribute attrib) {
        Object retval = getAttributes().put(attrib.getKey(), attrib);
        if (!_doingBulkUpdates) {
            String type = retval == null ? ADD_ATTRIBUTE : UPDATE_ATTRIBUTE;
            _propChange.firePropertyChange ( type, retval, attrib);
        }

        return retval;
    }

    /**
     * Remove an attribute from this data group.
     * @param key
     * @return previous _value associated with specified _key,
     * or null if there was no mapping for _key
     */
    public Object removeAttributes(String key) {
        Object retval = getAttributes().remove(key);
        if (!_doingBulkUpdates && retval != null) {
            _propChange.firePropertyChange ( REMOVE_ATTRIBUTE, retval, null);
        }
        return retval;
    }

    public Attribute getAttribute(String key) {
        return  getAttributes().get(key);
    }

    public Set<String> getAttributeKeys() {
        return Collections.unmodifiableSet(getAttributes().keySet());
    }

    public void clear() {
       beginBulkUpdate();
       DataObject s;

       for(Iterator<DataObject> i= _objects.iterator(); (i.hasNext()); ) {
          s= i.next();
          i.remove();
       }
       if (_attributes != null) {
           _attributes.clear();
       }
       endBulkUpdate();
    }



    public DataObject get(int i) { return _objects.get(i); }


    public static boolean containsKey(DataType[] dataTypes, String key) {
        boolean found=false;
        for(int i= 0; (i<dataTypes.length && !found);i++) {
            found= dataTypes[i].getKeyName().equals(key);
        }
        return found;
    }

    public boolean containsKey(String key) {
        return containsKey(this.getDataDefinitions(), key);
    }

    public String[] getKeySet() {
        DataType dataTypes[]= getDataDefinitions();
        String retval[]= new String[dataTypes.length];
        for(int i= 0; (i<dataTypes.length); i++) {
            retval[i]= dataTypes[i].getKeyName();
        }
        return retval;
    }

    public void setSelected (ArrayList<Integer> selection) {
        ArrayList<Integer> old = _selected;
        _selected = selection;

        _propChange.firePropertyChange(new PropertyChangeEvent(this, ROW_SELECTION, old, _selected));
    }

    public ArrayList<Integer> getSelected () {
        return _selected;
    }

    /**
     * Returns a subset of the datagroup between the specified
     * fromIndex, inclusive, and toIndex, exclusive
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
        copy._importantLength = _importantLength;
        copy._lastIdx = _lastIdx;
        copy._objects.addAll((ArrayList<DataObject>) _objects.clone());
        copy._selected = _selected;
        copy._attributes = _attributes == null ? null :
                        (LinkedHashMap<String, Attribute>) _attributes.clone();
        copy._current = _current;

        return copy;
    }

//=====================================================================
//----------- Add / Remove Listener Methods ---------------------------
//=====================================================================

    /**
     * Add a property changed listener.
     * @param p the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
    }


//======================================================================
//------------- Methods from TableConnectionList  ----------------------
//======================================================================

    public int size() { return _objects.size(); }
    public int indexOf(Object o) { return indexOf((DataObject)o); }
    public int indexOf(DataObject fixedObj) {
        return _objects.indexOf(fixedObj);
    }

//======================================================================
//------------------ Private / Protected / Package Methods --------------
//======================================================================

    public Map<String,Attribute> getAttributes() {
        if ( _attributes == null ) {
            _attributes = new LinkedHashMap<String,Attribute>();
        }
        return _attributes;
    }

    String availableKeys() {
        DataType dataTypes[]= getDataDefinitions();
        StringBuffer buff= new StringBuffer(dataTypes.length + 10);
        buff.append("Available Keys: ");
        for(int i= 0; (i<dataTypes.length); i++) {
            buff.append(dataTypes[i].getKeyName());
            if (i<dataTypes.length-1) buff.append(", ");
        }
        return buff.toString();
    }

//    List getDataDefinitionList() {
//        return _dataDefinitions;
//    }

    void setLastElementIdx(int lastIdx) {
        _lastIdx= lastIdx;
    }

    int getLastElementIdx() { return _lastIdx; }

    private int getExtraImportantLength(DataType data[]) {
        int len= 0;
        for(int i= 0; (i<data.length); i++) {
            if (data[i].getImportance() == DataType.Importance.HIGH) {
                len++;
            }
        }
        return len;
    }

    private static void validateDataDefs(DataType dataDef[]) {
        int tstCol;
        for(int i=0; i<dataDef.length; i++) {
            tstCol= dataDef[i].getColumnIdx();
            for(int j=i+1; j<dataDef.length; j++) {
                Assert.argTst(( tstCol!= dataDef[j].getColumnIdx()),
                              "data["+i+"] and data["+j+
                              "have the same column idx and, each must be unique");

            }
        }
    }

    /**
     * This method convert data definition type from String to HREF,
     * whenever all data elements with this data definition can be
     * converted to HREFs.
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
                href = HREF.parseHREF((String)obj);
                if (href != null) {
                    if (suspectHREFtypes == null) {
                        suspectHREFtypes = new ArrayList<DataType>();
                    }
                    suspectHREFtypes.add(dataGroup.getDataDefinitions()[columnIdx]);
                }
            }
            columnIdx++;
        }

        HREF [] hrefs;
        href = null;
        int rowIdx;
        if (suspectHREFtypes != null) {
            for (DataType dt : suspectHREFtypes) {
                hrefs = new HREF[dataGroup.size()];
                rowIdx = 0;
                for (DataObject o : dataGroup) {
                    href = HREF.parseHREF((String)o.getDataElement(dt));
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
//------------------------- Factory Methods -------------------------
//===================================================================

    protected List newListenerList()  { return new Vector(2,2); }


//===================================================================
//      public static classes
//===================================================================

    public static class Attribute implements Serializable, Cloneable {
        private String _type;
        private String _key;
        private Object _value;
        private String _formatString;
        private Class  _typeClass;

        public Attribute(String key, String value) {
            this(key, value, null, null);
        }

        public Attribute(String key, Object value, String type, String formatString) {
            _type = type == null ? "" : type;
            _key = key;
            _value = value;
            _formatString = formatString;
            _typeClass = IpacTableReader.resolveClass(_type);
        }

        public String getType() {
            return _type;
        }

        public String getKey() {
            return _key;
        }

        public Object getValue() {
            return _value;
        }

        public Class getTypeClass() {
            return _typeClass;
        }

        public String getFormatString() {
            return _formatString;
        }

        public boolean hasType() {
            return _type != null && _type.length() > 0;
        }

        public String formatValue() {
            return formatValue(null);
        }

        public String formatValue(Locale locale) {
            if ( _value == null ) {
                return null;
            } else if ( getFormatString() != null ) {
                return locale == null ? String.format(getFormatString(), _value) :
                            String.format(locale, getFormatString(), _value);
            } else {
                return _value.toString();
            }
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
