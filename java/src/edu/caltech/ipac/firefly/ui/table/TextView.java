package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.Style;
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
        textView.getElement().getStyle().setMargin(5, Style.Unit.PX);
        GwtUtil.setStyles(textView, "margin", "1em 0px",
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
            String val =  "<pre>" + tableDataToString(curPage) + "</pre>";
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
