package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
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
public class WebMouseReadout implements PropertyChangeListener {


    public enum DisplayMode {Quick, Group}

    public enum Choice {Good, OK, Bad}

    public enum Side {Left, Right, Top, Bottom, IRSA_LOGO}
    private static final int IRSA_LOGO_X= 139;

    private static final int WIDE_MAX_ROWS= 4;
    private static WebDefaultMouseReadoutHandler _defaultHandler = new WebDefaultMouseReadoutHandler();
    private static final boolean INTELLIGENT_CHOICE = false;
    private WebMouseReadoutHandler _currentHandler = _defaultHandler;

    private Map<Widget, String> _styleMap = new HashMap<Widget, String>(30);
    private WebPlot _currentPlot = null;
    private int _currentRows = 0;
    private int _currentCols = 0;
    private WebPlotView _plotView;
    private final PopupPane _popupPanel;
    private Grid _grid = new Grid();
    private Grid gridWide = new Grid();
    private boolean _showing = false;
    private Side _displaySide = Side.Left;
    private Image _lockIcon;
    private Image _unlockIcon;
    private static final NumberFormat _nfPix = NumberFormat.getFormat("#.##");
    private HTML _filePix = new HTML();
    private HTML _screenPix = new HTML();
    private HTML _zoomLevel = new HTML();

    private final DeckPanel _thumbDeck = new DeckPanel();
    private final DeckPanel _magDeck = new DeckPanel();
    private final List<WebPlotView> _pvList = new ArrayList<WebPlotView>(3);
    private int _laterX;
    private int _laterY;
    private ShowReadoutTimer _showReadoutTimer = null;
    private HideReadoutTimer _hideReadoutTimer = null;
    private GwtUtil.ImageButton _lockButton;
    private boolean _dialogLockedUp = false;

    private boolean _enabled = true;
    private DisplayMode _mode = DisplayMode.Quick;
    private final WebPlotView.MouseInfo _mi = new WebPlotView.MouseInfo(new ReadOut(),
                                                                        "Mouse mouse to see plot information");
    private boolean _mayLockOnce = false;
    private boolean _pixelClickLock = false;
    private MarkedPointDisplay _dataConnect = new MarkedPointDisplay();
    private DrawingManager _lockPointDrawMan = null;
    private CheckBox _lockMouCheckBox = new CheckBox("Lock By Click");
    private final boolean wide;
    private final Label titleLabel= new Label();
    private final MouseHandlers mouseHandlers= new MouseHandlers();

    private HTML arrowDesc = new HTML("Arrow: Eq. North & East");


    public WebMouseReadout() { this(false); }

