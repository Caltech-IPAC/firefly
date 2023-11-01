package edu.caltech.ipac.table;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Object representing the info. for group of columns, like GROUP element under the TABLE element in VOTable
 * Created by cwang on 9/28/18.
 */
public class GroupInfo implements Serializable, Cloneable{
    private String name;      // group name
    private String desc;      // group description
    private String ID;        // group ID
    private String ucd;
    private String utype;
    private List<ParamInfo> paramInfos = new ArrayList<>();    // params in this group
    private List<GroupInfo> groupInfos = new ArrayList<>();    // nested groups
    private List<RefInfo> columnRefs = new ArrayList<>();      // referenced columns in form of RefInfo objects
    private List<RefInfo> paramRefs = new ArrayList<>();       // referenced ParamInfo in form of RefInfo objects

    public GroupInfo() {}

    public GroupInfo(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public GroupInfo(String name, String desc, List<RefInfo> columnRefs) {
        this(name,desc);
        setColumnRefs(columnRefs);;
    }

    public void setName(String name) { this.name =  name;}
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

    public void setUCD(String ucd) {
        this.ucd = ucd;
    }
    public String getUCD() {
        return ucd;
    }

    public void setUtype(String utype) {
        this.utype = utype;
    }
    public String getUtype() {
        return utype;
    }

    public List<ParamInfo> getParamInfos() {
        return paramInfos;
    }
    public void setParamInfos(List<ParamInfo> paramInfos) { this.paramInfos = paramInfos; }

    public List<GroupInfo> getGroupInfos() {
        return groupInfos;
    }
    public void setGroupInfos(List<GroupInfo> groupInfos) { this.groupInfos = groupInfos; }

    public List<RefInfo> getColumnRefs() { return columnRefs; }
    public void setColumnRefs(List<RefInfo> refs) {
        columnRefs.clear();
        if (refs != null) columnRefs.addAll(refs);
    }

    public List<RefInfo> getParamRefs() { return paramRefs; }
    public void setParamRefs(List<RefInfo> refs) {
        paramRefs.clear();
        if (refs != null) paramRefs.addAll(refs);
    }

    // convert FieldRef into DataType
    public List<DataType> getReferencedColumns(List<DataType> columns) {
        List<DataType> dataTypeAry = new ArrayList<>();

        for (RefInfo fRef : columnRefs) {
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
        GroupInfo gobj = (GroupInfo) super.clone();
        gobj.paramInfos = new ArrayList<>(paramInfos);
        gobj.groupInfos = new ArrayList<>(groupInfos);
        gobj.columnRefs = new ArrayList<>(columnRefs);
        gobj.paramRefs = new ArrayList<>(paramRefs);
        return gobj;
    }


    public String toString() {
        StringBuilder sb =  new StringBuilder(StringUtils.isEmpty(name) ? "[" : name+", [");

        for (int i = 0; i < columnRefs.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("\"").append(columnRefs.get(i).getRef()).append("\"");
        }

        sb.append("]");
        return sb.toString();

    }

    public static class RefInfo implements Serializable, Cloneable {
        private String ref;
        private String ucd;
        private String utype;

        public RefInfo() {}

        public RefInfo(String ref, String ucd, String utype) {
            this.ref = ref;
            this.ucd = ucd;
            this.utype = utype;
        }

        public String getRef() {
            return ref;
        }
        public void setRef(String ref) { this.ref = ref;}

        public String getUcd() {
            return ucd;
        }
        public void setUcd(String ucd) { this.ucd = ucd; }

        public String getUtype() {
            return utype;
        }
        public void setUtype(String utype) { this.utype = utype; }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
