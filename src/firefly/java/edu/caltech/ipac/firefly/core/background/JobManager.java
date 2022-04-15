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
import static edu.caltech.ipac.firefly.core.background.Job.Type.SEARCH;
import static edu.caltech.ipac.firefly.core.background.JobInfo.PHASE.*;


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
            runningJobs.remove(jobInfo.getId());
        }
        // send updated jobInfo to client
        FluxAction addAction = new FluxAction(FluxAction.JOB_INFO, toJsonObject(jobInfo));
        ServerEventManager.fireAction(addAction, ServerEvent.Scope.USER);
    }

    public static JobInfo submit(Job job) {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        JobInfo info = new JobInfo(nextJobId());
        info.setOwner(reqOwner.getUserKey());
        info.setPhase(QUEUED);
        info.setEventConnId(reqOwner.getEventConnID());
        info.setType(job.getType());
        allJobInfos.put(info.getId(), info);

        job.runAs(reqOwner);
        job.setJobId(info.getId());

        logJobInfo(info);
        Future future = job.getType() == SEARCH ? searches.submit(job) : packagers.submit(job);

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
        rval.put("jobId", info.getId());
        rval.put("ownerId", info.getOwner());
        rval.put("phase", info.getPhase().toString());
        applyIfNotEmpty(info.getLabel(),   v -> rval.put("label", v));
        rval.put("startTime", info.getStartTime().toString());
        applyIfNotEmpty(info.getEndTime(),   v -> rval.put("endTime", v.toString()));
        applyIfNotEmpty(info.executionDuration(),   v -> rval.put("executionDuration", v));
        applyIfNotEmpty(info.getDestructionTime(),   v -> rval.put("destruction", v.toString()));
        rval.put("parameters", info.getParams());

        if (info.getPhase() == COMPLETED && info.getResults().size() == 0) {
            rval.put("results", Arrays.asList(asyncUrl + info.getId() + "/results/result"));
        } else {
            if (info.getResults().size() > 0) rval.put("results", info.getResults());
        }
        applyIfNotEmpty(info.getError(),   v -> rval.put("error", v.getMsg()));
        applyIfNotEmpty(info.getType(),   v -> rval.put("type", v.toString()));
        applyIfNotEmpty(info.isMonitored(),   v -> rval.put("monitored", v));
        rval.put("progress", info.getProgress());
        applyIfNotEmpty(info.getProgressDesc(),   v -> rval.put("progressDesc", v));
        applyIfNotEmpty(info.getDataOrigin(), v -> rval.put("dataOrigin", v));
        applyIfNotEmpty(info.getSummary(),   v -> rval.put("summary", v));

        return rval;
    }

    /**
     * @param infos
     * @return an array of job IDs under the 'jobs' prop as a json string
     */
    public static String toJsonJobList(List<JobInfo> infos) {
        JSONObject rval = new JSONObject();
        if (infos != null && infos.size() > 0) {
            // object with "jobs": array of JobInfo urls
            List<String> urls = infos.stream().map(i ->i.getId()).collect(Collectors.toList());
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
                rval.put("results", Arrays.asList(getAsyncUrl() + info.getId() + "/results/result"));
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


        sb.append("   Job Count       Active Count     Error Count   \n");
        sb.append("(Search/Package) (Search/Package) (Search/Package)\n");
        sb.append("---------------- ---------------- ----------------\n");

        long searchCnt = allJobInfos.values().stream().filter(v -> v.getType() == SEARCH).count();
        long packageCnt = allJobInfos.values().stream().filter(v -> v.getType() == PACKAGE).count();
        long errSearchCnt = allJobInfos.values().stream().filter(v -> v.getPhase() == ERROR && v.getType() == SEARCH).count();
        long errPackageCnt = allJobInfos.values().stream().filter(v -> v.getPhase() == ERROR && v.getType() == PACKAGE).count();
        long activeSearchCnt = allJobInfos.values().stream().filter(v -> v.getPhase() == EXECUTING && v.getType() == SEARCH).count();
        long activePackageCnt = allJobInfos.values().stream().filter(v -> v.getPhase() == EXECUTING && v.getType() == PACKAGE).count();

        sb.append(String.format("%,16d %,16d %,16d\n",
                allJobInfos.size(), activeSearchCnt+activePackageCnt, errSearchCnt+errPackageCnt));
        sb.append(String.format(" %,6d / %,6d  %,6d / %,6d  %,6d / %,6d\n",
                searchCnt, packageCnt, activeSearchCnt, activePackageCnt, errSearchCnt, errPackageCnt));

        if (details) {
            sb.append("\n");
            sb.append("JOB ID          PHASE       startTime                   elapsedTime(s) progress\n");
            sb.append("--------------- ----------- --------------------------- -------------- --------\n");
            allJobInfos.forEach((k, v) -> {
                sb.append(String.format("%15s %11s %27s %14s %8s\n",
                        v.getId(), v.getPhase(), v.getStartTime(), Duration.between(v.getStartTime(), Instant.now()).toMillis()/1000, v.getProgress()));
            });
        }
        return sb.toString();
    }

    static void logJobInfo(JobInfo info) {
        LOG.debug(String.format("JOB:%s  owner:%s  phase:%s  msg:%s", info.getId(), info.getOwner(), info.getPhase(), info.getSummary()));
        LOG.trace(String.format("JOB: %s details: %s", info.getId(), toJson(info)));
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
                abort(info.getId(), "Exceeded execution duration");
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
