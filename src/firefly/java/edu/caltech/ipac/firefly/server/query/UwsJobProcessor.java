/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FileUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase;

/**
 * Date: Sept 19, 2018
 *
 * @author loi
 */

@SearchProcessorImpl(id = UwsJobProcessor.ID, params = {
        @ParamDoc(name = UwsJobProcessor.JOB_URL, desc = "URL of a submitted UWS job")
})
public abstract class UwsJobProcessor extends EmbeddedDbProcessor {
    public static final String ID = "UwsJob";
    public static final String JOB_URL = "jobUrl";

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private String jobUrl;

    abstract HttpServiceInput createInput(TableServerRequest request) throws DataAccessException;

    public Job.Type getType() { return Job.Type.UWS; }

//====================================================================
//  implements EmbeddedDbProcessor
//====================================================================

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {

        jobUrl = req.getParam(JOB_URL);
        if (jobUrl == null) {
            jobUrl = submitJob(req);
            if (jobUrl != null) runJob(jobUrl);
        }

        int cnt = 0;
        try {
            while (true) {
                cnt++;
                switch (getPhase(jobUrl)) {
                    case COMPLETED:
                        return getResult(req);
                    case ERROR:
                    case UNKNOWN: {
                        String error = getError(jobUrl);
                        applyIfNotEmpty(getJob(), v -> v.setError(400,  error));
                        throw new DataAccessException( error );
                    }
                    case ABORTED:
                        if (getJob() != null) getJob().setPhase(Phase.ABORTED);
                        throw new DataAccessException.Aborted();
                    case PENDING:
                    case EXECUTING:
                    case QUEUED:
                    default:
                    {
                        int wait = cnt < 3 ? 500 : cnt < 20 ? 1000 : 2000;
                        TimeUnit.MILLISECONDS.sleep(wait);
                    }
                }
                jobExecIf(v -> v=null);         // check job phase.. exit loop if aborted.
            }
        } catch (InterruptedException e) {
            onAbort();
            throw new DataAccessException.Aborted();
        }
    }

    /**
     * Submit the UWS request then execute it if it's not currently executing.
     * @param req request info needed to create/submit the job
     * @return the job's URL.
     * @throws DataAccessException an any errors
     */
    String submitJob(TableServerRequest req) throws DataAccessException {
        HttpServiceInput input = createInput(req);
        final Ref<String> jobUrl = new Ref<>();
        input.setFollowRedirect(false);         // ensure redirect is off, although this is handled by HttpServices already.
        HttpServices.Status status = HttpServices.postData(input, (method) -> {
            jobUrl.set(HttpServices.getResHeader(method, "Location", null));
            if (jobUrl.has()) {
                applyIfNotEmpty(getJob(), v -> v.getJobInfo().setDataOrigin(jobUrl.get()));
                return HttpServices.Status.ok();
            } else {
                // Location header contains jobUrl.  Must be an error when there's not Location header
                String error = HttpServices.isOk(method) ? parseError(method, input.getRequestUrl())
                        : String.format("Error submitting job to %s: %s", input.getRequestUrl(), method.getStatusText());

                applyIfNotEmpty(getJob(), v -> v.setError(method.getStatusCode(), error));
                return new HttpServices.Status(400, error);
            }
        });
        if (status.isError()) throw new DataAccessException(status.getErrMsg());
        return jobUrl.get();
    }

    void runJob(String jobUrl) throws DataAccessException {
        if (jobUrl == null) return;
        if (getPhase(jobUrl) == Phase.PENDING) {
            jobUrl = jobUrl.trim().replaceAll("/$", "");        // cleanup URL
            HttpServices.postData(HttpServiceInput.createWithCredential(jobUrl + "/phase").setParam("PHASE", "RUN"));
        }
        applyIfNotEmpty(getJob(), v -> v.progressDesc("UWS job submitted..."));
    }

    /**
     * Create the HttpServiceInput used to retrieve a single result from this query.
     * UWS relies on /{jobs}/{job-id}/results to list all of the results.  TAP defines
     * /results/result when there's only one result returned from the query.
     * @param jobUrl the UWS job url
     * @return  HttpServiceInput used to query the result
     */
    HttpServiceInput createSingleResultInput(String jobUrl) {
        // using followRedirect because TAP specifically say this endpoint may be redirected.
        return HttpServiceInput.createWithCredential(jobUrl + "/results/result");
    }

