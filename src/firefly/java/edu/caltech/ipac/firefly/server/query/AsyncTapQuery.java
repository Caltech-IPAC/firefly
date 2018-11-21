/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.commons.httpclient.Header;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

@SearchProcessorImpl(id = AsyncTapQuery.ID, params = {
        @ParamDoc(name = "serviceUrl", desc = "base TAP url endpoint excluding '/async'"),
        @ParamDoc(name = "QUERY", desc = "query string"),
        @ParamDoc(name = "LANG", desc = "defaults to ADQL")
})
public class AsyncTapQuery extends AsyncSearchProcessor {
    public static final String ID = "AsyncTapQuery";

    public AsyncJob submitRequest(ServerRequest request) throws DataAccessException {
        String serviceUrl = request.getParam("serviceUrl");
        String lang = request.getParam("LANG");
        String queryStr = createQueryString(request);

        HttpServiceInput inputs = new HttpServiceInput();
        if (!StringUtils.isEmpty(queryStr)) inputs.setParam("QUERY", queryStr);
        if (StringUtils.isEmpty(lang)) { lang = "ADQL"; }
        inputs.setParam("LANG", lang); // in tap 1.0, lang param is required
        inputs.setParam("request", "doQuery"); // in tap 1.0, request param is required
        
        Ref<String> location = new Ref<>();
        HttpServices.postData(serviceUrl + "/async", inputs, (method -> {
            Header loc = method.getResponseHeader("Location");
            if (loc != null) {
                location.setSource(loc.getValue().trim());
            }
        }));
        
        if (location.getSource() == null) {
            throw new DataAccessException("Failed to submit async job to "+serviceUrl);
        }

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

        public DataGroup getDataGroup() throws DataAccessException {
            try {
                //download file first: failing to parse gaia results with topcat SAX parser from url
                String filename = baseJobUrl.replace("(http:|https:)", "").replace("/", "");
                File outFile = File.createTempFile(filename, ".vot", ServerContext.getTempWorkDir());
                URLDownload.getDataToFile(new URL(baseJobUrl + "/results/result"), outFile);
                DataGroup[] results = VoTableReader.voToDataGroups(outFile.getAbsolutePath());
                if (results.length > 0) {
                    DataGroup dg = results[0];
                    LinkInfo jobLink = new LinkInfo();
                    jobLink.setID("IVOA_UWS_JOB");
                    jobLink.setTitle("Universal Worker Service Job");
                    jobLink.setHref(baseJobUrl);
                    // update table links
                    dg.getLinkInfos().add(0, jobLink);
                }  else {
                    return null;
                }
                return results.length > 0 ? results[0] : null;
            } catch (Exception e) {
                throw new DataAccessException("Failure when retrieving results from "+baseJobUrl+"/results/result\n"+
                        e.getMessage());
            }
        }

        public boolean cancel() {
            return ! HttpServices.postData(baseJobUrl + "/phase", new ByteArrayOutputStream(),
                                            new HttpServiceInput().setParam("PHASE", Phase.ABORTED.name()))
                                .isError();
        }

        public Phase getPhase() throws DataAccessException {
            ByteArrayOutputStream phase = new ByteArrayOutputStream();
            HttpServices.Status status = HttpServices.getData(baseJobUrl + "/phase", phase, null);
            if (status.isError()) {
                throw new DataAccessException("Error getting phase from "+baseJobUrl+" "+status.getErrMsg());
            }
            try {
                return Phase.valueOf(phase.toString().replaceAll("\\p{Cntrl}", ""));
            } catch (Exception e) {
                logger.error("Unknown phase \""+phase.toString()+"\" from service "+baseJobUrl);
                return Phase.UNKNOWN;
            }
        }

        public String getErrorMsg()  {
            try {
                DataGroup[] results = VoTableReader.voToDataGroups(baseJobUrl + "/error", false);
                DataGroup errorTbl = results[0];
                return errorTbl.getAttribute("QUERY_STATUS");
            } catch (IOException e) {
                logger.error(e);
            } catch (DataAccessException e) {
                return e.getMessage();
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











