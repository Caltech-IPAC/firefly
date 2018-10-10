/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SearchProcessorImpl(id = AsyncTapQuery.ID, params = {
        @ParamDoc(name = "serviceUrl", desc = "base TAP url endpoint excluding '/async'"),
        @ParamDoc(name = "QUERY", desc = "query string"),
        @ParamDoc(name = "LANG", desc = "defaults to ADQL"),
        @ParamDoc(name = "PHASE", desc = "workaround IRSA's requirement of PHASE=RUN in the initial post.")
})
public class AsyncTapQuery extends AsyncSearchProcessor {
    public static final String ID = "AsyncTapQuery";

    public AsyncJob submitRequest(ServerRequest request) {
        String serviceUrl = request.getParam("serviceUrl");
        String phase = request.getParam("PHASE");
        String lang = request.getParam("LANG");
        String queryStr = createQueryString(request);

        HttpServiceInput inputs = new HttpServiceInput();
        if (!StringUtils.isEmpty(lang)) inputs.setParam("LANG", lang);
        if (!StringUtils.isEmpty(queryStr)) inputs.setParam("QUERY", queryStr);
        if (!StringUtils.isEmpty(phase)) inputs.setParam("PHASE", phase);

        Ref<String> location = new Ref<>();
        HttpServices.postData(serviceUrl + "/async", inputs, (method -> {
            Header loc = method.getResponseHeader("Location");
            if (loc != null) {
                location.setSource(loc.getValue().trim());
            }
        }));


        AsyncTapJob asyncTap = new AsyncTapJob(location.getSource());
        if (asyncTap.getPhase() == AsyncJob.Phase.PENDING) {
            HttpServices.postData(asyncTap.baseJobUrl + "/phase", new HttpServiceInput().setParam("PHASE", "RUN"), null);
        }
        return asyncTap;
    }


    /**
     * override this function to convert a request into an ADQL QUERY string
     * @param request
     * @return an ADQL QUERY string
     */
    protected String createQueryString(ServerRequest request) {
        return request.getParam("QUERY");
    }


    public class AsyncTapJob implements AsyncJob  {
        private Logger.LoggerImpl logger = Logger.getLogger();
        private String baseJobUrl;

        public AsyncTapJob(String baseJobUrl) {
            this.baseJobUrl = baseJobUrl;
        }

        public DataGroup getDataGroup() {
            try {
                DataGroup[] results = VoTableReader.voToDataGroups(baseJobUrl + "/results/result");
                return results.length > 0 ? results[0] : null;
            } catch (Exception ex) {
                logger.error(ex);
            }
            return null;
        }

        public boolean cancel() {
            return ! HttpServices.postData(baseJobUrl + "/phase", new ByteArrayOutputStream(),
                                            new HttpServiceInput().setParam("PHASE", Phase.ABORTED.name()))
                                .isError();
        }

        public Phase getPhase() {
            ByteArrayOutputStream phase = new ByteArrayOutputStream();
            HttpServices.postData(baseJobUrl + "/phase", phase, null);
            return Phase.valueOf(phase.toString());
        }

        public String getErrorMsg() {
            try {
                DataGroup[] results = VoTableReader.voToDataGroups(baseJobUrl + "/error");
                DataGroup errorTbl = results[0];
                return errorTbl.getAttribute("QUERY STATUS");
            } catch (IOException e) {
                logger.error(e);
            }
            return "";
        }

        public long getTimeout() {
            ByteArrayOutputStream duration = new ByteArrayOutputStream();
            HttpServices.postData(baseJobUrl + "/executionduration", duration, null);
            return Long.valueOf(duration.toString());
        }

        public void setTimeout(long duration) {
            HttpServices.postData(baseJobUrl + "/executionduration", new ByteArrayOutputStream(),
                    new HttpServiceInput().setParam("EXECUTIONDURATION", String.valueOf(duration)));
        }

        protected String getBaseJobUrl() {
            return baseJobUrl;
        }
    }
}











