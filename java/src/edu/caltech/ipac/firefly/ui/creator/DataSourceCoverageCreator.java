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
