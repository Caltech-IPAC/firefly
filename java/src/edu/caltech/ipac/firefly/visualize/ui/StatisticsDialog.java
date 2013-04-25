package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
//import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebDefaultMouseReadoutHandler;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;


/**
 * User: balandra
 * Date: Sep 2, 2009
 * Time: 9:31:59 AM
 */
public class StatisticsDialog extends BaseDialog implements WebEventListener {

    private static final WebClassProperties _prop= new WebClassProperties(StatisticsDialog.class);

    private SimplePanel panel = new SimplePanel();
    WebPlotView _wpv;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public StatisticsDialog (MiniPlotWidget mpw, BandInfo info) {
        super(mpw.getToplevelExpandRoot(), ButtonType.REMOVE, computeTitle(mpw), "visualization.fitsViewer");
        Button b = this.getButton(BaseDialog.ButtonID.REMOVE);
        b.setText("Close");
        _wpv = mpw.getPlotView();
        _wpv.addListener(this);
        createContents(info, _wpv);

    }

//======================================================================
//----------------------- Static Methods -------------------------------
//======================================================================


    public static void showStats(MiniPlotWidget mpw, BandInfo info) {
        new StatisticsDialog(mpw,info).setVisible(true);
    }

    private static String computeTitle(MiniPlotWidget mpw) {
        return _prop.getTitle() + ": " + VisUtil.getBestTitle(mpw.getCurrentPlot());
    }
  //======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        if (ev.getName().equals(Name.AREA_SELECTION)) {
             this.setVisible(false);
             _wpv.removeListener(this);
        }
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void setVisible(boolean v) {
        if (v) {
          setWidget(panel);
        }
        super.setVisible(v, PopupPane.Align.TOP_LEFT_POPUP_RIGHT, -25, 0);
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void createContents(BandInfo info, WebPlotView wpv) {
        HashMap<Band, HashMap<Metrics, Metric>> metricMap = info.getMetricsMap();
        HashMap<Band, String> stringMap = info.getStringMap();


        if(_wpv.getPrimaryPlot().getPlotState().isThreeColor()){
            TabPane<VerticalPanel> tab = new TabPane<VerticalPanel>();

            for(Band band :_wpv.getPrimaryPlot().getPlotState().getBands()){
                VerticalPanel vp = new VerticalPanel();

                HashMap<Metrics,Metric> metric = metricMap.get(band);
                String htmlString = stringMap.get(band);

                StatsGrid gridTop = new StatsGrid(metric);
                vp.add(gridTop);
                StatsGrid gridBottom = new StatsGrid(metric, htmlString, wpv);
                vp.add(gridBottom);

                tab.addTab(vp, band.name());
            }
            tab.setSize("410px", "185px");
            panel.add(tab);
        } else {
            VerticalPanel vp = new VerticalPanel();

            HashMap<Metrics,Metric> metric = metricMap.get(Band.NO_BAND);
            String htmlString = stringMap.get(Band.NO_BAND);

            StatsGrid gridTop = new StatsGrid(metric);
            vp.add(gridTop);
            StatsGrid gridBottom = new StatsGrid(metric, htmlString, wpv);
            vp.add(gridBottom);
            panel.add(vp);
        }
                
    }


    protected void inputComplete() {
    }


    protected void inputCanceled() {
    }

    protected boolean validateInput() throws ValidationException {
        return true;
    }
    
//======================================================================
//------------------ Inner classes-----------------------
//======================================================================

    private class StatsGrid extends Grid{

        private int cRow = -1;
        private WorldPt[] plots = new WorldPt[4];
        private Drawer _drawer;
        private WebPlotView _pv;

        public StatsGrid(HashMap<Metrics,Metric> metric){
            super(1, 3);
            createTopGrid(metric);
        }

        public StatsGrid(HashMap<Metrics,Metric> metric, String htmlString, WebPlotView wpv){
            super(4,3);
            _pv=wpv;
            createBottomGrid(metric, htmlString);
        }

        public void createTopGrid(HashMap<Metrics,Metric> metric){
            //build top grid
            this.setWidth("410px");
            this.setStyleName("statistics-panel-top");
            //this.setStyleName("table-row-highlight");

            //NumberFormat numFormat = NumberFormat.getDecimalFormat();
            //NumberFormat sciNotFormat = NumberFormat.getScientificFormat();

            HTMLTable.ColumnFormatter colF = this.getColumnFormatter();
            colF.setWidth(0, "110px");
            colF.setWidth(2,"135px");

            //Grid Row 1 Cell 0,0 Mean
            Metric mean = metric.get(Metrics.MEAN);
            this.setHTML(0,0,"<b>"+_prop.getName("mean-flux")+"</b><br>" + WebDefaultMouseReadoutHandler.formatFlux(mean.getValue()) + " " + mean.getUnits());

            //Grid Row 1 Cell 0,1 Std Dev
            Metric stdDev = metric.get(Metrics.STDEV);
            this.setHTML(0,1,"<b>" + _prop.getName("std-dev") + "</b><br>" + WebDefaultMouseReadoutHandler.formatFlux(stdDev.getValue()) + " " + stdDev.getUnits());

            //Grid Row 1 Cell 0,2 Integrated Flux
            Metric integrated = metric.get(Metrics.INTEGRATED_FLUX);
            this.setHTML(0,2,"<b>" + _prop.getName("int-flux") + "</b><br>" + WebDefaultMouseReadoutHandler.formatFlux(integrated.getValue()) + " " + integrated.getUnits());
        }

        public void createBottomGrid(HashMap<Metrics,Metric> metric, String htmlString){
            //build hover grid
            this.setWidth("410px");
            //this.setStyleName("statistics-panel");

            //NumberFormat numFormat = NumberFormat.getDecimalFormat();

            HTMLTable.ColumnFormatter colF = this.getColumnFormatter();
            colF.setWidth(0, "110px");
            colF.setWidth(2,"135px");

            String[] htmlStr = htmlString.split(";");
            WorldPt wp;
            //WebPlot webP = _pv.getPrimaryPlot();
            //Grid2 Row 1  MAX
            Metric max = metric.get(Metrics.MAX);
            wp = new WorldPt(Double.parseDouble(htmlStr[8]),Double.parseDouble(htmlStr[9]));
            plots[0] = wp;
            this.setHTML(0,0,"<b>" + _prop.getName("max-flux") + "</b><br>" + WebDefaultMouseReadoutHandler.formatFlux(max.getValue()) + " " + max.getUnits());
            this.setHTML(0,1,htmlStr[0]);
            this.setHTML(0,2,htmlStr[1]);

            //Grid2 Row 2 MIN
            Metric min = metric.get(Metrics.MIN);
            wp = new WorldPt(Double.parseDouble(htmlStr[10]),Double.parseDouble(htmlStr[11]));
            plots[1] = wp;
            this.setHTML(1,0,"<b>" +_prop.getName("min-flux") + "</b><br>" + WebDefaultMouseReadoutHandler.formatFlux(min.getValue()) + " " + min.getUnits());
            this.setHTML(1,1,htmlStr[2]);
            this.setHTML(1,2,htmlStr[3]);

            // Grid Row 4 Centroid
            //Metric centroid = metric.get(Metrics.CENTROID);
            wp = new WorldPt(Double.parseDouble(htmlStr[12]),Double.parseDouble(htmlStr[13]));
            plots[2] = wp;
            this.setHTML(2,0,"<b>" + _prop.getName("centroid") + "</b>");
            this.setHTML(2,1,htmlStr[4]);
            this.setHTML(2,2,htmlStr[5]);

            // Grid Row 5 Flux weighted centroid
            //Metric fwCentroid = metric.get(Metrics.FW_CENTROID);
            wp = new WorldPt(Double.parseDouble(htmlStr[14]),Double.parseDouble(htmlStr[15]));
            plots[3] = wp;
            this.setHTML(3,0,"<b>" + _prop.getName("fw-centroid") + "</b>");
            this.setHTML(3,1,htmlStr[6]);
            this.setHTML(3,2,htmlStr[7]);

            MouseMoveHandler h = new MouseMoveHandler() {
                public void onMouseMove(MouseMoveEvent event) {
                    if(_drawer == null){
                        _drawer = new Drawer(_pv,true, Drawer.DataType.NORMAL);
                    }
                    Element td = getEventTargetCell(Event.as(event.getNativeEvent()));
                    if (td != null) {
                        Element tr = (Element) td.getParentElement();
                        Element body = (Element) tr.getParentElement();
                        int row = DOM.getChildIndex(body, tr);
                        if (row != cRow) {
                            getRowFormatter().addStyleName(row, "table-row-highlight");

                            if(row == 0) {
                                _drawer.clear();
                                PointDataObj obj = new PointDataObj(plots[0]);
                                _drawer.setData(obj);
                            } else if (row == 1) {
                                _drawer.clear();
                                PointDataObj obj = new PointDataObj(plots[1]);
                                _drawer.setData(obj);
                            } else if (row == 2) {
                                _drawer.clear();
                                PointDataObj obj = new PointDataObj(plots[2]);
                                _drawer.setData(obj);
                            } else {
                                _drawer.clear();
                                PointDataObj obj = new PointDataObj(plots[3]);
                                _drawer.setData(obj);
                            }
                            
                            if(cRow >= 0) {
                                getRowFormatter().removeStyleName(cRow, "table-row-highlight");
                            }
                            cRow = row;

                        }
                    }
                }
             };
             addDomHandler(h, MouseMoveEvent.getType());

             MouseOutHandler moh = new MouseOutHandler(){
                public void onMouseOut(MouseOutEvent event){
                    _drawer.clear();
                    getRowFormatter().removeStyleName(cRow, "table-row-highlight");
                    cRow = -1;
                }
            };
            addDomHandler(moh, MouseOutEvent.getType());

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
