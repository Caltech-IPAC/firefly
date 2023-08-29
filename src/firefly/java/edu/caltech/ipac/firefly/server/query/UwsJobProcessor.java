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
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FileUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.firefly.server.network.HttpServices.defaultHandler;
import static edu.caltech.ipac.firefly.server.network.HttpServices.getWithAuth;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * Date: Sept 19, 2018
 *
 * @author loi
 */

@SearchProcessorImpl(id = UwsJobProcessor.ID, params = {
        @ParamDoc(name = UwsJobProcessor.JOB_URL, desc = "URL of a submitted UWS job")
})
public class UwsJobProcessor extends EmbeddedDbProcessor {
    public static final String ID = "UwsJob";
    public static final String JOB_URL = "jobUrl";

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private String jobUrl;

    /**
     * override this method to be able to create a HttpServiceInput object
     * @param request request info needed to create HttpServiceInput object
     * @return HttpServiceInput object or null
     * @throws DataAccessException when encountering an error
     */
     HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        return null;
     }


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
        if (input == null) throw new DataAccessException("createInput returned null");
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

    public String getJobUrl() { return jobUrl; }

    public DataGroup getResult(TableServerRequest request) throws DataAccessException {

        JobInfo jobInfo = getUwsJobInfo(jobUrl);

        if (jobInfo == null || jobInfo.getResults().size() < 1) {
            throw createDax("UWS job completed without results", jobUrl, null);
        } else {
            List<JobInfo.Result> results = jobInfo.getResults();
            if (results.size() == 1) {
                return getTableResult(results.get(0).href(), QueryUtil.getTempDir(request));
            } else {
                return convertResultsToObsCoreTable(results);
            }
        }
    }

    static String getFilename(String urlStr) {
        return urlStr.replace("(http:|https:)", "").replace("/", "");
    }

