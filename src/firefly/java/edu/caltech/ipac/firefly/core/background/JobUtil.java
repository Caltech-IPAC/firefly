package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.api.Async;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.util.AppProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;

import java.io.File;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.COMPLETED;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.ERROR;
import static edu.caltech.ipac.firefly.core.background.JobManager.getAllUserJobs;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.server.query.UwsJobProcessor.*;
import static edu.caltech.ipac.util.StringUtils.*;
import static edu.caltech.ipac.firefly.core.Util.Try;

public class JobUtil {
    // Services are defined as strings with three fields (url|serviceId|serviceType), separated by commas. Only url is required; the others are optional.
    public static final List<String> UWS_HISTORY_SVCS = Arrays.stream(AppProperties.getProperty("uws.history.svcs", "")
                                                            .split(",")).map(String::trim).toList();        // urls separated by comma
    public static final List<String> RUNID_IGNORE = Arrays.stream(AppProperties.getProperty("uws.runid.ignore", "")
                                                            .split(",")).map(String::trim).toList();        // strings separated by comma
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final long yearMs = 365*24*60*60*1000L;  // one year in milliseconds; 31_536_000_000
    public static final List<String> runIdIgnoreList = new ArrayList<>();

    static {
        runIdIgnoreList.add("TAP_SCHEMA");      // Firefly uses this when querying the tap schema
        if (!RUNID_IGNORE.isEmpty()) runIdIgnoreList.addAll(RUNID_IGNORE);
    }

    public static void logJobInfo(JobInfo info) {
        if (info == null) return;
        LOG.debug(String.format("JOB:%s  userKey:%s  phase:%s  msg:%s", info.getMeta().getJobId(), info.getMeta().getUserKey(), info.getPhase(), info.getMeta().getSummary()));
        LOG.trace(String.format("JOB: %s details: %s", info.getMeta().getJobId(), toJson(info)));
    }

    /**
     * Generates a unique Job ID for a job. In a clustered environment, this ID is unique across all instances.
     * The ID consists of up to 8 characters from the host name and the current time in milliseconds,
     * limited to a one-year range. The resulting format is "HOSTNAME_TIMESTAMP", with a maximum of 20 characters.
     * @return the next unique job ID for this host
     */
    static String nextJobId() {
        String hname = hostName();
        hname = hname.length() < 9 ? hname : hname.substring(hname.length() - 8);
        return "%s_%d".formatted(hname, System.currentTimeMillis() % yearMs);
    }

