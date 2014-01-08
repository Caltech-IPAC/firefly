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
