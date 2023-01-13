/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;


@SearchProcessorImpl(id = AsyncTapQuery.ID, params = {
        @ParamDoc(name = "serviceUrl", desc = "base TAP url endpoint excluding '/async'"),
        @ParamDoc(name = "QUERY", desc = "query string"),
        @ParamDoc(name = "LANG", desc = "defaults to ADQL"),
        @ParamDoc(name = "MAXREC", desc = "maximum number of records to be returned")
})
public class AsyncTapQuery extends UwsJobProcessor {
    public static final String ID = "AsyncTapQuery";

    private static final int MAXREC_HARD = AppProperties.getIntProperty("tap.maxrec.hardlimit", 10000000);

    public HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        String serviceUrl = request.getParam("serviceUrl");
        String queryStr = request.getParam("QUERY");
        String lang = request.getParam("LANG");
        String maxrecStr = request.getParam("MAXREC");
        int maxrec = -1; // maxrec is not set
        if (!StringUtils.isEmpty(maxrecStr)) {
            maxrec = Integer.parseInt(maxrecStr);
            if (maxrec < 0 || maxrec > MAXREC_HARD) {
                maxrec = -1;
                throw new IllegalArgumentException("MAXREC value "+Integer.toString(maxrec)+
                        " is not in (0,"+MAXREC_HARD+") range.");
            }
        }

        HttpServiceInput inputs = HttpServiceInput.createWithCredential(serviceUrl + "/async");
        if (!StringUtils.isEmpty(queryStr)) inputs.setParam("QUERY", queryStr);
        if (StringUtils.isEmpty(lang)) { lang = "ADQL"; }
        if (maxrec > -1) { inputs.setParam("MAXREC", Integer.toString(maxrec)); }
        inputs.setParam("LANG", lang); // in tap 1.0, lang param is required
        inputs.setParam("request", "doQuery"); // in tap 1.0, request param is required
        return inputs;
    }
}
