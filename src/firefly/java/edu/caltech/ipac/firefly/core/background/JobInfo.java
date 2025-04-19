/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.AppProperties;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;


/**
 * Value object containing information of a Job
 *
 * Date: 9/29/21
 *
 * @author loi
 * @version : $
 */
public class JobInfo implements Serializable {

    public enum Phase {PENDING, QUEUED, EXECUTING, COMPLETED, ERROR, ABORTED, HELD, SUSPENDED, ARCHIVED, UNKNOWN}
    public static final Set<Phase> CLEANUP_PHASES_EXCLUDES = Set.of(Phase.PENDING, Phase.QUEUED, Phase.EXECUTING, Phase.SUSPENDED);
    private static final int LIFE_SPAN = AppProperties.getIntProperty("job.lifespan", 60*60*24);        // default lifespan in seconds; kill job if exceed

    // these are uws:job defined properties
    public static final String JOB_ID = "jobId";
    public static final String RUN_ID = "runId";
    public static final String OWNER_ID = "ownerId";
    public static final String PHASE = "phase";
    public static final String QUOTE = "quote";
    public static final String CREATION_TIME = "creationTime";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String EXECUTION_DURATION = "executionDuration";
    public static final String DESTRUCTION = "destruction";
    public static final String PARAMETERS = "parameters";
    public static final String PARAMETER = "parameter";
    public static final String RESULTS = "results";
    public static final String RESULT = "result";
    public static final String ERROR_SUMMARY = "errorSummary";
    public static final String ERROR_TYPE = "type";
    public static final String ERROR_MSG = "message";
    public static final String META = "meta";
    public static final String JOB_INFO = "jobInfo";

    // These are additional info that's not in defined in uws:job but is needed by Firefly
    // In serialized form, it will go under uws:jobInfo block
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_DESC = "progressDesc";
    public static final String JOB_TYPE = "type";
    public static final String SUMMARY = "summary";
    public static final String JOB_URL = "jobUrl";
    public static final String MONITORED = "monitored";
    public static final String TITLE = "title";
    public static final String SVC_ID = "svcId";
    public static final String APP_URL = "appUrl";
    public static final String RUN_HOST = "runHost";
    public static final String USER_ID = "userId";
    public static final String USER_KEY = "userKey";
    public static final String USER_NAME = "userName";
    public static final String USER_EMAIL = "userEmail";
    public static final String SEND_NOTIF = "sendNotif";

    private String jobId;
    private String runId;
    private String ownerId;
    private Phase phase;
    private Instant quote;
    private Instant creationTime;
    private Instant startTime;
    private Instant endTime;
    private int executionDuration = LIFE_SPAN;
    private Instant destruction;
    private Map<String, String> params = new HashMap<>();
    private List<Result> results = new ArrayList<>();
    private Error error;

    //meta contains essential information needed to manage the job
    final private Meta meta = new Meta();
    //aux holds auxiliary data passed along as part of the usw::jobInfo block.
    final private Aux aux = new Aux();

    public JobInfo() {}
    public JobInfo(String id) { setJobId(id); }

    public void setJobId(String id) {
        this.jobId = id;
        if (meta.jobId == null) meta.jobId = id;
    }
    public String getJobId() {
        return jobId;
    }

