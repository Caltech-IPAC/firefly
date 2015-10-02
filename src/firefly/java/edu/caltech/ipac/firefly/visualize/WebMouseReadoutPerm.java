/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
/**
 * User: roby
 * Date: May 20, 2008
 * Time: 1:43:46 PM
 */


/**
 * @author Trey Roby
 * @version $Id: WebMouseReadout.java,v 1.65 2012/11/21 21:12:43 roby Exp $
 */
public class WebMouseReadoutPerm implements Readout {

    private static final int LABEL_IDX= 0;
    private static final int VALUE_IDX= 1;

    public static final String FILE_PIXEL= "Pixel Size";
    public static final String SCREEN_PIXEL= "Screen Pixel Size";

    final List<String> scaleRowOps= Arrays.asList(FILE_PIXEL, SCREEN_PIXEL);


    private boolean ALIGN_RIGHT= true;
    private static final int WIDE_MAX_ROWS= 5;
    private static WebDefaultMouseReadoutHandler _defaultHandler = new WebDefaultMouseReadoutHandler();
    private WebMouseReadoutHandler _currentHandler = _defaultHandler;

//    private Map<Widget, String> _styleMap = new HashMap<Widget, String>(30);
    private WebPlot _currentPlot = null;
    private int totalRows = 0;
    private WebPlotView _plotView;
    private FlowPanel gridPanel= new FlowPanel();
    private List<Grid> gridList= new ArrayList<Grid>(2);
//    private int scaleDisplayPos = -1;
//    private String scaleDisplayOp = FILE_PIXEL;
    private boolean active= false;

    private boolean _showing = false;
    private static final NumberFormat _nfPix = NumberFormat.getFormat("#.##");
    private FlowPanel imagePanel = new FlowPanel();
    private SimplePanel readoutWrapper;
//    private HTML _filePix = new HTML();

    private boolean _enabled = true;
    private final DeckPanel _thumbDeck = new DeckPanel();
    private final DeckPanel _magDeck = new DeckPanel();
    private final List<WebPlotView> _pvList = new ArrayList<WebPlotView>(3);
    private final WebPlotView.MouseInfo _mi = new WebPlotView.MouseInfo(new ReadOut(),
                                                                        "Mouse mouse to see plot information");
    private boolean _pixelClickLock = false;
//    private MarkedPointDisplay _dataConnect = new MarkedPointDisplay();
    private DrawingManager _lockPointDrawMan = null;
    private CheckBox _lockMouCheckBox = new CheckBox("Lock By Click");
    private final HTML titleLabel= new HTML();
    private ScreenPt lastPt= null;
    private HTML arrowDesc = new HTML("Arrow: Eq. North & East");



    public WebMouseReadoutPerm() {
        super();




        HorizontalPanel hp = new HorizontalPanel();


        _lockMouCheckBox.setTitle("Click on an image to lock the display at that point.");
        _lockMouCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            public void onValueChange(ValueChangeEvent<Boolean> lock) {
                setClickLock(lock.getValue());
            }
        });

        GwtUtil.setStyle(titleLabel,"whiteSpace", "nowrap");

        VerticalPanel titleArea= new VerticalPanel();
        titleArea.add(titleLabel);
        GwtUtil.setStyles(titleLabel, "padding", "0 0 3px 10px",
                                      "width", "120px",
                                      "height", "1em",
                                      "textOverflow", "ellipsis",
                                      "overflow", "hidden",
                                      "color", "white");
        if (!BrowserUtil.isTouchInput())  {
            titleArea.add(_lockMouCheckBox);
            GwtUtil.setStyles(_lockMouCheckBox, "padding", "15px 0 0 10px", "color", "white");
        }
        _lockMouCheckBox.setVisible(false);


        _lockMouCheckBox.addStyleName("lock-click");





        if (ALIGN_RIGHT) {
            hp.add(gridPanel);
            hp.add(titleArea);
        }
        else {
            hp.add(titleArea);
            hp.add(gridPanel);
        }



        GwtUtil.setStyles(hp,  "whiteSpace", "nowrap");

        readoutWrapper= new SimplePanel(hp);
        GwtUtil.setStyles(readoutWrapper, "paddingTop", "4px");
        GwtUtil.setStyles(hp, "marginLeft", "140px");

        if (ALIGN_RIGHT) {
            hp.addStyleName("right-floating");
        }



        GwtUtil.setStyle(imagePanel, "paddingTop", "1px");

        FlowPanel decPanel = new FlowPanel();
        decPanel.add(_thumbDeck);
        imagePanel.add(decPanel);
        decPanel.add(_magDeck);
        GwtUtil.setStyles(_thumbDeck,
                          "display", "inline-block",
                          "border", "1px solid #BBBBBB");
        GwtUtil.setStyles(_magDeck,
                          "border", "1px solid #BBBBBB",
                          "display", "inline-block",
                          "marginLeft", "2px");
        _magDeck.setSize("70px", "70px");

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                // todo
            }
        });


        WebEventManager.getAppEvManager().addListener(Name.REGION_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                hideMouseReadout();
            }
        });
        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_OPEN, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                hideMouseReadout();
            }
        });


    }

