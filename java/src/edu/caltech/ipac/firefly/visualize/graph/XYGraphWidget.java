package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.visualize.WebMouseReadout;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */



/**
 * @author Trey Roby
 */
public class XYGraphWidget extends Composite {


    private static final int RANGE = 100;
    DockPanel _panel= new DockPanel();
    Map<String, GeneralCommand> _commandMap= new HashMap<String, GeneralCommand>(13);
    private GChart _gcOverview = null;
    private GChart _gcZoom = null;
    private transient NumberFormat _nf=  NumberFormat.getFormat("#.###");
    private float _data[]= new float[1000];
//    private float _zoomData[]= new float[30];
    private GChart.Curve.Point  _lastMainPt= null;
    private boolean _programicOverviewTouch = false;
    private boolean _programicDetailTouch = false;

    WebMouseReadout _mouseReadout;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public XYGraphWidget() { this(200,200);
        GChart.setCanvasFactory(ChartingFactory.getInstance());
    }



    public XYGraphWidget(int width, int height) {
        initWidget(_panel);
        _panel.setSize("800px", "400px");
        layout();

    }

    public void graphData() {

    }

    public void makeNewChart() {
        if (_gcOverview !=null) {
            _panel.remove(_gcOverview);
        }
        if (_gcZoom !=null) {
            _panel.remove(_gcZoom);
        }
        dummyChart();
        zoomChart();
        _panel.add(_gcOverview, DockPanel.CENTER);
        _panel.add(_gcZoom, DockPanel.SOUTH);

    }

