/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

