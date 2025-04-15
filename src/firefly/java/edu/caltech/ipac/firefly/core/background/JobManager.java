/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.api.Async;
import edu.caltech.ipac.firefly.core.Util.Try;
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
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.lang.text.StrBuilder;
import org.json.simple.JSONObject;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobUtil.*;
import static edu.caltech.ipac.firefly.data.ServerParams.EMAIL;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.firefly.core.background.Job.Type.PACKAGE;
import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.*;
import static java.util.Optional.ofNullable;


/**
 * Date: 9/30/21
 *
 * @author loi
 * @version : $
 */
public class JobManager {

    public static final String BG_INFO = "background.info";
    public static final long CLEANUP_INTVL_MINS = AppProperties.getIntProperty("job.cleanup.interval", 60);     // run cleanup once every 60 minutes
    private static final int KEEP_ALIVE_INTERVAL = AppProperties.getIntProperty("job.keepalive.interval", 30);  // default keepalive interval in seconds
    private static final int WAIT_COMPLETE = AppProperties.getIntProperty("job.wait.complete", 1);              // wait for complete after submit in seconds
    private static final int MAX_PACKAGERS = AppProperties.getIntProperty("job.max.packagers", 10);             // maximum number of simultaneous packaging threads
    private static final int ARCHIVE_RETENTION_PERIOD = AppProperties.getIntProperty("job.archive.retention.period", 24*7);   // Time in hours to keep a job in the archived state before deletion.  Default to 7 days.
    private static final int ARCHIVE_AFTER = AppProperties.getIntProperty("job.archive.after", 24*7);           // Time in hours to keep a job after it has completed before archiving.  Default to 7 days.

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final ExecutorService packagers = Executors.newFixedThreadPool(MAX_PACKAGERS);
    private static final ExecutorService searches = Executors.newCachedThreadPool();
    private static final HashMap<String, JobEntry> runningJobs = new HashMap<>();
    private static final Cache<JobInfo> allJobInfos = CacheManager.getDistributedMap("ALL_JOB_INFOS");
    private static final String COMPLETED_HANDLER = AppProperties.getProperty("job.completed.handler");


