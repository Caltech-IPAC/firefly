package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlotView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Nov 6, 2009
 * Time: 4:27:33 PM
 */


/**
 * Will display DataConnection objects on plotview that want to show them
 * A DataConnectionDisplay can take many DataConnection objects and draw it on many different WebPlotView's.
 * the pairing of data to WebPlotView is done by ID
 * @author Trey Roby
 */
public class DataConnectionDisplay {


    private Map<String, TabularDrawingManager> _allDrawers= new HashMap<String, TabularDrawingManager>(5);
    private Map<WebPlotView, List<String>> _allPV= new HashMap<WebPlotView, List<String>>(7);
    private TablePreviewEventHub _hub;


    private final WebEventListener _addPrevList=  new WebEventListener() {
        public void eventNotify(WebEvent ev) {
            Object data= ev.getData();
            Object source= ev.getSource();
            if (source instanceof DrawingLayerProvider && data!=null && data instanceof DataConnection) {
                DrawingLayerProvider p= (DrawingLayerProvider)source;
                DataConnection dc= (DataConnection)data;
                Name evName= ev.getName();
                if (evName.equals(TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE)) {
                    updateData(dc, p.getID(), p.getEnablingPreferenceKey());
                }
                else if (evName.equals(TablePreviewEventHub.DRAWING_LAYER_ADD)) {
                    updateData(dc, p.getID(), p.getEnablingPreferenceKey());
                }
                else if (evName.equals(TablePreviewEventHub.DRAWING_LAYER_REMOVE)) {
                    removeData(p.getID());
                }


            }
        }
    };


//    private final WebEventListener _removePrevList=  new WebEventListener() {
//        public void eventNotify(WebEvent ev) {
//            removeData((DataConnection)ev.getData());
//        }
//    };






    public DataConnectionDisplay(TablePreviewEventHub hub) {
        setEventHub(hub);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setEventHub(TablePreviewEventHub hub) {

        WebEventManager man;
        if (_hub !=null) {
            man= hub.getEventManager();
            man.removeListener( TablePreviewEventHub.DRAWING_LAYER_INIT, _addPrevList);
            man.removeListener( TablePreviewEventHub.DRAWING_LAYER_ADD, _addPrevList);
            man.removeListener( TablePreviewEventHub.DRAWING_LAYER_REMOVE, _addPrevList);
            man.removeListener( TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE, _addPrevList);
        }

        _hub = hub;

        if (_hub !=null) {
            man= hub.getEventManager();
            man.addListener( TablePreviewEventHub.DRAWING_LAYER_INIT, _addPrevList);
            man.addListener( TablePreviewEventHub.DRAWING_LAYER_ADD, _addPrevList);
            man.addListener( TablePreviewEventHub.DRAWING_LAYER_REMOVE, _addPrevList);
            man.addListener( TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE, _addPrevList);
        }

    }

    public void updateData(DataConnection dataConnect, String id, String enablePrefKey) {
        if (dataConnect!=null || _allDrawers.containsKey(id)) {
            TabularDrawingManager drawManager= getDrawingManager(id,dataConnect,enablePrefKey);
            drawManager.setDataConnection(dataConnect, true);
        }
    }


    private TabularDrawingManager getDrawingManager(String id, DataConnection dataConnect, String enablePrefKey) {
        TabularDrawingManager drawer;
        if (_allDrawers.containsKey(id)) {
            drawer= _allDrawers.get(id);
        }
        else {
            Drawer.DataType hints= Drawer.DataType.VERY_LARGE;
            drawer= new TabularDrawingManager(id,hints);
            drawer.setGroupByTitleOrID(true);
            drawer.setEnablePrefKey(enablePrefKey);
            _allDrawers.put(id,drawer);
            for(Map.Entry<WebPlotView,List<String>> entry : _allPV.entrySet()) {
                WebPlotView pv= entry.getKey();
                for(String testID : entry.getValue()) {
                    if (testID.equals(id)) {
                        drawer.addPlotView(pv);
                    }
                }
            }
        }
        return drawer;
    }


    public void removeData(String id) {
        TabularDrawingManager drawManager= _allDrawers.get(id);
        if (drawManager!=null)  drawManager.dispose();
        if (_allDrawers.containsKey(id)) _allDrawers.remove(id);
    }

    public void addPlotView(WebPlotView pv, List<String> idList) {
        if (pv!=null && pv.isAlive()) {
            if (!_allPV.containsKey(pv)) {
                _allPV.put(pv,idList);
            }
            for(String id : idList) {
                if (_allDrawers.containsKey(id)) {
                    TabularDrawingManager drawer= _allDrawers.get(id);
                    if (!drawer.containsPlotView(pv)) {
                        drawer.addPlotView(pv);
                        drawer.redraw();
                    }
                }
            }
        }
    }

    public void removePlotView(WebPlotView pv) {
        if (pv!=null && _allPV.containsKey(pv)) {
            _allPV.remove(pv);
            for(TabularDrawingManager drawManager : _allDrawers.values()) {
                if (drawManager.containsPlotView(pv)) {
                    drawManager.removePlotView(pv);
                }
            }
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