//    DrawingManager getLockPointDrawing() {
//        if (_lockPointDrawMan==null) {
//            _lockPointDrawMan = new DrawingManager("Clicked Point", _dataConnect);
//        }
//        return _lockPointDrawMan;
//    }

    public void setEnabled(boolean enabled) {
        _enabled = enabled;
        if (!_enabled) {
            hideMouseReadout();
        }

    }

    public void addPlotView(WebPlotView pv) {
        if (!_pvList.contains(pv)) {
            _plotView = pv;
            _pvList.add(pv);
            int i = _pvList.indexOf(pv);
            _magDeck.insert(new MagnifiedView(pv, 70), i);
            _thumbDeck.insert(new ThumbnailView(pv,70), i);

            ((ThumbnailView) _thumbDeck.getWidget(i)).setParentShowingHint(false);
            pv.addPersistentMouseInfo(_mi);
            adjustGrid();
            // todo turn off click lock
            _lockMouCheckBox.setValue(false);
            setClickLock(false);
        }
    }


    public void removePlotView(WebPlotView pv) {
        if (_pvList.contains(pv)) {
            if (pv == _plotView) _plotView = null;
            int i = _pvList.indexOf(pv);
            pv.removePersistentMouseInfo(_mi);
            _magDeck.remove(i);
            _thumbDeck.remove(i);
            _pvList.remove(pv);
        }
    }



    public void clear() {
        if (!_pixelClickLock) {
            showReadout(null, null, true);
            active= false;
        }
    }


    public void updateReadout(ScreenPt pt) {
        ImagePt ipt;
        ipt = _currentPlot.getImageCoords(pt);
        lastPt= pt;
        showReadout(pt, ipt, false);
        notifyExternal(pt,ipt,Band.NO_BAND, 0, null, false);
    }

    public static void notifyExternal(ScreenPt pt,
                                      ImagePt ipt,
                                      Band band,
                                      double flux,
                                      String fluxUnits,
                                      boolean withFlux) {

        String plotId = AllPlots.getInstance().getMiniPlotWidget().getPlotId();
        WebPlot plot= AllPlots.getInstance().getPlotView().getPrimaryPlot();
        if (plotId==null) return;
        List<Ext.Extension> extensionList = AllPlots.getInstance().getExtensionList(plotId);


        Ext.ExtensionResult r= Ext.makeExtensionResult();
        r.setExtValue("plotId", plotId);
        r.setExtValue("zoomLevel", plot.getZoomFact()+"");
        r.setExtValue("spt", pt.serialize());
        r.setExtValue("ipt", ipt.serialize());
        if (withFlux) {
            r.setExtValue("band", band.toString());
            r.setExtValue("flux", flux+"");
            r.setExtValue("fluxUnits", fluxUnits);
        }

        try {
            WorldPt ptJ2= plot.getWorldCoords(ipt, CoordinateSys.EQ_J2000);
            r.setExtValue("wptJ2000", ptJ2.serialize());
        } catch (Exception e) {
        }

        try {
            WorldPt ptGal= plot.getWorldCoords(ipt, CoordinateSys.EQ_J2000);
            r.setExtValue("wptGal", ptGal.serialize());
        } catch (Exception e) {
        }


        for (Ext.Extension ext : extensionList) {
            if (ext.extType().equals(Ext.PLOT_MOUSE_READ_OUT)) {
                Ext.fireExtAction(ext, r);
            }
        }



    }

    public void setValue(int row, String labelText, String valueText) {
        setValue(row, labelText, valueText, null, false, false);

    }

    public void setValue(int row, String labelText, String valueText, boolean valueIsHtml) {
        setValue(row, labelText, valueText, null, valueIsHtml, false);

    }

    public void setValue(int row,
                         String labelText,
                         String valueText,
                         String valueStyle) {
        setValue(row, labelText, valueText, valueStyle, false, false);
    }


    public void setValue(int row,
                         String labelText,
                         String valueText,
                         String valueStyle,
                         boolean valueIsHtml,
                         boolean setOnlyIfActive) {
        if (_currentPlot==null) return;
        if (!active && setOnlyIfActive) return;
        final int rowFinal= row;
        row--;

        if (row>-1 && row < totalRows) {
            LineRef line= getLine(row);

            Label label = (Label) line.grid.getWidget(line.row, LABEL_IDX);

            if (label == null) {
                label = new Label(labelText);
                label.addStyleName("perm-readout-label");
                line.grid.setWidget(line.row, LABEL_IDX, label);
                List rowWithOps= getRowsWithOptions();
                if (rowWithOps!=null && rowWithOps.contains(row)) {
                    GwtUtil.makeIntoLinkButton(label);
                    label.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            showRowOps(rowFinal);
                        }
                    });
                }
            }
            label.setText(labelText);

            if (StringUtils.isEmpty(valueText))  valueText= "&nbsp;";
            String style= line.leftMostGrid ? "perm-readout-value-first":"perm-readout-value";
            String sV= "<div class=\""+style+ "\">"+valueText+"</div>";
            line.grid.setHTML(line.row, VALUE_IDX, sV);

            if (!_lockMouCheckBox.isVisible()) _lockMouCheckBox.setVisible(true);
        }

    }

    public List<Integer> getRowsWithOptions() {
        List<Integer> rowWithOps= _currentHandler.getRowsWithOptions();
        List<Integer> retval= new ArrayList<Integer>(rowWithOps.size());
        retval.addAll(rowWithOps);
//        if (scaleDisplayPos>-1) retval.add(scaleDisplayPos-1);
        return retval;
    }


    private List<String> getRowOptions(int row) {
//        if (row==scaleDisplayPos) {
//            return scaleRowOps;
//        }
//        else {
            return _currentHandler.getRowOptions(row);
//        }
    }




    private void showRowOps(final int row) {
        List<String> opList= getRowOptions(row);
        final SimpleInputField field= GwtUtil.createRadioBox("options", opList, getRowOption(row));
        final PopupPane popup= new PopupPane("Choose Option", field, true, true);
        popup.addCloseHandler(new CloseHandler<PopupPane>() {
            public void onClose(CloseEvent<PopupPane> event) {
                String s= field.getValue();
                setRowOption(row,s);
            }
        });

        field.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                popup.hide();
                String s= field.getValue();
                setRowOption(row,s);
                if (_currentPlot!=null && lastPt!=null && _pixelClickLock) {
                    try {
                        _currentHandler.computeMouseValue(_currentPlot, WebMouseReadoutPerm.this,
                                                          row, _currentPlot.getImageCoords(lastPt),
                                                          lastPt, new Date().getTime());
                    } catch (Exception e) {
                        _currentHandler.computeMouseExitValue(_currentPlot,WebMouseReadoutPerm.this, row);
                    }
                }
                else {
                    _currentHandler.computeMouseExitValue(_currentPlot,WebMouseReadoutPerm.this, row);
                }
//                updateScaleDisplay(false);
            }
        });
        popup.alignTo(gridPanel, PopupPane.Align.BOTTOM_CENTER);
        popup.show();
    }


    private String getRowOption(int row) {
//        if (row==scaleDisplayPos) {
//            return scaleDisplayOp;
//        }
//        else {
            return _currentHandler.getRowOption(row);
//        }
    }

    private  void setRowOption(int row, String op) {
//        if (row==scaleDisplayPos) {
//            scaleDisplayOp= op;
//        }
//        else {
            _currentHandler.setRowOption(row,op);
//        }
    }


    public void setTitle(String valueText, boolean valueIsHtml) {

        if (valueIsHtml) {
            titleLabel.setHTML(valueText);
        } else {
            titleLabel.setText(valueText);
        }
    }



    private void adjustGrid() {
        if (_plotView != null && _currentPlot != null) {
            if (_currentPlot.containsAttributeKey(WebPlot.READOUT_ATTR)) {
                Object o = _currentPlot.getAttribute(WebPlot.READOUT_ATTR);
                if (o instanceof WebMouseReadoutHandler) {
                    _currentHandler = (WebMouseReadoutHandler) o;
                } else {
                    _currentHandler = _defaultHandler;
                }
            } else {
                _currentHandler = _defaultHandler;
            }
        }

        int rows = _currentHandler.getRows(_currentPlot);
        int col =  1;

        if (totalRows != rows) {
            reinitGridSize(rows);
        }
    }

    private void reinitGridSize(int rows) {
        totalRows = rows+1;
//        scaleDisplayPos = totalRows;
        gridList.clear();

        gridPanel.clear();
        int gridCnt= totalRows / 2 + totalRows%2;
        for(int i=0; i<gridCnt; i++) {
            Grid g= new Grid(2,2);
            for (int r = 0; (r < 2); r++) {
                g.getCellFormatter().setHorizontalAlignment(r, LABEL_IDX, HasHorizontalAlignment.ALIGN_RIGHT);
            }
            GwtUtil.setStyles(g, "lineHeight", "1",
                                 "display", "inline-block" );

            gridList.add(g);
            gridPanel.add(g);
        }
    }



    private void update(int x, int y) {
        active= true;
        int i = _pvList.indexOf(_plotView);
        ((ThumbnailView) _thumbDeck.getWidget(i)).setParentShowingHint(true);
        if (_currentPlot!= _plotView.getPrimaryPlot()) {
            clear();
        }
        _currentPlot = _plotView.getPrimaryPlot();
        if (_currentPlot != null) {
            adjustGrid();
            updateReadout(new ScreenPt(x, y));
        }
    }


    private void showReadout(ScreenPt pt, ImagePt ipt, boolean doClear) {
        if ((pt==null || ipt==null) && !doClear) return;

        long callID = new Date().getTime();

        if (!doClear) {

            boolean minimal= isMinimal(_currentPlot);
            _thumbDeck.setVisible(!minimal);
            _magDeck.setVisible(!minimal);
            arrowDesc.setVisible(!minimal);
            GwtUtil.setStyle(_magDeck, "display", "inline-block");
            GwtUtil.setStyle(_thumbDeck, "display", "inline-block");
//            _filePix.setVisible(!minimal);
        }

        for (int row = 0; row < totalRows; row++) {
            if (doClear) {
                _currentHandler.computeMouseExitValue(_currentPlot, this, row);
            } else {
                _currentHandler.computeMouseValue(_currentPlot, this,
                                                  row, ipt, pt, callID);
            }
//            updateScaleDisplay(doClear);
        }
    }

