package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.IconMenuItem;
import edu.caltech.ipac.firefly.ui.imageGrid.BasicImageGrid;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */



/**
 * @author Trey Roby
 */
public class PlotWidgetGroup implements Iterable<MiniPlotWidget> {


    public enum MBarType { ImageOp, LockOp, NoImageOp, Float}
    public enum BarPopup { Inline, PopupOut }


    private static final Map<String,PlotWidgetGroup> _groups= new HashMap<String,PlotWidgetGroup>(5);
    private static int _cnt=0;



    private List<MiniPlotWidget> _mpwList= new ArrayList<MiniPlotWidget>(1);

    private boolean _initialized= false;
    private boolean _lockRelated= false;
    private MPWListener _pvListener;
    private List<WebPlotView> _ignoreList= new LinkedList<WebPlotView>(); // used to synchronize scrolling
    private Timer _clearIgnoreList= new ClearIgnoreList();      // used to synchronize scrolling

    private PlotRelatedPanel _extraPanels[];
    private boolean _enableScrollCheck= true;
    private boolean _dragging= false;
    private boolean _enablePdfDownload = false;
    private boolean _enableShowDrawingLayers = false;
    private boolean _enableChecking = true;
    private MiniPlotWidget _lastPoppedOutItem= null;
    private FloatingVisBar _floatingToolbar;
    private FloatingStatusBar _floatingStatusBar=null;
    private boolean _allCheck= false;