    public static String hostName() {
        return Try.it(() -> InetAddress.getLocalHost().getHostName()).getOrElse("SYS").split("\\.")[0];
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

    /**
     * Import job histories from the given service URL.
     * @param svcDef the service to import job histories from
     * @return the number of job histories imported
     */
    public static int importJobHistories(String svcDef) {
        int count = 0;

        String[] svcParts = ifNotNull(svcDef).getOrElse("").split("\\|", 3);
        String url = svcParts[0].trim();
        String svcId = svcParts.length > 1 ? svcParts[1].trim() : null;
        String svcType = svcParts.length > 2 ? svcParts[2].trim() : null;

        if (url.isEmpty()) return count;

        LOG.debug("Importing job histories from %s; svcId=%s svcType=%s".formatted(url, svcId, svcType));
        List<JobInfo> history = getAllUserJobs();

        HttpServiceInput input = HttpServiceInput.createWithCredential(url);
        Ref<List<JobInfo>> jobList = new Ref<>();
        HttpServices.getData(input, r -> {
           Try.it(() -> {
                Document doc = parse(r.getResponseBodyAsStream());
                jobList.set(convertToJobList(doc, url));
            }).getOrElse(e -> {
                LOG.error("Failed to import job histories from %s: %s".formatted(url, e.getMessage()));
            });
           return HttpServices.Status.ok();
        });
        if (jobList.get() == null || jobList.get().isEmpty()) return count;

        // remove jobs with no URL or runId in the ignore list
        boolean hasBadJobs = jobList.get().removeIf(j -> j.getAux().getJobUrl() == null || runIdIgnoreList.contains(String.valueOf(j.getRunId())));
        if (hasBadJobs) LOG.debug("Some jobs with no URL or ignored runId were removed from list");

        for (JobInfo job : jobList.get()) {
            JobInfo uws = Try.it(() -> getUwsJobInfo(job.getAux().getJobUrl())).get();
            if (uws == null) {
                LOG.debug("Failed to get job info for " + job.getAux().getJobUrl());
            } else if (runIdIgnoreList.contains(String.valueOf(uws.getRunId()))) {
                LOG.debug("Ignoring job at %s with RUNID=%s".formatted(job.getAux().getJobUrl(), uws.getRunId()));
            } else {
                count++;
                String jobId = uws.getJobId();
                JobInfo jobInfo = findJobInfo(jobId, history);
                mergeJobInfo(jobInfo, uws, svcId, svcType);
                LOG.trace("Job added jobUrl=%s jobId=%s".formatted(job.getAux().getJobUrl(),uws.getJobId()));
            }
        }
        LOG.debug("%d job histories imported".formatted(count));
        return count;
    }

    public static JobInfo mergeJobInfo(JobInfo local, JobInfo uws, String svcId, String svcType) {
        String jobId = local == null ? nextJobId() : local.getMeta().getJobId();
        return updateJobInfo(jobId, true, ji -> {
            ji.copyFrom(uws);
            Job.Type type = Try.it(() -> Job.Type.valueOf(svcType)).getOrElse(Job.Type.UWS);
            ji.getMeta().setType(type);
            ji.getMeta().setSvcId(svcId);
        });
    }

    public static JobInfo findJobInfo(String uwsJobId, List<JobInfo> mylist) {
        if (mylist == null || mylist.isEmpty()) return null;
        for(JobInfo ji : mylist) {
            if (ji.getJobId().equals(uwsJobId)) return ji;
        }

        return null;
    }

    ;

//====================================================================
//  JSON serialization
//====================================================================

    public static JSONObject toJsonObject(JobInfo info) {

        if (info == null) return null;

        JSONObject rval = new JSONObject();
        rval.put(JOB_ID, info.getJobId());
        applyIfNotEmpty(info.getRunId(), v -> rval.put(RUN_ID, v));
        applyIfNotEmpty(info.getOwnerId(), v -> rval.put(OWNER_ID, v));
        applyIfNotEmpty(info.getPhase(), v -> rval.put(PHASE, v.toString()));
        applyIfNotEmpty(info.getQuote(), v -> rval.put(QUOTE, v.toString()));
        applyIfNotEmpty(info.getCreationTime(), v -> rval.put(CREATION_TIME, v.toString()));
        applyIfNotEmpty(info.getStartTime(), v -> rval.put(START_TIME, v.toString()));
        applyIfNotEmpty(info.getEndTime(), v -> rval.put(END_TIME, v.toString()));
        applyIfNotEmpty(info.executionDuration(), v -> rval.put(EXECUTION_DURATION, v));
        applyIfNotEmpty(info.getDestruction(), v -> rval.put(DESTRUCTION, v.toString()));

        if (!info.getParams().isEmpty()) rval.put(PARAMETERS, info.getParams());
        if (!info.getResults().isEmpty())  rval.put(RESULTS, toResults(info.getResults()));

        applyIfNotEmpty(info.getError(), v -> {
            JSONObject errSum = new JSONObject();
            errSum.put(ERROR_MSG, v.msg());
            errSum.put(ERROR_TYPE, v.code() < 500 ? "fatal" : "transient");     // 5xx are typically system error, e.g. server down.
            rval.put(ERROR_SUMMARY, errSum);
        });

        JSONObject jsonAux = new JSONObject();
        rval.put(JOB_INFO, jsonAux);
        applyIfNotEmpty(info.getAux().getTitle(), v -> jsonAux.put(TITLE, v));
        applyIfNotEmpty(info.getAux().getUserId(), v -> jsonAux.put(USER_ID, v));
        applyIfNotEmpty(info.getAux().getUserName(), v -> jsonAux.put(USER_NAME, v));
        applyIfNotEmpty(info.getAux().getUserEmail(), v -> jsonAux.put(USER_EMAIL, v));
        applyIfNotEmpty(info.getAux().getJobUrl(), v -> jsonAux.put(JobInfo.JOB_URL, v));

        JSONObject jsonMeta = new JSONObject();
        rval.put(META, jsonMeta);
        Meta meta = info.getMeta();
        applyIfNotEmpty(meta.getJobId(), v -> jsonMeta.put(JOB_ID, v));
        applyIfNotEmpty(meta.getRunId(), v -> jsonMeta.put(RUN_ID, v));
        applyIfNotEmpty(meta.getUserKey(), v -> jsonMeta.put(USER_KEY, v));
        applyIfNotEmpty(meta.getType(), v -> jsonMeta.put(JOB_TYPE, v.toString()));
        applyIfNotEmpty(meta.getProgress(), v -> jsonMeta.put(PROGRESS, v));
        applyIfNotEmpty(meta.getProgressDesc(), v -> jsonMeta.put(PROGRESS_DESC, v));
        applyIfNotEmpty(meta.getSummary(), v -> jsonMeta.put(SUMMARY, v));
        applyIfNotEmpty(meta.isMonitored(), v -> jsonMeta.put(MONITORED, v));
        applyIfNotEmpty(meta.getSvcId(), v -> jsonMeta.put(SVC_ID, v));
        applyIfNotEmpty(meta.getAppUrl(), v -> jsonMeta.put(APP_URL, v));
        applyIfNotEmpty(meta.getRunHost(), v -> jsonMeta.put(RUN_HOST, v));
        applyIfNotEmpty(meta.getSendNotif(), v -> jsonMeta.put(SEND_NOTIF, v));

        if (!meta.getParams().isEmpty()) jsonMeta.put(PARAMETERS, meta.getParams());

        return rval;
    }

    public static JobInfo toJobInfo(JSONObject json) {
        if (isEmpty(json)) return null;
        JobInfo rval = ifNotNull(json.get(JOB_ID)).get(v -> new JobInfo(v.toString()));
        if (rval == null) return null;

        ifNotNull(json.get(RUN_ID)).apply(v -> rval.setRunId(v.toString()));
        ifNotNull(json.get(OWNER_ID)).apply(v -> rval.setOwnerId(v.toString()));
        ifNotNull(json.get(PHASE)).apply(v -> rval.setPhase(v.toString()));
        ifNotNull(json.get(QUOTE)).apply(v -> rval.setQuote(Instant.parse(v.toString())));
        ifNotNull(json.get(CREATION_TIME)).apply(v -> rval.setCreationTime(Instant.parse(v.toString())));
        ifNotNull(json.get(START_TIME)).apply(v -> rval.setStartTime(Instant.parse(v.toString())));
        ifNotNull(json.get(END_TIME)).apply(v -> rval.setEndTime(Instant.parse(v.toString())));
        ifNotNull(json.get(EXECUTION_DURATION)).apply(v -> rval.setExecutionDuration(((Long) v).intValue()));
        ifNotNull(json.get(DESTRUCTION)).apply(v -> rval.setDestruction(Instant.parse(v.toString())));

        ifNotNull(toParameters(json.get(PARAMETERS))).apply(p -> rval.setParams(p));
        ifNotNull(toResults(json.get(RESULTS))).apply(r -> rval.setResults(r));

        ifNotNull(json.get(ERROR_SUMMARY)).apply(v -> {
            if (v instanceof JSONObject jo) {
                int code = getInt(jo.get(ERROR_TYPE), 500);
                String msg = String.valueOf(jo.get(ERROR_MSG));
                rval.setError(new JobInfo.Error(code, msg));
            }
        });
        ifNotNull(json.get(META)).apply(v -> {
            if (v instanceof JSONObject ji) {
                ifNotNull(ji.get(JOB_ID)).apply(s -> rval.getMeta().setJobId(s.toString()));
                ifNotNull(ji.get(RUN_ID)).apply(s -> rval.getMeta().setRunId(s.toString()));
                ifNotNull(ji.get(USER_KEY)).apply(s -> rval.getMeta().setUserKey(s.toString()));
                ifNotNull(ji.get(JOB_TYPE)).apply(t -> rval.getMeta().setType(Job.Type.valueOf(t.toString())));
                ifNotNull(ji.get(PROGRESS)).apply(p -> rval.getMeta().setProgress(((Long) p).intValue()));
                ifNotNull(ji.get(PROGRESS_DESC)).apply(d -> rval.getMeta().setProgressDesc(d.toString()));
                ifNotNull(ji.get(SUMMARY)).apply(s -> rval.getMeta().setSummary(s.toString()));
                ifNotNull(ji.get(MONITORED)).apply(m -> rval.getMeta().setMonitored((Boolean) m));
                ifNotNull(ji.get(SVC_ID)).apply(s -> rval.getMeta().setSvcId(s.toString()));
                ifNotNull(ji.get(APP_URL)).apply(s -> rval.getMeta().setAppUrl(s.toString()));
                ifNotNull(ji.get(RUN_HOST)).apply(s -> rval.getMeta().setRunHost(s.toString()));
                ifNotNull(ji.get(SEND_NOTIF)).apply(o -> rval.getMeta().setSendNotif((Boolean) o));

                ifNotNull(toParameters(ji.get(PARAMETERS))).apply(p -> rval.getMeta().setParams(p));
            }
        });
        ifNotNull(json.get(JOB_INFO)).apply(v -> {
            if (v instanceof JSONObject ji) {
                ifNotNull(ji.get(TITLE)).apply(l -> rval.getAux().setTitle(l.toString()));
                ifNotNull(ji.get(USER_ID)).apply(s -> rval.getAux().setUserId(s.toString()));
                ifNotNull(ji.get(USER_NAME)).apply(s -> rval.getAux().setUserName(s.toString()));
                ifNotNull(ji.get(USER_EMAIL)).apply(s -> rval.getAux().setUserEmail(s.toString()));
                ifNotNull(ji.get(JobInfo.JOB_URL)).apply(o -> rval.getAux().setJobUrl(o.toString()));
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

    public static Map<String,String> toParameters(Object o) {
        if (o instanceof JSONObject jo) {
            Map<String, String> params = new HashMap<>();
            jo.forEach((key,val) -> params.put(String.valueOf(key), String.valueOf(val)));
            return params;
        }
        return null;
    }

    public static List<Result> toResults(Object o) {
        if (o instanceof JSONArray ja) {
            List<Result> reval = new ArrayList<>(ja.size());
            for(Object item : ja) {
                if (item instanceof JSONObject jo) {
                    reval.add(new Result(getStr(jo, "id"), getStr(jo, "href"), getStr(jo, "mimeType"), getStr(jo, "size")));
                }
            }
            return reval;
        }
        return null;
    }

    /**
     * @param infos
     * @return an array of job IDs under the 'jobs' prop as a json string
     */
    public static String toJsonJobList(List<JobInfo> infos) {
        JSONObject rval = new JSONObject();
        if (infos != null && infos.size() > 0) {
            // object with "jobs": array of JobInfo urls
            List<String> urls = infos.stream().map(i -> i.getMeta().getJobId()).collect(Collectors.toList());
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
                rval.put("results", Arrays.asList(Async.getAsyncUrl() + info.getMeta().getJobId() + "/results/result"));
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