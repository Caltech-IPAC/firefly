package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.dd.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Nov 16, 2009
 * Time: 9:12:47 AM
 */


/**
 * @author Trey Roby
 */
public class WebLayerItem implements HasValueChangeHandlers<String> {

                              // temporary util Preferences support session only prefs
    private static final Map<String,String> _prefMap= new HashMap<String, String>(50);


    private static final Map<String, UICreator> _additionUIMaker= new HashMap<String, UICreator>(10);
    private WebPlotView _pv;
    private final String _id;
    private String _subID = null;
    private String _title;
    private String _help;
    private Drawer _drawer;
    private WebPlotView.MouseInfo _mouseInfo;
    private boolean _active = true;
    private Object _workerObj;
    private boolean _groupByIDorTitle;
    private String _enablePrefKey;
    private DrawingManager drawingManager= null;
    private HandlerManager hManger= null;
    private final PrintableOverlay _printMaker;
    private boolean canDoRegion= true;

    public WebLayerItem(String id,
                        String title,
                        String help,
                        Drawer drawer,
                        WebPlotView.MouseInfo mouseInfo,
                        String enablePrefKey,
                        PrintableOverlay printMaker) {
        _id= id;
        _title= title;
        _help= help;
        _drawer= drawer;
        _mouseInfo=  mouseInfo;
        _enablePrefKey= enablePrefKey;
        _printMaker= printMaker;
        if (_enablePrefKey!=null && _prefMap.containsKey(_enablePrefKey)) {
            boolean v= Boolean.parseBoolean(_prefMap.get(_enablePrefKey));
            setOneVisible(v);
        }
    }

    public void setWorkerObj(Object obj) { _workerObj= obj; }
    public Object getWorkerObj() { return _workerObj; }

    public boolean isCanDoRegion() {
        return canDoRegion;
    }

    public void setCanDoRegion(boolean canDoRegion) {
        this.canDoRegion = canDoRegion;
    }

    public List<Region> asRegionList() {
        List<Region> retval;
        if (_drawer.getData()!=null) {
            retval= new ArrayList<Region>(_drawer.getData().size()*2);
            WebPlot plot= _pv.getPrimaryPlot();
            if (canDoRegion && plot!=null) {
                AutoColor ac= new AutoColor(plot.getColorTableID(),_drawer.getDefaultColor());
                for(DrawObj obj : _drawer.getData()) {
                    retval.addAll(obj.toRegion(plot,ac));
                }
            }
        }
        else {
            retval= Collections.emptyList();
        }
        return retval;
    }

    public PrintableOverlay getPrintableOverlay() { return _printMaker; }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        if (hManger==null) {
            hManger= new HandlerManager(this);
        }
        return hManger.addHandler(ValueChangeEvent.getType(),h);
    }

    public void setDrawingManager(DrawingManager drawingManager) {
        if (this.drawingManager!=drawingManager) {
            this.drawingManager=drawingManager;
            boolean v= _enablePrefKey==null;
            if (_prefMap.containsKey(_enablePrefKey))  v= Boolean.parseBoolean(_prefMap.get(_enablePrefKey));
            setOneVisible(v);
        }
    }

    public DrawingManager getDrawingManager() { return drawingManager; }

    /**
     * if true then all the WebLayerItem with the same title (as well as ID) will be treated together of actions such
     * as setVisible or changing color
     * @param g true will tread title & id as a group, false will tread only ID as a group
     */
    public void setGroupByTitleOrID(boolean g) {_groupByIDorTitle= g;}

    public String getID() { return _id;}


    /**
     * Sub ID is anything to more identify the item
     * @param queryID
     */
    public void setSubID(String queryID) { _subID = queryID; }
    public String getSubID() { return _subID; }
    public void setTitle(String title) {
        _title= title;
        ValueChangeEvent.fire(this, title);
    }


    public String getTitle() { return _title; }
    public String getHelp() { return _help; }
    public void setHelp(String helpText) { _help= helpText; }

    public void setActive(boolean active) { _active = active; }
    public boolean isActive() { return _active; }

    public Drawer getDrawer() { return _drawer; }


    public void fireEvent(GwtEvent<?> event) {
        if (hManger!=null)  hManger.fireEvent(event);
    }


    public List<WebLayerItem> getAllWithMatchingID() {
        List<WebLayerItem>retList= getAllWithMatchingID(getID());
        if (retList.size()==0) retList.add(this);
        if (_groupByIDorTitle) {
            List<MiniPlotWidget> mpwList= AllPlots.getInstance().getActiveList();
            for(MiniPlotWidget mpw : mpwList) {
                WebLayerItem wl= mpw.getPlotView().getItemByTitle(_title);
                if (wl!=null && !retList.contains(wl)) {
                    retList.add(wl);
                }
            }
        }
        return retList;
    }

    public static List<WebLayerItem> getAllWithMatchingID(String id) {
        List<WebLayerItem> retList= new ArrayList<WebLayerItem>(10);
        for(MiniPlotWidget mpwItem : AllPlots.getInstance().getActiveList()) {
            WebLayerItem wl= mpwItem.getPlotView().getItemByID(id);
            if (wl!=null) retList.add(wl);
        }
        return retList;
    }

    public void setVisible(final boolean v) {

        Vis.init(new Vis.InitComplete() {
            public void done() {

                if (_enablePrefKey!=null)   _prefMap.put(_enablePrefKey, v+"");

                for (WebLayerItem wl : getAllWithMatchingID()) {
                    wl.setOneVisible(v);
                }
            }
        });
    }

    private void setOneVisible(boolean v) {
        if (_drawer!=null) {
            if (drawingManager==null || !drawingManager.isDataLoadingAsync()) {
                if (_drawer.isVisible()!=v) { // normal visible change case
                    changeVisibility(v);
                }
            }
            else {
                if (v) {
                    drawingManager.requestLoad(new LoadCallback() {
                        public void loaded() {
                            _drawer.setVisible(false);
                            changeVisibility(true);
                        }
                    });
                }
                else {
                    changeVisibility(false);
                    drawingManager.disableLoad();
                }

            }
        }
    }

    private void changeVisibility(boolean v) {
        _drawer.setVisible(v);
        fireVisibleChange(v);
        if (_mouseInfo!=null)  _mouseInfo.setEnabled(v);
    }

    private void fireVisibleChange(boolean v) {
        if (_drawer!=null && _pv!=null) {
            WebEvent<Boolean> ev= new WebEvent<Boolean>(this, Name.LAYER_ITEM_VISIBLE, v);
            _pv.fireEvent(ev);
        }
    }

    public boolean isVisible() { return _drawer!=null ? _drawer.isVisible() : false ; }

    public String getColor() {
        return _drawer!=null ? _drawer.getDefaultColor() : Drawer.DEFAULT_DEFAULT_COLOR;
    }

    public String getAutoColorInterpreted() {
        AutoColor ac= new AutoColor(_pv.getPrimaryPlot().getColorTableID(),_drawer.getDefaultColor());
        String c= ac.getColor(_drawer.getDefaultColor());
        if (!c.startsWith("#") && GwtUtil.isHexColor(c)) c= "#" + c;
        return c;
    }

    public void setPlotView(WebPlotView pv) { _pv= pv; }
    public WebPlotView getPlotView() { return _pv; }


    public void setColor(final String c) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                for(WebLayerItem wl : getAllWithMatchingID()) {
                    if (wl._drawer!=null) {
                        wl._drawer.setDefaultColor(c);
                        ValueChangeEvent.fire(WebLayerItem.this, c);
                    }
                }
            }
        });

    }

