package edu.caltech.ipac.table;

import java.io.Serializable;
import java.util.*;
import edu.caltech.ipac.util.StringUtils;

/**
 * Object representing the info. for group of columns, like GROUP element under the TABLE element in VOTable
 * Created by cwang on 9/28/18.
 */
public class GroupInfo implements Serializable, Cloneable{
    private String name;      // group name
    private String desc;      // group description
    private String ID;        // group ID
    private List<FieldRef> fieldRefs = new ArrayList<>();      // referenced columns in form of FieldRef objects

    public GroupInfo(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    public String getDescription() {
        return desc;
    }

    public void setID(String id) {
        this.ID = id;
    }

    public String getID() {
        return ID;
    }

    public List<FieldRef> getFieldRefs() {
        return fieldRefs;
    }

    // convert FieldRef into DataType
    public List<DataType> getReferencedColumns(List<DataType> columns) {
        List<DataType> dataTypeAry = new ArrayList<>();

        for (FieldRef fRef : fieldRefs) {
            String refName = fRef.getRef();
            for (DataType col : columns) {
                String id = col.getID();
                if (id != null && id.equals(refName)) {
                    dataTypeAry.add(col);
                }
            }
        }
        return dataTypeAry;
    }

    public Object clone() throws CloneNotSupportedException {
        GroupInfo gobj = new GroupInfo(name, desc);
        List<FieldRef>  newRefs = gobj.getFieldRefs();

        for (FieldRef ref : fieldRefs) {
           newRefs.add((FieldRef)ref.clone());
        }
        return gobj;
    }


    public String toString() {
        StringBuilder sb =  new StringBuilder(StringUtils.isEmpty(name) ? "[" : name+", [");

        for (int i = 0; i < fieldRefs.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("\"").append(fieldRefs.get(i).getRef()).append("\"");
        }

        sb.append("]");
        return sb.toString();

    }

    public static class FieldRef implements Cloneable {
        private String ref;
        private String ucd;
        private String utype;

        public FieldRef(String ref, String ucd, String utype) {
            this.ref = ref;
            this.ucd = ucd;
            this.utype = utype;
        }

        public String getRef() {
            return ref;
        }

        public String getUcd() {
            return ucd;
        }

        public String getUtype() {
            return utype;
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
