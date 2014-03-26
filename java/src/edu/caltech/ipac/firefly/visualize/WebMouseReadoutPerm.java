package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
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
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final int WIDE_MAX_ROWS= 5;
    private static WebDefaultMouseReadoutHandler _defaultHandler = new WebDefaultMouseReadoutHandler();
    private WebMouseReadoutHandler _currentHandler = _defaultHandler;

    private Map<Widget, String> _styleMap = new HashMap<Widget, String>(30);
    private WebPlot _currentPlot = null;
    private int totalRows = 0;
    private WebPlotView _plotView;
    private FlowPanel gridPanel= new FlowPanel();
    private List<Grid> gridList= new ArrayList<Grid>(2);

    private boolean _showing = false;
    private static final NumberFormat _nfPix = NumberFormat.getFormat("#.##");
    private HTML _filePix = new HTML();

    private boolean _enabled = true;
    private final DeckPanel _thumbDeck = new DeckPanel();
    private final DeckPanel _magDeck = new DeckPanel();
    private final List<WebPlotView> _pvList = new ArrayList<WebPlotView>(3);
    private final WebPlotView.MouseInfo _mi = new WebPlotView.MouseInfo(new ReadOut(),
                                                                        "Mouse mouse to see plot information");
    private boolean _pixelClickLock = false;
    private MarkedPointDisplay _dataConnect = new MarkedPointDisplay();
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

        VerticalPanel fixedDisplay = new VerticalPanel();
        fixedDisplay.setSpacing(2);

        GwtUtil.setStyle(titleLabel,"whiteSpace", "nowrap");

        VerticalPanel titleArea= new VerticalPanel();
        titleArea.add(titleLabel);
        GwtUtil.setStyles(titleLabel, "padding", "0 0 3px 10px",
                                      "width", "130px",
                                      "textOverflow", "ellipsis",
                                      "overflow", "hidden",
                                      "color", "white");
        if (!BrowserUtil.isTouchInput())  {
            titleArea.add(_lockMouCheckBox);
            GwtUtil.setStyles(_lockMouCheckBox, "padding", "15px 0 0 10px", "color", "white");
        }

        fixedDisplay.add(_filePix);
        GwtUtil.setStyles(fixedDisplay, "width", "100px", "marginTop", "5px");

        GwtUtil.setStyles(_filePix, "marginTop", "3px",
                                    "paddingLeft", "1px",
                                    "fontSize", "10px",
                                    "color", "white",
                                    "textAlign", "left");

        _lockMouCheckBox.addStyleName("lock-click");
        _filePix.addStyleName("title-font-family");




        VerticalPanel imagePanel = new VerticalPanel();
        hp.add(titleArea);
        hp.add(gridPanel);
        GwtUtil.setStyles(hp,  "whiteSpace", "nowrap");


        SimplePanel readoutWrapper= new SimplePanel(hp);
        GwtUtil.setStyles(readoutWrapper, "paddingTop", "4px");
        GwtUtil.setStyles(hp, "marginLeft", "160px");

        Application.getInstance().getLayoutManager().getRegion(LayoutManager.VIS_READOUT_REGION).setDisplay(readoutWrapper);
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.VIS_PREVIEW_REGION).setDisplay(imagePanel);


        GwtUtil.setStyle(imagePanel, "paddingBottom", "2px");

        HorizontalPanel decPanel = new HorizontalPanel();
        GwtUtil.setStyles(_thumbDeck, "border", "1px solid #BBBBBB");
        GwtUtil.setStyles(_magDeck, "border", "1px solid #BBBBBB");
        decPanel.add(_thumbDeck);
        decPanel.setSpacing(2);
        decPanel.add(_magDeck);
        imagePanel.add(decPanel);



        GwtUtil.setStyle(_magDeck, "paddingLeft", "5px");
        _magDeck.setSize("70px", "70px");

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                // todo
            }
        });
    }

    DrawingManager getLockPointDrawing() {
        if (_lockPointDrawMan==null) {
            _lockPointDrawMan = new DrawingManager("Clicked Point", _dataConnect);
        }
        return _lockPointDrawMan;
    }

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
        showReadout(null, null, true);
    }


    public void updateReadout(ScreenPt pt) {
        ImagePt ipt;
        ipt = _currentPlot.getImageCoords(pt);
        lastPt= pt;
        showReadout(pt, ipt, false);
    }

    public void setValue(int row, String labelText, String valueText) {
        setValue(row, labelText, valueText, null, false);

    }

    public void setValue(int row, String labelText, String valueText, boolean valueIsHtml) {
        setValue(row, labelText, valueText, null, valueIsHtml);

    }

    public void setValue(int row,
                         String labelText,
                         String valueText,
                         String valueStyle) {

        setValue(row, labelText, valueText, valueStyle, false);
    }


    public void setValue(int row,
                         String labelText,
                         String valueText,
                         String valueStyle,
                         boolean valueIsHtml) {
        final int rowFinal= row;
        row--;

        if (row < totalRows) {



            LineRef line= getLine(row);

            Label label = (Label) line.grid.getWidget(line.row, LABEL_IDX);
            Label value = (Label) line.grid.getWidget(line.row, VALUE_IDX);

            if (label == null) {
                label = new Label(labelText);
                label.addStyleName("perm-readout-label");
                line.grid.setWidget(line.row, LABEL_IDX, label);
                List rowWithOps= _currentHandler.getRowsWithOptions();
                if (rowWithOps!=null && rowWithOps.contains(row)) {
                    GwtUtil.makeIntoLinkButton(label);
                    label.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            showRowOps(rowFinal);
                        }
                    });
                }
            }

            if (value == null) {
                value = new Label(valueText);
                value.addStyleName(line.leftMostGrid ? "perm-readout-value-first":"perm-readout-value");
                line.grid.setWidget(line.row, VALUE_IDX, value);
            }


            label.setText(labelText);
            if (valueIsHtml) {
                DOM.setInnerHTML(value.getElement(), valueText);
            } else {
                value.setText(valueText);
            }

            String oldStyle = _styleMap.get(value);
            if (oldStyle != null) value.removeStyleName(oldStyle);
            if (valueStyle != null) {
                value.addStyleName(valueStyle);
                _styleMap.put(value, valueStyle);
            }
        }

    }

    private void showRowOps(final int row) {
        List<String> opList= _currentHandler.getRowOptions(row);
        final SimpleInputField field= GwtUtil.createRadioBox("options", opList,
                                                             _currentHandler.getRowOption(row));
        final PopupPane popup= new PopupPane("Choose Option", field, true, true);
        popup.addCloseHandler(new CloseHandler<PopupPane>() {
            public void onClose(CloseEvent<PopupPane> event) {
                String s= field.getValue();
                _currentHandler.setRowOption(row,s);
            }
        });

        field.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                popup.hide();
                String s= field.getValue();
                _currentHandler.setRowOption(row,s);
                if (_currentPlot!=null && lastPt!=null) {
                    try {
                        _currentHandler.computeMouseValue(_currentPlot, WebMouseReadoutPerm.this,
                                                          row, _currentPlot.getImageCoords(lastPt),
                                                          lastPt, new Date().getTime());
                    } catch (Exception e) {
                        // ignore- do nothing
                    }
                }
            }
        });
        popup.alignTo(gridPanel, PopupPane.Align.BOTTOM_CENTER);
        popup.show();
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
        totalRows = rows;
        gridList.clear();

        gridPanel.clear();
        int gridCnt= rows / 2 + rows%2;
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

