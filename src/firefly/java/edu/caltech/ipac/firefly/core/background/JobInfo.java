/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
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
    public static final Set<Phase> TERMINATED_PHASES = Set.of(Phase.COMPLETED, Phase.ERROR, Phase.ABORTED);
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
    public static final String JOB_INFO = "jobInfo";

    // These are additional info that's not in defined in uws:job but is needed by Firefly
    // In serialized form, it will go under uws:jobInfo block
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_DESC = "progressDesc";
    public static final String JOB_TYPE = "type";
    public static final String SUMMARY = "summary";
    public static final String DATA_ORIGIN = "dataOrigin";
    public static final String MONITORED = "monitored";
    public static final String LABEL = "label";
    public static final String SVC_ID = "svcId";
    public static final String LOCAL_RUN_ID = "localRunId";


    private String jobId;
    private String runId;
    private String owner;
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

    final private AuxData auxData = new AuxData();

    public JobInfo(String id) {
        this.jobId = id;
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

    public AuxData getAuxData() {
        return auxData;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        switch (phase) {
            case UNKNOWN:
            case ERROR:
            case ABORTED:
                endTime = Instant.now();
                break;
            case COMPLETED:
                endTime = Instant.now();
                auxData.setProgress(100);
        }
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
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

    public String getOwner() { return owner;}

    public void setOwner(String owner) {
        this.owner = owner;
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
        return SrvParam.makeSrvParamSimpleMap(params);
    }

    public void copyFrom(JobInfo ji) {
        if (ji == null || ji == this) return;

        this.jobId = ji.jobId;
        this.runId = ji.runId;
        this.owner = ji.owner;
        this.phase = ji.phase;
        this.quote = ji.quote;
        this.creationTime = ji.creationTime;
        this.startTime = ji.startTime;
        this.endTime = ji.endTime;
        this.executionDuration = ji.executionDuration;
        this.destruction = ji.destruction;
        this.params = new HashMap<>(ji.params);
        this.results = new ArrayList<>(ji.results);
        this.error = ji.error;
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
    public static class AuxData implements Serializable {
        int progress;
        String progressDesc;
        Job.Type type;
        String summary;
        String dataOrigin;
        boolean monitored;
        String label;
        String svcId;       // the service id that this job is associated with
        String localRunId;  // Not all services support UWS RUNID.  Store info here instead.

        // these are not sent to client
        String eventConnId;
        String refJobId;    // similar to JOB_ID, but used internally to identify the job
        String refHost;     // the host where the job is running on
        UserInfo userInfo;  // firefly user who initiated the job

        public UserInfo getUserInfo() { return userInfo; }
        public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

        public String getRefJobId() { return refJobId; }
        public void setRefJobId(String refJobId) { this.refJobId = refJobId;}

        public String getRefHost() { return refHost; }
        public void setRefHost(String refHost) { this.refHost = refHost;}

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = Math.min(Math.max(progress, 0), 100); }

        public String getProgressDesc() { return progressDesc; }
        public void setProgressDesc(String progressDesc) { this.progressDesc = progressDesc; }

        public Job.Type getType() { return type; }
        public void setType(Job.Type type) { this.type = type; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getDataOrigin() { return dataOrigin; }
        public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }

        public boolean isMonitored() { return monitored; }
        public void setMonitored(boolean monitored) { this.monitored = monitored; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getSvcId() { return svcId; }
        public void setSvcId(String svcId) { this.svcId = svcId; }

        public String getLocalRunId() { return localRunId; }
        public void setLocalRunId(String localRunId) { this.localRunId = localRunId; }

        public String getEventConnId() { return eventConnId; }
        public void setEventConnId(String eventConnId) { this.eventConnId = eventConnId; }    }
}
