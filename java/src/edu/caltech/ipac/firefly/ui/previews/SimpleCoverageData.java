package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Apr 20, 2010
 * Time: 12:13:04 PM
 */


/**
 * @author Trey Roby
 */
public class SimpleCoverageData extends AbstractCoverageData {

    private final List<String> _sources;
    private List<String> _eventWorkerList = new ArrayList<String>(1);
    private final ZoomType _smartType;

    public SimpleCoverageData(List<String> sources,  ZoomType smartType) {
        smartType= WebPlotRequest.isSmartZoom(smartType) ? smartType : ZoomType.SMART;
        _smartType = smartType;
        if (sources==null) {
            _sources= new ArrayList<String>(0);
        }
        else {
            _sources= sources;
        }
    }


    public String getTitle() {
        return "Coverage";
    }

    public String getTip() {
        return "Shows the coverage of the table";
    }


    public boolean getHasCoverageData(TableCtx table) {
        boolean retval= false;

        if (table!=null  && table.hasData()) {
            boolean overlay=  isTreatCatalogsAsOverlays() &&
                              table.getMeta().containsKey(MetaConst.CATALOG_OVERLAY_TYPE);
            if(_sources.contains(table.getId()) || (_sources.isEmpty() && !overlay) ) {
                retval= TableMeta.getCenterCoordColumns(table.getMeta())!=null;
                if (!retval && getFallbackCenterCol()!=null) {
                    TableMeta.LonLatColumns llC= getFallbackCenterCol();
                    List<String> cList= table.getColumns();
                    retval= (cList.contains(llC.getLatCol()) && cList.contains(llC.getLonCol()));
                }
            }
        }
        return retval;
    }

    public String getCoverageBaseTitle(TableCtx panel) { return ""; }

    @Override
    public ZoomType getSmartZoomHint() { return _smartType; }


    @Override
    public List<String> getEventWorkerList() {
        return _eventWorkerList;
    }


    public void setEventWorkerList(List<String> l) {
        _eventWorkerList = l;
    }

}

