package edu.caltech.ipac.table;

import java.io.Serializable;
import java.util.*;

/**
 * Object representing the info. for group of columns, like GROUP element under the TABLE element in VOTable
 * Created by cwang on 9/28/18.
 */
public class GroupInfo implements Serializable, Cloneable{
    private String name;      // group name
    private String desc;      // group description
    private String ID;        // group ID
    private List<FieldRef> fieldRefs = new ArrayList<>();      // referenced columns in form of FieldRef objects
    private List<DataType> dataTypeAry = new ArrayList<>();    // referenced columns in form of DataType objects

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

    public void addFieldRef(String ref, String ucd, String utype) {
        FieldRef fRef = new FieldRef(ref, ucd, utype);
        addFieldRef(fRef);
    }

    public void addFieldRef(FieldRef ref) {
        fieldRefs.add(ref);
    }

    // convert FieldRef into DataType
    public List<DataType> getReferencedColumns(List<DataType> columns) {
        if (dataTypeAry.size() == 0 && fieldRefs.size() > 0) {
            for (FieldRef fRef : fieldRefs) {
                String refName = fRef.getRef();
                for (DataType col : columns) {
                    String id = col.getID();
                    if (id != null && id.equals(refName)) {
                        dataTypeAry.add(col);
                    }
                }
            }
        }
        return dataTypeAry;
    }

    public Object clone() throws CloneNotSupportedException {
        GroupInfo gobj = (GroupInfo) super.clone();
        for (FieldRef ref : fieldRefs) {
            gobj.addFieldRef((FieldRef)ref.clone());
        }
        return gobj;
    }

    private String outString(boolean isJson) {
        String q = isJson ? "\"" : "";
        StringBuffer sb = new StringBuffer("{");

        int idx = 0;

        idx = LinkInfo.outKeyValue(idx, q, "ID", getID(), sb );
        idx = LinkInfo.outKeyValue(idx, q, "name", getName(), sb );
        idx = LinkInfo.outKeyValue(idx, q, "description", desc, sb);

        if (idx > 0) {
            sb.append(", ");
        }
        sb.append(q).append("refs").append(q).append(":[");

        idx = 0;

        for (FieldRef ref : fieldRefs) {
            if (idx++ != 0) {
                sb.append(", ");
            }
            if (isJson) {
                sb.append(ref.toJsonString());
            } else {
                sb.append(ref.toString());
            }
        }
        sb.append("]}");

        return sb.toString();
    }

    public String toJsonString() {
        return outString(true);
    }

    public String toString() {
        return outString(false);
    }



    public static class FieldRef implements Serializable, Cloneable {
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


        private String outString(boolean isJson) {
            String q = isJson ? "\"" : "";
            StringBuffer sb = new StringBuffer("{");

            sb.append(q).append("ref").append(q).append(":\"").append(getRef()).append("\"");

            LinkInfo.outKeyValue(1, q, "ucd", getUcd(), sb);
            LinkInfo.outKeyValue(1, q, "utype", getUtype(), sb);

            sb.append("}");
            return sb.toString();
        }

        public String toJsonString() {
            return outString(true);
        }

        public String toString() {
            return outString(false);
        }
    }
}
