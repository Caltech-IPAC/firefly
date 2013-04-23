package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.WebPlot;


public abstract class BaseVisCmd extends GeneralCommand {
    private final WebPlotView _plotView;

    public BaseVisCmd(String commandName, WebPlotView plotView) {
        super(commandName);
        _plotView= plotView;
        plotViewListner();
        setEnabled(computeEnabled());
    }


    public WebPlotView getPlotView() { return _plotView; }


    protected boolean computeEnabled() {
        //return (_plotView.size() > 0);
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean retval= false;
        if (plot!=null) {
            retval= true;
        }
        return retval;
    }

    private void plotViewListner() {
        _plotView.addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                setEnabled(computeEnabled());
            }
        });
    }
}