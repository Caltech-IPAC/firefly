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

import static edu.caltech.ipac.firefly.core.Util.Try;
import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobManager.getJobInfo;
import static edu.caltech.ipac.firefly.server.network.HttpServices.*;
import static edu.caltech.ipac.firefly.server.query.DaliUtil.*;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * Date: Sept 19, 2018
 *
 * @author loi
 */

@SearchProcessorImpl(id = UwsJobProcessor.ID, params = {
        @ParamDoc(name = UwsJobProcessor.JOB_URL, desc = "URL of a submitted UWS job; for monitoring and retrieving the results"),
})
public class UwsJobProcessor extends EmbeddedDbProcessor {
    public static final String ID = "UwsJob";
    public static final String JOB_URL = "jobUrl";
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final String JOBS = "jobs";
    private static final String JOB_REF = "jobref";

    private String jobUrl;

    /**
     * Return a HttpServiceInput with the standard DALI parameters taken from the given request.
     * override this method to be able to create a HttpServiceInput object
     * @param request request info needed to create HttpServiceInput object
     * @return HttpServiceInput object or null
     * @throws DataAccessException when encountering an error
     */
     protected HttpServiceInput createInput(TableServerRequest request) throws DataAccessException {
        var inputs = new HttpServiceInput();
        populateKnownInputs(inputs, request);
        return inputs;
     }


    public Job.Type getType() { return Job.Type.UWS; }
    public boolean isSelfManaged() { return true; }         // defer to remote UWS service for job update

    public void onAbort() {
        // send abort request.  ignore if there's error
        if (isEmpty(jobUrl)) return;
        String phaseUrl = jobUrl.trim().replaceAll("/$", "") + "/phase" ;        // remove trailing slash
        HttpServices.postData(
                HttpServiceInput.createWithCredential(phaseUrl)
                        .setParam("PHASE", "ABORT")
        );
        Logger.getLogger().debug("UWS job aborted: " + jobUrl);
    }

//====================================================================
//  implements EmbeddedDbProcessor
//====================================================================

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // this worker is self-managed, so it needs to update job status
        try {
            jobUrl = req.getParam(JOB_URL);
            if (jobUrl == null) {
                jobUrl = submitJob(req);
                if (jobUrl != null) runJob(jobUrl);
                updateJob(ji -> {
                    ji.setPhase(Phase.PENDING);
                    ji.getMeta().setProgress(10, "UWS job submitted");
                });
            }
        } catch (Exception e) {
            updateJob(ji -> {
                ji.setError(new JobInfo.Error(400, e.getMessage()));
                ji.getMeta().setProgress(100);
            });
            throw new DataAccessException(e.getMessage());
        } finally {
            sendJobUpdate(ji -> {
                ji.getAux().setSvcUrl(jobUrl);
            });
        }