//    private void clearAllValues() {
//        int rowMax= _grid.getRowCount();
//        int colMax= _grid.getColumnCount();
//        for (int i = 0; (i < rowMax); i++) {
//            for (int j = 0; (j < colMax); j++) {
//                Widget w= _grid.getWidget(i,j);
//                if (w!=null && w instanceof Label) {
//                    ((Label)w).setText("");
//                }
//            }
//        }
//
//        if (gridWide!=null) {
//            rowMax= gridWide.getRowCount();
//            colMax= gridWide.getColumnCount();
//            for (int i = 0; (i < rowMax); i++) {
//                for (int j = 0; (j < colMax); j++) {
//                    Widget w= gridWide.getWidget(i,j);
//                    if (w!=null && w instanceof Label) {
//                        ((Label)w).setText("");
//                    }
//                }
//            }
//        }
//
//    }


    private void showReadout(ScreenPt pt, ImagePt ipt, boolean doClear) {
        if (pt==null || ipt==null) return;

        long callID = new Date().getTime();

        boolean minimal= isMinimal(_currentPlot);
        _thumbDeck.setVisible(!minimal);
        _magDeck.setVisible(!minimal);
        arrowDesc.setVisible(!minimal);
        _filePix.setVisible(!minimal);

        for (int row = 0; row < totalRows; row++) {
            if (doClear) {
                _currentHandler.computeMouseExitValue(_currentPlot, this, row);
            } else {
                _currentHandler.computeMouseValue(_currentPlot, this,
                                                  row, ipt, pt, callID);
                updateScaleDisplay();
            }
        }
    }

    private void updateScaleDisplay() {
        if (!_currentPlot.isBlankImage()) {
            String ipStr= _nfPix.format(_currentPlot.getImagePixelScaleInArcSec());
            _filePix.setHTML( ipStr+ "\"/&nbsp;file&nbsp;pix");
        }
        else {
            _filePix.setHTML("");
        }
    }



    public void hideMouseReadout() {
        _showing = false;
        int i = _pvList.indexOf(_plotView);
        if (i > 0 && i < _thumbDeck.getWidgetCount()) {
            ThumbnailView thumb = (ThumbnailView) _thumbDeck.getWidget(i);
            thumb.setParentShowingHint(false);
        }
    }

    public void displayMouseReadout() {
        if (_showing) return;
        _showing= true;
    }



