/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.ui.DisableableDeckPanel;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Mar 23, 2010
 * Time: 12:33:41 PM
 */


/**
 * @author Trey Roby
 */

public class DisableablePlotDeckPanel extends DisableableDeckPanel {
    private final MiniPlotWidget _mpw;
    private final int _plotIdx;
    private final boolean _previousOnError;


    public DisableablePlotDeckPanel(String defMessage, MiniPlotWidget mpw, boolean previousOnError) {
        super(defMessage);
        _mpw= mpw;
        _previousOnError= previousOnError;
        _mpw.setErrorDisplayHandler(new MiniPlotWidget.PlotError() {
            public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {
                if (StringUtils.isEmpty(briefDesc)) {
                    showNoData();
                }
                else {
                    String sep = getDefaultMsg().trim().endsWith(".") ? "" : ".";
                    showNoData(getDefaultMsg() +sep+"<br>"+ briefDesc);
                }
                if (_previousOnError) noDataMessageTimer.schedule(3000);
            }
        });

        add(_mpw);
        _plotIdx = getWidgetIndex(_mpw);
    }


    public MiniPlotWidget getMPW() { return _mpw; }

    public void showPlot() {
        showWidget(_plotIdx);
    }

    public boolean isPlotShowing() {
        return getVisibleWidget()==_plotIdx;
    }

    private Timer noDataMessageTimer = new Timer() {
        public void run() {
            showPlot();
        }
    };

}
