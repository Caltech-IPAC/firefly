/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 8/25/14
 * Time: 11:52 AM
 */


import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author Trey Roby
 */
public interface ImageSelectAccess {

    public int getPlotWidgetWidth();
    public WorldPt getJ2000Pos();
    public float getStandardPanelDegreeValue();
    public void updateSizeIfChange(double minDeg,double maxDeg,double defDeg);
    public Widget getMainPanel();
    public void hide();
    public void plot(PlotWidgetOps ops, PlotTypeUI ptype);

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

