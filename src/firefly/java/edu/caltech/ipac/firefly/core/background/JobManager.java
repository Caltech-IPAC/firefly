/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.events.WebsocketConnector;
import edu.caltech.ipac.firefly.server.packagedata.PackagedEmail;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.apache.commons.lang.text.StrBuilder;
import org.json.simple.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.api.Async.getAsyncUrl;
import static edu.caltech.ipac.firefly.data.ServerParams.EMAIL;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.firefly.core.background.Job.Type.PACKAGE;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.*;


/**
 * Date: 9/30/21
 *
 * @author loi
 * @version : $
 */
public class JobManager {

    private static final int KEEP_ALIVE_INTERVAL = AppProperties.getIntProperty("job.keepalive.interval", 30);    // default keepalive interval in seconds
    private static final int WAIT_COMPLETE = AppProperties.getIntProperty("job.wait.complete", 1);                // wait for complete after submit in seconds
    private static final int MAX_PACKAGERS = AppProperties.getIntProperty("job.max.packagers", 10);               // maximum number of simultaneous packaging threads

    private static Logger.LoggerImpl LOG = Logger.getLogger();
    private static ExecutorService packagers = Executors.newFixedThreadPool(MAX_PACKAGERS);
    private static ExecutorService searches = Executors.newCachedThreadPool();
    private static HashMap<String, JobEntry> runningJobs = new HashMap<>();
    private static HashMap<String, JobInfo> allJobInfos = new HashMap<>();

    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    JobManager::checkJobs, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);   // check every 30 seconds
    }

    private static String nextJobId() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * @return a list of JobInfo belonging to the current request owner
     */
    public static List<JobInfo> list() {
        String owner = ServerContext.getRequestOwner().getUserKey();
        return allJobInfos.values().stream()
                  .filter(info -> info != null && owner.equals(info.getOwner()))
                  .collect(Collectors.toList());
    }

    public static JobInfo getJobInfo(String jobId) {
        return allJobInfos.get(jobId);
    }


    public static void sendUpdate(JobInfo jobInfo) {
        if (jobInfo.getPhase() == COMPLETED) {
            runningJobs.remove(jobInfo.getJobId());
        }
        // send updated jobInfo to client
        FluxAction addAction = new FluxAction(FluxAction.JOB_INFO, toJsonObject(jobInfo));
        ServerEventManager.fireAction(addAction, ServerEvent.Scope.USER);
    }

    public static JobInfo submit(Job job) {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        JobInfo info = new JobInfo(nextJobId());
        Instant start = Instant.now();
        info.setStartTime(start);
        info.setCreationTime(start);
        info.setDestruction(start.plus(7, ChronoUnit.DAYS));
        info.setOwner(reqOwner.getUserKey());
        info.setPhase(QUEUED);
        info.setEventConnId(reqOwner.getEventConnID());
        info.setType(job.getType());
        allJobInfos.put(info.getJobId(), info);

        job.runAs(reqOwner);
        job.setJobId(info.getJobId());

        logJobInfo(info);
        Future future = job.getType() == PACKAGE ? packagers.submit(job) : searches.submit(job);

        try {
            future.get(WAIT_COMPLETE, TimeUnit.SECONDS);        // wait in seconds for a job to complete
        } catch (InterruptedException e) {
            info.setPhase(ABORTED);
            logJobInfo(info);
        } catch (TimeoutException e) {
            // it's ok.. job may take longer to complete
        } catch (Exception e) {
            if (info.getPhase() != ERROR) {
                job.setError(500, e.getMessage());
            }
            logJobInfo(info);
            LOG.error(e);
        }

        if (future.isDone() && info.getPhase() != ERROR && info.getPhase() != ABORTED) {
            info.setPhase(COMPLETED);
        } else {
            runningJobs.put(job.getJobId(), new JobEntry(future, job));
        }
        sendUpdate(info);
        return job.getJobInfo();
    }

    public static JobInfo abort(String jobId, String reason) {
        JobEntry jobEntry = runningJobs.get(jobId);
        if (jobEntry != null) {
            if (jobEntry.job != null) {
                if (jobEntry.job.getWorker() != null) {
                    jobEntry.job.getWorker().onAbort();
                }
            }
            if (jobEntry.future != null) jobEntry.future.cancel(true);

            runningJobs.remove(jobId);
            JobInfo info = getJobInfo(jobId);
            info.setError(new JobInfo.Error(410, reason));
            info.setPhase(ABORTED);
            sendUpdate(info);
        }
        return getJobInfo(jobId);
    }

    public static JobInfo setMonitored(String jobId, boolean isMonitored) {
        JobInfo info = getJobInfo(jobId);
        if (info != null && info.isMonitored() != isMonitored) {
            info.setMonitored(isMonitored);
            sendUpdate(info);
        }
        return info;
    }

    public static JobInfo sendEmail(String jobId, String email) {
        JobInfo info = getJobInfo(jobId);
        if (!isEmpty(email)) info.getParams().put(EMAIL, email);
        PackagedEmail.send(info);
        return info;
    }

    public static String results(String jobId) {
        return toJsonResults(getJobInfo(jobId));
    }


