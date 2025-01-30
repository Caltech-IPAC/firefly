/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.messaging.Message;
import edu.caltech.ipac.firefly.messaging.Messenger;
import edu.caltech.ipac.firefly.messaging.Subscriber;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.events.WebsocketConnector;
import edu.caltech.ipac.firefly.server.packagedata.PackagedEmail;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.lang.text.StrBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobUtil.logJobInfo;
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

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final ExecutorService packagers = Executors.newFixedThreadPool(MAX_PACKAGERS);
    private static final ExecutorService searches = Executors.newCachedThreadPool();
    private static final HashMap<String, JobEntry> runningJobs = new HashMap<>();
    private static final Cache<JobInfo> allJobInfos = CacheManager.getDistributedMap("ALL_JOB_INFOS");

    static {
        Messenger.subscribe(JobEvent.TOPIC, new JobEventHandler());
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    JobManager::checkJobs, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);   // check every 30 seconds
    }

    /**
     * @return a list of JobInfo belonging to the current request owner
     */
    public static List<JobInfo> list() {
        String owner = ServerContext.getRequestOwner().getUserKey();
        return getAllJobs().stream()
                  .filter(info -> info != null && owner.equals(info.getOwner()))
                  .toList();
    }

    public static JobInfo submit(Job job) {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        String jobId = JobUtil.nextJobId();
        updateJobInfo(jobId, true, ji -> {      // setting 'true' to add this jobInfo into the datastore
            Instant start = Instant.now();
            ji.setStartTime(start);
            ji.setCreationTime(start);
            ji.setDestruction(start.plus(7, ChronoUnit.DAYS));
            ji.setOwner(reqOwner.getUserKey());
            ji.setPhase(QUEUED);
            ji.setEventConnId(reqOwner.getEventConnID());
            ji.setType(job.getType());
        });

        job.runAs(reqOwner);
        job.setJobId(jobId);

        Future<String> future = job.getType() == PACKAGE ? packagers.submit(job) : searches.submit(job);
        runningJobs.put(jobId, new JobEntry(future, job));

        try {
            future.get(WAIT_COMPLETE, TimeUnit.SECONDS);        // wait in seconds for a job to complete
        } catch (InterruptedException e) {
            updateJobInfo(jobId, (ji) -> {
                ji.setPhase(ABORTED);
            });
        } catch (TimeoutException e) {
            // it's ok; job may take longer to complete
        } catch (Exception e) {
            ifNotNull(getJobInfo(jobId)).apply(ji -> {
                if (ji.getPhase() != ERROR) {
                    job.setError(500, e.getMessage());
                }
            });
            LOG.error(e);
        }

        JobInfo.Phase phase = ifNotNull(getJobInfo(jobId)).get(JobInfo::getPhase);
        if (future.isDone() && phase != ERROR && phase != ABORTED) {
            sendUpdate(jobId, (ji) -> ji.setPhase(COMPLETED));
        } else {
            sendUpdate(jobId, (ji) -> ji.setPhase(phase));      // make sure job info is sent to client
        }
        return getJobInfo(jobId);
    }

    public static JobInfo abort(String jobId, String reason) {
        JobInfo info = updateJobInfo(jobId, (ji) -> {
            ji.setError(new JobInfo.Error(410, reason));
            ji.setPhase(ABORTED);
        });
        if (info != null) {
            Messenger.publish(new JobEvent(JobEvent.EventType.ABORTED, info));      // notify all instances AFTER jobInfo is updated
        }
        return info;
    }

    static void handleAborted(JobInfo jobInfo) {
        if (jobInfo == null) return;
        JobEntry jobEntry = runningJobs.get(jobInfo.getJobId());
        if (jobEntry != null) {
            if (jobEntry.job != null) {
                if (jobEntry.job.getWorker() != null) {
                    jobEntry.job.getWorker().onAbort();
                }
            }
            if (jobEntry.future != null) jobEntry.future.cancel(true);
            runningJobs.remove(jobInfo.getJobId());
        }
    }

    public static JobInfo setMonitored(String jobId, boolean isMonitored) {
        JobInfo jobInfo = updateJobInfo(jobId, ji -> {
            if (ji.isMonitored() != isMonitored) {
                ji.setMonitored(isMonitored);
            }
        });
        if (jobInfo != null ) {
            Messenger.publish(new JobEvent(JobEvent.EventType.MONITORED, jobInfo));      // notify all instances AFTER jobInfo is updated
        }
        return getJobInfo(jobId);
    }

    public static JobInfo sendEmail(String jobId, String email) {
        updateJobInfo(jobId, (ji) -> ji.getParams().put(EMAIL, email));
        PackagedEmail.send(jobId);
        return getJobInfo(jobId);
    }

    public static String results(String jobId) {
        return ifNotNull(getJobInfo(jobId)).get(JobUtil::toJsonResults);
    }

