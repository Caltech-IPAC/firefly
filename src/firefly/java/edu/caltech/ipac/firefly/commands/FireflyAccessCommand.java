/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;

public class FireflyAccessCommand extends RequestCmd {

    public static final String COMMAND = "FireflyAccessCommand";
    private MiniPlotWidget _plotWidget= null;

    public FireflyAccessCommand(String title, String desc) {
        super(COMMAND, title, desc, true);
    }




    public boolean init() {
//        _plotWidget= new MiniPlotWidget(MiniPlotWidget.PopoutType.STAND_ALONE);
//        _plotWidget.addStyleName("standard-border");
//        _main.add(_plotWidget);
//        _main.setSize("100%", "100%");
//
//        _main.addStyleName("fits-input-cmd-main-widget");
//        _plotWidget.setRemoveOldPlot(true);
//        _plotWidget.setMinSize(50, 50);
//        _plotWidget.setAutoTearDown(false);
//        Application.getInstance().getLayoutManager().getRegion(LayoutManager.CONTENT_REGION).setDisplay(_plotWidget);
//        FitsViewerJSInterface.enable(_plotWidget);
        return true;
    }

    protected void doExecute(Request req, AsyncCallback<String> callback) {


//        _plotWidget.getOps(new MiniPlotWidget.OpsAsync() {
//            public void ops(final PlotWidgetOps ops) {
//                _plotWidget.setShowScrollBars(false);
//            }
//        });
        callback.onSuccess("success!");
    }




}

