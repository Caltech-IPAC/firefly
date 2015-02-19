/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.dd.UIAttrib;

import java.util.ArrayList;

/**
 * Date: Mar 19, 2008
 *
 * @author loi
 * @version $Id: MenuItemAttrib.java,v 1.5 2010/09/28 17:59:24 roby Exp $
 */
public class MenuItemAttrib extends UIAttrib {

    public enum ToolbarButtonType {NONE,REQUEST,COMMAND}
    private ArrayList<MenuItemAttrib> children = new ArrayList<MenuItemAttrib>();
    private MenuItemAttrib parent;
    private boolean separator= false;
    private boolean important= false;
    private ToolbarButtonType bType= ToolbarButtonType.NONE;
    private int preferWidth;

    public MenuItemAttrib() {
    }

    public MenuItemAttrib(boolean separator) {
        this.separator= separator;
    }

    public MenuItemAttrib(String name, String label, String desc, String shortDesc, String icon) {
        super(name, label, desc, shortDesc, icon);
    }

    public void addMenuItem(MenuItemAttrib menuItem) {
        children.add(menuItem);
        menuItem.parent = this;
    }

    public MenuItemAttrib[] getChildren() {
        return children.toArray(new MenuItemAttrib[children.size()]);
    }

    public MenuItemAttrib getParent() {
        return parent;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public boolean isSeparator() { return separator; 
    }

    public int getPreferWidth() {
        return preferWidth;
    }

    public void setPreferWidth(int preferWidth) {
        this.preferWidth = preferWidth;
    }

    public void setToolBarButtonType(ToolbarButtonType bType) {
        this.bType= bType;
    }

    public ToolbarButtonType getToolBarButtonType() { return bType; }

    public void setImportant(boolean important) {
        this.important= important;
    }

    public boolean isImportant() { return important; }

    /**
     * traverse this tree from this node.  return null if not found
     * @param name the name of the MenuItemAttrib to search for
     * @return the MenuItemAttrib with the given name
     */
    public MenuItemAttrib findMenuItem(String name) {

        if (name == null) return null;

        if (getName().equals(name)) {
            return this;
        } else {
            for (int i = 0; i < children.size(); i++) {
                MenuItemAttrib child = children.get(i);
                return child.findMenuItem(name);
            }
        }
        return null;
    }


}
