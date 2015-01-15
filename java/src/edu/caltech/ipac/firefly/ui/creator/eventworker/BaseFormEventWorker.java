/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.ActiveTabPane;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.data.Param;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author loi
 * @version $Id: BaseFormEventWorker.java,v 1.1 2011/09/04 01:49:57 loi Exp $
 */
public abstract class BaseFormEventWorker implements FormEventWorker {

    private List<FormHub> hubs = new ArrayList<FormHub>();
    private List<String> fnames = null;

    public void addHub(FormHub hub) {
        hubs.add(hub);
    }

    protected boolean containsField(String fname) {
        for(FormHub h : hubs) {
            if (h.containsField(fname)) {
                return true;
            }
        }
        return false;
    }

    protected String getValue(String fname) {
        for(FormHub h : hubs) {
            if (h.containsField(fname)) {
                return h.getValue(fname);
            }
        }
        return null;
    }

    protected void setValue(Param p) {
        for(FormHub h : hubs) {
            if (h.containsField(p.getName())) {
                h.setValue(p);
                break;
            }
        }
    }

    protected void setHidden(String name, boolean isHidden) {
        for(FormHub h : hubs) {
            if (h.containsField(name)) {
                h.setHidden(name, isHidden);
                break;
            }
        }
    }

    /**
     * this convenience method search for the name field and set visibility on it.
     * if there is not a field with the given name, it will try to find it by ID.
     * @param name
     * @param isVisible
     */
    protected void setVisible(String name, boolean isVisible) {
        for(FormHub h : hubs) {
            if (h.containsField(name)) {
                h.setVisible(name, isVisible);
                break;
            } else {
                // try searching name by ID
                Widget c = GwtUtil.findById(h.getForm(), name);
                if (c != null) {
                    c.setVisible(isVisible);
                    break;
                }
            }
        }
    }

    protected void setCollapsiblePanelVisibility(String name, boolean show) {
        for(FormHub h : hubs) {
            h.setCollapsiblePanelVisibility(name, show);
        }
    }

    protected TabPane getTabPane(String id) {
        for(FormHub h : hubs) {
            if (h.containsField(id)) {
                return h.getTabPane(id);
            }
        }
        return null;
    }

    protected ActiveTabPane getActiveTabPane(String id) {
        for(FormHub h : hubs) {
            ActiveTabPane atp = h.getActiveTabPane(id);
            if (atp != null) {
                return atp;
            }
        }
        return null;
    }


    protected List<String> getFieldIds() {
        int fsize = 0;
        for(FormHub h : hubs) {
            fsize += h.getFieldIds() == null ? 0 : h.getFieldIds().size();
        }
        if (fnames == null || fnames.size() != fsize) {
            fnames = new ArrayList<String>();
            for(FormHub h : hubs) {
                fnames.addAll(h.getFieldIds());
            }
        }
        return fnames;
    }

    protected boolean isVisible(String fname) {
        for(FormHub h : hubs) {
            if (h.isVisible(fname)) {
                return true;
            }
        }
        return false;
    }
}
