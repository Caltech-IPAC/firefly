package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.dd.ValidationException;
/**
 * User: roby
 * Date: Feb 11, 2009
 * Time: 11:36:20 AM
 */


/**
 * @author Trey Roby
*/
public abstract class PlotTypeUI {
    private final boolean _usesTarget;
    private final boolean _usesRadius;
    private final boolean _handlesSubmit;
    private final boolean _threeColor;
    public PlotTypeUI(boolean usesTarget,
                    boolean usesRadius,
                    boolean handlesSubmit,
                    boolean threeColor) {
        _usesTarget= usesTarget;
        _usesRadius= usesRadius;
        _handlesSubmit= handlesSubmit;
        _threeColor= threeColor;
    }

    public final boolean usesTarget() { return _usesTarget; }
    public final boolean usesRadius() { return _usesRadius; }
    public final boolean handlesSubmit() { return _handlesSubmit; }
    public final boolean delayHide() { return _handlesSubmit; }
    public       boolean isThreeColor() { return _threeColor; }
    public void submit(PlotWidgetOps ops) { }
    public Widget getAlternateRadiusWidget() { return null;  }
    protected boolean validateInput() throws ValidationException {
        return true;
    }
    public int getHeight() { return 90; }
    public abstract void addTab(TabPane<Panel> tabs);
    public abstract WebPlotRequest createRequest();

    /**
     * if isthreeColor() returns true then this method
     * must returnn a array of three WebPlotRequest, one of the three elements
     * must be non-null
     * @return a array of 3 WebPlotRequest
     */
    public WebPlotRequest[] createThreeColorRequest() { return null; }
    public abstract void updateSizeArea();
    public abstract String getDesc();
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