    public String getRunId() {
        return runId;
    }
    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Meta getMeta() {
        return meta;
    }
    public Aux getAux() {return aux; }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = Util.Try.it(() -> Phase.valueOf(phase)).getOrElse(Phase.UNKNOWN);  // convert to Phase enum or UNKNOWN
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        setPhase(Phase.ERROR);
        this.error = error;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = new ArrayList<>(results);
    }

    @Nonnull
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String,String> params) { this.params = params; }

    public String getOwnerId() { return ownerId;}

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getCreationTime() {
        return creationTime;
    }
    public void setCreationTime(Instant time) {
        creationTime = time;
    }

    public Instant getStartTime() {
        return startTime;
    }
    public void setStartTime(Instant time) {
        startTime = time;
    }

    public Instant getEndTime() {
        return endTime;
    }
    public void setEndTime(Instant time) { endTime = time; }

    public Instant getDestruction() { return destruction; }
    public void setDestruction(Instant time) { destruction = time; }

    public Instant getQuote() { return quote; }
    public void setQuote(Instant time) { quote = time; }

    public void addResult(Result result) { results.add(result);}

    /**
     * @return how long this job may run in seconds.  zero implies unlimited execution duration.
     */
    public long executionDuration() {
        return executionDuration;
    }
    public void setExecutionDuration(int duration) { executionDuration = duration; }

    /**
     * @return a SrvParam from the flatten params map
     */
    public SrvParam getSrvParams() {
        return SrvParam.makeSrvParamSimpleMap(getMeta().getParams());
    }

    public void copyFrom(JobInfo uws) {
        if (uws == null || uws == this) return;

        this.jobId=uws.jobId;
        this.runId = uws.runId;
        this.ownerId = uws.ownerId;
        this.phase = uws.phase;
        this.quote = uws.quote;
        this.creationTime = uws.creationTime;
        this.startTime = uws.startTime;
        this.endTime = uws.endTime;
        this.executionDuration = uws.executionDuration;
        this.destruction = uws.destruction;
        this.params = new HashMap<>(uws.params);
        this.results = new ArrayList<>(uws.results);
        this.error = uws.error;
        ifNotNull(uws.aux.getJobUrl()).apply(aux::setJobUrl);
        ifNotNull(uws.aux.getUserId()).apply(aux::setUserId);
        ifNotNull(uws.aux.getUserName()).apply(aux::setUserName);
        ifNotNull(uws.aux.getUserEmail()).apply(aux::setUserEmail);
        ifNotNull(uws.aux.getTitle()).apply(aux::setTitle);
    }

//====================================================================
//
//====================================================================

    public record Error ( int code, String msg) implements Serializable {}
    public record Result(String id, String href, String mimeType, String size) implements Serializable {};

    /**
     * Additional information required by Firefly that is not defined in `uws:job`.
     * Only some of this information will be sent to the client, and when it is,
     * it will be placed in the `uws:jobInfo` block.
     */
    public static class Meta implements Serializable {
        String jobId;
        String runId;
        Map<String, String> params = new HashMap<>();
        String userKey;
        Job.Type type;
        int progress;
        String progressDesc;
        String summary;
        boolean monitored;
        String svcId;       // the service id that this job is associated with
        String runHost;     // the host where the job is running on
        String appUrl;      // the URL of the app that created this job
        boolean sendNotif;

        // these are not sent to client
        String eventConnId;

        public String getJobId() { return jobId;}
        public void setJobId(String jobId) { this.jobId = jobId; }

        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }

        public Map<String, String> getParams() { return params; }
        public void setParams(Map<String, String> params) { this.params = params; }

        public String getUserKey() { return userKey; }
        public void setUserKey(String userKey) { this.userKey = userKey;}

        public String getRunHost() { return runHost; }
        public void setRunHost(String runHost) { this.runHost = runHost;}

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = Math.min(Math.max(progress, 0), 100); }

        public void setProgress(int progress, String desc) {
            setProgress(progress);
            setProgressDesc(desc);
        }

        public String getProgressDesc() { return progressDesc; }
        public void setProgressDesc(String progressDesc) { this.progressDesc = progressDesc; }

        public Job.Type getType() { return type; }
        public void setType(Job.Type type) { this.type = type; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public boolean isMonitored() { return monitored; }
        public void setMonitored(boolean monitored) { this.monitored = monitored; }

        public String getSvcId() { return svcId; }
        public void setSvcId(String svcId) { this.svcId = svcId; }

        public String getAppUrl() { return appUrl; }
        public void setAppUrl(String appUrl) { this.appUrl = appUrl; }

        public String getEventConnId() { return eventConnId; }
        public void setEventConnId(String eventConnId) { this.eventConnId = eventConnId; }

        public boolean getSendNotif() { return sendNotif; }
        public void setSendNotif(boolean flg) { this.sendNotif = flg; }
    }

    /**
     * Additional information required by Firefly that is not defined in `uws:job`.
     * Only some of this information will be sent to the client, and when it is,
     * it will be placed in the `uws:jobInfo` block.
     */
    public static class Aux implements Serializable {
        String userId;
        String userName;
        String userEmail;   // may need to be generic so that other user's info can be stored amd passed around
        String title;
        String jobUrl;      // the service URL associated with this job

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId;}

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName;}

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail;}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getJobUrl() { return jobUrl; }
        public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }
    }

}
