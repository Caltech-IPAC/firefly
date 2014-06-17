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

    private Set<BackgroundUIType> _supported= new HashSet<BackgroundUIType>();

    private ActivationFactory() {
        _supported.addAll(Arrays.asList(BackgroundUIType.ZIP, BackgroundUIType.CATALOG, BackgroundUIType.QUERY));
    }


    public static ActivationFactory getInstance() {
        if (_instance==null) {
            _instance= new ActivationFactory();
        }
        return _instance;
    }

    public Widget buildActivationUI(MonitorItem monItem, int idx) {
        Widget retval= null;
        BackgroundActivation a= createActivation(monItem.getBackgroundUIType());
        if (a!=null) retval= a.buildActivationUI(monItem,idx, monItem.isActivated(idx));
        return retval;
    }

    public void activate(MonitorItem monItem) { activate(monItem,0,false); }

    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
        BackgroundActivation a= createActivation(monItem.getBackgroundUIType());
        if (a!=null) a.activate(monItem,idx,byAutoActivation);
    }

    public String getWaitingMsg(BackgroundUIType type) {
        String retval= "Working...";
        switch (type) {

            case ZIP:
                retval= "Computing number of packages...";
                break;
            case CATALOG:
                retval= "Retrieving Catalog...";
                break;
            case QUERY:
                retval= "Waiting...";
                break;
            case SERVER_TASK:
                break;
        }
        return retval;
    }

    public BackgroundActivation createActivation(BackgroundUIType type) {
        BackgroundActivation retval= null;
        switch (type) {

            case ZIP:
                retval= new ZipPackageDownload ();
                break;
            case CATALOG:
                retval= new CatalogDataSetActivation();
                break;
            case QUERY:
                retval= new SearchActivation();
                break;
            case SERVER_TASK:
                break;
        }
        return retval;
    }

    public boolean isSupported(BackgroundUIType type)  { return type!=null && _supported.contains(type);  }

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
