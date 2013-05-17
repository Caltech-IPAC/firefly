package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * User: balandra, Trey
 */
public class FitsHeaderDialog extends BaseDialog implements WebEventListener{

    private static final WebClassProperties _prop= new WebClassProperties(FitsHeaderDialog.class);
    private static final NumberFormat _nfPix   = NumberFormat.getFormat("#.##");

    private final FitsHeaderPanel _panel = new FitsHeaderPanel();
    private final MiniPlotWidget _mpw;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public FitsHeaderDialog (MiniPlotWidget mpw) {
        super(mpw.getPlotView(), ButtonType.REMOVE, _prop.getTitle() + " " + mpw.getTitle(),
              "visualization.fitsViewer");
        getButton(BaseDialog.ButtonID.REMOVE).setText("Close");
        _mpw= mpw;
        _mpw.getPlotView().addListener(Name.REPLOT, this);
        _mpw.getOps().getFitsHeaderInfo(this);
    }

//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        ReplotDetails details= (ReplotDetails)ev.getData();
        ReplotDetails.Reason reason= details.getReplotReason();
        if (reason!=ReplotDetails.Reason.ZOOM && reason!=ReplotDetails.Reason.ZOOM_COMPLETED) {
            this.setVisible(false);
            _panel.clear();
            _mpw.getPlotView().removeListener(this);
        }
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        if (v)  setWidget(_panel);
        super.setVisible(v, PopupPane.Align.TOP_LEFT_POPUP_RIGHT, -25, 0);
        if (!v) {
            _mpw.getPlotView().removeListener(this);
        }
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public void createContents(BandInfo info) {
        this.setDefaultContentSize(410,425);
        HashMap<Band, RawDataSet> rawDataMap = info.getRawDataMap();
        WebPlot plot= _mpw.getCurrentPlot();

        if(plot.getPlotState().isThreeColor()){
            TabPane<VerticalPanel> tab = new TabPane<VerticalPanel>();
            for(Band band : plot.getPlotState().getBands()){
                DataSet data = DataSetParser.parse(rawDataMap.get(band));

                VerticalPanel vp= loadTable(data, plot.getImagePixelScaleInArcSec(),
                                            plot.getFitsData(band).getFitsFileSize());
                tab.addTab(vp, band.name());
            }
            tab.setSize("410px", "425px");
            _panel.add(tab);
        } else {
            DataSet data = DataSetParser.parse(rawDataMap.get(Band.NO_BAND));
            loadTableIntoPanel(data, plot.getImagePixelScaleInArcSec(),
                               plot.getFitsData(Band.NO_BAND).getFitsFileSize());
        }

    }

    private void loadTableIntoPanel(DataSet ds, double pixScale, long fileSize){
        //set column widths
        List<BaseTableData.RowData> rows = ds.getModel().getRows();
        int rowLength = 170;
        int dataLength;
        String rowValue;
        for(BaseTableData.RowData row : rows){
            rowValue = row.getValue(3);
            dataLength = rowValue.length()*6;
            //set column width to size of text
            if(dataLength > rowLength){
                rowLength = dataLength;
            }
        }

        TableDataView.Column c = ds.getColumn(0);
        c.setWidth(25);
        c = ds.getColumn(1);
        c.setWidth(75);
        c = ds.getColumn(2);
        c.setWidth(100);
        c = ds.getColumn(3);
        c.setWidth(rowLength);

        BasicTable table = new BasicTable(ds);
        table.setSize("400px", "375px");
        
//        String[] str = values.split(";");
        Grid grid = new Grid (1,2);
        HTMLTable.ColumnFormatter colF = grid.getColumnFormatter();
        colF.setWidth(0, "200px");
        grid.setHTML(0,0,"<b>Pixel Size:</b> " + _nfPix.format(pixScale)+ "''");
        grid.setHTML(0,1,"<b>File Size:</b> " + StringUtils.getSizeAsString(fileSize));
               
        _panel.add(grid);
        _panel.add(table);
    }

    private VerticalPanel loadTable(DataSet ds, double pixScale, long fileSize){
        VerticalPanel vp = new FitsHeaderPanel();

        //set column widths
        List<BaseTableData.RowData> rows = ds.getModel().getRows();
        int rowLength = 170;
        int dataLength;
        String rowValue;
        for(BaseTableData.RowData row : rows){
            rowValue = row.getValue(3);
            dataLength = rowValue.length()*6;
            //set column width to size of text
            if(dataLength > rowLength){
                rowLength = dataLength;
            }
        }

        TableDataView.Column c = ds.getColumn(0);
        c.setWidth(25);
        c = ds.getColumn(1);
        c.setWidth(75);
        c = ds.getColumn(2);
        c.setWidth(100);
        c = ds.getColumn(3);
        c.setWidth(rowLength);

        BasicTable table = new BasicTable(ds);
        table.setSize("400px", "375px");
        Grid grid = new Grid (1,2);
        HTMLTable.ColumnFormatter colF = grid.getColumnFormatter();
        colF.setWidth(0, "200px");
        grid.setHTML(0,0,"<b>Pixel Size:</b> " + _nfPix.format(pixScale)+ "''");
        grid.setHTML(0,1,"<b>File Size:</b> " + StringUtils.getSizeAsString(fileSize));

        vp.add(grid);
        vp.add(table);

        return vp;
    }

    private static class FitsHeaderPanel extends VerticalPanel implements RequiresResize {
        public void onResize() {
            String height = this.getParent().getOffsetHeight()+"px";
            String width = this.getParent().getOffsetWidth()+"px";
            this.setSize(width, height);
            for (Widget w: this.getChildren()) {
                if (w!=null && w instanceof BasicTable) {
                    resizeTable((BasicTable) w, getParent().getOffsetWidth()-10,getParent().getOffsetHeight());
                    break;
                } else if (w!=null && w instanceof TabPane) {
                    Widget t;
                    w.setSize(width, height);                    
                    for (int i=0; i<((TabPane) w).getWidgetCount();i++) {
                        t= ((FitsHeaderPanel)(((TabPane)w).getVisibleTab(i).getWidget(0))).getWidget(1);
                        resizeTable((BasicTable)t, getParent().getOffsetWidth()-10, getParent().getOffsetHeight());
                    }
                    break;
                }
            }
        }
        private void resizeTable(BasicTable t, int width, int height) {
            int colCount= t.getDataTable().getColumnCount();
            int nonCommentColsWidth = 0;
            int newColWidth;
            if (colCount > 1) {
                for (int i=0; i< colCount-1;i++) {
                    nonCommentColsWidth += t.getColumnWidth(i);
                }
                newColWidth = width - nonCommentColsWidth;
                if (newColWidth > 0) {
                    t.setColumnWidth(colCount-1, newColWidth);
                }
            } else {
                t.setColumnWidth(0, this.getParent().getOffsetWidth());
            }
            t.setSize(width+"px", height+"px");
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
