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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.Util.Try;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.*;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.server.query.UwsJobProcessor.convertToJobList;
import static edu.caltech.ipac.firefly.server.query.UwsJobProcessor.getUwsJobInfo;
import static edu.caltech.ipac.firefly.server.query.UwsJobProcessor.parse;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.getInt;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

public class JobUtil {
    // Services are defined as strings with three fields (url|serviceId|serviceType), separated by commas. Only url is required; the others are optional.
    public static final List<String> UWS_HISTORY_SVCS = Arrays.stream(AppProperties.getProperty("uws.history.svcs", "")
                                                            .split(",")).map(String::trim).toList();        // urls separated by comma
    public static final List<String> RUNID_IGNORE = Arrays.stream(AppProperties.getProperty("uws.runid.ignore", "")
                                                            .split(",")).map(String::trim).toList();        // strings separated by comma
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final long yearMs = 365*24*60*60*1000L;  // one year in milliseconds; 31_536_000_000
    public static final List<String> runIdIgnoreList = new ArrayList<>();
    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int BASE = ALPHABET.length;


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
     /**
     * Generates a random UUID and encodes it in Base58 for a compact, human-friendly,
     * and URL/file-system safe identifier.
     * A UUID is 128 bits (16 bytes). In hexadecimal form it is 32 characters.
     * Base58 uses the alphabet:
     *   123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz
     * It avoids visually confusing characters (0, O, I, l) and excludes punctuation.
     * The result is typically 21–22 characters long.
     * This makes the ID compact, human-friendly, and safe for use in job IDs,
     * filenames, and URLs without escaping.
     * Example:
     *   4hYQePYk74yT3UqpXzGrkK
     *   7YpTuwWbPBJ4yF5CkKcUZm
     * @return a 22-character, URL-safe Base64 string derived from a random UUID
     */
    static String nextJobId() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        byte[] bytes = buffer.array();

        // Convert to BigInteger for easy base conversion
        BigInteger value = new BigInteger(1, bytes);