//    private void updateScaleDisplay(boolean doClear) {
//        if (scaleDisplayPos>-1) {
//            if (!doClear && _currentPlot!=null && !_currentPlot.isBlankImage() && !isMinimal(_currentPlot)) {
//                String ipStr="";
//                if (scaleDisplayOp.equals(FILE_PIXEL)) {
//                    ipStr= _nfPix.format(_currentPlot.getImagePixelScaleInArcSec());
//                }
//                else if (scaleDisplayOp.equals(SCREEN_PIXEL)) {
//                    float size = (float) _currentPlot.getImagePixelScaleInArcSec() / _currentPlot.getZoomFact();
//                    ipStr= _nfPix.format(size);
//                }
//                setValue(scaleDisplayPos, scaleDisplayOp+":", ipStr+ "\"", true);
//            }
//            else {
//                setValue(scaleDisplayPos, scaleDisplayOp+":", "");
//            }
//        }
//    }



    public void hideMouseReadout() {
        _showing = false;
        int i = _pvList.indexOf(_plotView);
        if (i > 0 && i < _thumbDeck.getWidgetCount()) {
            ThumbnailView thumb = (ThumbnailView) _thumbDeck.getWidget(i);
            thumb.setParentShowingHint(false);
        }
        LayoutManager lm= Application.getInstance().getLayoutManager();
        lm.getRegion(LayoutManager.VIS_PREVIEW_REGION).hide();
        lm.getRegion(LayoutManager.VIS_READOUT_REGION).hide();
    }

    public void displayMouseReadout() {
//        if (_showing) return;
        LayoutManager lm= Application.getInstance().getLayoutManager();
        lm.getRegion(LayoutManager.VIS_PREVIEW_REGION).setDisplay(imagePanel);
        lm.getRegion(LayoutManager.VIS_READOUT_REGION).setDisplay(readoutWrapper);
        _showing= true;
    }