//    static int cnt = 1;

    private void move(WebPlotView pv, ScreenPt spt, boolean fromTouch, boolean updateDisplay) {
        int i = _pvList.indexOf(pv);
        if (i > -1) {
            _thumbDeck.showWidget(i);
            _magDeck.showWidget(i);

            WebPlotView oldPV = _plotView;
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
                if (!_showing) {
                    displayMouseReadout();
                }
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
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt) {
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
            if (_pixelClickLock) {
                _dataConnect.setPoint(pv.getPrimaryPlot().getWorldCoords(spt), pv.getPrimaryPlot());
                getLockPointDrawing().redraw();
            }
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
        DrawingManager dm= getLockPointDrawing();
        if (_pixelClickLock) {
            _dataConnect.setPoint(null, null);
            dm.redraw();
            for (WebPlotView pv : _pvList) {
                dm.addPlotView(pv);
            }
        } else {
            for (WebPlotView pv : _pvList) {
                dm.removePlotView(pv);
            }
        }

    }


    private static class MarkedPointDisplay extends SimpleDataConnection {

        private final List<DrawObj> list = new ArrayList<DrawObj>(1);
        private WebPlot markedPlot = null;

        MarkedPointDisplay() {
            super("Clicked Point", "Point lock to your click", AutoColor.PT_3);
        }

        public void setPoint(WorldPt wp, WebPlot plot) {
            list.clear();
            if (wp != null && plot != null) {
                PointDataObj obj = new PointDataObj(wp, DrawSymbol.CIRCLE);
                list.add(obj);
                markedPlot = plot;
            }
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            List<DrawObj> retList = list;
            if (list.size() > 0 && plot != null && markedPlot != null && plot == markedPlot) {
                PointDataObj obj = new PointDataObj(list.get(0).getCenterPt(), DrawSymbol.SQUARE);
                retList = new ArrayList<DrawObj>(2);
                retList.addAll(list);
                retList.add(obj);
            }
            return retList;
        }


        @Override
        public boolean getSupportsMouse() {
            return false;
        }
    }



    public static boolean isMinimal(WebPlot plot) {
        boolean minimal= false;
        if (plot.containsAttributeKey(WebPlot.MINIMAL_READOUT)) {
            minimal= Boolean.valueOf(plot.getAttribute(WebPlot.MINIMAL_READOUT).toString());
        }
        return minimal;
    }

    private LineRef getLine(int row) {
        int gridIdx= (row/2);
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