    public WebMouseReadout(boolean wide) {
        super();
        this.wide= wide;

        HorizontalPanel hp = new HorizontalPanel();

        _popupPanel = new PopupPane(null, hp, PopupType.STANDARD, false, false, false, PopupPane.HeaderType.NONE);
        _popupPanel.alignTo(null, PopupPane.Align.DISABLE);
        _popupPanel.setRolldownAnimation(true);
        _popupPanel.setAnimateDown(true);
        _popupPanel.setAnimationEnabled(true);

        _lockIcon = new Image(VisIconCreator.Creator.getInstance().getLocked());
        _unlockIcon = new Image(VisIconCreator.Creator.getInstance().getUnlocked());






        hp.addDomHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                lockDialogOnce();
            }
        }, TouchStartEvent.getType());

        _lockButton = GwtUtil.makeImageButton(_unlockIcon, "Lock readout area to stay showing", new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (_dialogLockedUp) unlockDialog();
                else lockDialog();
            }
        });
        _lockButton.setSize("20px", "20px");

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


        addMouseHandlers(_popupPanel.getPopupPanel(), true);
        addMouseHandlers(hp,true);
        addMouseHandlers(_grid,true);
        addMouseHandlers(gridWide,true);
        addMouseHandlers(fixedDisplay,true);


        if (wide) {
            wideControlArea= new VerticalPanel();
            wideControlArea.add(_lockButton);
            wideControlArea.add(titleLabel);
            GwtUtil.setStyles(titleLabel, "padding", "13px 0 10px 0", "width", "160px");
            if (!BrowserUtil.isTouchInput())  {
                wideControlArea.add(_lockMouCheckBox);
                GwtUtil.setStyles(_lockMouCheckBox, "paddingTop", "15px");
            }

            fixedDisplay.add(_filePix);
            fixedDisplay.add(_screenPix);
            fixedDisplay.add(_zoomLevel);
            GwtUtil.setStyles(fixedDisplay, "width", "100px");


        }
        else {
            fixedDisplay.add(_lockButton);
            fixedDisplay.add(_filePix);
            fixedDisplay.add(_screenPix);
            fixedDisplay.add(_zoomLevel);
            if (!BrowserUtil.isTouchInput())  fixedDisplay.add(_lockMouCheckBox);
        }



        if (wide){
            GwtUtil.setStyles(_filePix, "marginTop", "3px",
                                        "paddingLeft", "1px",
                                        "fontSize", "10px",
                                        "textAlign", "left");
            GwtUtil.setStyles(_screenPix, "padding", "1px 12px 0px 1px",
                                          "fontSize", "10px",
                                          "textAlign", "left");
            GwtUtil.setStyles(_zoomLevel, "padding", "12px 0 3px 0",
                                          "textAlign", "center",
                                          "color", "green",
                                          "fontSize", "9pt");
        }
        else {
            GwtUtil.setStyles(_filePix, "marginTop", "-7px",
                                        "fontSize", "10px",
                                        "textAlign", "center");
            GwtUtil.setStyles(_screenPix, "paddingTop", "5px",
                                          "fontSize", "10px",
                                          "textAlign", "center");
            GwtUtil.setStyles(_zoomLevel, "padding", "2px 0 3px 0",
                                          "textAlign", "center",
                                          "color", "green",
                                          "fontSize", "9pt");

        }

        _lockMouCheckBox.addStyleName("lock-click");
        _filePix.addStyleName("title-font-family");
        _screenPix.addStyleName("title-font-family");
        _zoomLevel.addStyleName("title-font-family");

        GwtUtil.setStyles(_grid, "lineHeight", "1",
                                 "marginLeft", "-8px");

        if (wide) {
            GwtUtil.setStyles(gridWide, "lineHeight", "1");
        }




        VerticalPanel imagePanel = new VerticalPanel();
        if (wide) hp.add(wideControlArea);
        hp.add(fixedDisplay);
        hp.add(_grid);
        hp.add(imagePanel);

        HorizontalPanel decPanel = new HorizontalPanel();
        decPanel.add(_thumbDeck);
        decPanel.setSpacing(2);
        decPanel.add(_magDeck);
        imagePanel.add(decPanel);


        if (wide) {
            hp.add(gridWide);
        }
        else {
            arrowDesc.addStyleName("title-font-family");
            GwtUtil.setStyles(arrowDesc, "fontSize", "10px",
                              "padding", "0 0 0 35px");

            imagePanel.add(arrowDesc);
        }

        GwtUtil.setStyle(_magDeck, "paddingLeft", "5px");
        if (wide) {
            _magDeck.setSize("70px", "70px");
        }
        else {
            _magDeck.setSize("100px", "100px");
        }

        WebEventManager.getAppEvManager().addListener(Name.REGION_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                hideMouseReadout();
            }
        });


        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (BrowserUtil.isTouchInput()) hideMouseReadout();  // tablet resizing only
            }
        });
    }

    DrawingManager getLockPointDrawing() {
        if (_lockPointDrawMan==null) {
            _lockPointDrawMan = new DrawingManager("Clicked Point", _dataConnect);
        }
        return _lockPointDrawMan;
    }


    private void lockDialogOnce() {
        if (_mayLockOnce) {
            lockDialog();
            _mayLockOnce = false;
        }

    }

    private void lockDialog() {
        if (!_dialogLockedUp) {
            _lockButton.setImage(_lockIcon);
            cancelHideTimer();
            _dialogLockedUp = true;
        }
    }

    private void unlockDialog() {
        _lockButton.setImage(_unlockIcon);
        _dialogLockedUp = false;
        if (BrowserUtil.isTouchInput()) hideMouseReadoutLater(1000);
    }

    public void setDisplayMode(DisplayMode mode) {
        _mode = mode;
    }


    public void addPlotView(WebPlotView pv) {
        if (!_pvList.contains(pv)) {
            _plotView = pv;
            _pvList.add(pv);
            int i = _pvList.indexOf(pv);
            if (wide) {
                _magDeck.insert(new MagnifiedView(pv, 70), i);
                _thumbDeck.insert(new ThumbnailView(pv,70), i);
            }
            else {
                _magDeck.insert(new MagnifiedView(pv, 100), i);
                _thumbDeck.insert(new ThumbnailView(pv, 100), i);
            }

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


    public void setEnabled(boolean enabled) {
        _enabled = enabled;
        if (!_enabled) {
            hideMouseReadout();
        }

    }


    public void clear() {
        showReadout(null, null, true);
    }

//    public static WebDefaultMouseReadoutHandler getDefaultHandler() {
//        return _defaultHandler;
//    }

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
        if (wide) {
            row--;
            rowMin= 0;
        }
        int labelIdx = col * 2;
        int valueIdx = labelIdx + 1;
        int gridRowCount= _grid.getRowCount() + (wide? gridWide.getRowCount():0);

        if (gridRowCount > row) {

            Grid workingGrid= _grid;
            int workingRow= row;
            if (wide && row>=WIDE_MAX_ROWS) {
                workingGrid= gridWide;
                workingRow= row-WIDE_MAX_ROWS;
                if ((gridRowCount-WIDE_MAX_ROWS) < (WIDE_MAX_ROWS)) workingRow++;
            }


            Label label = (Label) workingGrid.getWidget(workingRow, labelIdx);
            Label value = (Label) workingGrid.getWidget(workingRow, valueIdx);

            if (workingRow>=rowMin && label == null) {
                label = new Label(labelText);
                label.addStyleName("readout-label");
                workingGrid.setWidget(workingRow, labelIdx, label);
            }

            if (value == null) {
                value = new Label(valueText);
                value.addStyleName("readout-value");
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
            DOM.setInnerHTML(titleLabel.getElement(), valueText);
        } else {
            titleLabel.setText(valueText);
        }

        if (!wide) {
            _grid.setWidget(0, 1, titleLabel);
        }
    }


//=========================================================================
//--------------------- Methods from PlotViewStatusListener ---------------
//=========================================================================

//=========================================================================
//--------------------- Methods from PropertyChangeListener ---------------
//=========================================================================

    public void propertyChange(PropertyChangeEvent ev) {
        introduceNewReadout();
    }

//===================================================================
//------------ Private Methods --------------------------------------
//===================================================================

    private void introduceNewReadout() {
        _currentPlot = _plotView.getPrimaryPlot();
        if (_currentPlot != null) adjustGrid();
        clear();
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
        int rowMin= wide ? 0 : 1;
        int rowMax= wide ? WIDE_MAX_ROWS : rows;
        _grid.resize(rowMax, col * 2);
        for (int i = rowMin; (i < rowMax); i++) {
            for (int j = 0; (j < col * 2); j += 2) {
                _grid.getCellFormatter().setHorizontalAlignment(i, j, HasHorizontalAlignment.ALIGN_RIGHT);
            }
        }
        if (wide) {
            gridWide.resize(rows-WIDE_MAX_ROWS, col * 2);
            for (int i=0; (i < rows-WIDE_MAX_ROWS); i++) {
                for (int j = 0; (j < col * 2); j += 2) {
                    gridWide.getCellFormatter().setHorizontalAlignment(i, j, HasHorizontalAlignment.ALIGN_RIGHT);
                }
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
            if (wide) _filePix.setHTML( ipStr+ "\"/&nbsp;file&nbsp;pix");
            else      _filePix.setHTML( ipStr+ "\"/<br>file&nbsp;pix");
        }
        else {
            _filePix.setHTML("");
        }
        float size = (float) _currentPlot.getImagePixelScaleInArcSec() / _currentPlot.getZoomFact();


        String pStr= _nfPix.format(size);

        if (wide) _screenPix.setHTML(pStr + "\"/&nbsp;screen&nbsp;pix");
        else      _screenPix.setHTML(pStr + "\"/<br>screen&nbsp;pix");

        _zoomLevel.setHTML(ZoomUtil.convertZoomToString(_currentPlot.getZoomFact()));
    }

/*    public Side getDisplaySide() {
        return _displaySide;
    }*/

    public void setDisplaySide(Side side) {
        _displaySide = side;
        if (_showing) displayMouseReadout();
    }

    public void suggestHideMouseReadout() {
        if (!_dialogLockedUp) hideMouseReadout(false);


    }

    public void hideMouseReadoutLater(int millsToHide) {
        cancelShowTimer();
        if (_hideReadoutTimer == null && !_dialogLockedUp) {
            _hideReadoutTimer = new HideReadoutTimer();
            _hideReadoutTimer.schedule(millsToHide);
        }

    }

    public void hideMouseReadout() {
        hideMouseReadout(true);
    }

    public void hideMouseReadout(boolean animation) {
        cancelTimers();
        _popupPanel.setAnimationEnabled(animation);
        _popupPanel.hide();
        _showing = false;
        int i = _pvList.indexOf(_plotView);
        if (i > 0 && i < _thumbDeck.getWidgetCount()) {
            ThumbnailView thumb = (ThumbnailView) _thumbDeck.getWidget(i);
            thumb.setParentShowingHint(false);
        }
    }

    public void displayMouseReadout() {
        displayMouseReadout(0, 0);
    }

    public void displayMouseReadoutLater(int mouseX, int mouseY) {
        _laterX = mouseX;
        _laterY = mouseY;
        if (_showReadoutTimer == null) {
            _showReadoutTimer = new ShowReadoutTimer();
            _showReadoutTimer.schedule(300);
        }
    }


    public void displayMouseReadout(int mouseX, int mouseY) {

        boolean alreadyUp = _showing;
        cancelShowTimer();
        mouseX = mouseX - _plotView.getScrollX();
        mouseY = mouseY - _plotView.getScrollY();
        if (!alreadyUp) _popupPanel.getPopupPanel().setVisible(false);
        _popupPanel.setAnimationEnabled(false);
        if (!alreadyUp) _popupPanel.show();

        ScreenPt pt;

        switch (_displaySide) {
            case Left:
            case Right:
            case IRSA_LOGO:
                pt = computeLeftRight(mouseX, mouseY, _displaySide, true);
                break;
            case Top:
            case Bottom:
                pt = computeTopBottom(mouseX, mouseY, _displaySide, true);
                break;
            default:
                WebAssert.fail("unknown case");
                return;


        }


        _popupPanel.setPopupPosition(pt.getIX(), pt.getIY());
        if (alreadyUp) {
            _popupPanel.setAnimationEnabled(false);
        } else {
            _popupPanel.hide();
            _popupPanel.getPopupPanel().setVisible(true);
            _popupPanel.setAnimationEnabled(true);
        }
        _popupPanel.show();

        _showing = true;
        _mayLockOnce = BrowserUtil.isTouchInput();
        _popupPanel.setAnimationEnabled(true);
//        _thumbnail.setParentShowingHint(true);


    }

    int getContentHeight() {
        int retval= 0;
        if (_popupPanel.isVisible()) {
            retval= _popupPanel.getWidget().getOffsetHeight();
        }
        else {
            _popupPanel.getPopupPanel().setVisible(false);
            _popupPanel.setAnimationEnabled(false);
            _popupPanel.show();
            retval= _popupPanel.getWidget().getOffsetHeight();
            _popupPanel.hide();
            _popupPanel.setAnimationEnabled(true);
            _popupPanel.getPopupPanel().setVisible(true);

        }
        return retval;

    }

    private ScreenPt computeLeftRight(int mouseX, int mouseY, Side side, boolean choose) {

        WebAssert.argTst((side == Side.Left || side == Side.Right || side == Side.IRSA_LOGO),
                          "Side must be left, right, or IRSA_LOGO");


        int x = 0;
        Side otherSide;
        int popWidth = _popupPanel.getPopupPanel().getOffsetWidth();
        int popHeight = _popupPanel.getPopupPanel().getOffsetHeight();

        if (side == Side.Left) {
            otherSide = Side.Right;

            switch (_mode) {
                case Quick:
                    x = _plotView.getAbsoluteLeft() - popWidth;
                    break;
                case Group:
                    x = 3;
                    break;
                default:
                    WebAssert.argTst(false, "not a supported mode");
                    break;
            }


        } else {
            otherSide = Side.Left;

            switch (_mode) {
                case Quick:
                    x = _plotView.getAbsoluteLeft() + _plotView.getOffsetWidth();
                    break;
                case Group:
                    x = Window.getClientWidth() - popWidth;
                    if (side==Side.IRSA_LOGO && x>IRSA_LOGO_X) {
                        x= IRSA_LOGO_X;
                    }
                    break;
                default:
                    WebAssert.argTst(false, "not a supported mode");
                    break;
            }
        }

        int y = 0;
        switch (_mode) {
            case Quick:
                y = _plotView.getAbsoluteTop();
                break;
//            case Group : y= Window.getClientWidth()- popHeight-3; break;
            case Group:
                y = 0;
                break;
            default:
                WebAssert.argTst(false, "not a supported mode");
                break;
        }

        ScreenPt op1Pt = adjustXY(x, y);
        x = op1Pt.getIX();

        ScreenPt retval = op1Pt;

        if (INTELLIGENT_CHOICE) {
            ScreenPt op2Pt = null;
            ScreenPt op3Pt = null;

            Choice op2Choice = Choice.Bad;
            Choice op3Choice = Choice.Bad;
            Choice op1Choice = computeChoice(op1Pt, mouseX, mouseY);

            if (op1Choice != Choice.Good) {
                y = _plotView.getAbsoluteTop() + _plotView.getOffsetHeight();
                op2Pt = adjustXY(x, y);

                op2Choice = computeChoice(op2Pt, mouseX, mouseY);
            }

            if (op1Choice != Choice.Good && op2Choice != Choice.Good) {
                y = _plotView.getAbsoluteTop() - popHeight;
                op3Pt = adjustXY(x, y);

                op3Choice = computeChoice(op3Pt, mouseX, mouseY);
            }


            retval = chooseBest(op1Choice, op1Pt,
                                op2Choice, op2Pt,
                                op3Choice, op3Pt);

            if (retval == null && choose) {
                retval = computeLeftRight(mouseX, mouseY, otherSide, false);
                if (retval == null) retval = op1Pt;
            }
        }


        return retval;
    }


    private ScreenPt computeTopBottom(int mouseX, int mouseY, Side side, boolean choose) {

        WebAssert.argTst((side == Side.Top || side == Side.Bottom), "Side must be Top or Bottom");


        Side otherSide;
        int y = 0;
        int popHeight = _popupPanel.getPopupPanel().getOffsetHeight();
        if (side == Side.Top) {
            otherSide = Side.Bottom;

            switch (_mode) {
                case Quick:
                    y = _plotView.getAbsoluteTop() - popHeight;
                    break;
                case Group:
                    y = 3;
                    break;
                default:
                    WebAssert.argTst(false, "not a supported mode");
                    break;
            }


        } else {
            otherSide = Side.Top;


            switch (_mode) {
                case Quick:
                    y = _plotView.getAbsoluteTop() + _plotView.getOffsetHeight();
                    break;
                case Group:
                    y = Window.getClientHeight() - popHeight - 3;
                    break;
                default:
                    WebAssert.argTst(false, "not a supported mode");
                    break;
            }


        }

        int x = 0;

        switch (_mode) {
            case Quick:
                x = _plotView.getAbsoluteLeft();
                break;
            case Group:
                x = 3;
                break;
            default:
                WebAssert.argTst(false, "not a supported mode");
                break;
        }

        ScreenPt op1Pt = adjustXY(x, y);


        ScreenPt op2Pt = null;
        ScreenPt op3Pt = null;

        Choice op2Choice = Choice.Bad;
        Choice op3Choice = Choice.Bad;
        Choice op1Choice = computeChoice(op1Pt, mouseX, mouseY);

        if (op1Choice != Choice.Good) {
            x = _plotView.getAbsoluteLeft() + _plotView.getOffsetWidth();
            op2Pt = adjustXY(x, y);

            op2Choice = computeChoice(op2Pt, mouseX, mouseY);
        }

        if (op1Choice != Choice.Good && op2Choice != Choice.Good) {
            int popWidth = _popupPanel.getPopupPanel().getOffsetWidth();
            x = _plotView.getAbsoluteLeft() - popWidth;
            op3Pt = adjustXY(x, y);

            op3Choice = computeChoice(op3Pt, mouseX, mouseY);
        }


        ScreenPt retval = chooseBest(op1Choice, op1Pt,
                                     op2Choice, op2Pt,
                                     op3Choice, op3Pt);

        if (retval == null && choose) {
            retval = computeLeftRight(mouseX, mouseY, otherSide, false);
            if (retval == null) retval = op1Pt;
        }


        return retval;
    }


    private ScreenPt chooseBest(Choice op1Choice, ScreenPt op1Pt,
                                Choice op2Choice, ScreenPt op2Pt,
                                Choice op3Choice, ScreenPt op3Pt) {
        ScreenPt retval = null;
        switch (op1Choice) {
            case Good:
                retval = op1Pt;
                break;
            case OK:
                switch (op2Choice) {
                    case Good:
                        retval = op2Pt;
                        break;
                    case OK:
                        retval = (op3Choice == Choice.Good) ? op3Pt : op2Pt;
                        break;
                    case Bad:
                        retval = (op3Choice == Choice.Good) ? op3Pt : op1Pt;
                        break;
                }
                break;
            case Bad:
                switch (op2Choice) {
                    case Good:
                        retval = op2Pt;
                        break;
                    case OK:
                        retval = (op3Choice == Choice.Good) ? op3Pt : op2Pt;
                        break;
                    case Bad:
                        retval = (op3Choice == Choice.Good) ? op3Pt : null;
                        break;
                }
                break;
        }
        return retval;
    }


    private Choice computeChoice(ScreenPt pt, int mouseX, int mouseY) {
        return computeChoice(isCoveringPlot(pt.getIX(), pt.getIY()),
                             isOverMouse(pt.getIX(), pt.getIY(), mouseX, mouseY));

    }


    private Choice computeChoice(boolean coversPlot, boolean overMouse) {
        Choice retval = Choice.Good;
        if (overMouse) retval = Choice.Bad;
        else if (coversPlot) retval = Choice.OK;
        return retval;
    }


    private ScreenPt adjustXY(int x, int y) {
        int popWidth = _popupPanel.getPopupPanel().getOffsetWidth();
        int popHeight = _popupPanel.getPopupPanel().getOffsetHeight();
        int bWidth = Window.getClientWidth();
        int bHeight = Window.getClientHeight();
        int sx = Window.getScrollLeft();
        int sy = Window.getScrollTop();
        if (x + popWidth > bWidth) x = (bWidth - popWidth) + sx;
        if (y + popHeight > bHeight) y = (bHeight - popHeight) + sy;

        if (x < sx) x = sx;
        if (y < sy) y = sy;
        return new ScreenPt(x, y);

    }


    private boolean isCoveringPlot(int x, int y) {
        int width = _popupPanel.getPopupPanel().getOffsetWidth();
        int height = _popupPanel.getPopupPanel().getOffsetHeight();
        int px = _plotView.getAbsoluteLeft();
        int py = _plotView.getAbsoluteTop();
        int pw = Math.min(_plotView.getScrollWidth(), _plotView.getPrimaryPlot().getScreenWidth());
        int ph = Math.min(_plotView.getScrollHeight(), _plotView.getPrimaryPlot().getScreenHeight());
        return VisUtil.intersects(x, y, width, height, px, py, pw, ph);
    }


    private boolean isOverMouse(int x, int y, int mouseX, int mouseY) {
        int width = _popupPanel.getPopupPanel().getOffsetWidth();
        int height = _popupPanel.getPopupPanel().getOffsetHeight();
        int mx = _plotView.getAbsoluteLeft() + mouseX;
        int my = _plotView.getAbsoluteTop() + mouseY;
        return (mx >= x && mx <= x + width && my >= y && my <= y + height);
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

    private void cancelTimers() {
        cancelShowTimer();
        cancelHideTimer();
    }


    private void cancelHideTimer() {
        if (_hideReadoutTimer != null) {
//            GwtUtil.showDebugMsg("cancel",5,90);
            _hideReadoutTimer.cancel();
            _hideReadoutTimer = null;
        }
    }

    private void cancelShowTimer() {
        if (_showReadoutTimer != null) {
            _showReadoutTimer.cancel();
            _showReadoutTimer = null;
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
//                int x= ev.getX();
//                int y= ev.getY();
            int x = spt.getIX();
            int y = spt.getIY();
            if (_enabled && isOverPlot(x, y)) {
                cancelHideTimer();
                if (updateDisplay) {
                    update(x, y);
                    if (_pixelClickLock) {
                        MagnifiedView mv = (MagnifiedView) _magDeck.getWidget(_pvList.indexOf(pv));
                        if (mv != null) mv.update(spt);
                    }
                }
                if (!_showing || oldPV != _plotView) {
                    switch (_mode) {
                        case Quick:
                            displayMouseReadout(x, y);
                            break;
                        case Group:
                            if (fromTouch || _showing) {
                                displayMouseReadout(x, y);
                            } else {
                                displayMouseReadoutLater(x, y);
                            }
                            break;
                        default:
                            WebAssert.argTst(false, "not a supported mode");
                            break;
                    }
                }
            }
        }

    }

    private class ReadOut extends WebPlotView.DefMouseAll {


        @Override
        public void onMouseOut(WebPlotView pv) {
            switch (_mode) {
                case Quick:
                    hideMouseReadout();
                    break;
                case Group:
                    hideMouseReadoutLater(2000);
                    break;
                default:
                    WebAssert.argTst(false, "not a supported mode");
                    break;
            }
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
                try {
                    _dataConnect.setPoint(pv.getPrimaryPlot().getWorldCoords(spt), pv.getPrimaryPlot());
                    getLockPointDrawing().redraw();
                } catch (ProjectionException e) {
                    // ignore
                }
            }
        }

        @Override
        public void onTouchEnd(WebPlotView pv) {
            hideMouseReadoutLater(2000);
        }
    }

    private class ShowReadoutTimer extends Timer {
        @Override
        public void run() {
            _showReadoutTimer.cancel();
            _showReadoutTimer = null;
            displayMouseReadout(_laterX, _laterY);
        }

    }

    private class HideReadoutTimer extends Timer {
        @Override
        public void run() {
            _hideReadoutTimer.cancel();
            _hideReadoutTimer = null;
            hideMouseReadout();
        }
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

    private void addMouseHandlers(Widget w, boolean includeHide)  {
        w.addDomHandler(mouseHandlers, MouseOverEvent.getType());
        w.addDomHandler(mouseHandlers, MouseMoveEvent.getType());

        if (includeHide) {
            w.addDomHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent ev) {
                    int x= ev.getScreenX();
                    int y= ev.getScreenY();
                    int py= _popupPanel.getPopupPanel().getAbsoluteTop();
                    int px= _popupPanel.getPopupPanel().getAbsoluteLeft();
                    int w= _popupPanel.getPopupPanel().getOffsetWidth();
                    int h= _popupPanel.getPopupPanel().getOffsetHeight();
                    if ( x<px || x>px+w-1 || y<py || y>py+h-1 ) {
                        hideMouseReadoutLater(700);
//                        GwtUtil.showDebugMsg("hiding: x="+x+",y="+y+",px="+px+",py="+py+",w="+w+",h="+h,5,90);
                    }
                    else {
//                        GwtUtil.showDebugMsg("x="+x+",y="+y+",px="+px+",py="+py+",w="+w+",h="+h,5,90);
                    }
                }
            }, MouseOutEvent.getType());
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


    private class MouseHandlers implements MouseOverHandler, MouseMoveHandler {
            public void onMouseOver(MouseOverEvent ev) { cancelHideTimer(); }
            public void onMouseMove(MouseMoveEvent ev) { cancelHideTimer(); }
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
