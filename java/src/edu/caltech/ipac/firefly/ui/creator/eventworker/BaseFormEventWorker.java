package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.ActiveTabPane;
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

    protected void setVisible(String name, boolean isVisible) {
        for(FormHub h : hubs) {
            if (h.containsField(name)) {
                h.setVisible(name, isVisible);
                break;
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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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

