/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 6/3/13
 * Time: 2:57 PM
 */


import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class CatalogDataConnection extends TableDataConnection {

//    private static int RA_IDX= 0;
//    private static int DEC_IDX= 1;
    private static int NAME_IDX= 2;
    private static Map<String, DrawSymbol> SYMBOL_MAP= new HashMap<String, DrawSymbol>();
    public static final DrawSymbol DEF_SYMBOL= DrawSymbol.SQUARE;
    private static final String SYMBOL="SYMBOL";
    private static boolean symbolMapLoaded= false;

    private final String nameCol;
    private final String raColName;
    private final String decColName;
    private final CoordinateSys csys;
    private DrawSymbol symbol = DEF_SYMBOL;


    CatalogDataConnection(TablePanel table, boolean supportsMouse, boolean onlyVisibleIfTabActive) {
        super(table, CatalogDisplay.HELP_STR,true,true,true,supportsMouse,onlyVisibleIfTabActive);

        if (!symbolMapLoaded) {
            SYMBOL_MAP.put("X",         DrawSymbol.X);
            SYMBOL_MAP.put("SQUARE",    DrawSymbol.SQUARE);
            SYMBOL_MAP.put("CROSS",     DrawSymbol.CROSS);
            SYMBOL_MAP.put("EMP_CROSS", DrawSymbol.EMP_CROSS);
            SYMBOL_MAP.put("DIAMOND",   DrawSymbol.DIAMOND);
            SYMBOL_MAP.put("DOT",       DrawSymbol.DOT);
            symbolMapLoaded= true;
        }

        TableMeta meta= table.getDataset().getMeta();
        nameCol= meta.getAttribute(MetaConst.CATALOG_TARGET_COL_NAME);
        TableMeta.LonLatColumns llc= meta.getLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS);
        if (llc!=null) {
            raColName= llc.getLonCol();
            decColName= llc.getLatCol();
            csys= llc.getCoordinateSys();
        }
        else {
            raColName= null;
            decColName= null;
            csys= null;
        }
        if (meta.contains(SYMBOL)) {
            String key = meta.getAttribute(SYMBOL);
            if (SYMBOL_MAP.containsKey(key)) symbol= SYMBOL_MAP.get(key);
        }
    }


    public List<DrawObj> getDataImpl() {
        if (raColName==null || decColName==null || csys==null) return null;

        String name;
        int tabSize= getTableDatView().getSize();
        PointDataObj obj;

        TableData model= getTableDatView().getModel();
        List<DrawObj> _graphObj=  new ArrayList<DrawObj>(tabSize+2);
        for(int i= 0; i<tabSize; i++) {
            TableData.Row<String> r= getTableDatView().getModel().getRow(i);
            WorldPt graphPt = getWorldPt(r);
            if (graphPt != null) {
//                    name= (isSelected(i) && nameCol!=null) ? getName(i,NAME_IDX) : null;
                obj= new PointDataObj(graphPt, symbol);
                obj.setRepresentCnt(getWeight(i));
                _graphObj.add(obj);
            }
        }
        return _graphObj;
    }


    public List<DrawObj> getHighlightDataImpl() {
        if (raColName==null || decColName==null || csys==null) return null;
        TableData.Row<String> r= getTableHighlightedRow();
        List<DrawObj> retval= Collections.emptyList();
        if (r!=null) {
            WorldPt graphPt = getWorldPt(r);
            PointDataObj obj= new PointDataObj(graphPt, symbol);
//            obj.setSize(obj.getSize()+1);
            obj.setHighlighted(true);
            retval= Arrays.asList((DrawObj)obj);
        }
        return retval;
    }


    @Override
    public boolean isPointData() { return true; }


    public boolean isActive() {
        return true;
    }


    private WorldPt getWorldPt(TableData.Row<String> r) {
        try {
            double ra= Double.parseDouble(r.getValue(raColName));
            double dec= Double.parseDouble(r.getValue(decColName));
            return new WorldPt(ra,dec,csys);
        } catch (NumberFormatException e) {
            return null;
        }
    }

//    private boolean isSelected(int row) {
//        return tableDataView.h
//        return getTable().getDataset().getSelectionInfo().isSelected(row);
//    }


    private String getName(int row, int nameIdx) {
        TableData.Row<String> r=getTableDatView().getModel().getRow(row);
        return r.getValue(nameIdx);
    }

    protected List<String> getDataColumns() {
        List<String> colList= new ArrayList<String>(3);
        colList.add(raColName);
        colList.add(decColName);
        if (nameCol!=null) colList.add(nameCol);
        return colList;
    }


}

