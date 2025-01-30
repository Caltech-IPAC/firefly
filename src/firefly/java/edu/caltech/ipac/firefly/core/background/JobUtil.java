package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.api.Async;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.messaging.Message;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.caltech.ipac.firefly.core.background.JobManager.JobEvent;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.COMPLETED;
import static edu.caltech.ipac.util.StringUtils.*;

public class JobUtil {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final long yearMs = 365*24*60*60*1000L;  // one year in milliseconds

    public static void logJobInfo(JobInfo info) {
        if (info == null) return;
        LOG.debug(String.format("JOB:%s  owner:%s  phase:%s  msg:%s", info.getJobId(), info.getOwner(), info.getPhase(), info.getSummary()));
        LOG.trace(String.format("JOB: %s details: %s", info.getJobId(), toJson(info)));
    }

    /**
     * Generates a unique Job ID for a job. In a clustered environment, this ID is unique across all instances.
     * The ID consists of up to 8 characters from the host name and the current time in milliseconds,
     * limited to a one-year range. The resulting format is "HOSTNAME_TIMESTAMP", with a maximum of 20 characters.
     * @return the next unique job ID for this host
     */
    static String nextJobId() {
        String hname = Util.Try.it(() -> InetAddress.getLocalHost().getHostName()).getOrElse("SYS").split("\\.")[0];
        hname = hname.length() < 9 ? hname : hname.substring(hname.length() - 8);
        return "%s_%d".formatted(hname, System.currentTimeMillis() % yearMs);
    }

    public static String jobIdToDir(String jobId) {
        if (isEmpty(jobId)) return jobId;
        String[] parts = jobId.split("_");
        return isEmpty(parts[1]) ? jobId : parts[0] + "_" + parts[1].substring(0, 4);       // keeping 7 digits in ms(2.78hrs) within the same directory;
    }

