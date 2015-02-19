/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.previews.AbstractPreviewData;
import edu.caltech.ipac.firefly.ui.previews.ThreeColorPreviewData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.ui.PlotTypeUI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 15, 2010
 * Time: 11:41:20 AM
 */


/**
 * @author Trey Roby
 */
public class ThreeColorDataViewCreator extends DataViewCreator {

    public AbstractPreviewData makePreviewData(Map<String, String> params) {



        Map<Band,List<Param>> extraParamMap= new HashMap<Band,List<Param>> (5);

        // params for all bands
        String allBandsStr = getParamDestruct(params,CommonParams.EXTRA_PARAMS);

        String extraStr= getParamDestruct(params,CommonParams.RED_EXTRA_PARAMS);
        extraParamMap.put(Band.RED, (allBandsStr != null)?GwtUtil.parseParams(extraStr + "," + allBandsStr):GwtUtil.parseParams(extraStr));
        extraStr= getParamDestruct(params,CommonParams.GREEN_EXTRA_PARAMS);
        extraParamMap.put(Band.GREEN, (allBandsStr != null)?GwtUtil.parseParams(extraStr + "," + allBandsStr):GwtUtil.parseParams(extraStr));
        extraStr= getParamDestruct(params,CommonParams.BLUE_EXTRA_PARAMS);
        extraParamMap.put(Band.BLUE, (allBandsStr != null)?GwtUtil.parseParams(extraStr + "," + allBandsStr):GwtUtil.parseParams(extraStr));

        Map<Band,String> serverReqKeyMap= new HashMap<Band,String>(5);
        serverReqKeyMap.put(Band.RED,  getParamDestruct(params,CommonParams.RED_SEARCH_PROCESSOR_ID));
        serverReqKeyMap.put(Band.GREEN,getParamDestruct(params,CommonParams.GREEN_SEARCH_PROCESSOR_ID));
        serverReqKeyMap.put(Band.BLUE, getParamDestruct(params,CommonParams.BLUE_SEARCH_PROCESSOR_ID));

        Band bands[]= new Band[] {Band.RED,Band.GREEN,Band.BLUE};


        ThreeColorPreviewData retval= new ThreeColorPreviewData(serverReqKeyMap, extraParamMap,bands);

        retval.setContinueOnFail(Band.RED, getBooleanParam(params,CommonParams.RED_CONTINUE_ON_FAIL,false,true));
        retval.setContinueOnFail(Band.GREEN, getBooleanParam(params,CommonParams.GREEN_CONTINUE_ON_FAIL,false,true));
        retval.setContinueOnFail(Band.BLUE, getBooleanParam(params,CommonParams.BLUE_CONTINUE_ON_FAIL,false,true));

        if (params.containsKey("DV_IMAGE_SELECT_PANEL")) {
            String imSelKey= getParamDestruct(params,"DV_IMAGE_SELECT_PANEL");
            PlotTypeUI ptUI= Application.getInstance().getWidgetFactory().createPlotTypeUI(imSelKey,params);
            if (ptUI!=null && ptUI instanceof PlotRelatedPanel) {
                retval.setExtraPanel((PlotRelatedPanel)ptUI);
            }
        }

        return retval;

    }


}
