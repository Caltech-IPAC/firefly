package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Sep 16, 2010
 *
 * @author loi
 * @version $Id: SearchSummaryPanel.java,v 1.7 2012/10/04 23:30:27 loi Exp $
 */
public class SearchSummaryPanel extends Composite implements TablePreview {

    private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.PX);
    private ArrayList<SearchSummaryItem> searchItems = new ArrayList<SearchSummaryItem>();
    private FlexTable table;
    private String name;
    private String shortDesc;
    private String id= null;
    private EventHub hub;
    private int iconColIdx = 0;
    private List<TableDataView.Column> headers = new ArrayList<TableDataView.Column>();
    private String curGroupByName;
    private List<String> groupByCols;
    private List<Timer> bgList = new ArrayList<Timer>();
    private String helpId = null;

    public SearchSummaryPanel() {
        this(null, null);
    }

    public SearchSummaryPanel(String name, String shortDesc) {
        setName(name);
        setShortDesc(shortDesc);
        initWidget(mainPanel);
        GwtUtil.setStyle(this, "marginTop", "10px");
    }

    public EventHub getHub() {
        return hub;
    }

    public void setHelpId(String helpId) {
        this.helpId = helpId;
    }

    public boolean hasGroupBy() {
        return groupByCols != null && groupByCols.size() > 0;
    }

    public void setGroupByCols(List<String> groupByCols) {
        this.groupByCols = groupByCols;
    }

    public String getCurGroupByName() {
        return curGroupByName;
    }

    public void setCurGroupByName(String curGroupByName) {
        this.curGroupByName = curGroupByName;
    }

    public void addItem(SearchSummaryItem item) {
        searchItems.add(item);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public void addHeaders(TableDataView.Column... headers) {
        if (headers == null) return;

        for(TableDataView.Column s : headers) {
            this.headers.add(s);
        }
    }

    public void bind(EventHub hub) {
        this.hub = hub;
        layout();
    }

    public int getPrefHeight() {
        return 0;
    }

    public int getPrefWidth() {
        return 0;
    }

    public Widget getDisplay() {
        return this;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public String getName() {
        return name;
    }

    public void onShow() {
    }

    public void onHide() {
    }


    public void setPreviewVisible(boolean v) {
        Widget w= getDisplay();
        if (w!=null && v!=w.isVisible()) {
            w.setVisible(v);
            if (v) onShow();
            else onShow();
        }
    }


    public void setID(String id) {
        this.id= id;
    }

    public String getID() {
        return id;
    }

    public void layout() {

        // clear any backgrounded processes.
        for(Timer t : bgList) {
            t.cancel();
        }

        table = new FlexTable();
        mainPanel.clear();
        if (!StringUtils.isEmpty(name) || !StringUtils.isEmpty(helpId)) {

            String n = StringUtils.isEmpty(name) ? "" : name.trim();
            HorizontalPanel h = new HorizontalPanel();
            h.setWidth("100%");
            HTML lname = new HTML("<b>" + n + "</b>");
            if (shortDesc != null) {
                lname.setTitle(shortDesc);
            }
            GwtUtil.setStyles(lname, "textAlign", "center");
            h.add(lname);
            h.setCellWidth(lname, "100%");
            
            if (!StringUtils.isEmpty(helpId)) {
                final Widget helpIcon = HelpManager.makeHelpIcon(helpId);
                h.add(helpIcon);
                GwtUtil.setStyles(helpIcon, "marginRight", "11px");
            }
            mainPanel.addNorth(h, 20);
        }

        // setup group by selection
        if (groupByCols != null && groupByCols.size() > 1) {
            EnumFieldDef gb = new EnumFieldDef("groupBy");
            gb.setLabel("Group By");
            gb.setDesc("Select a group by column to update the data table");
            gb.setPreferWidth(200);
            gb.setDefaultValue(curGroupByName);
            for (TableDataView.Column item : headers) {
                if (groupByCols.contains(item.getName())){
                    gb.addItem(item.getName(), item.getTitle());
                }
            }
            final SimpleInputField sif = SimpleInputField.createByDef(gb);
            mainPanel.addNorth(sif, 28);
            sif.getField().addValueChangeHandler(new ValueChangeHandler(){
                public void onValueChange(ValueChangeEvent ve) {
                    curGroupByName = sif.getValue();
                    layout();
                }
            });
        }

        ScrollPanel sp = new ScrollPanel();
        sp.add(table);

        mainPanel.add(sp);

        table.setStyleName("firefly-summary-table");
        table.setSize("100%", "100%");
        iconColIdx = headers.size();
        String titleCol = null;

        // render headers
        int colIdx = 0;
        for (int i = 0; i < headers.size(); i++) {
            TableDataView.Column col = headers.get(i);
            if (curGroupByName == null || !curGroupByName.equals(col.getName())) {
                table.setText(0, colIdx, col.getTitle());
                table.getCellFormatter().setStyleName(0, colIdx, "title-bar");
                colIdx++;
                if (titleCol == null) {
                    titleCol = col.getName();
                }
            }
        }
        table.setText(0, headers.size(), "");
        table.getCellFormatter().setWidth(0, headers.size(), "100%");

        ArrayList<SearchSummaryItem> itemList = searchItems;

        if (!StringUtils.isEmpty(curGroupByName)) {
            itemList = new ArrayList<SearchSummaryItem>();

            GroupFinder finder = new GroupFinder("");
            List<GroupedSummaryItem> groupList = new ArrayList<GroupedSummaryItem>();
            for(int i = 0; i < searchItems.size(); i++) {
                SearchSummaryItem dsi = searchItems.get(i);
                String cGroupValue = dsi.getValue(curGroupByName);
                GroupedSummaryItem cGroup = CollectionUtil.findFirst(groupList, finder.setName(cGroupValue));
                if (cGroup == null) {
                    cGroup = new GroupedSummaryItem(cGroupValue);
                    groupList.add(cGroup);
                    itemList.add(cGroup);
                }
                cGroup.addChild(dsi);
            }
        }

        for(SearchSummaryItem ssi : itemList) {
            ssi.setTitleCol(titleCol);
            layout(ssi, 0);
        }
    }

    private void layout(final SearchSummaryItem ssi, final int depth) {

        final int row = table.getRowCount();
        final Image loading = new Image(GwtUtil.LOADING_ICON_URL);
        ssi.checkUpdate();

        table.setWidget(row, iconColIdx, loading);

        if (ssi.isLoaded()) {
            ssi.renderItem(table, row, curGroupByName);
            GwtUtil.setStyles(loading, "visibility", "hidden");
        } else {
            ssi.checkUpdate();
            table.setWidget(row, iconColIdx, loading);
            Timer timer = new Timer() {
                public void run() {
                    ssi.checkUpdate();
                    ssi.renderItem(table, row, curGroupByName);
                    if (ssi.isLoaded()) {
                        cancel();
                        GwtUtil.setStyles(loading, "visibility", "hidden");
                    }
                }
            };
            bgList.add(timer);
            timer.scheduleRepeating(1000);
        }

        if (ssi.getChildren() != null && ssi.getChildren().size() > 0) {
            for(SearchSummaryItem child : ssi.getChildren()) {
                layout(child, depth+1);
            }
        }
    }

    List<TableDataView.Column> getHeaders() {
        return headers;
    }

    int getCol(String header) {
        return headers.indexOf(header);
    }

    FlexTable getTable() {
        return table;
    }

//====================================================================
//
//====================================================================

    public static  SearchSummaryPanel createDataSetSummary(SearchSummaryPanel sumPanel, DataSet dataset, List<String> groupByColumns,
                                                           SearchSummaryItem.Activation activator) {

        SearchSummaryPanel ssp = sumPanel == null ? new SearchSummaryPanel() : sumPanel;
        boolean hasGroup = groupByColumns != null && groupByColumns.size() > 0;
        String cGCol = null;
        if (hasGroup) {
            cGCol = groupByColumns.get(0);
            ssp.setCurGroupByName(cGCol);
            ssp.setGroupByCols(groupByColumns);
        }

        //setup headers
        List<String> cols = new ArrayList<String>();
        for(TableDataView.Column c : dataset.getColumns()) {
            if (c.isVisible()) {
                ssp.addHeaders(c);
                cols.add(c.getName());
            }
        }

        for(int i = 0; i < dataset.getTotalRows(); i++) {
            TableData.Row r = dataset.getModel().getRow(i);
            SearchSummaryItem dsi = new DataSetSummaryItem(r, dataset.getColumns());
            if (activator != null) {
                dsi.setActivation(activator);
            }
            dsi.setColumns(cols);
            ssp.addItem(dsi);
        }
        return ssp;
    }

    static class GroupFinder extends CollectionUtil.FilterImpl<GroupedSummaryItem> {
        String name = "";

        GroupFinder(String name) {
            this.name = name;
        }

        public CollectionUtil.Filter<GroupedSummaryItem> setName(String name) {
            this.name = name;
            return this;
        }

        public boolean accept(GroupedSummaryItem gsi) {
            return gsi.getName().equals(name);
        }
    }

}