        StringBuilder sb = new StringBuilder();
        while (value.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger[] divRem = value.divideAndRemainder(BigInteger.valueOf(BASE));
            value = divRem[0];
            int digit = divRem[1].intValue();
            sb.append(ALPHABET[digit]);
        }
        return sb.reverse().toString();
    }

    public static String hostName() {
        return Try.it(() -> InetAddress.getLocalHost().getHostName()).getOrElse("SYS").split("\\.")[0];
    }

    /**
     * Take the 22-character Base58 job ID and derive a directory name structure so files don’t all pile into one folder.
     * Using prefix-based sharding; first 2 chars from job ID: 64² = 4096 possibilities.
     * @return the directory name for the given job ID
     */
    public static String jobIdToDir(String jobId) {
        if (isEmpty(jobId) || jobId.length() < 2) return "no_id";
        return jobId.substring(0, 2);
    }

    public static String toJson(JobInfo info) {
        JSONObject jsonObject = toJsonObject(info);
        return jsonObject == null ? "null" : jsonObject.toJSONString();
    }

    /**
     * Import job histories from the given service URL.
     * @param svcDef   the service to import job histories from
     * @param userJobs
     * @return the set of job IDs that were imported
     */
    public static Set<String> importJobHistories(String svcDef, List<JobInfo> userJobs) {
        int count = 0;

        String[] svcParts = ifNotNull(svcDef).getOrElse("").split("\\|", 3);
        String url = svcParts[0].trim();
        String svcId = svcParts.length > 1 ? svcParts[1].trim() : null;
        String svcType = svcParts.length > 2 ? svcParts[2].trim() : null;
        if (url.isEmpty()) return Set.of();

        URL urlObs= Try.it(() -> new URI(url).toURL()).getOrElse((URL)null);
        String paramStr= urlObs == null ? "" : urlObs.getQuery();
        String urlBase= (!isEmpty(paramStr) && url.contains("?"))  ? url.split("\\?")[0] : url;

        HttpServiceInput input = new HttpServiceInput(urlBase);
        if (!isEmpty(paramStr)) input.setRequestUrl(input.getRequestUrl()+"?"+paramStr);
        LOG.info("Importing job histories from %s; svcId=%s svcType=%s".formatted(input.getRequestUrl(), svcId, svcType));
        Ref<List<JobInfo>> jobList = new Ref<>();
        HttpServices.getData(input, r -> {
           Try.it(() -> {
                Document doc = parse(r.getResponseBodyAsStream());
                jobList.set(convertToJobList(doc, urlBase));
            }).getOrElse(e -> {
                LOG.error("Failed to import job histories from %s: %s".formatted(url, e.getMessage()));
            });
           return HttpServices.Status.ok();
        });
        if (jobList.get() == null || jobList.get().isEmpty()) return Set.of();

        // remove jobs with no URL or runId in the ignore list
        boolean hasBadJobs = jobList.get().removeIf(j -> j.getAux().getJobUrl() == null || runIdIgnoreList.contains(String.valueOf(j.getRunId())));
        if (hasBadJobs) LOG.debug("Some jobs with no URL or ignored runId were removed from list");
        HashSet<String> importedIds = new HashSet<>();
        for (JobInfo job : jobList.get()) {
            JobInfo jobInfo = findJobInfo(job.getJobId(), userJobs);
            if (jobInfo == null || isActive(jobInfo)) {
                // for performance, we only get updated job info if the job is not in history, or it's still active
                JobInfo uws = Try.it(() -> getUwsJobInfo(job.getAux().getJobUrl())).get();
                if (uws == null) {
                    LOG.debug("Failed to get job info for " + job.getAux().getJobUrl());
                } else {
                    count++;
                    mergeJobInfo(jobInfo, uws, svcId, svcType);
                    if (jobInfo == null )  userJobs.add(uws);           // update passed in userJobs; to avoid having to call getUserJobs, which can be expensive
                    importedIds.add(uws.getJobId());
                    LOG.trace("Job added jobUrl=%s jobId=%s".formatted(job.getAux().getJobUrl(),uws.getJobId()));
                }
            } else {
                LOG.debug("Job %s is already completed, skipping".formatted(job.getJobId()));
            }
        }
        LOG.debug("%d job histories imported".formatted(count));
        return importedIds;     // return imported job IDs to the caller to avoid having to call the uws service again
    }

    public static JobInfo mergeJobInfo(JobInfo local, JobInfo uws, String svcId, String svcType) {
        String jobId = local == null ? nextJobId() : local.getMeta().getJobId();
        return updateJobInfo(jobId, true, ji -> {
            ji.copyFrom(uws);
            Job.Type type = Try.it(() -> Job.Type.valueOf(svcType)).getOrElse(Job.Type.UWS);
            ji.getMeta().setType(type);
            if (svcId != null )  ji.getMeta().setSvcId(svcId);
        });
    }

    public static JobInfo findJobInfo(String uwsJobId, List<JobInfo> mylist) {
        if (mylist == null || mylist.isEmpty()) return null;
        for(JobInfo ji : mylist) {
            if (ji.getJobId().equals(uwsJobId)) return ji;
        }

        return null;
    }

    public static boolean isActive(JobInfo jobInfo) {
        Phase phase = jobInfo.getPhase();
        return phase == PENDING
            || phase == QUEUED
            || phase == EXECUTING;
    }
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
    public static String toJsonJobList(List<JobInfo> infos, boolean overflow) {
        JSONObject rval = new JSONObject();
        if (infos != null && !infos.isEmpty()) {
            // object with "jobs": array of JobInfo and "overflow: true" if the list exceed 'LAST' or default limit
            List<JSONObject> jobs = infos.stream().map(JobUtil::toJsonObject).collect(Collectors.toList());
            rval.put("jobs", jobs);
            if (overflow) rval.put("overflow", true);
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