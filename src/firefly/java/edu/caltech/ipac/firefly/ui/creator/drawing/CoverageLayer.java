/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 3/23/12
 * Time: 1:42 PM
 */


import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingDef;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;

/**
* @author Trey Roby
*/
class CoverageLayer extends ProviderDataConnection {

    private List<DrawObj> list;
    private DrawSymbol _symbol;
    private final boolean _box;
    private TableMeta.LonLatColumns[] _fallbackCornerCols = null;
    private TableMeta.LonLatColumns _fallbackCenterCol= null;
	private int symbSize = PointDataObj.DEFAULT_SIZE;

    CoverageLayer(DatasetDrawingLayerProvider provider,
                  DataSet dataset,
                  String title,
                  DrawSymbol symbol,
                  String color,
                  boolean box) {
        super(provider, title, null, color == null ? DrawingDef.COLOR_PT_1 : color);
        _symbol= symbol;
		symbSize = _symbol.equals(DrawSymbol.DOT) ? PointDataObj.DOT_DEFAULT_SIZE : PointDataObj.DEFAULT_SIZE;
        _box= box;
        updateData(dataset);

    }

    public void setFallbackCornerCols(TableMeta.LonLatColumns[] fallbackCornerCols ) {
        _fallbackCornerCols= fallbackCornerCols;
    }

    public void setFallbackCenterCols(TableMeta.LonLatColumns fallbackCenterCol) {
        _fallbackCenterCol= fallbackCenterCol;
    }

    @Override
    public void updateData(DataSet dataset) {
        if (dataset==null) {
            list= null;
            return;
        }

        TableMeta.LonLatColumns llc= dataset.getMeta().getCenterCoordColumns();
        TableMeta.LonLatColumns llcCorners[]= null;
        if (!containCenter(llc))llc= _fallbackCenterCol;


        boolean box= _box;
        if (box) {
            llcCorners= getCornersColumns(dataset);
            if (llcCorners==null) box= false;
        }



        list = new ArrayList<DrawObj>(200);
        try {
            if (llc != null) {
                int raIdx = dataset.getModel().getColumnIndex(llc.getLonCol());
                int decIdx = dataset.getModel().getColumnIndex(llc.getLatCol());
                for (Object o : dataset.getModel().getRows()) {
                    try {
                        TableData.Row<String> row = (TableData.Row) o;
                        if (box) {
                            WorldPt [] wpts = new WorldPt[llcCorners.length];
                            int idx= 0;
                            for(TableMeta.LonLatColumns  corner : llcCorners) {
                                WorldPt graphPt = getWorldPt(row.getValue(corner.getLonCol()),
                                                             row.getValue(corner.getLatCol()),
                                                             corner.getCoordinateSys());
                                if (graphPt != null) wpts[idx++] = graphPt;
                                else                 break;
                            }
                           FootprintObj fp= new FootprintObj(wpts);
//                            fp.setColor(getInitDefaultColor());
                            list.add(fp);
                        }
                        else {
                            WorldPt wp = getWorldPt(row.getValue(raIdx),
                                                    row.getValue(decIdx),
                                                    llc.getCoordinateSys());
                            PointDataObj obj = new PointDataObj(wp, _symbol);
                            obj.setSize(this.symbSize);
                            list.add(obj);
                        }
                    } catch (NumberFormatException e) {
                        // ignore and more on
                    }
                }
            }

        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    public List<DrawObj> getData(boolean rebuild, WebPlot p) { return list; }

    @Override
    public boolean getSupportsMouse() { return false; }


    private WorldPt getWorldPt(String raStr, String decStr, CoordinateSys csys) {
        try {
            double ra= Double.parseDouble(raStr);
            double dec= Double.parseDouble(decStr);
            return new WorldPt(ra,dec,csys);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean containCenter(TableMeta.LonLatColumns centerCol) {
        boolean retval= false;
        if (centerCol!=null) {
            String lat= centerCol.getLatCol();
            String lon= centerCol.getLonCol();
            retval= !StringUtils.isEmpty(lon) && !StringUtils.isEmpty(lat) &&
                    !lon.equals("null") && !lat.equals("null");
        }
        return retval;
    }

    public TableMeta.LonLatColumns[] getCornersColumns(DataSet dataSet) {
        TableMeta.LonLatColumns[] retval= null;
        if (canDoCorners(dataSet)) {
            retval= dataSet.getMeta().getCorners();
            if (retval==null && _fallbackCornerCols !=null) {
                retval= _fallbackCornerCols;
            }
        }
        return retval;
    }

	public void setSize(int size){
		this.symbSize = size;
	}
	
	public void setSymbol(DrawSymbol symbol){
		this._symbol = symbol;
	}
	
    public boolean canDoCorners(DataSet dataSet) {
        TableMeta meta= dataSet.getMeta();
        TableMeta.LonLatColumns col[]= meta.getCorners();
        boolean doesCorners= (col!=null && col.length>1);
        if (!doesCorners) { // if not meta corners defined then decide if we can use the fallback
            TableMeta.LonLatColumns centerCol= meta.getCenterCoordColumns();
            /*
             * if meta does define a center and I have a fallback corners then return true
             * this is because not defining a center probably means there is no meta specified
             * a center define means that the corners where intentionally not defined
             */
            if (!containCenter(centerCol) && _fallbackCornerCols !=null) {
                doesCorners= true;
                for(TableMeta.LonLatColumns llC : _fallbackCornerCols) {
                    if ( dataSet.findColumn(llC.getLatCol())==null || dataSet.findColumn(llC.getLonCol())==null) {
                        doesCorners= false;
                        break;
                    }
                }
            }
            else if (meta.contains(CommonParams.HAS_COVERAGE_DATA)) {
                doesCorners= true;
            }
        }
        return doesCorners;
    }


}

