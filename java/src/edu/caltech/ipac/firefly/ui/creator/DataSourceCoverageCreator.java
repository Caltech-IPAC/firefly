package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.previews.DataSourceCoveragePreview;
import edu.caltech.ipac.firefly.ui.previews.XmlDataSourceCoverageData;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.Map;
/**
 * User: roby
 * Date: Apr 15, 2010
 * Time: 11:41:20 AM
 */


/**
 * @author Trey Roby
 */
public class DataSourceCoverageCreator implements ObsResultCreator {

    public TablePreview create(Map<String, String> params) {


        ZoomType hint= ZoomType.SMART;
        if (params.containsKey(WebPlotRequest.ZOOM_TYPE)) {
            try {
                hint= Enum.valueOf(ZoomType.class, params.get(WebPlotRequest.ZOOM_TYPE));
            } catch (Exception e) {
                hint= ZoomType.SMART;
            }
        }
        XmlDataSourceCoverageData covData= new XmlDataSourceCoverageData(hint);
        if (params.containsKey("EVENT_WORKER_ID")) {
            covData.setEventWorkerList(DataViewCreator.getListParam(params,"EVENT_WORKER_ID"));
        }
        else if (params.containsKey("DrawingId")) {
            covData.setEventWorkerList(DataViewCreator.getListParam(params,"DrawingId"));
        }

        String title = params.get(CommonParams.TITLE);
        if (title!=null)  covData.setTitle(title);

        String group= params.get(CommonParams.PLOT_GROUP);
        if (group!=null)  covData.setGroup(group);


        boolean details=  DataViewCreator.getBooleanParam(params, CommonParams.ENABLE_DETAILS,true);
        covData.setEnableDetails(details);

        boolean blank= DataViewCreator.getBooleanParam(params,CommonParams.BLANK);
        covData.setUseBlankPlot(blank);

        return new DataSourceCoveragePreview(covData);
    }
}

