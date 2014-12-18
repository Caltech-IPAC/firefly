package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Nov 9, 2010
*
* @author loi
* @version $Id: SearchSummaryItem.java,v 1.3 2010/11/30 01:53:15 loi Exp $
*/
public class SearchSummaryItem {
    private String desc;
    private boolean isLoaded;
    private List<SearchSummaryItem> children = new ArrayList<SearchSummaryItem>();
    private Map<String, CellData> values = new HashMap<String, CellData>();
    private List<String> columns = new ArrayList<String>();
    private SearchSummaryItem parent = null;
    private Activation activation = null;
    private String titleCol;
    private boolean activatable = true;

    public SearchSummaryItem() {
    }

    public String getTitleCol() {
        return titleCol;
    }

    public void setTitleCol(String titleCol) {
        this.titleCol = titleCol;
        for (SearchSummaryItem i : children) {
            i.setTitleCol(titleCol);
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    void setActivatable(boolean activatable) {
        this.activatable = activatable;
    }

    public List<SearchSummaryItem> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(SearchSummaryItem child) {
        if (child != null) {
            children.add(child);
            child.setParent(this);
        }
    }

    public int getDepth() {
        if (parent == null) {
            return 0;
        } else {
            return 1 + parent.getDepth();
        }
    }

    public SearchSummaryItem getParent() {
        return parent;
    }

    public void setParent(SearchSummaryItem parent) {
        this.parent = parent;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean canActivate() {
        return activation != null && activatable;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns.clear();
        if (columns != null) {
            this.columns.addAll(columns);
        }
    }

    public void setActivation(Activation activation) {
        this.activation = activation;
    }

    /**
     * default rendering.  first column is trigger for activate.  the rest is html/text
     * @param table
     * @param row
     */
    public void renderItem(FlexTable table, int row, String... ignoreCols) {
        List<String> ignoreList = Arrays.asList(ignoreCols);

        int colIdx = titleCol == null ? 0 : 1;
        for(int i = 0; i < getColumns().size(); i++) {
            String col = getColumns().get(i);
            if (titleCol != null && titleCol.equals(col)) {
                int depth = getDepth();
                CellData cdata = getCellData(col);
                String v = cdata == null || cdata.value == null ? "" : cdata.value;
                HTML title = new HTML(v);
                Widget titleC;
                if (depth > 0) {
                    HorizontalPanel fp = new HorizontalPanel();
                    fp.add(GwtUtil.getFiller((depth * 15)-6, 1));
                    fp.add(title);
                    titleC = fp;
                } else {
                    titleC = title;
                }

                if (canActivate()) {
                    GwtUtil.makeIntoLinkButton(title);
                    title.addClickHandler(new ClickHandler(){
                        public void onClick(ClickEvent event) {
                            activation.activate(SearchSummaryItem.this);
                        }
                    });
                }

                title.setTitle(getDesc());
                table.setWidget(row, 0, titleC);
            } else {
                if (!ignoreList.contains(col)) {
                    CellData cdata = getCellData(col);
                    if (cdata != null) {
                        table.setHTML(row, colIdx, cdata.value);
                        if (cdata.halign != null) {
                            table.getCellFormatter().setHorizontalAlignment(row, colIdx, cdata.halign);
                        }
                        if (cdata.valign != null) {
                            table.getCellFormatter().setVerticalAlignment(row, colIdx, cdata.valign);
                        }
                        table.getCellFormatter().setWordWrap(row, colIdx, cdata.nowrap);
                        colIdx++;
                    } else {
                        System.out.println("blah");
                    }
                }
            }
        }
    }

    public String getValue(String colname) {
        CellData cd = getCellData(colname);
        return cd == null ? "" : cd.value;
    }

    public CellData getCellData(String colname) {
        return values.get(colname);

    }

    public CellData setValue(String colname, String value) {
        CellData data = values.get(colname);
        if (data == null) {
            data = new CellData(value);
            values.put(colname, data);
        }
        data.value = value;
        return data;
    }

    /**
     * if not loaded, checkUpdate() will get called until isLoaded() return true.
     */
    public void checkUpdate() {}

    public static class CellData {
        String value;
        HasHorizontalAlignment.HorizontalAlignmentConstant halign;
        HasVerticalAlignment.VerticalAlignmentConstant valign;
        boolean nowrap = true;

        public CellData(String value) {
            this(value, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_BOTTOM, true);
        }

        public CellData(String value, HasHorizontalAlignment.HorizontalAlignmentConstant halign) {
            this(value, halign, HasVerticalAlignment.ALIGN_BOTTOM, true);
        }

        public CellData(String value, HasHorizontalAlignment.HorizontalAlignmentConstant halign,
                        HasVerticalAlignment.VerticalAlignmentConstant valign, boolean nowrap) {
            this.value = value;
            this.halign = halign;
            this.valign = valign;
            this.nowrap = nowrap;
        }

        public void setHalign(HasHorizontalAlignment.HorizontalAlignmentConstant halign) {
            this.halign = halign;
        }

        public void setValign(HasVerticalAlignment.VerticalAlignmentConstant valign) {
            this.valign = valign;
        }

        public void setNowrap(boolean nowrap) {
            this.nowrap = nowrap;
        }
    }

    public static interface Activation {
        void activate(SearchSummaryItem ssi);
    }
}