    public void update() {
        _gcOverview.update();
        _gcZoom.update();
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void dummyChart() {
        _lastMainPt= null;
        _gcOverview = new GChart(800,75);
        _gcOverview.setChartTitle("<b>this is a</b>test title of what <i>you can do</i>");
        _gcOverview.setPadding("5px");
        GChart.Axis xAxis= _gcOverview.getXAxis();
        GChart.Axis yAxis= _gcOverview.getYAxis();
        xAxis.setAxisLabel("this is the X Axis");
        xAxis.setHasGridlines(true);
        xAxis.setTickCount(10);
        xAxis.setTickLabelFormat("#.##");

        yAxis.setAxisLabel("Y Axis");
        yAxis.setHasGridlines(true);
        yAxis.setTickCount(5);
        yAxis.setAxisMin(0);
        yAxis.setAxisMax(120);

        _gcOverview.addCurve();
        GChart.Curve curve= _gcOverview.getCurve();

        curve.setLegendLabel("A Legend");
        GChart.Symbol symbol= curve.getSymbol();
        symbol.setBorderColor("red");
        symbol.setBackgroundColor("blue");
//        symbol.setSymbolType(GChart.SymbolType.LINE);
        symbol.setSymbolType(GChart.SymbolType.LINE);
        symbol.setWidth(0);
        symbol.setFillSpacing(5);
        symbol.setFillThickness(1);
        symbol.setBrushHeight(300);

        symbol.setHoverAnnotationSymbolType(GChart.SymbolType.ANCHOR_NORTHEAST);
        symbol.setHoverSelectionWidth(5);
        symbol.setHoverSelectionHeight(5);
//        symbol.setHoverSelectionBackgroundColor("blue");
        symbol.setHoverSelectionEnabled(true);
//        symbol.setHoverLocation(GChart.AnnotationLocation.NORTHEAST);


        symbol.setHoverSelectionSymbolType(
                GChart.SymbolType.LINE);

        symbol.setHoverSelectionSymbolType(
                GChart.SymbolType.XGRIDLINE);
        symbol.setHoverSelectionWidth((int)((RANGE/1000F) * 800F)+20);
        symbol.setHoverSelectionBorderColor("green");
        symbol.setHoverSelectionBorderWidth(2);
//        symbol.setHoverSelectionBorderWidth(2);
        // with kannotation on top of this line (above chart)
//        curve.getSymbol().setHoverAnnotationSymbolType(
//                GChart.SymbolType.XGRIDLINE);
//        curve.getSymbol().setHoverLocation(
//                GChart.AnnotationLocation.NORTH);
        // small gap between plot area and hover popup HTML
        curve.getSymbol().setHoverYShift(5);


        makeData();
        for(int i= 0; (i<_data.length); i++) {
            curve.addPoint(i,_data[i]);
        }



        _gcOverview.setHoverParameterInterpreter(new PrimaryHover());
    }



    private void zoomChart() {
        _gcZoom = new GChart(800,200);
        _gcZoom.setChartTitle("zoom chart");
        _gcZoom.setPadding("5px");
        GChart.Axis xAxis= _gcZoom.getXAxis();
        GChart.Axis yAxis= _gcZoom.getYAxis();
        xAxis.setAxisLabel("this is the X Axis");
        xAxis.setHasGridlines(true);
        xAxis.setTickCount(10);
        xAxis.setTickLabelFormat("#.##");

        yAxis.setAxisLabel("Y Axis");
        yAxis.setHasGridlines(true);
        yAxis.setTickCount(5);
        yAxis.setAxisMin(0);
        yAxis.setAxisMax(120);

        updateZoomData(0,20);
    }


    private void updateZoomData(int begin, int end) {

        GChart.Curve curve;

        if (_gcZoom.getNCurves()>0) {
            curve= _gcZoom.getCurve();
            _gcZoom.removeCurve(curve);
        }

        _gcZoom.addCurve();
        curve= _gcZoom.getCurve();

        curve.setLegendLabel("A Legend");
        GChart.Symbol symbol= curve.getSymbol();
        symbol.setBorderColor("red");
        symbol.setBackgroundColor("blue");
        symbol.setSymbolType(GChart.SymbolType.LINE);
        symbol.setWidth(0);
        symbol.setFillSpacing(5);
        symbol.setFillThickness(1);
        symbol.setBrushHeight(300);
        symbol.setBrushWidth(10);

        symbol.setHoverAnnotationSymbolType(GChart.SymbolType.ANCHOR_NORTHEAST);
        symbol.setHoverSelectionWidth(5);
        symbol.setHoverSelectionHeight(5);
        symbol.setHoverSelectionBackgroundColor("blue");
        symbol.setHoverSelectionEnabled(true);


        symbol.setHoverSelectionSymbolType(
                GChart.SymbolType.LINE);

        // small gap between plot area and hover popup HTML
        curve.getSymbol().setHoverYShift(5);


        makeData();
        for(int i= begin; (i<=end); i++) {
            curve.addPoint(i,_data[i]);
        }
        _gcZoom.setHoverParameterInterpreter(new DetailHover());
        _gcZoom.update();
    }


    private void makeData() {
        float i=0;
        for(int j= 0; (j<1000); j++) {
            if (j<100) {
                _data[j]= (float)(Math.random()*100) % 30;
            }
            else if (j<200) {
                _data[j]= (float)((Math.random()*100) % 30) + 30;
            }
            else if (j<300) {
                _data[j]= (float)((Math.random()*100) % 30) + 60;
            }
            else if (j<400) {
                _data[j]= (float)((Math.random()*100) % 30) + 90;
            }
            else if (j<1000) {
                _data[j]= (float)Math.random()*100;
            }
        }
    }


    private void layout() {
    }


    private class PrimaryHover implements HoverParameterInterpreter {
        public String getHoverParameter(String paramName, GChart.Curve.Point hoveredOver) {
                int span= RANGE *2;
                String retval;
                if (paramName.equals("x")) {
                    retval=  "x: " + _nf.format(hoveredOver.getX());
                }
                else {
                    retval=  "y: " + _nf.format(hoveredOver.getY());
                }
            if (_programicOverviewTouch) {
               return null; 
            }
            else  {
                final int x= (int)hoveredOver.getX();
                final int xBegin= (x< RANGE) ? 0 : x- RANGE;
                int xEnd= (xBegin+span> _data.length-1)  ? _data.length-1 : xBegin+span;
                updateZoomData(xBegin,xEnd);
                _lastMainPt= hoveredOver;


                _programicDetailTouch = true;
                //                int idx= _gcOverview.getCurve().getPointIndex(hoveredOver);
                GChart.Curve.Point detailPt= _gcZoom.getCurve().getPoint(x-xBegin);

                _gcZoom.touch(detailPt);
                _gcZoom.update(GChart.TouchedPointUpdateOption.TOUCHED_POINT_LOCKED);
//                _gcOverview.update();
                _programicDetailTouch = false;
            }
            return retval;
        }
    }

    private class DetailHover implements HoverParameterInterpreter {
        public String getHoverParameter(String paramName, GChart.Curve.Point hoveredOver) {
            if (_programicDetailTouch)  return null;
            String retval;
            if (paramName.equals("x")) {
                retval=  "x: " + _nf.format(hoveredOver.getX());
            }
            else {
                retval=  "y: " + _nf.format(hoveredOver.getY());
            }
            if (_lastMainPt!=null &&!_programicDetailTouch ) {
                _programicOverviewTouch = true;
                int idx= _gcOverview.getCurve().getPointIndex(_lastMainPt);
//                GwtUtil.showDebugMsg("point: " + idx);
                _gcOverview.touch(_lastMainPt);
                _gcOverview.update(GChart.TouchedPointUpdateOption.TOUCHED_POINT_LOCKED);
//                _gcOverview.update();
                _programicOverviewTouch = false;
//                _gcOverview.touch(null);
            }
            return retval;
//            return "param:"+ paramName+" x: " + _nf.format(hoveredOver.getIX())+ ", y: "+ _nf.format(hoveredOver.getIY());
        }
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

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
