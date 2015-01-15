/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

