/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

import java.io.Serializable;

/**
 * Date: Mar 19, 2008
 *
 * @author loi
 * @version $Id: UIAttrib.java,v 1.1 2010/09/28 17:58:39 roby Exp $
 */
public class UIAttrib implements UIAttributes, Serializable {
    private String name;
    private String label;
    private String desc;
    private String shortDesc;
    private String icon;

    public UIAttrib() {
    }

    public UIAttrib(String name, String label, String desc, String shortDesc, String icon) {
        this.name = name;
        this.label = label;
        this.desc = desc;
        this.shortDesc = shortDesc;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
