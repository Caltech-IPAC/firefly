package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Nov 6, 2009
 * Time: 4:27:33 PM
 */


/**
 * Will display a set of catalogs on a set of plotviews
 * @author Trey Roby
 */
public class CatalogDisplay {


    private static final String BASE_ID= "CatalogID-";
    private Map<TablePanel, DrawingManager> _allDrawers= new HashMap<TablePanel, DrawingManager>(5);
    private List<WebPlotView> _allPV= new ArrayList<WebPlotView>(3);
    public static final String HELP_STR= "Click to select an object, Check table to show name";
    private static int idCnt= 0;


    private final WebEventListener _addPrevList=  new WebEventListener() {
        public void eventNotify(WebEvent ev) {
            final TablePanel table= (TablePanel)ev.getData();
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    addCatalog(table);
                }
            });
        }
    };


    private final WebEventListener _removePrevList=  new WebEventListener() {
        public void eventNotify(WebEvent ev) {
            removeCatalog((TablePanel)ev.getData());
        }
    };




    private EventHub _hub;


    public CatalogDisplay() { this(null); }


    public CatalogDisplay(EventHub hub) {
        setEventHub(hub);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void beginBulkUpdate() {
        for(DrawingManager drawManager : _allDrawers.values()) {
            drawManager.beginBulkUpdate();
        }
    }
    public void endBulkUpdate() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                for(DrawingManager drawManager : _allDrawers.values()) {
                    drawManager.endBulkUpdate();
                }
            }
        });
    }

    

    public void setEventHub(EventHub hub) {

        WebEventManager man;
        if (_hub !=null) {
            man= hub.getEventManager();
            man.removeListener( EventHub.ON_TABLE_ADDED, _addPrevList);
            man.removeListener( EventHub.ON_TABLE_REMOVED, _removePrevList);
            //TODO: should a remove all WebPlotViews and DrawingManagers when I do this?
        }

        _hub = hub;

        if (_hub !=null) {
            man= hub.getEventManager();
            man.addListener( EventHub.ON_TABLE_ADDED, _addPrevList);
            man.addListener( EventHub.ON_TABLE_REMOVED, _removePrevList);
        }

    }




    public void addCatalog(TablePanel table) {

        TableMeta meta= table.getDataset().getMeta();
        if (table.getDataset().getTotalRows()>0 &&
                meta.contains(MetaConst.CATALOG_OVERLAY_TYPE) &&
                meta.contains(MetaConst.CATALOG_COORD_COLS)) {

            Hints hints= new Hints(meta.getAttribute(MetaConst.CATALOG_HINTS));
            idCnt++;
            DrawingManager drawManager= new DrawingManager(BASE_ID+idCnt, null);
            //Change default color if defined in table meta
            //user can define a default color value in table's header.
            //e.g. green, lightgreen, red, cyan, yellow, 00ffaa, 103aff
            if (meta.contains(MetaConst.DEFAULT_COLOR)) {
                String color = meta.getAttribute(MetaConst.DEFAULT_COLOR);
                drawManager.setDefaultColor(color);
            }
            else {
                String defColor;
                switch (idCnt%5) {
                    case 0:
                        defColor= AutoColor.PT_1;
                        break;
                    case 1:
                        defColor= AutoColor.PT_2;
                        break;
                    case 2:
                        defColor= AutoColor.PT_3;
                        break;
                    case 3:
                        defColor= AutoColor.PT_5;
                        break;
                    default:
                    case 4:
                        defColor= AutoColor.PT_6;
                        break;
                }
                drawManager.setDefaultColor(defColor);
            }

            for(WebPlotView pv : _allPV) {
                drawManager.addPlotView(pv);
            }
            CatalogDataConnection conn= new CatalogDataConnection(table,
                                                         !hints.getDisableMouse(),
                                                         hints.getOnlyVisibleIfActiveTab());
            if (hints.isUsingSubgroup()) {
                conn.setSubgroupList(hints.getSubGroupList());
            }

            drawManager.setDataConnection(conn, true);
            _allDrawers.put(table,drawManager);
        }

    }



    public void removeCatalog(TablePanel table) {
        DrawingManager drawManager= _allDrawers.get(table);
        if (drawManager!=null)  drawManager.dispose();
        if (_allDrawers.containsKey(table)) _allDrawers.remove(table);
    }



    public void addPlotView(WebPlotView pv) {
        if (pv!=null && !_allPV.contains(pv) && pv.isAlive() ) {
            _allPV.add(pv);
            for(DrawingManager drawManager : _allDrawers.values()) {
                if (!drawManager.containsPlotView(pv)) {
                    drawManager.addPlotView(pv);
                }
            }
        }
    }

    public void addPlotViewList(List<WebPlotView> pvList) {
        if (pvList!=null) {
            List<WebPlotView> addList= new ArrayList<WebPlotView>(pvList.size());
            for(WebPlotView pv : pvList) {
                if (!_allPV.contains(pv) && pv.isAlive()) addList.add(pv);
            }

            for(WebPlotView pv : addList) {
                if (pv.isAlive()) {
                    _allPV.add(pv);
                    for(DrawingManager drawManager : _allDrawers.values()) {
                        if (!drawManager.containsPlotView(pv)) {
                            drawManager.addPlotView(pv);
                        }
                    }
                }
            }



//
//            for(WebPlotView pv : addList) {
//                if (pv.isAlive()) {
//                    _allPV.add(pv);
//                    for(DrawingManager drawManager : _allDrawers.values()) {
//                        if (!drawManager.containsPlotView(pv)) {
//                            drawManager.addPlotViewList(pvList);
//                        }
//                    }
//                }
//            }


        }
    }


    public void removePlotView(WebPlotView pv) {
        if (pv!=null && _allPV.contains(pv)) {
            _allPV.remove(pv);
            for(DrawingManager drawManager : _allDrawers.values()) {
                if (drawManager.containsPlotView(pv)) {
                    drawManager.removePlotView(pv);
                }
            }
        }

    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================

    private static class Hints {
        private List<String> _hintList;


        public Hints(String hintStr) {
            _hintList= (hintStr!=null)  ?
                       StringUtils.asList(hintStr, ",", true) :
                       Collections.<String>emptyList();
        }

        public boolean getDisableMouse() {
            return _hintList.contains("DisableMouse".toLowerCase());
        }
        public boolean getOnlyVisibleIfActiveTab() {
            return _hintList.contains("OnlyIfVisible".toLowerCase());
        }

        public boolean isUsingSubgroup() {
            return findSubgroupHint()!=null;
        }

        public List<String> getSubGroupList() {
            List<String> retval= null;
            String sg= findSubgroupHint();
            if (sg!=null) {
                String sAry[]=sg.split("=");
                if (sAry.length==2) {
                    retval= StringUtils.asList(sAry[1],":");
                }
            }
            return retval;
        }

        private String findSubgroupHint() {
            String retval= null;
            for(String s : _hintList) {
                if (s.toLowerCase().startsWith("subgroup")) {
                    retval= s;
                    break;
                }
            }
            return retval;

        }
    }


}

