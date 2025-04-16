package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.api.Async;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.COMPLETED;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.ERROR;
import static edu.caltech.ipac.util.StringUtils.*;

public class JobUtil {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final long yearMs = 365*24*60*60*1000L;  // one year in milliseconds; 31_536_000_000

    public static void logJobInfo(JobInfo info) {
        if (info == null) return;
        LOG.debug(String.format("JOB:%s  owner:%s  phase:%s  msg:%s", info.getJobId(), info.getOwner(), info.getPhase(), info.getAuxData().getSummary()));
        LOG.trace(String.format("JOB: %s details: %s", info.getJobId(), toJson(info)));
    }

    /**
     * Generates a unique Job ID for a job. In a clustered environment, this ID is unique across all instances.
     * The ID consists of up to 8 characters from the host name and the current time in milliseconds,
     * limited to a one-year range. The resulting format is "HOSTNAME_TIMESTAMP", with a maximum of 20 characters.
     * @return the next unique job ID for this host
     */
    static String nextRefJobId() {
        String hname = hostName();
        hname = hname.length() < 9 ? hname : hname.substring(hname.length() - 8);
        return "%s_%d".formatted(hname, System.currentTimeMillis() % yearMs);
    }

    public static String hostName() {
        return Util.Try.it(() -> InetAddress.getLocalHost().getHostName()).getOrElse("SYS").split("\\.")[0];
    }

    /**
     * The directory name is derived from the 8th to the 11th number of System.currentTimeMillis. (~ 2.78 hours each increment)
     * Since the job ID is limited to yearMs(11 digits), we'll take the first 4 digits.
     * @param jobId the job ID
     * @return the directory name for the given job ID
     */
    public static String jobIdToDir(String jobId) {
        if (isEmpty(jobId)) return jobId;
        String[] parts = jobId.split("_");
        return isEmpty(parts[1]) ? jobId : parts[0] + "_" + parts[1].substring(0, 4);
    }

    public static String toJson(JobInfo info) {
        JSONObject jsonObject = toJsonObject(info);
        return jsonObject == null ? "null" : jsonObject.toJSONString();
    }

//====================================================================
//  JSON serialization
//====================================================================
    public static JSONObject userInfoToJson(UserInfo userInfo) {
        JSONObject rval = new JSONObject();
        rval.put("firstName", userInfo.getFirstName());
        rval.put("lastName", userInfo.getLastName());
        rval.put("email", userInfo.getEmail());
        return rval;
    }

    public static UserInfo jsonToUserInfo(JSONObject userInfo) {
        UserInfo rval = new UserInfo();
        rval.setFirstName(String.valueOf(userInfo.get("firstName")));
        rval.setLastName(String.valueOf(userInfo.get("lastName")));
        rval.setEmail(String.valueOf(userInfo.get("email")));
        return rval;
    }

    public static JSONObject toJsonObject(JobInfo info) {
        return toJsonObject(info, true);
    }

    public static JSONObject toJsonObject(JobInfo info, boolean inclInternProps) {
        if (info == null) return null;

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

        if (!info.getResults().isEmpty()) {
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
            AuxData aux = info.getAuxData();
            applyIfNotEmpty(aux.getType(), v -> addtlInfo.put(JOB_TYPE, v.toString()));
            applyIfNotEmpty(aux.getLabel(), v -> addtlInfo.put(LABEL, v));
            applyIfNotEmpty(aux.getProgress(), v -> addtlInfo.put(PROGRESS, v));
            applyIfNotEmpty(aux.isMonitored(), v -> addtlInfo.put(MONITORED, v));
            applyIfNotEmpty(aux.getProgressDesc(), v -> addtlInfo.put(PROGRESS_DESC, v));
            applyIfNotEmpty(aux.getDataOrigin(), v -> addtlInfo.put(DATA_ORIGIN, v));
            applyIfNotEmpty(aux.getSummary(), v -> addtlInfo.put(SUMMARY, v));
            applyIfNotEmpty(aux.getSvcId(), v -> addtlInfo.put(SVC_ID, v));
            applyIfNotEmpty(aux.getLocalRunId(), v -> addtlInfo.put(LOCAL_RUN_ID, v));

            applyIfNotEmpty(aux.getRefHost(), v -> addtlInfo.put("refHost", v));
            applyIfNotEmpty(aux.getRefJobId(), v -> addtlInfo.put("refJobId", v));
            applyIfNotEmpty(aux.getUserInfo(), v -> addtlInfo.put("userInfo", userInfoToJson(v)));
        }

        return rval;
    }

