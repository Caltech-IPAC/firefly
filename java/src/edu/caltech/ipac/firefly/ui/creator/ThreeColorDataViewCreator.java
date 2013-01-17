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