//    public void remove() {}

    public boolean getNeedsExtraUI() { return _additionUIMaker.containsKey(getID()); }
    public Widget makeExtraUI() {
        UICreator c= _additionUIMaker.get(getID());
        return c!=null ? c.makeExtraUI(this) : null;
    }

    public boolean getHasColorSetting() {
        UICreator c= _additionUIMaker.get(getID());
        return c!=null ? c.getHasColorSetting() : true;
    }
    public boolean getHasDelete() {
        UICreator c= _additionUIMaker.get(getID());
        return c!=null ? c.getHasDelete() : false;
    }

    public boolean getHasDetails() {
        UICreator c= _additionUIMaker.get(getID());
        return c!=null ? c.getHasDetails() : false;
    }

    public Widget makeUserDefinedColUI() {
        UICreator c= _additionUIMaker.get(getID());
        return c!=null ? c.makeExtraColumnWidget(this) : null;
    }

    public void suggestDelete() {
        UICreator c= _additionUIMaker.get(getID());
        if (c!=null) c.delete(this);
    }
    public void showDetails() {
        UICreator c= _additionUIMaker.get(getID());
        if (c!=null) c.showDetails(this);
    }

    public static void addUICreator(String id, UICreator uiCreator) {
        if (!_additionUIMaker.containsKey(id)) {
            _additionUIMaker.put(id, uiCreator);
            AllPlots.getInstance().fireEvent(new WebEvent(WebLayerItem.class,Name.LAYER_ITEM_UI_CHANGE));
        }

    }

    public static void removeUICreator(String id) {
        if (_additionUIMaker.containsKey(id)) _additionUIMaker.remove(id);
    }

    public static boolean hasUICreator(String id) { return _additionUIMaker.containsKey(id); }

    public static UICreator getUICreator(String id) { return _additionUIMaker.get(id); }


    public static class UICreator {
        private  boolean hasColorSettings;
        private  boolean hasDelete;

        public UICreator() { this(true, false); }

        public UICreator(boolean hasColorSettings, boolean hasDelete) {
            this.hasColorSettings= hasColorSettings;
            this.hasDelete= hasDelete;
        }

        public Widget makeExtraColumnWidget(WebLayerItem item) { return null;}
        public Widget makeExtraUI(WebLayerItem item) { return null;}
        public boolean getHasColorSetting() { return hasColorSettings; }
        public boolean getHasDelete() { return hasDelete; }
        public boolean getHasDetails() { return false; }
        public void delete(WebLayerItem item) {}
        public void showDetails(WebLayerItem item) {}
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
