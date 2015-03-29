/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.WebPlot;


public abstract class BaseVisCmd extends GeneralCommand {
    private final WebPlotView plotView;

    public BaseVisCmd(String commandName, WebPlotView plotView) {
        super(commandName);
        this.plotView = plotView;
        plotViewListener();
        setEnabled(computeEnabled());
    }

    public WebPlotView getPlotView() { return plotView; }


    protected boolean computeEnabled() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean retval= false;
        if (plot!=null) {
            retval= true;
        }
        return retval;
    }

    private void plotViewListener() {
        plotView.addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                setEnabled(computeEnabled());
            }
        });
    }

    @Override
    public boolean hasIcon() { return true; }
}