    public String getJobUrl() { return jobUrl; }

    public DataGroup getResult(TableServerRequest request) throws DataAccessException {
        HttpServiceInput input = createSingleResultInput(jobUrl).setFollowRedirect(true);            // ensure redirects are followed
        try {
            //download file first: failing to parse gaia results with topcat SAX parser from url
            String filename = getFilename(jobUrl);
            File outFile = File.createTempFile(filename, ".vot", QueryUtil.getTempDir(request));
            HttpServices.Status status = HttpServices.getData(input, outFile);
            if (status.isError()) {
                throw new DataAccessException(String.format("Fail to fetch result at: %s \n\t with exception: %s",
                        input.getRequestUrl(), status.getErrMsg()));
            }
            DataGroup[] results = VoTableReader.voToDataGroups(outFile.getAbsolutePath());
            return results.length > 0 ? results[0] : null;
        } catch (Exception e) {
            throw new DataAccessException(String.format("Fail to fetch result at: %s \n\t with exception: %s",
                    input.getRequestUrl(), e.getMessage()));
        }
    }

    String getFilename(String urlStr) {
        return urlStr.replace("(http:|https:)", "").replace("/", "");
    }

//====================================================================
//  UWS utils
//====================================================================
    public static JobInfo getUwsJobInfo(String jobUrl) throws DataAccessException {
        if (isEmpty(jobUrl)) return null;

        Ref<JobInfo> jInfo = new Ref<>();
        HttpServices.Status status = HttpServices.getData(new HttpServiceInput(jobUrl), method -> {
            try {
                jInfo.set(parse(method.getResponseBodyAsStream()));
            } catch (Exception e) {
                return new HttpServices.Status(400, String.format("Fail to fetch UWS job info at: %s \n\t with exception: %s", jobUrl, e.getMessage()));
            }
            return HttpServices.Status.ok();
        });

        if (status.isError()) throw new DataAccessException(status.getErrMsg());

        return jInfo.get();
    }

    public static void onAbort(String jobUrl) {
        cancel(jobUrl);
    }

    public static boolean cancel(String jobUrl) {
        if (isEmpty(jobUrl)) return false;
        boolean cancelled = !HttpServices.getData(
                HttpServiceInput.createWithCredential(jobUrl + "/phase").setParam("PHASE", Phase.ABORTED.name()),
                new ByteArrayOutputStream()
        ).isError();
        if (cancelled) {
            Logger.getLogger().debug("UWS job cancelled: " + jobUrl);
        }
        return cancelled;
    }

    public static Phase getPhase(String jobUrl) throws DataAccessException {

        ByteArrayOutputStream phase = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(HttpServiceInput.createWithCredential(jobUrl + "/phase"), phase);
        if (status.isError()) {
            throw new DataAccessException("Error getting phase from "+ jobUrl +" "+status.getErrMsg());
        }
        try {
            return Phase.valueOf(phase.toString().trim());
        } catch (Exception e) {
            logger.error("Unknown phase \""+phase.toString()+"\" from service "+ jobUrl);
            return Phase.UNKNOWN;
        }
    }

    public static String getError(String jobUrl)  {
        String errorUrl = jobUrl + "/error";
        HttpServiceInput input = HttpServiceInput.createWithCredential(errorUrl);
        HttpServices.Status status = HttpServices.getData(input, (method -> {
            try {
                return new HttpServices.Status(200, parseError(method, errorUrl));
            } catch (Exception e) {
                return new HttpServices.Status(500, "Unexpected exception: " + e.getMessage());
            }
        }));
        return status.getErrMsg();
    }

    public long getTimeout(String jobUrl) {
        ByteArrayOutputStream duration = new ByteArrayOutputStream();
        HttpServices.postData(HttpServiceInput.createWithCredential(jobUrl + "/executionduration"), duration);
        return Long.parseLong(duration.toString());
    }