        int cnt = 0;
        try {
            while (true) {
                cnt++;
                JobInfo uwsJob = getUwsJobInfo(jobUrl);
                if (uwsJob == null) {
                    String msg = "Failed to retrieve UWS job info";
                    updateJob(ji -> ji.setError(new JobInfo.Error(500, msg)));
                    throw new DataAccessException(msg);
                }

                updateJob(ji -> ji.copyFrom(uwsJob));
                Phase phase = ifNotNull(uwsJob.getPhase()).getOrElse(Phase.UNKNOWN);

                if (phase == Phase.COMPLETED) {
                    return getResult(req);
                } else if (phase == Phase.ABORTED) {
                    throw new DataAccessException.Aborted();        // exit; stop tracking
                } else if (phase == Phase.UNKNOWN) {
                    updateJob(ji -> ji.setError(new JobInfo.Error(500, "Unknown phase")));
                } else {
                    int wait = cnt < 3 ? 500 : cnt < 20 ? 1000 : 2000;
                    TimeUnit.MILLISECONDS.sleep(wait);
                    if (phase == Phase.EXECUTING) {
                        int progress = (int)(95 * (1 - Math.pow(2.0 / 3.0, cnt)));
                        sendJobUpdate(ji -> {
                            ji.getMeta().setProgress(progress, "Job is being processed");
                            ji.setPhase(phase);
                        });
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new DataAccessException.Aborted();
        } finally {
            sendJobUpdate(ji -> {
                ji.getMeta().setProgress(100);
            });
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
                return HttpServices.Status.ok();
            } else {
                // Location header contains jobUrl.  Must be an error when there's not a Location header
                String error = HttpServices.isOk(method) ? parseError(method, input.getRequestUrl())
                        : String.format("Error submitting job to %s: %s", input.getRequestUrl(), method.getStatusText());

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
    }

    public String getJobUrl() { return jobUrl; }

    public DataGroup getResult(TableServerRequest request) throws DataAccessException {

        JobInfo jobInfo = ifNotNull(getJob()).get(j -> getJobInfo(j.getJobId()));
        if (jobInfo == null) jobInfo = getUwsJobInfo(jobUrl);           // there's no job when it's not running in the background

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

//====================================================================
//  UWS utils
//====================================================================

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
            File outFile = File.createTempFile("results-", ".vot", workDir);

            // Must followRedirect because TAP specifically say this endpoint may be redirected.
            // Using 'getWithAuth' because it will handle credential when redirected
            HttpServices.Status status = getWithAuth(url, HttpServices.defaultHandler(outFile));
            if (status.isError()) {
                throw createDax("Failed to retrieve the result from", url, status.getException());
            }
            DataGroup[] results = VoTableReader.voToDataGroups(outFile.getAbsolutePath());
            return results.length > 0 ? results[0] : null;

        } catch (Exception e) {
            throw createDax("Fail to fetch result", url, e);
        }
    }

    public static JobInfo getUwsJobInfo(String jobUrl) throws DataAccessException {
        if (isEmpty(jobUrl)) return null;

        Ref<JobInfo> jInfo = new Ref<>();
        HttpServices.Status status = getWithAuth(jobUrl, method -> {
            try {
                Document doc = parse(getResponseBodyAsStream(method));
                jInfo.set(convertToJobInfo(doc));
                return HttpServices.Status.ok();
            } catch (Exception e) {
                return new HttpServices.Status(400, e.getMessage());
            }
        });
        if (status.isError()) throw createDax("Fail to fetch UWS job info", jobUrl, status.getException());

        return jInfo.get();
    }

    public static Phase getPhase(String jobUrl) throws DataAccessException {

        ByteArrayOutputStream phase = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getWithAuth(new HttpServiceInput(jobUrl + "/phase"), defaultHandler(phase));
        if (status.isError()) {
            throw createDax("Error getting phase", jobUrl, status.getException());
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

    public static List<Result> convertToJobList(Document doc) throws Exception {
        List<Result> rval = new java.util.ArrayList<>();
        String prefix = getUwsNS(doc);
        Element jobs = doc.getDocumentElement();
        if (jobs != null && jobs.getTagName().equals(prefix + JOBS)) {
            NodeList jlist = jobs.getElementsByTagName(prefix + JOB_REF);
            for (int i = 0; i < jlist.getLength(); i++) {
                Node r = jlist.item(i);
                rval.add( new JobInfo.Result( getAttr(r, "id"), getAttr(r,"xlink:href"),null,null));
            }
        }
        return rval;
    }

    public static JobInfo convertToJobInfo(Document doc) throws Exception {
        String prefix = getUwsNS(doc);
        Element root = doc.getDocumentElement();
        if (prefix != null && root.getTagName().equals(prefix + "job")) {              // verify that this is a UWS job doc
            String id = getVal(root, prefix + JOB_ID);
            JobInfo jobInfo = new JobInfo(id);
            applyIfNotEmpty(getVal(root, prefix + RUN_ID), jobInfo::setRunId);
            applyIfNotEmpty(getVal(root, prefix + OWNER_ID), jobInfo::setOwnerId);
            applyIfNotEmpty(getVal(root, prefix + PHASE), v -> jobInfo.setPhase(
                    Try.it(() -> Phase.valueOf(v)).getOrElse(Phase.UNKNOWN))
            );
            applyIfNotEmpty(getVal(root, prefix + QUOTE), v -> jobInfo.setQuote(getInstant(v)));
            applyIfNotEmpty(getVal(root, prefix + CREATION_TIME), v -> jobInfo.setCreationTime(getInstant(v)));
            applyIfNotEmpty(getVal(root, prefix + START_TIME), v -> jobInfo.setStartTime(getInstant(v)));
            applyIfNotEmpty(getVal(root, prefix + END_TIME), v -> jobInfo.setEndTime(getInstant(v)));
            applyIfNotEmpty(getVal(root, prefix + EXECUTION_DURATION), v -> jobInfo.setExecutionDuration(Integer.parseInt(v)));
            applyIfNotEmpty(getVal(root, prefix + DESTRUCTION), v -> jobInfo.setDestruction(getInstant(v)));

            applyIfNotEmpty(getEl(root, prefix + PARAMETERS), params -> {
                NodeList plist = params.getElementsByTagName(prefix + PARAMETER);
                for (int i = 0; i < plist.getLength(); i++) {
                    Node p = plist.item(i);
                    jobInfo.getParams().put(getAttr(p, "id"), p.getTextContent());
                }
            });

            applyIfNotEmpty(getEl(root, prefix + RESULTS), results -> {
                NodeList rlist = results.getElementsByTagName(prefix + RESULT);
                for (int i = 0; i < rlist.getLength(); i++) {
                    Node r = rlist.item(i);
                    jobInfo.getResults().add(
                            new JobInfo.Result(
                                getAttr(r, "id"),
                                getAttr(r,"xlink:href"),
                                getAttr(r, "mime-type"),
                                getAttr(r, "size")
                            )
                    );
                }
            });

            applyIfNotEmpty(getEl(root, prefix + ERROR_SUMMARY), errsum -> {
                String type = errsum.getAttribute(ERROR_TYPE);
                int code = type.equals("transient") ? 500 : 400;
                String msg = getVal(errsum, prefix + ERROR_MSG);
                if (!isEmpty(msg)) {
                    jobInfo.setError(new JobInfo.Error(code, msg));
                }
            });

            return jobInfo;
        }
        throw new ParseException("Invalid UWS job document", 0);
    }

    public static Document parse(InputStream is) throws Exception {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        if (doc != null) {
            return doc;
        }
        throw new ParseException("Invalid UWS response", 0);
    }

    /**
     * @param doc the XML document to check
     * @return the UWS namespace prefix, or null if it's not a UWS document
     */
    public static String getUwsNS(Document doc) {
        Element root = doc.getDocumentElement();
        // resolve uws prefix:  normally empty-string for default namespace or 'uws' when prefix is used.
        NamedNodeMap attribs = root.getAttributes();
        for (int i = 0; i < attribs.getLength(); i++) {
            String name = attribs.item(i).getNodeName();
            String val = attribs.item(i).getNodeValue();
            if (name.startsWith("xmlns") && val != null && val.toLowerCase().contains("www.ivoa.net/xml/uws")) {
                String[] parts = name.split(":", 2);
                return parts.length == 1 ? "" : parts[1].trim() + ":";
            }
        }
        return null;
    }

    private static Instant getInstant(String v) {
        // UWS: Times must be expressed in the UTC timezone, and this is signified with the 'Z' timezone designator
        // many services failed to follow standard.  Apply simple fix when possible
        v = v.replaceFirst("[+-]0000$", "Z");
        v = v.endsWith("Z") ? v : v + "Z";
        try {
            return Instant.parse(v);
        } catch (Exception e) { return null; }
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


