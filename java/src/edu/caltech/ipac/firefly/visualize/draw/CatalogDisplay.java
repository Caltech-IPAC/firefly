package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

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
    private Map<TablePanel, TabularDrawingManager> _allDrawers= new HashMap<TablePanel, TabularDrawingManager>(5);
    private List<WebPlotView> _allPV= new ArrayList<WebPlotView>(3);
    public static final String HELP_STR= "Click to select an object, Check table to show name";
    public static final DrawSymbol DEF_SYMBOL= DrawSymbol.SQUARE;
    private static final String SYMBOL="SYMBOL";
    private static final String DEFAULT_COLOR="DEFAULT_COLOR";
    private static Map<String, DrawSymbol> SYMBOL_MAP= new HashMap<String, DrawSymbol>();
    private static int idCnt= 0;

    static {
        SYMBOL_MAP.put("X", DrawSymbol.X);
        SYMBOL_MAP.put("SQUARE", DrawSymbol.SQUARE);
        SYMBOL_MAP.put("CROSS", DrawSymbol.CROSS);
        SYMBOL_MAP.put("EMP_CROSS", DrawSymbol.EMP_CROSS);
        SYMBOL_MAP.put("DIAMOND", DrawSymbol.DIAMOND);
        SYMBOL_MAP.put("DOT", DrawSymbol.DOT);
    }

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




    private TablePreviewEventHub _hub;


    public CatalogDisplay() { this(null); }


    public CatalogDisplay(TablePreviewEventHub hub) {
        setEventHub(hub);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void beginBulkUpdate() {
        for(TabularDrawingManager drawManager : _allDrawers.values()) {
            drawManager.beginBulkUpdate();
        }
    }
    public void endBulkUpdate() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                for(TabularDrawingManager drawManager : _allDrawers.values()) {
                    drawManager.endBulkUpdate();
                }
            }
        });
    }

    

    public void setEventHub(TablePreviewEventHub hub) {

        WebEventManager man;
        if (_hub !=null) {
            man= hub.getEventManager();
            man.removeListener( TablePreviewEventHub.ON_TABLE_ADDED, _addPrevList);
            man.removeListener( TablePreviewEventHub.ON_TABLE_REMOVED, _removePrevList);
            //TODO: should a remove all WebPlotViews and DrawingManagers when I do this?
        }

        _hub = hub;

        if (_hub !=null) {
            man= hub.getEventManager();
            man.addListener( TablePreviewEventHub.ON_TABLE_ADDED, _addPrevList);
            man.addListener( TablePreviewEventHub.ON_TABLE_REMOVED, _removePrevList);
        }

    }




    public void addCatalog(TablePanel table) {

        TableMeta meta= table.getDataset().getMeta();
        if (meta.contains(MetaConst.CATALOG_OVERLAY_TYPE)) {
            Hints hints= new Hints(meta.getAttribute(MetaConst.CATALOG_HINTS));
            idCnt++;
            TabularDrawingManager drawManager= new TabularDrawingManager(BASE_ID+idCnt, Drawer.DataType.NORMAL);
            //Change default color if defined in table meta
            //user can define a default color value in table's header.
            //e.g. green, lightgreen, red, cyan, yellow, 00ffaa, 103aff
            if (meta.contains(DEFAULT_COLOR)) {
                String color = meta.getAttribute(DEFAULT_COLOR);
                drawManager.setDefaultColor(color);
            }

            for(WebPlotView pv : _allPV) {
                drawManager.addPlotView(pv);
            }
//            drawManager.setAutoSymbol(true);
            drawManager.setDataConnection(new DetailData(table,
                                                         !hints.getDisableMouse(),
                                                         hints.getOnlyIfActiveTab()), true);
            _allDrawers.put(table,drawManager);
        }

    }



    public void removeCatalog(TablePanel table) {
        TabularDrawingManager drawManager= _allDrawers.get(table);
        if (drawManager!=null)  drawManager.dispose();
        if (_allDrawers.containsKey(table)) _allDrawers.remove(table);
    }



    public void addPlotView(WebPlotView pv) {
        if (pv!=null && !_allPV.contains(pv)) {
            _allPV.add(pv);
            for(TabularDrawingManager drawManager : _allDrawers.values()) {
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
                if (!_allPV.contains(pv)) addList.add(pv);
            }
            for(WebPlotView pv : addList) {
                _allPV.add(pv);
                for(TabularDrawingManager drawManager : _allDrawers.values()) {
                    if (!drawManager.containsPlotView(pv)) {
                        drawManager.addPlotViewList(pvList);
                    }
                }

            }


        }
    }


    public void removePlotView(WebPlotView pv) {
        if (pv!=null && _allPV.contains(pv)) {
            _allPV.remove(pv);
            for(TabularDrawingManager drawManager : _allDrawers.values()) {
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
        public boolean getOnlyIfActiveTab() {
            return _hintList.contains("OnlyIfVisible".toLowerCase());
        }
    }

    public class DetailData extends TableDataConnection {

        private final List<DrawObj> _graphObj=  new ArrayList<DrawObj>(100);

        DetailData(TablePanel table, boolean supportsMouse, boolean onlyIfTabActive) {
            super(table,HELP_STR,true,supportsMouse,onlyIfTabActive);
        }


        public List<DrawObj> getData(boolean rebuild) {
            TablePanel table= getTable();
            TableMeta meta= table.getDataset().getMeta();
            String nameCol= meta.getAttribute(MetaConst.CATALOG_TARGET_COL_NAME);
            TableMeta.LonLatColumns llc= meta.getLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS);
            if (llc==null) return null;
            String raColName= llc.getLonCol();
            String decColName= llc.getLatCol();
            CoordinateSys csys= llc.getCoordinateSys();
            String name;

            int nameIdx= -1;
            DrawSymbol symbol = DEF_SYMBOL;
            // Change symbol if defined in table meta
            // e.g. X,SQUARE,CROSS,DIAMOND,DOT
            if (meta.contains(SYMBOL)) {
                String key = meta.getAttribute(SYMBOL);
                if (SYMBOL_MAP.containsKey(key)) symbol= SYMBOL_MAP.get(key); 
            }

            if (rebuild || _graphObj.size()==0) {
                _graphObj.clear();
                int tabSize= table.getRowCount();
                TableData model= table.getDataset().getModel();

                int raIdx= model.getColumnIndex(raColName);
                int decIdx= model.getColumnIndex(decColName);
                if (nameCol!=null)  nameIdx= model.getColumnIndex(nameCol);


                PointDataObj obj;

                for(int i= 0; i<tabSize; i++) {
                    WorldPt graphPt = getWorldPt(i, raIdx, decIdx, csys);
                    if (graphPt != null) {
                        name= (isSelected(i) && nameIdx>-1) ? getName(i,nameIdx) : null;
                        obj= new PointDataObj(graphPt);
                        obj.setText(name);
                        obj.setSymbol(symbol);
                        _graphObj.add(obj);
                    }
                }

            }
            return _graphObj;
        }

        @Override
        public boolean isPointData() { return true; }

        public boolean isActive() {
            return true;
        }

        private WorldPt getWorldPt(int row, int raIdx, int decIdx, CoordinateSys csys) {
            TableData.Row r=getTable().getTable().getRowValue(row);
            WebAssert.argTst(r!=null, "row : " +row+" should not be null");
            String raStr= (String)r.getValue(raIdx);
            String decStr= (String)r.getValue(decIdx);

            try {
                double ra= Double.parseDouble(raStr);
                double dec= Double.parseDouble(decStr);
                return new WorldPt(ra,dec,csys);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private boolean isSelected(int row) {
            return getTable().getDataset().getSelectionInfo().isSelected(row);
        }


        private String getName(int row, int nameIdx) {
            TableData.Row r=getTable().getTable().getRowValue(row);
            return (String)r.getValue(nameIdx);
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
