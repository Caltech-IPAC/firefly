package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 8/21/14
 * Time: 9:20 AM
 */


import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.FinderChartRequestUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.ImageSet;

import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class FinderChartGridRenderer implements GridRenderer {

    private FlexTable grid= new FlexTable();
    private ScrollPanel scroll= new ScrollPanel(grid);
    private List<String> showMask= null;
    private Map<String,MiniPlotWidget> mpwMap= null;
    private List<XYPlotWidget> xyList= null;
    private Dimension dimension= null;

    int dssRow= -1;
    int sdssRow= -1;
    int massRow= -1;
    int wiseRow= -1;
    int irasRow= -1;



    public  void reinitGrid(Map<String,MiniPlotWidget> mpwMap, List<XYPlotWidget> xyList) {
        this.mpwMap= mpwMap;
        grid.clear();
        grid.removeAllRows();
        if (mpwMap==null && xyList==null) return;
        int mpwSize= showMask==null ? mpwMap.size() : showMask.size();
        int size = mpwSize + xyList.size();



        int dssCnt= 0;
        int sdssCnt= 0;
        int massCnt= 0;
        int wiseCnt= 0;
        int irasCnt= 0;


        for(String key : mpwMap.keySet()) {
            if      (key.startsWith("DSS")) dssCnt++;
            else if (key.startsWith("SDSS")) sdssCnt++;
            else if (key.startsWith("TWOMASS")) massCnt++;
            else if (key.startsWith("WISE"))  wiseCnt++;
            else if (key.startsWith("IRAS")) irasCnt++;
        }

        dssRow=  (dssCnt>0) ? addMissionRow(ImageSet.DSS.title) : -1;
        sdssRow= (sdssCnt>0) ? addMissionRow(ImageSet.SDSS.title) : -1;
        massRow= (massCnt>0) ? addMissionRow(ImageSet.TWOMASS.title) : -1;
        wiseRow= (wiseCnt>0) ? addMissionRow(ImageSet.WISE.title) : -1;
        irasRow= (irasCnt>0) ? addMissionRow(ImageSet.IRIS.title) : -1;


        int dssCol= 0;
        int sdssCol= 0;
        int massCol= 0;
        int wiseCol= 0;
        int irasCol= 0;



        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
            String key= entry.getKey();
            MiniPlotWidget mpw= entry.getValue();
            if      (key.startsWith("DSS"))  grid.setWidget(dssRow,dssCol++,mpw);
            else if (key.startsWith("SDSS")) grid.setWidget(sdssRow,sdssCol++,mpw);
            else if (key.startsWith("TWOMASS")) grid.setWidget(massRow,massCol++,mpw);
            else if (key.startsWith("WISE"))  grid.setWidget(wiseRow,wiseCol++,mpw);
            else if (key.startsWith("IRAS")) grid.setWidget(irasRow,irasCol++,mpw);

            if (dimension!=null) {
                mpw.setSize(dimension.getWidth()+"px", dimension.getHeight()+"px");
            }
            else {
                mpw.setSize("192px","192px");
            }
        }


        grid.setCellPadding(2);


        AllPlots.getInstance().updateUISelectedLook();

    }

    public Element getMaskingElement(String key) {
        Element retval= null;
        int row= -1;
        if      (key.toUpperCase().startsWith("DSS"))     row= dssRow;
        else if (key.toUpperCase().startsWith("SDSS"))    row= sdssRow;
        else if (key.toUpperCase().startsWith("TWOMASS")) row= massRow;
        else if (key.toUpperCase().startsWith("2MASS"))   row= massRow;
        else if (key.toUpperCase().startsWith("WISE"))    row= wiseRow;
        else if (key.toUpperCase().startsWith("IRAS"))    row= irasRow;

        if (row>-1) {
            retval= grid.getRowFormatter().getElement(row);
        }

        return retval;
    }

    public void postPlotting() {
        boolean hasDss= false;
        boolean hasSdss= false;
        boolean has2mass= false;
        boolean hasWise= false;
        boolean hasIras= false;

        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
            String key= entry.getKey();
            MiniPlotWidget mpw= entry.getValue();
            if (mpw.getCurrentPlot()!=null) {
                if (key.startsWith("DSS"))  hasDss= true;
                if (key.startsWith("SDSS")) hasSdss= true;
                if (key.startsWith("TWOMASS")) has2mass= true;
                if (key.startsWith("WISE"))  hasWise= true;
                if (key.startsWith("IRAS"))  hasIras= true;
            }
        }

        if (!hasDss) clearRow(dssRow, "DSS", true);
        if (!hasSdss) clearRow(sdssRow, "SDSS", true);
        if (!has2mass) clearRow(massRow, "2MASS", true);
        if (!hasWise) clearRow(wiseRow, "WISE", true);
        if (!hasIras) clearRow(irasRow, "IRAS", true);
    }

    public void clearRow(int row, String rowDesc, boolean wasQueried) {
        if (row==-1) return;
        int colCnt= grid.getCellCount(row);
        for(int i=0; (i<colCnt); i++) {
            grid.clearCell(row,i);
        }
        if (wasQueried) {
            grid.getFlexCellFormatter().setColSpan(row,0,5);
            HTML html= new HTML("No data available for "+ rowDesc);
            grid.setWidget(row,0,html);
        }
    }

    public void setDimension(Dimension dim) {
       this.dimension= dim;
    }

    public void clear() {
        grid.clear();
    }

    public void onResize() {
    }

    public Widget getWidget() {
        return scroll;
    }

    public void setShowMask(List<String> showMask) {
        if (this.showMask==null || !this.showMask.equals(showMask)) {
            this.showMask= showMask;
            reinitGrid(mpwMap,xyList);
        }
    }


    private int addMissionRow(String title) {
        int cRow= grid.insertRow(grid.getRowCount());
        grid.getFlexCellFormatter().setColSpan(cRow,0,5);
        HTML html= new HTML(title);
        html.setStyleName("finderChartRenderTitle");
        grid.setWidget(cRow,0,html );
        return grid.insertRow(grid.getRowCount());
    }



}

