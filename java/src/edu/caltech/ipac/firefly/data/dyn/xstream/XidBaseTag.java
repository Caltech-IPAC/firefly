package edu.caltech.ipac.firefly.data.dyn.xstream;

import edu.caltech.ipac.util.dd.UIComponent;

import java.io.Serializable;

// use this class to make use of the xid/refXid functionality 
public abstract class XidBaseTag implements UIComponent, Serializable {
    private String xid;

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }
}