    public static JobInfo toJobInfo(JSONObject json) {
        if (isEmpty(json)) return null;
        JobInfo rval = ifNotNull(json.get(JOB_ID)).get(v -> new JobInfo(v.toString()));
        if (rval == null) return null;

        ifNotNull(json.get(RUN_ID)).apply(v -> rval.setRunId(v.toString()));
        ifNotNull(json.get(OWNER_ID)).apply(v -> rval.setOwner(v.toString()));
        ifNotNull(json.get(PHASE)).apply(v -> rval.setPhase(Phase.valueOf(v.toString())));
        ifNotNull(json.get(OWNER_ID)).apply(v -> rval.setOwner(v.toString()));
        ifNotNull(json.get(QUOTE)).apply(v -> rval.setQuote(Instant.parse(v.toString())));
        ifNotNull(json.get(CREATION_TIME)).apply(v -> rval.setCreationTime(Instant.parse(v.toString())));
        ifNotNull(json.get(START_TIME)).apply(v -> rval.setStartTime(Instant.parse(v.toString())));
        ifNotNull(json.get(END_TIME)).apply(v -> rval.setEndTime(Instant.parse(v.toString())));
        ifNotNull(json.get(EXECUTION_DURATION)).apply(v -> rval.setExecutionDuration(((Long) v).intValue()));
        ifNotNull(json.get(DESTRUCTION)).apply(v -> rval.setDestruction(Instant.parse(v.toString())));

        ifNotNull(json.get(PARAMETERS)).apply(v -> {
            if (v instanceof JSONObject jo) {
                Map<String, String> params = new HashMap<>();
                jo.forEach((key,val) -> params.put(String.valueOf(key), String.valueOf(val)));
                rval.setParams(params);
            }
        });
        ifNotNull(json.get(RESULTS)).apply(v -> {
            if (v instanceof JSONArray ja) {
                List<Result> results = ja.stream().map(o -> toResult((JSONObject) o)).toList();
                rval.setResults(results);
            }
        });
        ifNotNull(json.get(ERROR)).apply(v -> {
            if (v instanceof JSONObject jo) {
                int code = getInt(jo.get(ERROR_TYPE), 500);
                String msg = String.valueOf(jo.get(ERROR_MSG));
                rval.setError(new JobInfo.Error(code, msg));
            }
        });
        ifNotNull(json.get(JOB_INFO)).apply(v -> {
            if (v instanceof JSONObject ji) {
                ifNotNull(ji.get(JOB_TYPE)).apply(t -> rval.getAuxData().setType(Job.Type.valueOf(t.toString())));
                ifNotNull(ji.get(LABEL)).apply(l -> rval.getAuxData().setLabel(l.toString()));
                ifNotNull(ji.get(PROGRESS)).apply(p -> rval.getAuxData().setProgress(((Long) p).intValue()));
                ifNotNull(ji.get(MONITORED)).apply(m -> rval.getAuxData().setMonitored((Boolean) m));
                ifNotNull(ji.get(PROGRESS_DESC)).apply(d -> rval.getAuxData().setProgressDesc(d.toString()));
                ifNotNull(ji.get(DATA_ORIGIN)).apply(o -> rval.getAuxData().setDataOrigin(o.toString()));
                ifNotNull(ji.get(SUMMARY)).apply(s -> rval.getAuxData().setSummary(s.toString()));
                ifNotNull(ji.get(SVC_ID)).apply(s -> rval.getAuxData().setSvcId(s.toString()));
                ifNotNull(ji.get(LOCAL_RUN_ID)).apply(s -> rval.getAuxData().setLocalRunId(s.toString()));
                ifNotNull(ji.get("refHost")).apply(s -> rval.getAuxData().setRefHost(s.toString()));
                ifNotNull(ji.get("refJobId")).apply(s -> rval.getAuxData().setRefJobId(s.toString()));
                ifNotNull(ji.get("userInfo")).apply(s -> {
                    if (s instanceof JSONObject u) {
                        rval.getAuxData().setUserInfo(jsonToUserInfo(u));
                    }
                });
            }
        });
        return rval;
    }

    public static List<JSONObject> toResults(List<JobInfo.Result> results) {
        return results.stream().map(r -> toJsonResult(r))
                .collect(Collectors.toList());
    }

    public static JSONObject toJsonResult(JobInfo.Result result) {
        if (result == null) return null;
        JSONObject ro = new JSONObject();
        applyIfNotEmpty(result.id(), v -> ro.put("id", v));
        applyIfNotEmpty(result.href(), v -> ro.put("href", v));
        applyIfNotEmpty(result.mimeType(), v -> ro.put("mimeType", v));
        applyIfNotEmpty(result.size(), v -> ro.put("size", v));
        return ro;
    }

    private static String getStr(JSONObject jo, String key) {
        return jo.get(key) == null ? null : jo.get(key).toString();
    }

    public static Result toResult(JSONObject jo) {
        return new Result(getStr(jo,"id"), getStr(jo, "href"), getStr(jo, "mimeType"), getStr(jo, "size"));
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

    public static File getJobWorkDir(String jobId) {
        var baseDir = new File(ServerContext.getStageWorkDir(), "jobs");
        File dir = new File(baseDir, jobIdToDir(jobId));
        dir.mkdirs();
        return dir;
    }
}