//====================================================================
//  Getter/Setter
//====================================================================

    /**
     * Retrieves the stored JobInfo with the given jobId.
     * Returned value is read-only.  Use updateJobInfo to update the JobInfo.
     * @param jobId the ID of the job
     * @return the JobInfo with the jobId, or null not found
     */
    public static JobInfo getJobInfo(String jobId) {
        if (isEmpty(jobId)) return null;
        return allJobInfos.get(cacheKey(jobId));
    }

    /**
     *  see {@link #updateJobInfo(String, boolean, Consumer)}
     */
    public static JobInfo updateJobInfo(String jobId, Consumer<JobInfo> func) {
        return updateJobInfo(jobId, false, func);
    }

    /**
     * Retrieves the stored JobInfo, applies the updates, and returns the updated JobInfo.
     * This is done only when there is a JobInfo with the given jobId.
     * @param jobId the job ID
     * @param addIfNoFound if true, a new JobInfo will be created and stored if no JobInfo is found with the given jobId
     * @param func the update function to apply to the JobInfo
     * @return the updated JobInfo, or null if no JobInfo is found
     */
    public static JobInfo updateJobInfo(String jobId, boolean addIfNoFound, Consumer<JobInfo> func) {
        JobInfo info = getJobInfo(jobId);
        if (info == null && addIfNoFound) {
            info = new JobInfo(jobId);
        }
        if (info == null || func == null) return null;
        func.accept(info);
        updateJobInfo(info);
        return info;
    }

    /**
     * This method updates the JobInfo using the provided function and publishes an update event.
     * @param jobId the ID of the job to update
     * @param func the function to apply to the JobInfo
     */
    public static void sendUpdate(String jobId, Consumer<JobInfo> func) {
        JobInfo jobInfo = updateJobInfo(jobId, func);
        if (jobInfo != null) {
            Messenger.publish(new JobEvent(JobEvent.EventType.UPDATED, jobInfo));
        }
    }

    /**
     * internal method to update the JobInfo in the datastore
     * @param info
     */
    private static void updateJobInfo(JobInfo info) {
        CacheKey key = cacheKey(info);
        if (key != null) {
            allJobInfos.put(key, info);
            if (info.getPhase() == COMPLETED) {
                runningJobs.remove(info.getJobId());
                Messenger.publish(new JobCompleted(info));       // notify all instances this job is completed
            }
            logJobInfo(info);
        }
    }

    /**
     * internal method to notify all clients with the updated jobInfo
     * @param jobInfo
     */
    static void updateClient(JobInfo jobInfo) {
        if (jobInfo == null) return;
        // send updated jobInfo to client
        FluxAction addAction = new FluxAction(FluxAction.JOB_INFO, JobUtil.toJsonObject(jobInfo));
        ServerEvent.EventTarget evt = new ServerEvent.EventTarget(ServerEvent.Scope.USER);
        evt.setUserKey(jobInfo.getOwner());
        ServerEventManager.fireAction(addAction, evt);
    }