//====================================================================
//  UWS utils
//====================================================================
    static DataAccessException createDax(String url, String title, String errMsg) {
        String msg = String.format("%s from the URL: [%s]", title, url);
        if (errMsg != null) msg += "\n\t with exception: " + errMsg;
        return new DataAccessException(msg);
    }


    public DataGroup convertResultsToObsCoreTable(List<JobInfo.Result> results) {
        DataGroup table = new DataGroup("UWS results", new DataType[]{
                new DataType("dataproduct_type", String.class),
                new DataType("access_url", String.class),
                new DataType("access_format", String.class),
                new DataType("access_estsize", Integer.class)
        });

        results.forEach(result -> {
            DataObject row = new DataObject(table);
            String prodtype = guessProductType(result);
            row.setData(new Object[]{prodtype, result.href(), result.mimeType(), getInt(result.size(), 0)});
            table.add(row);
        });
        table.trimToSize();
        return table;
    }

    private String guessProductType(JobInfo.Result result) {
        String ext = FileUtil.getExtension(result.href());
        String mimeType = isEmpty(result.mimeType()) ? "" : result.mimeType().toLowerCase();
        if (mimeType.matches("text/plain|.*/csv|.*/xml|.*/x-votable.*") ||
            ext.matches("tbl|xml|vot|csv|tsv|ipac")) {
            return "table";
        } else if (mimeType.matches(".*image.*|.*/fits.*") ||
                   ext.matches("fits")) {
            return "image";
        }

        return "unknown";
    }


    public static DataGroup getTableResult(String url, File workDir) throws DataAccessException {
        try {
            // download file first: failing to parse gaia results with topcat SAX parser from url
            String filename = getFilename(url);
            File outFile = File.createTempFile(filename, ".vot", workDir);

            // Must followRedirect because TAP specifically say this endpoint may be redirected.
            // Using 'getWithAuth' because it will handle credential when redirected
            HttpServices.Status status = getWithAuth(url, HttpServices.defaultHandler(outFile));
            if (status.isError()) {
                throw createDax("Fail to fetch result", url, status.getErrMsg());
            }
            DataGroup[] results = VoTableReader.voToDataGroups(outFile.getAbsolutePath());
            return results.length > 0 ? results[0] : null;

        } catch (Exception e) {
            throw createDax("Fail to fetch result", url, e.getMessage());
        }
    }

    public static JobInfo getUwsJobInfo(String jobUrl) throws DataAccessException {
        if (isEmpty(jobUrl)) return null;

        Ref<JobInfo> jInfo = new Ref<>();
        HttpServices.Status status = getWithAuth(jobUrl, method -> {
            try {
                jInfo.set(parse(method.getResponseBodyAsStream()));
                return HttpServices.Status.ok();
            } catch (Exception e) {
                return new HttpServices.Status(400, e.getMessage());
            }
        });
        if (status.isError()) throw createDax("Fail to fetch UWS job info", jobUrl, status.getErrMsg());

        return jInfo.get();
    }

    public static void onAbort(String jobUrl) {
        cancel(jobUrl);
    }

    public static boolean cancel(String jobUrl) {
        if (isEmpty(jobUrl)) return false;
        HttpServiceInput input = new HttpServiceInput(jobUrl + "/phase").setParam("PHASE", Phase.ABORTED.name());
        boolean cancelled = !HttpServices.getWithAuth(input, m -> HttpServices.Status.ok()).isError();
        if (cancelled) {
            Logger.getLogger().debug("UWS job cancelled: " + jobUrl);
        }
        return cancelled;
    }

    public static Phase getPhase(String jobUrl) throws DataAccessException {

        ByteArrayOutputStream phase = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getWithAuth(new HttpServiceInput(jobUrl + "/phase"), defaultHandler(phase));
        if (status.isError()) {
            throw createDax("Error getting phase", jobUrl, status.getErrMsg());
        }
        try {
            return Phase.valueOf(phase.toString().trim());
        } catch (Exception e) {
            logger.error("Unknown phase \"" + phase + "\" from service "+ jobUrl);
            return Phase.UNKNOWN;
        }
    }

    public static String getError(String jobUrl)  {
        String errorUrl = jobUrl + "/error";
        HttpServices.Status status = HttpServices.getWithAuth(errorUrl, method -> {
            try {
                return new HttpServices.Status(200, parseError(method, errorUrl));
            } catch (Exception e) {
                return new HttpServices.Status(500, "Unexpected exception: " + e.getMessage());
            }
        });
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

            final Ref<String> prefix = new Ref<>();
            // resolve uws prefix:  normally empty-string for default namespace or 'uws' when prefix is used.
            NamedNodeMap attribs = root.getAttributes();
            for (int i = 0; i < attribs.getLength(); i++) {
                String name = attribs.item(i).getNodeName();
                String val = attribs.item(i).getNodeValue();
                if (name.startsWith("xmlns") && val != null && val.toLowerCase().contains("www.ivoa.net/xml/uws")) {
                    String[] parts = name.split(":");
                    prefix.set(parts.length == 1 ? "" : parts[1].trim() + ":");
                }
            }
            if (prefix.has() && root.getTagName().equals(prefix + "job")) {              // verify that this is a UWS job doc
                String id = getVal(root, prefix + "jobId");
                JobInfo jobInfo = new JobInfo(id);
                applyIfNotEmpty(getVal(root, prefix + "runId"), jobInfo::setRunId);
                applyIfNotEmpty(getVal(root, prefix + "ownerId"), jobInfo::setOwner);
                applyIfNotEmpty(getVal(root, prefix + "phase"), v -> jobInfo.setPhase(Phase.valueOf(v)));

                applyIfNotEmpty(getVal(root, prefix + "quote"), v -> jobInfo.setQuote(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, prefix + "creationTime"), v -> jobInfo.setCreationTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, prefix + "startTime"), v -> jobInfo.setStartTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, prefix + "endTime"), v -> jobInfo.setEndTime(Instant.parse(v)));
                applyIfNotEmpty(getVal(root, prefix + "executionDuration"), v -> jobInfo.setExecutionDuration(Integer.parseInt(v)));
                applyIfNotEmpty(getVal(root, prefix + "destruction"), v -> jobInfo.setDestruction(Instant.parse(v)));

                applyIfNotEmpty(getEl(root, prefix + "parameters"), params -> {
                    NodeList plist = params.getElementsByTagName(prefix + "parameter");
                    for (int i = 0; i < plist.getLength(); i++) {
                        Node p = plist.item(i);
                        jobInfo.getParams().put(getAttr(p, "id"), p.getTextContent());
                    }
                });

                applyIfNotEmpty(getEl(root, prefix + "results"), results -> {
                    NodeList rlist = results.getElementsByTagName(prefix + "result");
                    for (int i = 0; i < rlist.getLength(); i++) {
                        Node r = rlist.item(i);
                        jobInfo.getResults().add(
                                new JobInfo.Result(
                                    getAttr(r,"xlink:href"),
                                    getAttr(r,"xlink:type"),        // added for completeness. No idea how it's used.
                                    getAttr(r, "mime-type"),
                                    getAttr(r, "size")
                                )
                        );
                    }
                });

                applyIfNotEmpty(getEl(root, "uws:errorSummary"), errsum -> {
                    String type = errsum.getAttribute("type");
                    int code = type == null || type.equals("transient") ? 500 : 400;
                    jobInfo.setError(new JobInfo.Error(code, getVal(errsum, prefix + "message")));
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


