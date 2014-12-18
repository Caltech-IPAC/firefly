package edu.caltech.ipac.firefly.server.db.spring;

import java.util.Date;
import java.util.List;

/**
 * Date: Nov 7, 2008
*
* @author loi
* @version $Id: ColumnDef.java,v 1.1 2009/01/23 01:44:14 loi Exp $
*/
public class ColumnDef {
    String name;
    Class type;
    List data;

    public ColumnDef(String name, Class type) {
        this(name, type, null);
    }

    public ColumnDef(String name, List data) {
        this(name, data.get(0).getClass());
    }

    public ColumnDef(String name, Class type, List data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getType() {
        if (type.isAssignableFrom(Integer.class)) {
            return "integer";
        } else if (type.isAssignableFrom(Float.class)) {
            return "float";
        } else if (type.isAssignableFrom(Date.class)) {
            return "datetime";
        } else {
            return "varchar(255)";
        }
    }

    public List getData() {
        return data;
    }

    public void setData(List data) {
        this.data = data;
    }

    public String toString() {
        return name + " " + getType();
    }
}
