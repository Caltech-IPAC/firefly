/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.background.CatalogDataSetActivation;
import edu.caltech.ipac.firefly.ui.background.SearchActivation;
import edu.caltech.ipac.firefly.ui.background.ZipPackageDownload;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/**
 * User: roby
 * Date: Aug 31, 2010
 * Time: 12:32:53 PM
 */


/**
 * @author Trey Roby
 */
public class ActivationFactory {

    private static ActivationFactory _instance= null;

    private Set<BackgroundUIHint> _supported= new HashSet<BackgroundUIHint>();

    private ActivationFactory() {
        _supported.addAll(Arrays.asList(BackgroundUIHint.ZIP, BackgroundUIHint.CATALOG, BackgroundUIHint.QUERY));
    }


    public static ActivationFactory getInstance() {
        if (_instance==null) {
            _instance= new ActivationFactory();
        }
        return _instance;
    }

    public Widget buildActivationUI(MonitorItem monItem, int idx) {
        Widget retval= null;
        BackgroundActivation a= createActivation(monItem.getUIHint());
        if (a!=null) retval= a.buildActivationUI(monItem,idx, monItem.isActivated(idx));
        return retval;
    }

    public void activate(MonitorItem monItem) { activate(monItem,0,false); }

    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
        BackgroundActivation a= createActivation(monItem.getUIHint());
        if (a!=null) a.activate(monItem,idx,byAutoActivation);
    }

    public String getWaitingMsg(BackgroundUIHint type) {
        String retval= "Working...";
        switch (type) {

            case ZIP:
                retval= "Computing number of packages...";
                break;
            case CATALOG:
            case RAW_DATA_SET:
                retval= "Retrieving Catalog...";
                break;
            case QUERY:
                retval= "Waiting...";
                break;
        }
        return retval;
    }

    public BackgroundActivation createActivation(BackgroundUIHint type) {
        BackgroundActivation retval= null;
        switch (type) {

            case ZIP:
                retval= new ZipPackageDownload ();
                break;
            case CATALOG:
            case RAW_DATA_SET:
                retval= new CatalogDataSetActivation();
                break;
            case QUERY:
                retval= new SearchActivation();
                break;
        }
        return retval;
    }

    public boolean isSupported(BackgroundUIHint type)  { return type!=null && _supported.contains(type);  }

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}

