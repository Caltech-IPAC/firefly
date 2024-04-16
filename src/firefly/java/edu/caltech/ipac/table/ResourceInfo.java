package edu.caltech.ipac.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object representing the RESOURCE block of VOTABLE.  It is by design that INFO, LINK, TABLE, and RESOURCE are not included.
 * It is designed to record non-table related resources under VOTABLE and made accessible by all TABLEs under VOTABLE.
 */
public class ResourceInfo implements Serializable, Cloneable {
    private String ID;
    private String name;
    private String type;
    private String utype;
    private String desc;
    private List<GroupInfo> groups = new ArrayList<>();     // for <GROUP> under <RESOURCE>
    private List<ParamInfo> params = new ArrayList<>();     // for <PARAM> under <RESOURCE>
    private Map<String, String> infos = new HashMap<>();     // for <INFO> under <RESOURCE>

    public ResourceInfo() {}

    public ResourceInfo(String ID, String name, String type, String utype, String desc) {
        this.ID = ID;
        this.name = name;
        this.type = type;
        this.utype = utype;
        this.desc = desc;
    }

    public String getID() { return ID; }
    public void setID(String ID) { this.ID = ID; }

    public String getName() { return name;}
    public void setName(String name) { this.name = name;}

    public String getType() { return type; }
    public void setType(String type) { this.type = type;}

    public String getUtype() { return utype;}
    public void setUtype(String utype) { this.utype = utype; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc;}

    public List<GroupInfo> getGroups() { return groups;}
    public void setGroups(List<GroupInfo> groups) { this.groups = groups; }

    public List<ParamInfo> getParams() { return params; }
    public void setParams(List<ParamInfo> params) { this.params = params; }

    public Map<String, String> getInfos() { return infos; }
    public void setInfos(Map<String, String> infos) { this.infos = infos; }
}


