package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.table.EventHub;
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


    private Map<String, DrawingManager> _allDrawers= new HashMap<String, DrawingManager>(5);
    private Map<WebPlotView, List<String>> _allPV= new HashMap<WebPlotView, List<String>>(7);
    private EventHub _hub;


    private final WebEventListener _addPrevList=  new WebEventListener() {
        public void eventNotify(WebEvent ev) {
            Object data= ev.getData();
            Object source= ev.getSource();
            if (source instanceof DrawingLayerProvider && data!=null && data instanceof DataConnection) {
                DrawingLayerProvider p= (DrawingLayerProvider)source;
                DataConnection dc= (DataConnection)data;
                Name evName= ev.getName();
                if (evName.equals(EventHub.DRAWING_LAYER_INIT)) {
                    updateData(dc, p.getID(), p.getEnablingPreferenceKey());
                }
                else if (evName.equals(EventHub.ON_EVENT_WORKER_COMPLETE)) {
                    updateData(dc, p.getID(), p.getEnablingPreferenceKey());
                }
                else if (evName.equals(EventHub.DRAWING_LAYER_ADD)) {
                    updateData(dc, p.getID(), p.getEnablingPreferenceKey());
                }
                else if (evName.equals(EventHub.DRAWING_LAYER_REMOVE)) {
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






    public DataConnectionDisplay(EventHub hub) {
        setEventHub(hub);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setEventHub(EventHub hub) {

        WebEventManager man;
        if (_hub !=null) {
            man= hub.getEventManager();
            man.removeListener( EventHub.DRAWING_LAYER_INIT, _addPrevList);
            man.removeListener( EventHub.DRAWING_LAYER_ADD, _addPrevList);
            man.removeListener( EventHub.DRAWING_LAYER_REMOVE, _addPrevList);
            man.removeListener( EventHub.ON_EVENT_WORKER_COMPLETE, _addPrevList);
        }

        _hub = hub;

        if (_hub !=null) {
            man= hub.getEventManager();
            man.addListener( EventHub.DRAWING_LAYER_INIT, _addPrevList);
            man.addListener( EventHub.DRAWING_LAYER_ADD, _addPrevList);
            man.addListener( EventHub.DRAWING_LAYER_REMOVE, _addPrevList);
            man.addListener( EventHub.ON_EVENT_WORKER_COMPLETE, _addPrevList);
        }

    }

    public void updateData(DataConnection dataConnect, String id, String enablePrefKey) {
        if (dataConnect!=null || _allDrawers.containsKey(id)) {
            DrawingManager drawManager= getDrawingManager(id,enablePrefKey);
            drawManager.setDataConnection(dataConnect, true);
        }
    }


    private DrawingManager getDrawingManager(String id, String enablePrefKey) {
        DrawingManager drawer;
        if (_allDrawers.containsKey(id)) {
            drawer= _allDrawers.get(id);
        }
        else {
            drawer= new DrawingManager(id,null);
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
        DrawingManager drawManager= _allDrawers.get(id);
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
                    DrawingManager drawer= _allDrawers.get(id);
                    if (!drawer.containsPlotView(pv)) {
                        drawer.addPlotView(pv);
                    }
                }
            }
        }
    }

    public void removePlotView(WebPlotView pv) {
        if (pv!=null && _allPV.containsKey(pv)) {
            _allPV.remove(pv);
            for(DrawingManager drawManager : _allDrawers.values()) {
                if (drawManager.containsPlotView(pv)) {
                    drawManager.removePlotView(pv);
                }
            }
        }

    }

}

