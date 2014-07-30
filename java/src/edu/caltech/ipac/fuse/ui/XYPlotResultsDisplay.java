package edu.caltech.ipac.fuse.ui;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.HashMap;

/**
 * Date: 7/1/14
 *
 * @author loi
 * @version $Id: $
 */
public class XYPlotResultsDisplay extends BaseLayoutElement {



    public XYPlotResultsDisplay() {


        XYPlotMeta meta = new XYPlotMeta("none", 0, 0, new CustomMetaSource(new HashMap<String, String>()));
        final XYPlotWidget xyPlotWidget = new XYPlotWidget(meta);
        xyPlotWidget.setTitleAreaAlwaysHidden(true);
        setContent(xyPlotWidget);

        Application.getInstance().getEventHub().getEventManager().addListener(EventHub.ON_TABLE_SHOW, new WebEventListener() {
                        public void eventNotify(WebEvent ev) {
                            final TablePanel table = Application.getInstance().getEventHub().getActiveTable();

                            table.getDataModel().getData(new AsyncCallback<TableDataView>() {
                                public void onFailure(Throwable throwable) {
                                    //TODO: something on error
                                    xyPlotWidget.removeCurrentChart();
                                    Window.alert("Unable to get table data: " + throwable.getMessage());
                                }

                                public void onSuccess(TableDataView tableDataView) {
                                    xyPlotWidget.makeNewChart(table.getDataModel(), null);
                                }
                            }, 0);
                        }
                    });
    }

//====================================================================
//
//====================================================================

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
