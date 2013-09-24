package edu.caltech.ipac.vamp.ui.previews;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.imageGrid.ImageGridPanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.RowDetailPreview;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.vamp.searches.SearchAvmInfo;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: tlau
 * Date: May 3, 2010
 * Time: 2:24:57 PM
 * @version $Id: MetaResourcePreview.java,v 1.13 2012/05/25 21:48:47 tatianag Exp $
 */
public class MetaResourcePreview extends RowDetailPreview {
    private static final String REQ_AVM_ID = "avm_id";
    private static final String REQ_PUBLISHER_ID = "publisher_id";
    private static final String sql_meta = "select * from avm_meta where " +
            "avm_id = ? and publisher_id = ?";
    private static final String sql_resources = "select distinct avm_resources.file_dimension, " +
            "avm_resources.file_size, avm_resources.file_type, avm_resources.resource_id, avm_resources.resource_url " +
            "from avm_resources,avm_meta where " +
            "avm_meta.avm_id = ? and avm_meta.publisher_id = ? and avm_resources.avm_meta_id=avm_meta.avm_meta_id";
    private SplitLayoutPanel display = new SplitLayoutPanel();
    private VerticalPanel resourcesDetails = new VerticalPanel();
    private ScrollPanel resourcesView;

    //Foreign keys: avm_id, publisher_id
    protected String _reqAvmId;
    protected String _reqPublisherId;
    protected String _cReqAvmId;
    protected String _cReqPublisherId;

    public MetaResourcePreview(String name) {
        this(name, REQ_AVM_ID, REQ_PUBLISHER_ID);
    }

    public MetaResourcePreview(String name, String reqAvmId, String reqPublisherId) {
        super(name);
        _reqAvmId = reqAvmId;
        _reqPublisherId = reqPublisherId;

        resourcesDetails.addStyleName("avm-details-panel");
        resourcesView = new ScrollPanel(resourcesDetails);

        display.addNorth(resourcesView, 100);
        Widget tableDisplay = super.getDisplay();
        display.add(tableDisplay);
        display.addStyleName("avm-details-panel");
        display.setSize("100%", "100%");
    }

    @Override
    public void clear() {
        _cReqAvmId = "";
        _cReqPublisherId = "";
    }

    protected void handleAvmTableLoad(TableData.Row selRow) {
        if (selRow != null) {
            final String reqAvmIdF = String.valueOf(selRow.getValue(_reqAvmId));
            final String reqPublisherIdF = String.valueOf(selRow.getValue(_reqPublisherId));

            if (reqAvmIdF.equals(_cReqAvmId) && reqPublisherIdF.equals(_cReqPublisherId)) {
                return;  // request for the same info.. skip
            } else {
                _cReqAvmId = reqAvmIdF;
                _cReqPublisherId = reqPublisherIdF;
            }

            ServerTask<RawDataSet> metaTask = new ServerTask<RawDataSet>(getView(), "Get AVM Meta", true) {
                public void doTask(AsyncCallback<RawDataSet> passAlong) {
                    SearchAvmInfo.Req request = new SearchAvmInfo.Req(_cReqAvmId, _cReqPublisherId, sql_meta);

                    SearchServices.App.getInstance().getRawDataSet(request, passAlong);
                }

                public void onSuccess(RawDataSet result) {
                    DataSet dataset = DataSetParser.parse(result);
                    Map<String, String> info = new LinkedHashMap<String, String>();
                    if (dataset.getTotalRows() > 0) {
                        for (TableDataView.Column c : dataset.getColumns()) {
                            if (c.isVisible()) {
                                info.put(c.getTitle(), String.valueOf(
                                            dataset.getModel().getRow(0).getValue(c.getName())));
                            }
                        }
                    }
                    loadTable(info);
                }

                public void onFailure(Throwable caught) {
                    showTable(false);
                    PopupUtil.showSevereError(caught);
                }

            };
            metaTask.start();

            ServerTask<RawDataSet> resourcesTask = new ServerTask<RawDataSet>(getView(), "Get AVM Resources", true) {
                public void doTask(AsyncCallback<RawDataSet> passAlong) {
                    SearchAvmInfo.Req request = new SearchAvmInfo.Req(_cReqAvmId, _cReqPublisherId, sql_resources);

                    SearchServices.App.getInstance().getRawDataSet(request, passAlong);
                }

                public void onSuccess(RawDataSet result) {
                    DataSet dataset = DataSetParser.parse(result);
                    Map<String, String> info = new LinkedHashMap<String, String>();
                    String title;
                    String url;
                    TableData.Row row;
                    Widget widget;
                    resourcesDetails.clear();
                    StringBuffer pd;
                    if (dataset.getTotalRows() > 0) {
                        GwtUtil.SplitPanel.showWidget(display, resourcesView);
                        for (Object o: dataset.getModel().getRows()) {
                            pd = new StringBuffer();
                            row = (TableData.Row)o;
                            info.clear();
                            for (TableDataView.Column c : dataset.getColumns()) {
                                if (c.isVisible()) {
                                    info.put(c.getTitle(), String.valueOf(
                                                row.getValue(c.getName())));
                                }
                            }
                            pd.append("Type: ");
                            pd.append(info.get("file_type"));
                            pd.append(" - Size: ");
                            pd.append(info.get("file_size"));
                            pd.append("KB - Dimension: ");
                            pd.append(info.get("file_dimension").replace(";"," x "));
                            title = pd.toString();

                            url = info.get("resource_URL");
                            if (url!=null) {
                                widget = new Hyperlink(title,url);
                            } else {
                                widget = new Label(title);
                            }
                            resourcesDetails.add(widget);
                        }
                    } else {
                        GwtUtil.SplitPanel.hideWidget(display, resourcesView);    
                    }
//                    loadTable(info);
                }

                public void onFailure(Throwable caught) {
                    showTable(false);
                    PopupUtil.showSevereError(caught);
                }
            };
            resourcesTask.start();
        }
    }

    @Override
    public void bind(EventHub hub) {
        WebEventListener wel =  new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TableData.Row selRow = null;
                    clear();
                    if (ev.getSource() instanceof ImageGridPanel) {
                        ImageGridPanel grid = (ImageGridPanel) ev.getSource();
                        if (grid != null && GwtUtil.isOnDisplay(grid)) {
                            TableDataView dv = grid.getDataModel().getCurrentData();
                            int idx = dv.getSelected().get(0);
                            selRow =dv.getModel().getRow(idx);
                        }
                    }
                    
                    if (selRow != null) updateDisplay(selRow);
                }
            };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }

    protected void updateDisplay(TableData.Row selRow) {
        if (!GwtUtil.isOnDisplay(display)) {
            showTable(false);
            return;
        }

        try {
            doTableLoad(selRow);
        } catch (Exception e) {
            loadTable(null);
        }
    }

    protected void doTableLoad(TableData.Row selRow) {
        handleAvmTableLoad(selRow);
    }

    @Override
    public Widget getDisplay() {
        return display;
    }
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
