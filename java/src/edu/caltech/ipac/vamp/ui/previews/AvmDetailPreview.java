package edu.caltech.ipac.vamp.ui.previews;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
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
import edu.caltech.ipac.firefly.ui.TitlePanel;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.imageGrid.ImageGridPanel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.vamp.searches.SearchAvmInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 1, 2010
 * Time: 5:32:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AvmDetailPreview extends AbstractTablePreview {
    private static final String REQ_AVM_ID = "avm_id";
    private static final String REQ_PUBLISHER_ID = "publisher_id";

    private static final String sql_meta = "select * from avm_meta where " +
            "avm_id = ? and publisher_id = ?";
    private static final String sql_resources = "select distinct avm_resources.file_dimension, " +
            "avm_resources.file_size, avm_resources.file_type, avm_resources.resource_id, avm_resources.resource_url " +
            "from avm_resources,avm_meta where " +
            "avm_meta.avm_id = ? and avm_meta.publisher_id = ? and avm_resources.avm_meta_id=avm_meta.avm_meta_id";


    //Foreign keys: avm_id, publisher_id
    protected String _reqAvmId;
    protected String _reqPublisherId;
    protected String _cReqAvmId;
    protected String _cReqPublisherId;

    private VerticalPanel generalPanel = new VerticalPanel();
    private TabPane<VerticalPanel> tabs = new TabPane<VerticalPanel>();
    private VerticalPanel avmDescriptionPanel = new VerticalPanel();
    private VerticalPanel avmMetaPanel = new VerticalPanel();
    private VerticalPanel avmContactPanel = new VerticalPanel();
    private VerticalPanel avmSpatialPanel = new VerticalPanel();
    private VerticalPanel avmSpectralPanel = new VerticalPanel();
    private VerticalPanel display = new VerticalPanel();

    public AvmDetailPreview(String name) {
        this(name, REQ_AVM_ID, REQ_PUBLISHER_ID);
    }

    public AvmDetailPreview(String name, String reqAvmId, String reqPublisherId) {
        super(name, "Get additional information for the highlighted row");
        _reqAvmId = reqAvmId;
        _reqPublisherId = reqPublisherId;
        TitlePanel tableTitlePanel = new TitlePanel("AVM",tabs);

        tabs.setSize("100%", "100%");
/*        tabs.addTab(avmDescriptionPanel, "Description");
        tabs.addTab(avmMetaPanel, "MetaData");
        tabs.addTab(avmContactPanel, "Contact");
        tabs.addTab(avmSpatialPanel, "Spatial");
        tabs.addTab(avmSpectralPanel, "Spectral");*/
        tabs.selectTab(0);


        display.add(generalPanel);
        display.add(tableTitlePanel);
    }

    public void clear() {
        _cReqAvmId = "";
        _cReqPublisherId = "";
    }                                             

    protected void handleAvmTableLoad(TableData.Row selRow) {
        if (selRow == null) {return;}

        final String reqAvmIdF = String.valueOf(selRow.getValue(_reqAvmId));
        final String reqPublisherIdF = String.valueOf(selRow.getValue(_reqPublisherId));

        if (reqAvmIdF.equals(_cReqAvmId) && reqPublisherIdF.equals(_cReqPublisherId)) {
            return;  // request for the same info.. skip
        } else {
            _cReqAvmId = reqAvmIdF;
            _cReqPublisherId = reqPublisherIdF;
        }

        //todo clear tabPanel content

        ServerTask<RawDataSet> metaTask = new ServerTask<RawDataSet>(tabs, "Get AVM Meta", true) {
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
                //todo load data to detailPanel
                loadTable(info);
            }

            public void onFailure(Throwable caught) {
                //todo hide detailPanel and generalPanel
                //showTable(false);
                PopupUtil.showSevereError(caught);
            }

        };
        metaTask.start();

        ServerTask<RawDataSet> resourcesTask = new ServerTask<RawDataSet>(generalPanel, "Get AVM Resources", true) {
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
                generalPanel.clear();
                StringBuffer pd;
                if (dataset.getTotalRows() > 0) {
                    //todo show detailPanel
                    //GwtUtil.SplitPanel.showWidget(display, resourcesView);
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
                        generalPanel.add(widget);
                    }
                } else {
                    //todo hide detailPanel
                }
//                    loadTable(info);
            }

            public void onFailure(Throwable caught) {
                //hide detailPanel and generalPanel
                //showTable(false);
                PopupUtil.showSevereError(caught);
            }
        };
        resourcesTask.start();        
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

    protected void updateDisplay(TablePanel table) {

    }

    protected void updateDisplay(TableData.Row selRow) {
        /*if (!GwtUtil.isOnDisplay(display)) {
            //showTable(false);
            return;
        }   */

        try {
            doTableLoad(selRow);
        } catch (Exception e) {
            //loadTable(null);
        }
    }

    protected void doTableLoad(TableData.Row selRow) {
        handleAvmTableLoad(selRow);
    }

    @Override
    public Widget getDisplay() {
        return display;
    }

    private void loadTable(Map<String, String> info) {
        System.out.println("debug: loadTable();");
        if (avmDescriptionPanel!=null) {
            avmDescriptionPanel.clear();
            HTML headline = new HTML(info.get("headline"));

            HTML contents = new HTML(info.get("description"));
            ScrollPanel scroller = new ScrollPanel(contents);
            scroller.setAlwaysShowScrollBars(false);
            scroller.addStyleName("avm-details-panel");
            contents.addStyleName("avm-details-panel");
            avmDescriptionPanel.add(new HTML(info.get("title")));
            avmDescriptionPanel.add(new HTML(info.get("headline")));
            //descriptionPanel.add(new Label("Description"));
            avmDescriptionPanel.add(scroller);
            tabs.selectTab(0);
        }
        tabs.addTab(avmDescriptionPanel, "Description");

    }
}
