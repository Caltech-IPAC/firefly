package edu.caltech.ipac.firefly.ui.table.filter;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;


/**
 * Date: May 21, 2008
 *
 * @author loi
 * @version $Id: FilterDialog.java,v 1.2 2012/08/09 01:09:28 loi Exp $
 */
public class FilterDialog extends BaseDialog {

    public static String SHOW = "show";
    public static String HIDE = "hide";

    private FilterPanel filterPanel;
    private GeneralCommand applyCmd;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private Button applyButton;

    public FilterDialog(Widget parent, FilterPanel filterPanel) {
        super(parent, ButtonType.OK_CANCEL_HELP, "Filter Dialog", "results.filter");
        this.filterPanel = filterPanel;
        applyButton = getButton(BaseDialog.ButtonID.OK);
        applyButton.setText("Apply");
        applyButton.setTitle("Apply the filter(s) from the list to the table results");
        setWidget(filterPanel);
        filterPanel.addPropertyChangeListener(FilterPanel.SEL_UPDATED,
                         new PropertyChangeListener(){
                             public void propertyChange(PropertyChangeEvent pce) {
                                 applyButton.setEnabled(true);
                             }
                         });
    }

    public FilterPanel getFilterPanel() {
        return filterPanel;
    }

    public void setApplyListener( GeneralCommand applyCmd) {
        this.applyCmd = applyCmd;
    }

    

    @Override
    protected void onVisible() {
        applyButton.setEnabled(false);
        filterPanel.init();
        pcs.firePropertyChange(SHOW, false, true);
    }

    protected void inputComplete() {
        if (applyCmd != null) {
            applyCmd.execute();
        }
    }

//====================================================================
//  PropertyChange aware
//====================================================================

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

    public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(propName, pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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