    public void setTimeout(String jobUrl, long duration) {
        HttpServices.postData(
                HttpServiceInput.createWithCredential(jobUrl + "/executionduration")
                        .setParam("EXECUTIONDURATION",
                                String.valueOf(duration)), new ByteArrayOutputStream()
        );
    }

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public static JobInfo parse(InputStream is) throws Exception {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        if (doc != null) {
            Element root = doc.getDocumentElement();
            String id = getVal(root, "uws:jobId");
            if (root.getTagName().equals("uws:job") && !isEmpty(id)) {              // verify that this is a UWS job doc
                JobInfo jobInfo = new JobInfo(id);
                applyIfNotEmpty(getVal(root, "uws:runId"), jobInfo::setRunId);
                applyIfNotEmpty(getVal(root, "uws:ownerId"), jobInfo::setOwner);
                applyIfNotEmpty(getVal(root, "uws:phase"), v -> jobInfo.setPhase(Phase.valueOf(v)));

                applyIfNotEmpty(getVal(root, "uws:quote"), v -> jobInfo.setQuote(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, "uws:creationTime"), v -> jobInfo.setCreationTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, "uws:startTime"), v -> jobInfo.setStartTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, "uws:endTime"), v -> jobInfo.setEndTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, "uws:executionDuration"), v -> jobInfo.setExecutionDuration(Integer.parseInt(v)));
                applyIfNotEmpty(getVal(root, "uws:destruction"), v -> jobInfo.setDestruction(Instant.parse(v)));

                applyIfNotEmpty(getEl(root, "uws:parameters"), params -> {
                    NodeList plist = params.getElementsByTagName("uws:parameter");
                    for (int i = 0; i < plist.getLength(); i++) {
                        Node p = plist.item(i);
                        jobInfo.getParams().put(getAttr(p, "id"), p.getTextContent());
                    }
                });

                applyIfNotEmpty(getEl(root, "uws:results"), results -> {
                    NodeList rlist = results.getElementsByTagName("uws:result");
                    for (int i = 0; i < rlist.getLength(); i++) {
                        Node r = rlist.item(i);
                        jobInfo.getResults().add(getAttr(r,"xlink:href"));
                    }
                });

                applyIfNotEmpty(getEl(root, "uws:errorSummary"), errsum -> {
                    String type = errsum.getAttribute("type");
                    int code = type == null || type.equals("transient") ? 500 : 400;
                    jobInfo.setError(new JobInfo.Error(code, getVal(errsum, "uws:message")));
                });

                return jobInfo;
            }
        }
        throw new ParseException("Invalid UWS job document", 0);
    }

    private static String parseError(HttpMethod method, String errorUrl) {

        String errMsg;
        if (HttpServices.isOk(method)) {
            String contentType = HttpServices.getResHeader(method, "Content-Type", "");
            boolean isText = contentType.startsWith("text/plain");
            try {
                if (isText) {
                    // error is text doc
                    errMsg = HttpServices.getResponseBodyAsString(method);
                } else {
                    // error is VOTable doc
                    InputStream is = HttpServices.getResponseBodyAsStream(method);
                    try {
                        String voError = VoTableReader.getError(is, errorUrl);
                        if (voError == null) {
                            voError = "Non-compliant error doc " + errorUrl;
                        }
                        errMsg = voError;
                    } finally {
                        FileUtil.silentClose(is);
                    }
                }
            } catch (Exception e) {
                errMsg = String.format("Error retrieving error document from %s: %s", errorUrl, e.getMessage());
            }
        } else {
            errMsg = String.format("Error retrieving error document from %s: %s", errorUrl, method.getStatusText());
        }
        return errMsg;
    }

    /**
     * @return  return the value of the first element matches the given tag, or null
     */
    private static String getVal(Element from, String tag) {
        NodeList rval = from.getElementsByTagName(tag);
        return (rval.getLength() > 0) ? rval.item(0).getTextContent() : null;
    }

    private static Element getEl(Element from, String tag) {
        NodeList rval = from.getElementsByTagName(tag);
        return (rval.getLength() > 0) ? (Element)rval.item(0) : null;
    }

    private static String getAttr(Node from, String name) {
        Node a = from.getAttributes().getNamedItem(name);
        return a == null ? null : a.getTextContent();
    }

}