//    static int cnt = 1;

    private void move(WebPlotView pv, ScreenPt spt, boolean fromTouch, boolean updateDisplay) {
        int i = _pvList.indexOf(pv);
        if (i > -1) {
            _thumbDeck.showWidget(i);
            _magDeck.showWidget(i);

            _plotView = pv;
            int x = spt.getIX();
            int y = spt.getIY();
            if (isOverPlot(x, y)) {
                if (updateDisplay) {
                    update(x, y);
                    if (_pixelClickLock) {
                        MagnifiedView mv = (MagnifiedView) _magDeck.getWidget(_pvList.indexOf(pv));
                        if (mv != null) mv.update(spt);
                    }
                }
                displayMouseReadout();
            }
        }

    }


    private boolean isOverPlot(int x, int y) {
        ScreenPt wcsMargin= _plotView.getWcsMargins();
        int mx= wcsMargin.getIX();
        int my= wcsMargin.getIY();
        x+=mx;
        y+=my;

        int sx = _plotView.getScrollX();
        int sy = _plotView.getScrollY();

        int ex = sx + _plotView.getScrollWidth();
        int ey = sy + _plotView.getScrollHeight();

        return (x >= sx && x <= ex && y >= sy && y <= ey);
    }



    private class ReadOut extends WebPlotView.DefMouseAll {


        @Override
        public void onMouseOut(WebPlotView pv) {
            clear();
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) {
            move(pv, spt, false, !_pixelClickLock);
        }

        @Override
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
            move(pv, spt, true, true);
        }

        @Override
        public void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev) {
            move(pv, spt, true, true);
        }


        @Override
        public void onClick(WebPlotView pv, ScreenPt spt) {
            move(pv, spt, false, true);
//            if (_pixelClickLock) {
//                _dataConnect.setPoint(pv.getPrimaryPlot().getWorldCoords(spt), pv.getPrimaryPlot());
//                getLockPointDrawing().redraw();
//            }
        }

        @Override
        public void onTouchEnd(WebPlotView pv) { }
    }

    private void setClickLock(boolean clickLock) {
        if (_pixelClickLock == clickLock) return;
        _pixelClickLock = clickLock;
        int tot = _magDeck.getWidgetCount();
        for (int i = 0; (i < tot); i++) {
            Widget w = _magDeck.getWidget(i);
            ((MagnifiedView) w).setFreezeView(_pixelClickLock);
        }
//        DrawingManager dm= getLockPointDrawing();
        if (_pixelClickLock) {
            AllPlots.getInstance().enableActivePointSelection(false);
//            _dataConnect.setPoint(null, null);
//            dm.redraw();
//            for (WebPlotView pv : _pvList) {
//                dm.addPlotView(pv);
//            }
        } else {
            AllPlots.getInstance().disableActivePointSelection(true);
//            for (WebPlotView pv : _pvList) {
//                dm.removePlotView(pv);
//            }
        }

    }


