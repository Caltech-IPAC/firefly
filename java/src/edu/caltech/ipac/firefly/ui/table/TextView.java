package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Dec 19, 2011
 *
 * @author loi
 * @version $Id: TextView.java,v 1.6 2012/01/25 00:35:51 loi Exp $
 */
public class TextView implements TablePanel.View {

    public static final Name NAME = new Name("Text View",
                                                        "Display the table's content as text");
    private TablePanel tablePanel = null;
    private HTML textView;
    private ScrollPanel textViewHolder;
    private TablePanel.View cview;
    private boolean isHidden = true;

    public TextView() {
        textView = new HTML();
        textViewHolder = new ScrollPanel(textView);
        GwtUtil.setStyle(textViewHolder, "borderTop", "1px solid gray");
        GwtUtil.setStyles(textView, "margin", "5px 1em",
                                    "display", "block",
                                    "fontFamily", "monospace",
                                    "whiteSpace", "pre");
    }

    public int getViewIdx() {
        return 10;
    }

    public Name getName() {
        return NAME;
    }

    public String getShortDesc() {
        return NAME.getDesc();
    }

    public Widget getDisplay() {
        return textViewHolder;

    }

    public void onViewChange(TablePanel.View newView) {
        cview = newView;
        if (newView == this) {
            loadTextView();
        }
    }

    public TablePanel getTablePanel() {
        return tablePanel;
    }

    public void onMaximize() {
    }

    public void onMinimize() {
    }

    public ImageResource getSelectedIcon() {
        return null;
    }

    public ImageResource getIcon() {
        return null;
    }

    public void bind(TablePanel table) {
        this.tablePanel = table;
        tablePanel.getEventManager().addListener(TablePanel.ON_PAGE_LOAD, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        if (cview == TextView.this) {
                            loadTextView();
                        }
                    }
                });
    }

    public void bind(EventHub hub) {
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean flg) {
        isHidden = flg;
    }

    protected void loadTextView() {

        DataSet curPage = tablePanel.getDataset().subset(0, 0);
        int end = tablePanel.getTable().getAbsoluteLastRowIndex() - (tablePanel.getTable().getCurrentPage() * tablePanel.getTable().getPageSize()) + 1;

        if (tablePanel.getTable().getRowCount() >= end) {
            for(int i = 0; i < end; i++) {
                curPage.getModel().addRow(tablePanel.getTable().getRowValue(i));
            }
            String val =  tableDataToString(curPage);
            textView.setHTML(val);
        }
    }

    private static String tableDataToString(TableDataView view) {
        StringBuffer sb = new StringBuffer();

        // create headers
        String sep = "";
        for (TableDataView.Column c : view.getColumns()) {
            if (c.isVisible()) {
                if (sep.length() > 0) {
                    sb.append("  ");
                    sep += "  ";
                }
                int w = Math.max(c.getWidth(), c.getTitle().length());
                sb.append(StringUtils.pad(w, c.getTitle()));
                sep += StringUtils.pad(w, "", StringUtils.Align.LEFT, '-');
            }
        }
        sb.append("\n").append(sep).append("\n");

        for(int r = 0; r < view.getModel().size(); r ++) {
            TableData.Row row = view.getModel().getRow(r);
            boolean firstLine = true;
            for (TableDataView.Column c : view.getColumns()) {
                if (c.isVisible()) {
                    sb.append( firstLine ? "" : "  " );
                    int w = Math.max(c.getWidth(), c.getTitle().length());
                    String txt = String.valueOf(row.getValue(c.getName()));
                    sb.append(escape(StringUtils.pad(w,txt, getAlign(c))));
                    firstLine = false;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String escape(String s) {
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    private static StringUtils.Align getAlign(TableDataView.Column col) {

        // hard-code to left-align string field.  left align looks better.
        String type = col.getType() == null ? "" : col.getType().toLowerCase();
        if (type.startsWith("c")) return StringUtils.Align.LEFT;

        Object align = col.getAlign();
        if (align == TableDataView.Align.RIGHT) {
            return StringUtils.Align.RIGHT;
        } else if (align == TableDataView.Align.CENTER) {
            return StringUtils.Align.MIDDLE;
        } else {
            return StringUtils.Align.LEFT;
        }

    }

}
