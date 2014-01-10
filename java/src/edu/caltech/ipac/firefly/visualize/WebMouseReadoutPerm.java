package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
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


    private static final int IRSA_LOGO_X= 139;

    private static final int WIDE_MAX_ROWS= 4;
//    private static final int MAX_ROWS= 4;
    private static WebDefaultMouseReadoutHandler _defaultHandler = new WebDefaultMouseReadoutHandler();
    private WebMouseReadoutHandler _currentHandler = _defaultHandler;

    private Map<Widget, String> _styleMap = new HashMap<Widget, String>(30);
    private WebPlot _currentPlot = null;
    private int _currentRows = 0;
    private int _currentCols = 0;
    private WebPlotView _plotView;
    private final PopupPanel _popupPanel;
    private Grid _grid = new Grid();
    private Grid gridWide = new Grid();
    private boolean _showing = false;
    private Image _lockIcon;
    private Image _unlockIcon;
    private static final NumberFormat _nfPix = NumberFormat.getFormat("#.##");
    private HTML _filePix = new HTML();
    private HTML _screenPix = new HTML();
    private HTML _zoomLevel = new HTML();

    private boolean _enabled = true;
    private final DeckPanel _thumbDeck = new DeckPanel();
    private final DeckPanel _magDeck = new DeckPanel();
    private final List<WebPlotView> _pvList = new ArrayList<WebPlotView>(3);
    private int _laterX;
    private int _laterY;
    private final WebPlotView.MouseInfo _mi = new WebPlotView.MouseInfo(new ReadOut(),
                                                                        "Mouse mouse to see plot information");
    private boolean _mayLockOnce = false;
    private boolean _pixelClickLock = false;
    private MarkedPointDisplay _dataConnect = new MarkedPointDisplay();
    private DrawingManager _lockPointDrawMan = null;
    private CheckBox _lockMouCheckBox = new CheckBox("Lock By Click");
    private final HTML titleLabel= new HTML();

    private HTML arrowDesc = new HTML("Arrow: Eq. North & East");



    public WebMouseReadoutPerm() {
        super();

        HorizontalPanel hp = new HorizontalPanel();

        _popupPanel = new PopupPanel();
        _popupPanel.setStyleName("permMouseReadout");
        _popupPanel.setPopupPosition(50, 1);
        _popupPanel.setWidget(hp);
        GwtUtil.setStyles(_popupPanel, "zIndex", "5");



        _lockIcon = new Image(VisIconCreator.Creator.getInstance().getLocked());
        _unlockIcon = new Image(VisIconCreator.Creator.getInstance().getUnlocked());

        _lockMouCheckBox.setTitle("Click on an image to lock the display at that point.");
        _lockMouCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            public void onValueChange(ValueChangeEvent<Boolean> lock) {
                setClickLock(lock.getValue());
            }
        });

        VerticalPanel fixedDisplay = new VerticalPanel();
        fixedDisplay.setSpacing(2);

        GwtUtil.setStyle(titleLabel,"whiteSpace", "nowrap");

        VerticalPanel wideControlArea= null;

        wideControlArea= new VerticalPanel();
        wideControlArea.add(titleLabel);
        GwtUtil.setStyles(titleLabel, "padding", "12px 0 10px 10px",
                                      "width", "130px",
                                      "textOverflow", "ellipsis",
                                      "overflow", "hidden",
                                      "color", "white");
        if (!BrowserUtil.isTouchInput())  {
            wideControlArea.add(_lockMouCheckBox);
            GwtUtil.setStyles(_lockMouCheckBox, "padding", "15px 0 0 10px", "color", "white");
        }

        fixedDisplay.add(_filePix);
        fixedDisplay.add(_screenPix);
        fixedDisplay.add(_zoomLevel);
        GwtUtil.setStyles(fixedDisplay, "width", "100px", "marginTop", "5px");





        GwtUtil.setStyles(_filePix, "marginTop", "3px",
                                    "paddingLeft", "1px",
                                    "fontSize", "10px",
                                    "color", "white",
                                    "textAlign", "left");

        GwtUtil.setStyles(_screenPix, "padding", "1px 12px 0px 1px",
                                      "fontSize", "10px",
                                      "color", "white",
                                      "textAlign", "left");

        GwtUtil.setStyles(_zoomLevel, "padding", "12px 0 3px 0",
                                      "textAlign", "center",
                                      "color", "green",
                                      "fontSize", "9pt");

        _lockMouCheckBox.addStyleName("lock-click");
        _filePix.addStyleName("title-font-family");
        _screenPix.addStyleName("title-font-family");
        _zoomLevel.addStyleName("title-font-family");

        GwtUtil.setStyles(_grid, "lineHeight", "1",
                                 "margin", "5px 0 0 -8px");

        GwtUtil.setStyles(gridWide, "lineHeight", "1");



        VerticalPanel imagePanel = new VerticalPanel();
        hp.add(wideControlArea);
        hp.add(fixedDisplay);
        hp.add(_grid);
        hp.add(imagePanel);
        hp.add(gridWide);

        HorizontalPanel decPanel = new HorizontalPanel();
        GwtUtil.setStyle(_thumbDeck, "border", "1px solid #BBBBBB");
        GwtUtil.setStyle(_magDeck, "border", "1px solid #BBBBBB");
        decPanel.add(_thumbDeck);
        decPanel.setSpacing(2);
        decPanel.add(_magDeck);
        imagePanel.add(decPanel);



        GwtUtil.setStyle(_magDeck, "paddingLeft", "5px");
        _magDeck.setSize("70px", "70px");

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                updateToCenter();
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
        showReadout(pt, ipt, false);
    }

    public void updateReadout(ImagePt ipt) {
        ImageWorkSpacePt iwspt = new ImageWorkSpacePt(ipt.getX(), ipt.getY());
        ScreenPt pt = _currentPlot.getScreenCoords(iwspt);
        showReadout(pt, ipt, false);
    }

    public void updateReadout(WebPlot plot, ImagePt ipt) {
        ImageWorkSpacePt iwspt = new ImageWorkSpacePt(ipt.getX(), ipt.getY());
        ScreenPt pt = _currentPlot.getScreenCoords(iwspt);
        _currentPlot = plot;
        adjustGrid();
        showReadout(pt, ipt, false);
    }

    public void setValue(int row, int col, String labelText, String valueText) {
        setValue(row, col, labelText, valueText, null, false);

    }

    public void setValue(int row, int col, String labelText, String valueText, boolean valueIsHtml) {
        setValue(row, col, labelText, valueText, null, valueIsHtml);

    }

    public void setValue(int row,
                         int col,
                         String labelText,
                         String valueText,
                         String valueStyle) {

        setValue(row, col, labelText, valueText, valueStyle, false);
    }


    public void setValue(int row,
                         int col,
                         String labelText,
                         String valueText,
                         String valueStyle,
                         boolean valueIsHtml) {
        int rowMin= 1;
        row--;
        rowMin= 0;
        int labelIdx = col * 2;
        int valueIdx = labelIdx + 1;
        int gridRowCount= _grid.getRowCount() + gridWide.getRowCount();

        if (gridRowCount > row) {


            Grid workingGrid= _grid;
            int workingRow= row;
            if (row>=WIDE_MAX_ROWS) {
                workingGrid= gridWide;
                workingRow= row-WIDE_MAX_ROWS;
                if ((gridRowCount-WIDE_MAX_ROWS) < (WIDE_MAX_ROWS)) workingRow++;
            }


            Label label = (Label) workingGrid.getWidget(workingRow, labelIdx);
            Label value = (Label) workingGrid.getWidget(workingRow, valueIdx);

            if (workingRow>=rowMin && label == null) {
                label = new Label(labelText);
                label.addStyleName("perm-readout-label");
                workingGrid.setWidget(workingRow, labelIdx, label);
            }

            if (value == null) {
                value = new Label(valueText);
                value.addStyleName("perm-readout-value");
                workingGrid.setWidget(workingRow, valueIdx, value);
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

    public void setTitle(String valueText, boolean valueIsHtml) {

        if (valueIsHtml) {
            titleLabel.setHTML(valueText);
        } else {
            titleLabel.setText(valueText);
        }

        if (false) { //todo - should I put the title somewhere?
            _grid.setWidget(0, 1, titleLabel);
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
        int col =  _currentHandler.getColumns(_currentPlot);

        if (_currentRows != rows || _currentCols != col) {
            reinitGridSize(rows, col);
        }
    }

    private void reinitGridSize(int rows, int col) {
        _currentRows = rows;
        _currentCols = col;
        int rowMin= 0;
        int rowMax= WIDE_MAX_ROWS;
        _grid.resize(rowMax, col * 2);
        for (int i = rowMin; (i < rowMax); i++) {
            for (int j = 0; (j < col * 2); j += 2) {
                _grid.getCellFormatter().setHorizontalAlignment(i, j, HasHorizontalAlignment.ALIGN_RIGHT);
            }
        }
        gridWide.resize(rows-WIDE_MAX_ROWS, col * 2);
        for (int i=0; (i < rows-WIDE_MAX_ROWS); i++) {
            for (int j = 0; (j < col * 2); j += 2) {
                gridWide.getCellFormatter().setHorizontalAlignment(i, j, HasHorizontalAlignment.ALIGN_RIGHT);
            }
        }
    }

    private void update(int x, int y) {
        int i = _pvList.indexOf(_plotView);
        ((ThumbnailView) _thumbDeck.getWidget(i)).setParentShowingHint(true);
        _currentPlot = _plotView.getPrimaryPlot();
        if (_currentPlot != null) {
            adjustGrid();
            updateReadout(new ScreenPt(x, y));
        }
    }


    private void showReadout(ScreenPt pt, ImagePt ipt, boolean doClear) {
        if (pt==null || ipt==null) return;

        long callID = new Date().getTime();

        boolean minimal= isMinimal(_currentPlot);
        _thumbDeck.setVisible(!minimal);
        _magDeck.setVisible(!minimal);
        arrowDesc.setVisible(!minimal);
        _filePix.setVisible(!minimal);
        _screenPix.setVisible(!minimal);
        _zoomLevel.setVisible(!minimal);

        for (int col = 0; col < _currentCols; col++) {
            for (int row = 0; row < _currentRows; row++) {
                if (doClear) {
                    _currentHandler.computeMouseExitValue(_currentPlot, this,
                                                          row, col);
                } else {
                    _currentHandler.computeMouseValue(_currentPlot, this,
                                                      row, col, ipt, pt, callID);
                    updateScaleDisplay();
                }
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
        float size = (float) _currentPlot.getImagePixelScaleInArcSec() / _currentPlot.getZoomFact();


        String pStr= _nfPix.format(size);

        _screenPix.setHTML(pStr + "\"/&nbsp;screen&nbsp;pix");

        _zoomLevel.setHTML(ZoomUtil.convertZoomToString(_currentPlot.getZoomFact()));
    }

/*    public Side getDisplaySide() {
        return _displaySide;
    }*/




    public void hideMouseReadout() {
        _popupPanel.hide();
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
        _popupPanel.setVisible(false);
        _popupPanel.show();
        updateToCenter();
        _popupPanel.hide();
        _popupPanel.setVisible(true);
        _popupPanel.show();
    }

    void updateToCenter() {
        if (_showing) {
            RootPanel logo= RootPanel.get("irsa-logo");
            int x;
            if (logo!=null) {
                int logoWidth= logo.getOffsetWidth();
                int logoX= logo.getAbsoluteLeft();
                x= logoX+logoWidth+30;
            }
            else {
                int w= _popupPanel.getOffsetWidth();
                int totalW= Window.getClientWidth();
                x= (totalW-w)/2;
                if (x<168) x= 168;
            }
            _popupPanel.setPopupPosition(x+8,1);
        }
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