    public static String toJson(JobInfo info) {
        JSONObject jsonObject = toJsonObject(info);
        return jsonObject == null ? "null" : jsonObject.toJSONString();
    }

//====================================================================
//  JSON serialization
//====================================================================
    public static JobInfo fromMsg(Message msg) {
        JobInfo info = new JobInfo(msg.getValue(null, JobEvent.JOB, JOB_ID));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, RUN_ID), info::setRunId);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, OWNER_ID), info::setOwner);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, PHASE), v -> info.setPhase(Phase.valueOf(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, QUOTE), v -> info.setQuote(Instant.parse(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, CREATION_TIME), v -> info.setCreationTime(Instant.parse(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, START_TIME), v -> info.setStartTime(Instant.parse(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, END_TIME), v -> info.setEndTime(Instant.parse(v.toString())));
        applyIfNotEmpty(msg.<Long>getValue(null, JobEvent.JOB, EXECUTION_DURATION), (v) -> info.setExecutionDuration(v.intValue()));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, DESTRUCTION), v -> info.setDestruction(Instant.parse(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, PARAMETERS), info::setParams);
        applyIfNotEmpty(msg.<JSONArray>getValue(null, JobEvent.JOB, RESULTS), (v) -> {
            List<Result> results = v.stream().map(o -> toResult((JSONObject) o)).toList();
            info.setResults(results);
        });
        applyIfNotEmpty(msg.<JSONObject>getValue(null, JobEvent.JOB, ERROR_SUMMARY), v -> {
            info.setError(new JobInfo.Error(getInt(v.get(ERROR_TYPE), 500), v.get(ERROR_MSG).toString()));
        });
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, JOB_TYPE), (v) -> info.setType(Job.Type.valueOf(v.toString())));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, LABEL), info::setLabel);
        applyIfNotEmpty(msg.<Long>getValue(null, JobEvent.JOB, JOB_INFO, PROGRESS), (v) -> info.setProgress(v.intValue()));
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, MONITORED), info::setMonitored);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, PROGRESS_DESC), info::setProgressDesc);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, DATA_ORIGIN), info::setDataOrigin);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, SUMMARY), info::setSummary);
        applyIfNotEmpty(msg.getValue(null, JobEvent.JOB, JOB_INFO, LOCAL_RUN_ID), info::setLocalRunId);
        return info;
    }

    public static JSONObject toJsonObject(JobInfo info) {
        return toJsonObject(info, true);
    }

    public static JSONObject toJsonObject(JobInfo info, boolean inclInternProps) {
        if (info == null) return null;
        String asyncUrl = Async.getAsyncUrl();

        JSONObject rval = new JSONObject();
        rval.put(JOB_ID, info.getJobId());
        applyIfNotEmpty(info.getRunId(), v -> rval.put(RUN_ID, v));
        applyIfNotEmpty(info.getOwner(), v -> rval.put(OWNER_ID, v));
        applyIfNotEmpty(info.getPhase(), v -> rval.put(PHASE, v.toString()));
        applyIfNotEmpty(info.getQuote(), v -> rval.put(QUOTE, v.toString()));
        applyIfNotEmpty(info.getCreationTime(), v -> rval.put(CREATION_TIME, v.toString()));
        applyIfNotEmpty(info.getStartTime(), v -> rval.put(START_TIME, v.toString()));
        applyIfNotEmpty(info.getEndTime(), v -> rval.put(END_TIME, v.toString()));
        applyIfNotEmpty(info.executionDuration(), v -> rval.put(EXECUTION_DURATION, v));
        applyIfNotEmpty(info.getDestruction(), v -> rval.put(DESTRUCTION, v.toString()));
        if (!info.getParams().isEmpty()) rval.put(PARAMETERS, info.getParams());

        if (info.getPhase() == COMPLETED && info.getResults().isEmpty()) {
            rval.put(RESULTS, Arrays.asList(toJsonResult(asyncUrl + info.getJobId() + "/results/result", null, null)));
        } else if (!info.getResults().isEmpty()) {
            rval.put(RESULTS, toResults(info.getResults()));
        }
        applyIfNotEmpty(info.getError(), v -> {
            JSONObject errSum = new JSONObject();
            errSum.put(ERROR_MSG, v.msg());
            errSum.put(ERROR_TYPE, v.code() < 500 ? "fatal" : "transient");     // 5xx are typically system error, e.g. server down.
            rval.put(ERROR_SUMMARY, errSum);
        });

        if (inclInternProps) {
            JSONObject addtlInfo = new JSONObject();
            rval.put(JOB_INFO, addtlInfo);
            applyIfNotEmpty(info.getType(), v -> addtlInfo.put(JOB_TYPE, v.toString()));
            applyIfNotEmpty(info.getLabel(), v -> addtlInfo.put(LABEL, v));
            applyIfNotEmpty(info.getProgress(), v -> addtlInfo.put(PROGRESS, v));
            applyIfNotEmpty(info.isMonitored(), v -> addtlInfo.put(MONITORED, v));
            applyIfNotEmpty(info.getProgressDesc(), v -> addtlInfo.put(PROGRESS_DESC, v));
            applyIfNotEmpty(info.getDataOrigin(), v -> addtlInfo.put(DATA_ORIGIN, v));
            applyIfNotEmpty(info.getSummary(), v -> addtlInfo.put(SUMMARY, v));
            applyIfNotEmpty(info.getLocalRunId(), v -> addtlInfo.put(LOCAL_RUN_ID, v));
        }

        return rval;
    }

    public static List<JSONObject> toResults(List<JobInfo.Result> results) {
        return results.stream().map(r -> toJsonResult(r.href(), r.mimeType(), r.size()))
                .collect(Collectors.toList());
    }

    public static JSONObject toJsonResult(String href, String mimeType, String size) {
        JSONObject ro = new JSONObject();
        applyIfNotEmpty(href, v -> ro.put("href", v));
        applyIfNotEmpty(mimeType, v -> ro.put("mimeType", v));
        applyIfNotEmpty(size, v -> ro.put("size", v));
        return ro;
    }

    private static String getStr(JSONObject jo, String key) {
        return jo.get(key) == null ? null : jo.get(key).toString();
    }

    public static Result toResult(JSONObject jo) {
        return new Result(getStr(jo, "href"), getStr(jo, "hrefType"), getStr(jo, "mimeType"), getStr(jo, "size"));
    }

    /**
     * @param infos
     * @return an array of job IDs under the 'jobs' prop as a json string
     */
    public static String toJsonJobList(List<JobInfo> infos) {
        JSONObject rval = new JSONObject();
        if (infos != null && infos.size() > 0) {
            // object with "jobs": array of JobInfo urls
            List<String> urls = infos.stream().map(i -> i.getJobId()).collect(Collectors.toList());
            rval.put("jobs", urls);
        }
        return rval.toJSONString();
    }

    /**
     * @param info
     * @return an array of result URLs for the given job
     */
    public static String toJsonResults(JobInfo info) {
        JSONObject rval = new JSONObject();
        if (info.getPhase() == COMPLETED) {
            if (info.getResults().size() == 0) {
                rval.put("results", Arrays.asList(Async.getAsyncUrl() + info.getJobId() + "/results/result"));
            } else {
                rval.put("results", info.getResults());
            }
        }
        return rval.toJSONString();
    }

}