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
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.*;


@SearchProcessorImpl(id = AsyncTapQuery.ID, params = {
        @ParamDoc(name = "serviceUrl", desc = "base TAP url endpoint excluding '/async'"),
        @ParamDoc(name = "QUERY", desc = "query string"),
        @ParamDoc(name = "LANG", desc = "defaults to ADQL"),
        @ParamDoc(name = "MAXREC", desc = "maximum number of records to be returned")
})
public class AsyncTapQuery extends AsyncSearchProcessor {
    public static final String ID = "AsyncTapQuery";

    private static int MAXREC_HARD = AppProperties.getIntProperty("tap.maxrec.hardlimit", 10000000);

    public AsyncJob submitRequest(ServerRequest request) throws DataAccessException {
        String serviceUrl = request.getParam("serviceUrl");
        String queryStr = createQueryString(request);
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
        
        Ref<String> location = new Ref<>();
        HttpServices.postData(inputs, (method -> {
            location.setSource(HttpServices.getResHeader(method, "Location", null));
        }));
        
        if (location.getSource() == null) {
            throw new DataAccessException("Failed to submit async job to "+serviceUrl);
        }

        AsyncTapJob asyncTap = new AsyncTapJob(location.getSource());
        if (asyncTap.getPhase() == AsyncJob.Phase.PENDING) {
            HttpServices.postData(HttpServiceInput.createWithCredential(asyncTap.baseJobUrl + "/phase").setParam("PHASE", "RUN"));
        }
        return asyncTap;
    }


    /**
     * override this function to convert a request into an ADQL QUERY string
     * @param request server request
     * @return an ADQL QUERY string
     */
    private String createQueryString(ServerRequest request) {
        return request.getParam("QUERY");
    }


    public class AsyncTapJob implements AsyncJob  {
        private Logger.LoggerImpl logger = Logger.getLogger();
        private String baseJobUrl;

        AsyncTapJob(String baseJobUrl) {
            this.baseJobUrl = baseJobUrl;
        }

        public DataGroup getDataGroup() throws DataAccessException {
            try {
                //download file first: failing to parse gaia results with topcat SAX parser from url
                String filename = getFilename(baseJobUrl);
                File outFile = File.createTempFile(filename, ".vot", ServerContext.getTempWorkDir());
                HttpServiceInput input = HttpServiceInput.createWithCredential(baseJobUrl + "/results/result")
                                                         .setFollowRedirect(false);
                HttpServices.getData(input, (method -> {
                    try {
                        if(HttpServices.isOk(method)) {
                            HttpServices.defaultHandler(outFile).handleResponse(method);
                        } else if (HttpServices.isRedirected(method)) {
                            String location = HttpServices.getResHeader(method, "Location", null);
                            if (location != null) {
                                HttpServices.getData(HttpServiceInput.createWithCredential(location), outFile);
                            } else {
                                throw new RuntimeException("Request redirected without a location header");
                            }
                        } else {
                            throw new RuntimeException("Request failed with status:" + method.getStatusText());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }));

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
                return results[0];
            } catch (Exception e) {
                throw new DataAccessException("Failure when retrieving results from "+baseJobUrl+"/results/result\n"+
                        e.getMessage());
            }
        }

        public boolean cancel() {
            return !HttpServices.postData(
                        HttpServiceInput.createWithCredential(baseJobUrl + "/phase").setParam("PHASE", Phase.ABORTED.name()),
                        new ByteArrayOutputStream()
            ).isError();
        }

        public Phase getPhase() throws DataAccessException {
            ByteArrayOutputStream phase = new ByteArrayOutputStream();
            HttpServices.Status status = HttpServices.getData(HttpServiceInput.createWithCredential(baseJobUrl + "/phase"), phase);
            if (status.isError()) {
                throw new DataAccessException("Error getting phase from "+baseJobUrl+" "+status.getErrMsg());
            }
            try {
                return Phase.valueOf(phase.toString().trim());
            } catch (Exception e) {
                logger.error("Unknown phase \""+phase.toString()+"\" from service "+baseJobUrl);
                return Phase.UNKNOWN;
            }
        }

        public String getErrorMsg()  {
            String errorUrl = baseJobUrl + "/error";

            Ref<String> err = new Ref<>();
            HttpServices.Status status = HttpServices.getData(HttpServiceInput.createWithCredential(errorUrl),
                    (method -> {
                        boolean isText = false;
                        String contentType = HttpServices.getResHeader(method, "Content-Type", "");
                        if (contentType.startsWith("text/plain")) {
                            isText = true;
                        }
                        try {
                            if (isText) {
                                // error is text doc
                                err.setSource(HttpServices.getResponseBodyAsString(method));
                            } else {
                                // error is VOTable doc
                                InputStream is = HttpServices.getResponseBodyAsStream(method);
                                try {
                                    String voError = VoTableReader.getError(is, errorUrl);
                                    if (voError == null) {
                                        voError = "Non-compliant error doc " + errorUrl;
                                    }
                                    err.setSource(voError);
                                } finally {
                                    FileUtil.silentClose(is);
                                }
                            }
                        } catch (DataAccessException e) {
                            err.setSource(e.getMessage());
                        } catch (Exception e) {
                            err.setSource("Unable to get error from " + errorUrl);
                        }
                    }));
            if (status.isError()) {
                return "Unable to get error from "+baseJobUrl+" "+status.getErrMsg();
            }
            return err.getSource();
        }

        public long getTimeout() {
            ByteArrayOutputStream duration = new ByteArrayOutputStream();
            HttpServices.postData(HttpServiceInput.createWithCredential(baseJobUrl + "/executionduration"), duration);
            return Long.valueOf(duration.toString());
        }

        public void setTimeout(long duration) {
            HttpServices.postData(
                    HttpServiceInput.createWithCredential(baseJobUrl + "/executionduration")
                            .setParam("EXECUTIONDURATION",
                    String.valueOf(duration)), new ByteArrayOutputStream()
            );
        }

        protected String getBaseJobUrl() {
            return baseJobUrl;
        }

        private String getFilename(String urlStr) {
            return urlStr.replace("(http:|https:)", "").replace("/", "");
        }
    }
}











