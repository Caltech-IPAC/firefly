package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.previews.AbstractPreviewData;
import edu.caltech.ipac.firefly.ui.previews.SimplePreviewData;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;
/**
 * User: roby
 * Date: Apr 15, 2010
 * Time: 11:41:20 AM
 */


/**
 * @author Trey Roby
 */
public class SimpleDataViewCreator extends DataViewCreator {

    public AbstractPreviewData makePreviewData(Map<String, String> params) {

        String searchProcessorID= getParamDestruct(params,CommonParams.SEARCH_PROCESSOR_ID);
        String extraStr= getParamDestruct(params,CommonParams.EXTRA_PARAMS); // used all the time
        String noPreviewConditions= getParamDestruct(params,"NoPreviewConditions");

        SimplePreviewData previewData = new SimplePreviewData(searchProcessorID, GwtUtil.parseParams(extraStr));

        if (!StringUtils.isEmpty(noPreviewConditions)) {
            previewData.setNoPreviewCondition(GwtUtil.parseParams(noPreviewConditions));
        }
        return previewData;
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