//====================================================================
//
//====================================================================

    /**
     * Print job statistics info
     * @return String
     */
    public static String getStatistics(boolean details) {
        StrBuilder sb = new StrBuilder();


        sb.append        ("          |   Job Count Active Count  Error Count\n");
        sb.append        ("          |------------ ------------ ------------\n");

        Arrays.stream(Job.Type.values()).forEach(type -> {
            long total = getAllJobs().stream().filter(v -> v.getType() == type).count();
            long active = getAllJobs().stream().filter(v -> v.getPhase() == EXECUTING && v.getType() == type).count();
            long error = getAllJobs().stream().filter(v -> v.getPhase() == ERROR && v.getType() == type).count();

            sb.append(String.format("%9s |%,12d %,12d %,12d\n", type, total, active, error));
        });

        if (details) {
            sb.append("\n");
            sb.append("JOB ID               PHASE       startTime              elapsedTime(s) progress monitored owner                                \n");
            sb.append("-------------------- ----------- ---------------------- -------------- -------- --------- -------------------------------------\n");
            getAllJobs().forEach((ji) -> {
                Instant endT = ji.getEndTime() != null ? ji.getEndTime() : Instant.now();
                sb.append(String.format("%-20s %-11s %-22s %,14d %8d %9s %-37s\n",
                        ji.getJobId(), ji.getPhase(), ji.getStartTime().truncatedTo(ChronoUnit.SECONDS), Duration.between(ji.getStartTime(), endT).toSeconds(), ji.getProgress(), ji.isMonitored(), ji.getOwner()));
            });
        }
        return sb.toString();
    }

    static List<JobInfo> getRunningJobs() {
        return runningJobs.values().stream()
                .map(je -> getJobInfo(je.job.getJobId()))               // convert JobEntry to JobInfo
                .filter(Objects::nonNull).toList();                     // return only non-null JobInfo
    }

    static List<JobInfo> getAllJobs() {
        return allJobInfos.getKeys().stream()
                .map(allJobInfos::get)
                .filter(Objects::nonNull).toList();
    }

//====================================================================
//
//====================================================================

    private static CacheKey cacheKey(JobInfo jobInfo) {
        return cacheKey(jobInfo.getJobId());
    }

    private static CacheKey cacheKey(String jobId) {
        return isEmpty(jobId) ? null : new StringKey(jobId);
    }

    private static void checkJobs() {

        // ping clients with active(EXECUTING) job
        getAllJobs().stream().filter(fi -> fi != null && fi.getPhase() == EXECUTING)     // all running jobs
                .map(fi -> fi.getOwner() + "::" + fi.getEventConnId()).distinct()       // get distinct list of owner and connId
                .forEach(client -> {                                                    // ensure it gets pinged only once, regardless of the number of jobs it has.
                    String[] ownerConnId = client.split("::");
                    WebsocketConnector.pingClient(ownerConnId[0], ownerConnId[1]);      // this will only ping client with the given owner/connId
                });

        // kill expired jobs
        getRunningJobs().forEach(fi -> {
                    long duration = fi.executionDuration();
                    if (duration != 0 && fi.getStartTime().plus(duration, ChronoUnit.SECONDS).isBefore(Instant.now())) {
                        abort(fi.getJobId(), "Exceeded execution duration");
                    }
                });
    }

    private static class JobEntry {
        Future<String> future;
        Job job;

        public JobEntry(Future<String> future, Job job) {
            this.future = future;
            this.job = job;
        }
    }

    private static class JobEventHandler implements Subscriber {
        public void onMessage(Message msg) {
            JobEvent.EventType type = JobEvent.EventType.valueOf(msg.getValue(null, JobEvent.TYPE));
            JobInfo jobInfo = JobUtil.fromMsg(msg);
            updateClient(jobInfo);        // update jobInfo to client
            if (type == JobEvent.EventType.ABORTED) {
                handleAborted(jobInfo);
            }
        }
    }

    public static class JobEvent extends Message {
        public static final String TOPIC = "JobEvent";
        public enum EventType { UPDATED, COMPLETED, ABORTED, MONITORED }
        public static final String JOB = "job";
        public static String TYPE = "type";

        public JobEvent(EventType type, JobInfo jobInfo) {
            setTopic(TOPIC);
            setValue(type.toString(), TYPE);
            setValue(JobUtil.toJsonObject(jobInfo), JOB);
        }
    }

    /**
     * Published event notifying that a job has completed.
     * It contains all necessary information for a COMPLETED handler to perform its task.
     */
    public static final class JobCompleted extends JobEvent {
        public static final String TOPIC = "JobCompleted";
//        static final String FROM = AppProperties.getProperty("mail.smtp.from", "donotreply@ipac.caltech.edu");
        public JobCompleted(JobInfo jobInfo) {
            super(EventType.COMPLETED, jobInfo);
            setTopic(TOPIC);
            setValue(JobUtil.toJsonObject(jobInfo), JOB);
            UserInfo user = ServerContext.getRequestOwner().getUserInfo();
            if (!user.isGuestUser()) {
                setValue(user.getName(), "user", "name");
                setValue(user.getEmail(), "user", "email");
                setValue(user.getLoginName(), "user", "loginName");
            }
            applyIfNotEmpty(ServerContext.getRequestOwner().getSsoAdapter(), adpt -> setValue(adpt.getAuthToken(), "auth_info"));
        }
    }
}