//====================================================================
//
//====================================================================

    public static String toJson(JobInfo info) {
        JSONObject jsonObject = toJsonObject(info);
        return jsonObject == null ? "null" : jsonObject.toJSONString();
    }

    public static JSONObject toJsonObject(JobInfo info) {
        if (info == null) return null;
        String asyncUrl = getAsyncUrl();

        JSONObject rval = new JSONObject();
        rval.put("jobId", info.getJobId());
        applyIfNotEmpty(info.getRunId(), v -> rval.put("runId", v));
        applyIfNotEmpty(info.getOwner(), v -> rval.put("ownerId", v));
        applyIfNotEmpty(info.getPhase(), v -> rval.put("phase", v.toString()));
        applyIfNotEmpty(info.getQuote(),   v -> rval.put("quote", v.toString()));
        applyIfNotEmpty(info.getCreationTime(),   v -> rval.put("creationTime", v.toString()));
        applyIfNotEmpty(info.getStartTime(),   v -> rval.put("startTime", v.toString()));
        applyIfNotEmpty(info.getEndTime(),   v -> rval.put("endTime", v.toString()));
        applyIfNotEmpty(info.executionDuration(),   v -> rval.put("executionDuration", v.toString()));
        applyIfNotEmpty(info.getDestruction(), v -> rval.put("destruction", v.toString()));
        rval.put("parameters", info.getParams());

        if (info.getPhase() == COMPLETED && info.getResults().size() == 0) {
            rval.put("results", Arrays.asList(toResult(asyncUrl + info.getJobId() + "/results/result", null, null)));
        } else if (info.getResults().size() > 0) {
            rval.put("results", toResults(info.getResults()));
        }
        applyIfNotEmpty(info.getError(),   v -> {
            JSONObject errSum = new JSONObject();
            errSum.put("message", v.msg());
            errSum.put("type", v.code() < 500 ? "fatal" : "transient");     // 5xx are typically system error, e.g. server down.
            rval.put("errorSummary", errSum);
        });


        JSONObject addtlInfo = new JSONObject();
        rval.put("jobInfo", addtlInfo);
        applyIfNotEmpty(info.getType(),   v -> addtlInfo.put("type", v.toString()));
        applyIfNotEmpty(info.getLabel(), v -> addtlInfo.put("label", v));
        applyIfNotEmpty(info.getProgress(),   v -> addtlInfo.put("progress", v));
        applyIfNotEmpty(info.isMonitored(),   v -> addtlInfo.put("monitored", v));
        applyIfNotEmpty(info.getProgressDesc(),   v -> addtlInfo.put("progressDesc", v));
        applyIfNotEmpty(info.getDataOrigin(), v -> addtlInfo.put("dataOrigin", v));
        applyIfNotEmpty(info.getSummary(),   v -> addtlInfo.put("summary", v));
        applyIfNotEmpty(info.getLocalRunId(),   v -> addtlInfo.put("localRunId", v));

        return rval;
    }

    public static List<JSONObject> toResults(List<JobInfo.Result> results) {
        return results.stream().map(r -> toResult(r.href(), r.mimeType(), r.size()))
                .collect(Collectors.toList());
    }

    private static JSONObject toResult(String href, String mimeType, String size) {
        JSONObject ro = new JSONObject();
        applyIfNotEmpty(href,   v -> ro.put("href", v));
        applyIfNotEmpty(mimeType,   v -> ro.put("mimeType", v));
        applyIfNotEmpty(size,   v -> ro.put("size", v));
        return ro;
    }

    /**
     * @param infos
     * @return an array of job IDs under the 'jobs' prop as a json string
     */
    public static String toJsonJobList(List<JobInfo> infos) {
        JSONObject rval = new JSONObject();
        if (infos != null && infos.size() > 0) {
            // object with "jobs": array of JobInfo urls
            List<String> urls = infos.stream().map(i ->i.getJobId()).collect(Collectors.toList());
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
                rval.put("results", Arrays.asList(getAsyncUrl() + info.getJobId() + "/results/result"));
            } else {
                rval.put("results", info.getResults());
            }
        }
        return rval.toJSONString();
    }

    /**
     * Print job statistics info
     * @return String
     */
    public static String getStatistics(boolean details) {
        StrBuilder sb = new StrBuilder();


        sb.append        ("          |   Job Count Active Count  Error Count\n");
        sb.append        ("          |------------ ------------ ------------\n");

        Arrays.stream(Job.Type.values()).forEach(type -> {
            long total = allJobInfos.values().stream().filter(v -> v.getType() == type).count();
            long active = allJobInfos.values().stream().filter(v -> v.getPhase() == EXECUTING && v.getType() == type).count();
            long error = allJobInfos.values().stream().filter(v -> v.getPhase() == ERROR && v.getType() == type).count();

            sb.append(String.format("%9s |%,12d %,12d %,12d\n", type, total, active, error));
        });

        if (details) {
            sb.append("\n");
            sb.append("JOB ID          PHASE       startTime              elapsedTime(s) progress\n");
            sb.append("--------------- ----------- ---------------------- -------------- --------\n");
            allJobInfos.forEach((k, v) -> {
                Instant endT = v.getEndTime() != null ? v.getEndTime() : Instant.now();
                sb.append(String.format("%15s %11s %22s %,14d %8d\n",
                        v.getJobId(), v.getPhase(), v.getStartTime().truncatedTo(ChronoUnit.SECONDS), Duration.between(v.getStartTime(), endT).toSeconds(), v.getProgress()));
            });
        }
        return sb.toString();
    }

    static void logJobInfo(JobInfo info) {
        LOG.debug(String.format("JOB:%s  owner:%s  phase:%s  msg:%s", info.getJobId(), info.getOwner(), info.getPhase(), info.getSummary()));
        LOG.trace(String.format("JOB: %s details: %s", info.getJobId(), toJson(info)));
    }



//====================================================================
//
//====================================================================

    private static void checkJobs() {

        // ping clients with active(EXECUTING) job
        List<String> activeClients = runningJobs.values().stream().map(jobEntry -> jobEntry.job.getJobInfo())
                                    .filter(info -> info != null && info.getPhase() == EXECUTING)
                                    .map(info -> info.getOwner() + "||" + info.getEventConnId())
                                    .distinct()
                                    .collect(Collectors.toList());
        activeClients.forEach(client -> {
            String[] ownerConnId = client.split("||");
            WebsocketConnector.pingClient(ownerConnId[0], ownerConnId[1]);
        });

        // kill expired jobs
        runningJobs.values().forEach(je -> {
            JobInfo info = je.job.getJobInfo();
            long duration = info.executionDuration();
            if (duration != 0 && info.getStartTime().plus(duration, ChronoUnit.SECONDS).isBefore(Instant.now())) {
                abort(info.getJobId(), "Exceeded execution duration");
            }
        });
    }

    private static class JobEntry {
        Future future;
        Job job;

        public JobEntry(Future future, Job job) {
            this.future = future;
            this.job = job;
        }
    }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