    static {
        if (!isEmpty(COMPLETED_HANDLER)) {
            Class<?> clz = Try.it(() -> Class.forName(COMPLETED_HANDLER)).get();
            if (clz != null && JobCompletedHandler.class.isAssignableFrom(clz)) {
                JobCompletedHandler handler = Try.it(() -> (JobCompletedHandler) clz.newInstance()).get();
                if (handler != null)    Messenger.subscribe(JobCompletedEvent.TOPIC, handler);
            } else {
                LOG.error("Invalid JobCompletedHandler class: " + COMPLETED_HANDLER);
            }
        }

        Messenger.subscribe(JobEvent.TOPIC, new JobEventHandler());
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    JobManager::checkJobs, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);   // check every 30 seconds
    }

    /**
     * @return a list of JobInfo belonging to the current request owner
     */
    public static List<JobInfo> list() {
        String userKey = ServerContext.getRequestOwner().getUserKey();
        return ofNullable(getAllJobs())
                .orElse(Collections.emptyList())
                .stream()
                .filter(info -> userKey.equals(info.getMeta().getUserKey()) && info.getMeta().isMonitored())  // only return monitored jobs belonging to the current user
                .toList();
    }

    public static JobInfo submit(Job job) {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        String jobId = nextJobId();
        updateJobInfo(jobId, true, ji -> {      // setting 'true' to add this jobInfo into the datastore
            Instant start = Instant.now();
            ji.setCreationTime(start);
            ji.setDestruction(start.plus(7, ChronoUnit.DAYS));
            ji.getMeta().setType(job.getType());
            ji.getMeta().setUserKey(reqOwner.getUserKey());
            ji.getMeta().setEventConnId(reqOwner.getEventConnID());
            ji.getMeta().setRunHost(hostName());
            ji.getMeta().setMonitored(true);                // all async jobs are monitored by default
            setupUserProps(ji);
        });
        // update Job after jobInfo has been created
        job.runAs(reqOwner);
        job.setJobId(jobId);

        sendUpdate(jobId, ji -> {
            ji.setPhase(QUEUED);
            ji.getMeta().setProgress(0);
        });

        try {
            Future<String> future = job.getType() == PACKAGE ? packagers.submit(job) : searches.submit(job);
            runningJobs.put(jobId, new JobEntry(future, job));

            future.get(WAIT_COMPLETE, TimeUnit.SECONDS);        // wait in seconds for a job to complete
        } catch (TimeoutException e) {
            // it's ok; job may take longer to complete
        } catch (Exception e) {
            // job run() handles exceptions; this only happens if submit or future.get() fails
            sendUpdate(jobId, (ji) -> {
                ji.setError(new JobInfo.Error(500, e.getMessage()));
                ji.getMeta().setProgress(100, null);
            });
            LOG.error(e);
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
        JobEntry jobEntry = runningJobs.get(jobInfo.getMeta().getJobId());
        if (jobEntry != null) {
            if (jobEntry.future != null) jobEntry.future.cancel(true);
            runningJobs.remove(jobInfo.getMeta().getJobId());
        }
    }

    @Nonnull
    public static BackGroundInfo getBackgroundInfo() {
        BackGroundInfo bgInfo = CacheManager.<BackGroundInfo>getUserCache().get(new StringKey(BG_INFO));
        return bgInfo == null ? new BackGroundInfo(false, "") : bgInfo;
    }

    public static void setBackgroundInfo(BackGroundInfo bfInfo) {
        CacheManager.getUserCache().put(new StringKey(BG_INFO), bfInfo);
    }

    public static JobInfo setMonitored(String jobId, boolean isMonitored) {
        return sendUpdate(jobId, ji -> ji.getMeta().setMonitored(isMonitored));
    }

    public static JobInfo sendEmail(String jobId, String email) {
        updateJobInfo(jobId, (ji) -> ji.getMeta().getParams().put(EMAIL, email));
        JobInfo jobInfo = getJobInfo(jobId);
        if (jobInfo != null) EmailNotification.sendNotification(jobInfo);
        return jobInfo;
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
     * @param jobId refers to Firefly's internal jobId, accessible via JobInfo.getMeta().getJobId().
     * @param addIfNoFound if true, a new JobInfo will be created and stored if no JobInfo is found with the given jobId
     * @param func the update function to apply to the JobInfo
     * @return the updated JobInfo, or null if no JobInfo is found
     */
    public static JobInfo updateJobInfo(String jobId, boolean addIfNoFound, Consumer<JobInfo> func) {
        JobInfo info = getJobInfo(jobId);
        if (info == null && addIfNoFound) {
            info = new JobInfo(jobId);
            info.getMeta().setJobId(jobId);
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
    public static JobInfo sendUpdate(String jobId, Consumer<JobInfo> func) {
        JobInfo jobInfo = updateJobInfo(jobId, func);
        if (jobInfo != null) {
            Messenger.publish(new JobEvent(JobEvent.EventType.UPDATED, jobInfo));
            Logger.getLogger().trace("sendUpdate: " + jobInfo.getMeta().getJobId() + " " + jobInfo.getPhase() + jobInfo.getMeta().getProgressDesc());
        }
        return jobInfo;
    }

    /**
     * internal method to update the JobInfo in the datastore
     * @param info
     */
    private static void updateJobInfo(JobInfo info) {
        CacheKey key = cacheKey(info);
        if (key != null) {
            boolean isCompleted = ifNotNull(allJobInfos.get(key)).get(JobInfo::getPhase) == COMPLETED;
            allJobInfos.put(key, info);
            if (info.getPhase() == COMPLETED && !isCompleted) {     // job changed from not completed to completed
                if (info.getResults().isEmpty()) {                  // if no results, add a default result
                    info.setResults(List.of(new Result("result", Async.getAsyncUrl() + info.getMeta().getJobId() + "/results/result", null, null)));
                }
                runningJobs.remove(info.getMeta().getJobId());
                if (info.getMeta().isMonitored()) {
                    publishCompleted(info);
                }
            }
            logJobInfo(info);
        }
    }

    static void publishCompleted(JobInfo jobInfo) {
        if (jobInfo != null) {
            BackGroundInfo bgInfo = getBackgroundInfo();
            if (bgInfo.sendNotif()) {
                if (isEmpty(jobInfo.getAux().getUserEmail())) jobInfo.getAux().setUserEmail(bgInfo.email);
                Messenger.publish(new JobCompletedEvent(jobInfo));       // notify all instances this job is completed
            }
        }
    }
    

    /**
     * internal method to notify all clients with the updated jobInfo
     * @param jobInfo
     */
    private static void updateClient(JobInfo jobInfo) {
        if (jobInfo == null) return;
        // send updated jobInfo to client
        FluxAction addAction = new FluxAction(FluxAction.JOB_INFO, toJsonObject(jobInfo));
        ServerEvent.EventTarget evt = new ServerEvent.EventTarget(ServerEvent.Scope.USER);
        evt.setUserKey(jobInfo.getMeta().getUserKey());
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
            long total = getAllJobs().stream().filter(v -> v.getMeta().getType() == type).count();
            long active = getAllJobs().stream().filter(v -> v.getPhase() == EXECUTING && v.getMeta().getType() == type).count();
            long error = getAllJobs().stream().filter(v -> v.getPhase() == ERROR && v.getMeta().getType() == type).count();

            sb.append(String.format("%9s |%,12d %,12d %,12d\n", type, total, active, error));
        });

        if (details) {
            sb.append("\n");
            sb.append("JOB ID               LOCAL JOB ID         PHASE       startTime              elapsedTime(s) progress monitored userKy                               \n");
            sb.append("-------------------- -------------------- ----------- ---------------------- -------------- -------- --------- -------------------------------------\n");
            getAllJobs().forEach((ji) -> {
                Instant endT = ji.getEndTime() != null ? ji.getEndTime() : Instant.now();
                sb.append(String.format("%-20s %-20s %-11s %-22s %,14d %8d %9s %-37s\n",
                        ji.getJobId(),
                        ji.getMeta().getJobId(),
                        ji.getPhase(),
                        ji.getStartTime().truncatedTo(ChronoUnit.SECONDS),
                        Duration.between(ji.getStartTime(), endT).toSeconds(),
                        ji.getMeta().getProgress(),
                        ji.getMeta().isMonitored(),
                        ji.getMeta().getUserKey()));
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
        List<? extends CacheKey> keys = allJobInfos.getKeys();
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        ArrayList<JobInfo> rval = new ArrayList<JobInfo>(keys.size());
        for (CacheKey key : keys) {
            JobInfo jobInfo = ifNotNull(() -> allJobInfos.get(key)).get();  // ignore error
            if (jobInfo != null) {
                rval.add(jobInfo);
            }
        }
        return rval;
    }

    /**
     * User info specifically for Job Notification
     */
    private static void setupUserProps(JobInfo ji) {
        String email = getBackgroundInfo().email;
        UserInfo uInfo = ServerContext.getRequestOwner().getUserInfo();
        email = isEmpty(email) && !uInfo.isGuestUser() ? uInfo.getEmail() : email;
        String name = Stream.of(uInfo.getFirstName(), uInfo.getLastName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" "));

        ji.getAux().setUserName(name);
        ji.getAux().setUserEmail(email);
        ji.getAux().setUserId(uInfo.getLoginName());
    }

    public record BackGroundInfo(boolean sendNotif, String email) implements Serializable {}
//====================================================================
//
//====================================================================

    private static CacheKey cacheKey(JobInfo jobInfo) {
        return cacheKey(jobInfo.getMeta().getJobId());
    }

    private static CacheKey cacheKey(String jobId) {
        return isEmpty(jobId) ? null : new StringKey(jobId);
    }

    private static void checkJobs() {

        // ping clients with active(EXECUTING) job
        getAllJobs().stream().filter(fi -> fi != null && fi.getPhase() == EXECUTING)     // all running jobs
                .map(fi -> fi.getMeta().getUserKey() + "::" + fi.getMeta().getEventConnId()).distinct()       // get distinct list of userKey and connId
                .forEach(client -> {                                                    // ensure it gets pinged only once, regardless of the number of jobs it has.
                    String[] ownerConnId = client.split("::");
                    WebsocketConnector.pingClient(ownerConnId[0], ownerConnId[1]);      // this will only ping client with the given owner/connId
                });

        // kill expired jobs
        getRunningJobs().forEach(fi -> {
                    long duration = fi.executionDuration();
                    if (duration != 0 && fi.getStartTime().plus(duration, ChronoUnit.SECONDS).isBefore(Instant.now())) {
                        abort(fi.getMeta().getJobId(), "Exceeded execution duration");
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
            ifNotNull(JobEvent.getJobInfo(msg)).apply(jobInfo -> {
                JobEvent.EventType type = Try.it(() -> JobEvent.EventType.valueOf(msg.getValue(null, JobEvent.TYPE))).get();
                if (type == JobEvent.EventType.ABORTED) {
                    handleAborted(jobInfo);
                } else {
                    updateClient(jobInfo);        // update jobInfo to client
                }
            });
        }
    }

    public static class JobEvent extends Message {
        public static final String TOPIC = "JobEvent";
        public enum EventType { UPDATED, COMPLETED, ABORTED }
        public static final String JOB = "job";
        public static String TYPE = "type";

        public JobEvent(EventType type, JobInfo jobInfo) {
            setTopic(TOPIC);
            setValue(type.toString(), TYPE);
            if (jobInfo != null) setValue(toJsonObject(jobInfo), JOB);
        }

        public static JobInfo getJobInfo(Message msg) {
            if (msg.getValue(null, JOB) instanceof JSONObject jo) {
                return toJobInfo(jo);
            }
            return null;
        }

        public static boolean isJobEvent(Message msg) {
            EventType type = Try.it(() -> EventType.valueOf(msg.getValue("", TYPE))).get();
            return type != null && msg.getValue(null, JOB) != null;
        }
    }

    /**
     * Published event notifying that a job has completed.
     * It contains all necessary information for a COMPLETED handler to perform its task.
     */
    public static final class JobCompletedEvent extends JobEvent {
        public static final String TOPIC = "JobCompleted";

        public JobCompletedEvent(JobInfo jobInfo) {
            super(EventType.COMPLETED, jobInfo);
            setTopic(TOPIC);
        }

        public static boolean isJobCompletedEvent(Message msg) {
            EventType type = Try.it(() -> EventType.valueOf(msg.getValue("", TYPE))).get();      // make sure it does not fail on back messages
            return type == EventType.COMPLETED &&
                    msg.getValue("", TOPIC_KEY).equals(TOPIC) &&
                    msg.getValue(null, JOB) != null;
        }
    }

    public static void cleanup() {
        allJobInfos.getKeys().forEach( k -> {
            JobInfo job = allJobInfos.get(k);
            if (!job.getMeta().isMonitored() && job.getEndTime().plus(1, ChronoUnit.HOURS).isBefore(Instant.now())) {
                LOG.info("Removing non-monitored job: " + k);
                allJobInfos.remove(k);      // remove non-monitored job after 1 hour
            } else if (TERMINATED_PHASES.contains(job.getPhase()) && job.getEndTime().plus(ARCHIVE_AFTER, ChronoUnit.HOURS).isBefore(Instant.now())) {
                LOG.info("Archiving job: " + k);
                updateJobInfo(job.getMeta().getJobId(), jobInfo -> jobInfo.setPhase(ARCHIVED));
            } else if (job.getPhase().equals(ARCHIVED) && job.getEndTime().plus(ARCHIVE_RETENTION_PERIOD, ChronoUnit.HOURS).isBefore(Instant.now())) {
                LOG.info("Removing archived job: " + k);
                allJobInfos.remove(k);
            }
        });
    }
}