//    private static class MarkedPointDisplay extends SimpleDataConnection {
//
//        private List<DrawObj> list = new ArrayList<DrawObj>(1);
//        private WebPlot markedPlot = null;
//
//        MarkedPointDisplay() {
//            super("Clicked Point", "Point lock to your click", AutoColor.PT_3);
//        }
//
//        public void setPoint(WorldPt wp, WebPlot plot) {
//            list= null;
//            if (wp != null && plot != null) {
//                list= Arrays.asList((DrawObj)new PointDataObj(wp, DrawSymbol.CIRCLE));
//                markedPlot = plot;
//            }
//        }
//
//
//        @Override
//        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
//            List<DrawObj> retList = list;
//            if (list!=null && list.size() > 0 && plot==markedPlot) {
//                PointDataObj obj = new PointDataObj(list.get(0).getCenterPt(), DrawSymbol.SQUARE);
//                retList= Arrays.asList(list.get(0),obj);
//            }
//            return retList;
//        }
//        public boolean getHasPerPlotData() { return true; }
//
//        @Override
//        public boolean getSupportsMouse() {
//            return false;
//        }
//    }



    public static boolean isMinimal(WebPlot plot) {
        boolean minimal= false;
        if (plot!=null && plot.containsAttributeKey(WebPlot.MINIMAL_READOUT)) {
            minimal= Boolean.valueOf(plot.getAttribute(WebPlot.MINIMAL_READOUT).toString());
        }
        return minimal;
    }

    private LineRef getLine(int row) {
        int gridIdx= ALIGN_RIGHT ?   gridList.size() - ((row/2)+1)  : (row/2);
        int rowIdx= row%2;
        return new LineRef(gridList.get(gridIdx), rowIdx, row<2);

    }


    private static class LineRef {
        private Grid grid;
        private int row;
        private boolean leftMostGrid;

        private LineRef(Grid grid, int row, boolean leftMostGrid) {
            this.grid = grid;
            this.row = row;
            this.leftMostGrid= leftMostGrid;
        }
    }


}