    private final String _groupName;
    private Map<MiniPlotWidget, HandlerRegistration> _hReg= new HashMap<MiniPlotWidget, HandlerRegistration>(11);
    private BasicImageGrid _imageGrid= null;
    private int _gridPopoutColumns = 4;
    private VisUtil.FullType _gridPopoutZoomType = VisUtil.FullType.ONLY_WIDTH;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private PlotWidgetGroup(String groupName) { _groupName= groupName; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static PlotWidgetGroup makeSingleUse() {
            _cnt++;
            return new PlotWidgetGroup("UNIQUE_GROUP"+_cnt);
    }




    public void setLastPoppedOut(MiniPlotWidget mpw) {
        if (mpw==null && contains(mpw)) { _lastPoppedOutItem= mpw; }
    }

    public MiniPlotWidget getLastPoppedOut() { return _lastPoppedOutItem; }

    public String getName() { return _groupName; }

    public void updateFloatingStatus(String status, Widget alignWidget) {
        if (_floatingStatusBar==null) {
            _floatingStatusBar = new FloatingStatusBar(this, alignWidget);
        }
        _floatingStatusBar.updateStatus(status);
        _floatingStatusBar.show();
    }

    public void enableFloatVisBar(Widget alignWidget) {
        if (_floatingToolbar==null) {
            _floatingToolbar= new FloatingVisBar(this, alignWidget);
        }
        else {
            _floatingToolbar.updateAlignWidget(alignWidget);

        }
        _floatingToolbar.setAllCheckNoEvent(_allCheck);
        _floatingToolbar.show();
        for(MiniPlotWidget mpw : this)  mpw.clearPlotToolbar();

    }

    public void setFloatingToolbarShowing(boolean s) {
        if (_floatingToolbar!=null) {
            if (s) _floatingToolbar.show();
            else _floatingToolbar.hide();
        }
    }


    public boolean isFloatVisBar() { return _floatingToolbar!=null; }

    public static PlotWidgetGroup getShared(String name) {
        PlotWidgetGroup retval;
        if (_groups.containsKey(name)) {
            retval= _groups.get(name);
        }
        else {
            retval= new PlotWidgetGroup(name);
            _groups.put(name,retval);
        }
        return retval;
    }

    public int size() { return _mpwList.size(); }

    public Iterator<MiniPlotWidget> iterator() { return getAllActive().iterator(); }

    void autoTearDownPlots() {
        if (_floatingToolbar!=null) _floatingToolbar.hide();
        List<MiniPlotWidget> l= new ArrayList<MiniPlotWidget>(_mpwList);
        for(MiniPlotWidget mpw : l)  {
            if (mpw.isAutoTearDown()) removeMiniPlotWidget(mpw);
        }

    }


    public void setExtraPanels(PlotRelatedPanel ptUI[]) { _extraPanels= ptUI;}
    public PlotRelatedPanel[] getExtraPanels() { return _extraPanels;}

    public void setDragging(boolean dragging) {
        if (!dragging && _dragging) {
            for (MiniPlotWidget mpw : _mpwList)  {
                if (mpw.isInit()) mpw.getPlotView().scrollDragEnded();
            }
        }
        _dragging= dragging;
    }

    public void setAllChecked(boolean checked) {
        setAllChecked(checked,true);
    }

    public boolean getEnablePdfDownload() { return _enablePdfDownload; }
    public void setEnablePdfDownload(boolean b) {
        _enablePdfDownload = b;
    }

    public boolean getShowDrawingLayers() {return _enableShowDrawingLayers;}
    public void setShowDrawingLayers(boolean b) {
        _enableShowDrawingLayers = b;
    }

    public boolean getEnableChecking() { return _enableChecking;}
    public void setEnableChecking(boolean b) {
        _enableChecking = b;
    }

    void setAllChecked(boolean checked, boolean updateUI) {
        _allCheck= checked;
        if (updateUI &&_floatingToolbar!=null) _floatingToolbar.setAllCheckNoEvent(_allCheck);
        for(MiniPlotWidget mpw : this)  mpw.setChecked(checked);

        WebEvent<Boolean> wev= new WebEvent<Boolean>(this, Name.ALL_CHECKED_PLOT_CHANGE, checked);
        AllPlots.getInstance().getEventManager().fireEvent(wev);
    }


    public void checkboxForcedOffAllChecked() {
        _allCheck= false;
//        WebEvent<Boolean> wev= new WebEvent<Boolean>(this, Name.ALL_CHECKED_PLOT_CHANGE, false);
//        AllPlots.getInstance().getEventManager().fireEvent(wev);
        if (_floatingToolbar!=null) {
            _floatingToolbar.setAllCheckNoEvent(false);
        }
    }


    public boolean isAllChecked() { return _allCheck;  }

    /**
     * add a new MiniPlotWidget.
     * don't call this method until MiniPlotWidget.getPlotView() will return a non-null value
     * @param mpw the MiniPlotWidget to add
     */
    void addMiniPlotWidget(MiniPlotWidget mpw) {
        _mpwList.add(mpw);
    }

    void initMiniPlotWidget(MiniPlotWidget mpw) {
        AllPlots.getInstance().addMiniPlotWidget(mpw);

        init();

        WebPlotView pv= mpw.getPlotView();
        pv.getEventManager().addListener(_pvListener);
        HandlerRegistration h= pv.addScrollHandler(new ScrollWatcher(mpw));
        _hReg.put(mpw,h);
    }

    void removeMiniPlotWidget(MiniPlotWidget mpw) {
        _mpwList.remove(mpw);
        if (Vis.isInitialized()) {
            AllPlots.getInstance().removeMiniPlotWidget(mpw);
            WebPlotView pv= mpw.getPlotView();
            if (pv!=null) pv.getEventManager().removeListener(_pvListener);
            if (_hReg.containsKey(mpw)) _hReg.get(mpw).removeHandler();
        }
    }


    public List<MiniPlotWidget> getAllActive() {
        List<MiniPlotWidget> retval= new ArrayList<MiniPlotWidget>(_mpwList.size());
        for(MiniPlotWidget mpw : _mpwList) {
            if (mpw.isInit()) retval.add(mpw);
        }
        return retval;
    }


    public List<MiniPlotWidget> getAllInactive() {
        List<MiniPlotWidget> retval= new ArrayList<MiniPlotWidget>(_mpwList.size());
        for(MiniPlotWidget mpw : _mpwList) {
            if (!mpw.isInit()) retval.add(mpw);
        }
        return retval;
    }

    public List<MiniPlotWidget> getAll() { return _mpwList; }


    public boolean contains(WebPlot p) {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return pv!=null && pv.contains(p);
    }
    public boolean contains(MiniPlotWidget mpw) { return _mpwList.contains(mpw); }

    public void setImageGrid(BasicImageGrid imageGrid) {
        _imageGrid = imageGrid;
    }

    public BasicImageGrid getImageGrid() {
        return _imageGrid;
    }

    public int getGridPopoutColumns() { return _gridPopoutColumns; }
    public void setGridPopoutColumns(int cols) { _gridPopoutColumns=cols; }

    public VisUtil.FullType getGridPopoutZoomType() { return _gridPopoutZoomType; }
    public void setGridPopoutZoomType(String gridPopoutZoomType) {
        _gridPopoutZoomType= VisUtil.FullType.valueOf(gridPopoutZoomType);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void init() {
        if (!_initialized) {
            _initialized= true;
            _pvListener= new MPWListener();
        }
    }

    public boolean getLockRelated() { return _lockRelated; }

    public void setLockRelated(boolean lock) {
        _lockRelated= lock;
        AllPlots.getInstance().updateUISelectedLook();
    }



    private MenuItem makeZoomLevel(BarPopup popType) {
        String padLeft;
        String padBottom;
//        if (_shared) {
            if (popType==BarPopup.Inline) {
                padLeft= "6px";
                padBottom= "5px";
            }
            else {
                padLeft= "5px";
                padBottom= "3px";
            }

//        }
//        else {
//            padLeft= "25px";
//            padBottom= "7px";
//        }
        MenuItem zoomLevel= new MenuItem("2x",new Command() { public void execute() { } });
        GwtUtil.setStyles(zoomLevel.getElement(), "borderColor", "transparent",
                                                   "background", "transparent",
                                                   "paddingLeft", padLeft,
                                                   "color",      "#49a344",
                                                   "paddingBottom", padBottom);
        return zoomLevel;
    }

    private MenuItem makeHelp(BarPopup popType) {
        IconMenuItem help= new IconMenuItem(HelpManager.makeHelpImage(),
                                            new Command() {
                                                public void execute() {
                                                    Application.getInstance().getHelpManager().showHelpAt("visualization");
                                                }
                                            }, false);
//        if (_shared) {
            if (popType==BarPopup.PopupOut) {
                GwtUtil.setStyles(help.getElement(), "paddingLeft",   "7px",
                                  "paddingBottom", "3px",
                                  "borderColor", "transparent");
            }
            else {
                GwtUtil.setStyles(help.getElement(),
                                  "padding", "0 0 2px 10px",
                                  "borderColor",   "transparent");

            }
//        }
//        else {
//            GwtUtil.setStyles(help.getElement(), "paddingLeft",   "10px",
//                                                 "paddingBottom", "5px",
//                                                 "paddingTop",    "3px",
//                                                 "borderColor",   "transparent");
//
//        }
        return help;
    }

    void updateScrollPositions(MiniPlotWidget originMpw) {
        WebPlotView pv= originMpw.getPlotView();
        if (pv!=null && _mpwList.size()>1 && _lockRelated) {
            if (_ignoreList.contains(pv)) {
                _ignoreList.remove(pv);
                return;
            }
            _enableScrollCheck= false;
            int sx= pv.getScrollX();
            int sy= pv.getScrollY();
            int width= pv.getPrimaryPlot().getScreenWidth();
            int height= pv.getPrimaryPlot().getScreenHeight();
            int viewW= pv.getScrollWidth();
            int viewH= pv.getScrollHeight();

            float percentX= (float)(sx+viewW/2) / (float)width;
            float percentY= (float)(sy+viewH/2) / (float)height;

            for (MiniPlotWidget mpw : _mpwList) {
                if (mpw!=originMpw && mpw.isInit()) {
                    pv= mpw.getPlotView();
                    if (pv!=null && pv.getPrimaryPlot()!=null) {
                        viewW= pv.getScrollWidth();
                        viewH= pv.getScrollHeight();
                        int testWidth= pv.getPrimaryPlot().getScreenWidth();
                        int testHeight= pv.getPrimaryPlot().getScreenHeight();

                        sx= (int)(testWidth*percentX - viewW/2);
                        sy= (int)(testHeight*percentY - viewH/2);

                        if (pv.getScrollX()!=sx || pv.getScrollY()!=sy) {
                            pv.setScrollXY(new ScreenPt(sx,sy), _dragging);
                            _ignoreList.add(pv);
                        }

                    }


                }
            }
            if (_ignoreList.size()>0) {
                _clearIgnoreList.cancel();
                _clearIgnoreList.schedule(200);
            }
            _enableScrollCheck= true;

        }
    }


//======================================================================
//------------------ Package Methods                    ----------------
//------------------ should only be called by           ----------------
//------------------ by the ServerTask classes in this  ----------------
//------------------ package that are called for plotting --------------
//======================================================================


    private class MPWListener implements WebEventListener {

        public void eventNotify(WebEvent ev) {
            Name n= ev.getName();
            if (n==Name.REPLOT       || n==Name.PLOT_ADDED ||
                n==Name.PLOT_REMOVED || n==Name.PRIMARY_PLOT_CHANGE) {
                _ignoreList.clear();
            }
        }
    }



    private class ScrollWatcher implements ScrollHandler {

        private MiniPlotWidget _mpw;
        ScrollWatcher(MiniPlotWidget mpw) { _mpw= mpw; }


        public void onScroll(ScrollEvent ev) {
            if (_enableScrollCheck) updateScrollPositions(_mpw);
        }
    }

    private class ClearIgnoreList extends Timer {
        @Override
        public void run() {
            _ignoreList.clear(); }
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