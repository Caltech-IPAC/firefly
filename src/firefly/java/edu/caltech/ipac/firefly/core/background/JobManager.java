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
import edu.caltech.ipac.firefly.server.cache.DistribMapCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.events.WebsocketConnector;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.lang.text.StrBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import redis.clients.jedis.params.ScanParams;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotEmpty;
import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobInfo.*;
import static edu.caltech.ipac.firefly.core.background.JobUtil.*;
import static edu.caltech.ipac.firefly.data.ServerParams.EMAIL;
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

    public static final String BG_INFO = "background.info";
    public static final String ALL_JOB_CACHE_KEY = "ALL_JOB_INFOS"; // cache key for all job infos
    public static final long CLEANUP_INTVL_MINS = AppProperties.getIntProperty("job.cleanup.interval", 12*60);          // run cleanup once every 12 hours
    public static final int JOB_LIST_DEFAULT_LIMIT = AppProperties.getIntProperty("job.list.default.limit", 100_000);    // the default limit for job list queries
    private static final int KEEP_ALIVE_INTERVAL = AppProperties.getIntProperty("job.keepalive.interval", 60);  // default keepalive interval in seconds
    private static final int WAIT_COMPLETE = AppProperties.getIntProperty("job.wait.complete", 1);              // wait for complete after submit in seconds
    private static final int MAX_PACKAGERS = AppProperties.getIntProperty("job.max.packagers", 10);             // maximum number of simultaneous packaging threads
    private static final int JOB_EXPIRY_HOURS = AppProperties.getIntProperty("job.expiry.hours", 24*14);        // Time in hours to keep a job after it has ended.  Default to 14 days.
    public static final int JOB_SCAN_BATCH_SIZE = AppProperties.getIntProperty("job.scan.batch_size", 10_000);   // batch size for scanning job keys in Redis.  Default to 10,000.  Larger value return more keys per call but use more CPU and memory per iteration.  this is a good size for larger redis store.

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final ExecutorService packagers = Executors.newFixedThreadPool(MAX_PACKAGERS);
    private static final ExecutorService searches = Executors.newCachedThreadPool();
    private static final HashMap<String, JobEntry> runningJobs = new HashMap<>();
    private static final DistribMapCache<JobInfo> allJobInfos = new DistribMapCache<>(ALL_JOB_CACHE_KEY, 0, new JobInfoSerializer()); // the all job hash should never expire
    private static final String COMPLETED_HANDLER = AppProperties.getProperty("job.completed.handler");
    private static final CacheKey JOB_CACHE_VERSION_KEY = new StringKey("job.all.cache.version");
    private static final String JOB_CACHE_VERSION = "1.0";

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


        String jobCacheVersion = (String) CacheManager.getDistributed().get(JOB_CACHE_VERSION_KEY);
        if (isEmpty(jobCacheVersion) || !jobCacheVersion.equals(JOB_CACHE_VERSION)) {
            LOG.info("Migrating job cache keys to new format");
            int count = migrateRedisKeys();
            LOG.info("Migrated " + count + " job cache keys to new format");
            CacheManager.getDistributed().put(JOB_CACHE_VERSION_KEY, JOB_CACHE_VERSION); // set the version
        }
    }

    /**
     * @return a list of JobInfo belonging to the current request owner
     */
    public static List<JobInfo> list() {
        List<JobInfo> userJobs = JobManager.getUserJobs();      // Ensure getUserJobs() is called only once, since it may be expensive. List is updated and returned at the end.
        UWS_HISTORY_SVCS.forEach(svc -> {
            Try.it(() -> importJobHistories(svc, userJobs)).getOrElse(LOG::error);
        });
        return userJobs;
    }

    public static JobInfo submit(Job job) {
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        String jobId = nextJobId();
        updateJobInfo(jobId, true, ji -> {      // setting 'true' to add this jobInfo into the datastore
            Instant start = Instant.now();
            ji.setCreationTime(start);
            ji.setDestruction(start.plus(7, ChronoUnit.DAYS));
            ji.getMeta().setType(job.getType());
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

    public static void removeJob(String jobId) {
        ifNotNull(getJobInfo(jobId)).apply(ji -> {
            allJobInfos.remove(cacheKey(ji));
            removeLocalJob(ji);
        });
    }

    static void removeLocalJob(JobInfo jobInfo) {
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

    public static JobInfo getJobInfo(String jobId) {
        String userKey = ServerContext.getRequestOwner().getUserKey();
        return getJobInfo(jobId, userKey);
    }

    /**
     * Retrieves the stored JobInfo with the given jobId and userKey.
     * Returned value is read-only.  Use updateJobInfo to update the JobInfo.
     * @param jobId the ID of the job
     * @return the JobInfo with the jobId, or null not found
     */
    public static JobInfo getJobInfo(String jobId, String userKey) {
        if (isEmpty(jobId)) return null;
        return allJobInfos.get(cacheKey(jobId, userKey));
    }

    public static JobInfo getJobInfo(CacheKey ckey) {
        if (isEmpty(ckey)) return null;
        return allJobInfos.get(ckey);
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
            initNewJob(info);
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
            if (jobInfo.getMeta().getSendNotif()) {
                Messenger.publish(new JobCompletedEvent(jobInfo));       // notify all instances this job is completed
            }
        }
    }

    public record BackGroundInfo(boolean notifEnabled, String email) implements Serializable {}

//====================================================================
//  Job statistics
//====================================================================

    /**
     * Print job statistics info
     * @return String
     */
    public static String getStatistics(boolean details) {
        StrBuilder sb = new StrBuilder();

        sb.append        ("          |   Job Count Active Count  Error Count\n");
        sb.append        ("          |------------ ------------ ------------\n");
        List<JobInfo> allJobs = getAllJobs();

        Arrays.stream(Job.Type.values()).forEach(type -> {
            long total = allJobs.stream().filter(v -> v.getMeta().getType() == type).count();
            long active = allJobs.stream().filter(v -> v.getPhase() == EXECUTING && v.getMeta().getType() == type).count();
            long error = allJobs.stream().filter(v -> v.getPhase() == ERROR && v.getMeta().getType() == type).count();

            sb.append(String.format("%9s |%,12d %,12d %,12d\n", type, total, active, error));
        });

        if (details) {
            allJobs.sort(
                    Comparator.comparing(
                            JobInfo::getCreationTime,
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    ).reversed()
            );


            sb.append("\n (ONLY THE LATEST 100 JOBS ARE SHOWN BELOW) \n");
            sb.append("JOB ID               LOCAL JOB ID         PHASE       creationTime           elapsedTime(s) progress monitored userKy                               \n");
            sb.append("-------------------- -------------------- ----------- ---------------------- -------------- -------- --------- -------------------------------------\n");
            allJobs.stream().limit(100).forEach((ji) -> {
                Instant endT = ji.getEndTime();
                Instant startT = ji.getStartTime();
                sb.append(String.format("%-20s %-20s %-11s %-22s %,14d %8d %9s %-37s\n",
                        ji.getJobId(),
                        ji.getMeta().getJobId(),
                        ji.getPhase(),
                        ji.getCreationTime() == null ? "" : ji.getCreationTime().truncatedTo(ChronoUnit.SECONDS),
                        startT == null || endT == null ? -1 : Duration.between(startT, endT).toSeconds(),
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
    /**
     * Get all job keys in the datastore.
     * @return a list of all job keys in the datastore
     */
    static List<String> getAllJobKeys() {
        return allJobInfos.getKeys().stream().map(CacheKey::getUniqueString).toList();
    }

    /**
     * Get jobs history.  This may be expensive when there are huge number of jobs, so use with caution.
     * @return a list of all JobInfo in the datastore
     */
    static List<JobInfo> getAllJobs() {
        ScanParams scanParams = new ScanParams().match("*").count(JOB_SCAN_BATCH_SIZE); // adjust count as needed
        return allJobInfos.getValuesFor(scanParams);
    }

    /**
     * Get all jobs that belong to the current user
     * @return a list of JobInfo that belong to the current user
     */
    static List<JobInfo> getUserJobs() {
        String userKey = ServerContext.getRequestOwner().getUserKey();
        ScanParams scanParams = new ScanParams().match("*:%s".formatted(userKey)).count(JOB_SCAN_BATCH_SIZE); // adjust count as needed
        return allJobInfos.getValuesFor(scanParams);
    }

//====================================================================
//  internal methods
//====================================================================

    /**
     * Setup required job meta and other information used by internal sub-systems.
     * @param ji the JobInfo to initialize
     */
    private static void initNewJob(JobInfo ji) {
        // set required job meta
        RequestOwner reqOwner = ServerContext.getRequestOwner();
        ji.getMeta().setJobId(ji.getJobId());
        ji.getMeta().setUserKey(reqOwner.getUserKey());
        ji.getMeta().setEventConnId(reqOwner.getEventConnID());
        ji.getMeta().setRunHost(hostName());
        ji.getMeta().setAppUrl(ServerContext.getRequestOwner().getBaseUrl());
        ji.getMeta().setMonitored(true);                // all async jobs are monitored by default

        // set user info
        UserInfo uInfo = reqOwner.getUserInfo();
        String email = ifNotEmpty(ji.getAux().getUserEmail()).getOrElse(uInfo.getEmail());
        String name = Stream.of(uInfo.getFirstName(), uInfo.getLastName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        ji.getAux().setUserName(name);
        ji.getAux().setUserEmail(email);
        ji.getAux().setUserId(uInfo.getLoginName());
    }

    private static CacheKey cacheKey(JobInfo jobInfo) {
        return cacheKey(jobInfo.getMeta().getJobId(), jobInfo.getMeta().getUserKey());
    }

    /**
     * Redis key is compose of jobId and userKey.  This is so that we can filter the keys for a specific user.
     * jobId should still be unique.  If jobId is empty, it will return null.
     * @param jobId
     * @param userKey
     * @return a CacheKey for the jobId and userKey, or null if jobId is empty
     */
    private static CacheKey cacheKey(String jobId, String userKey) {
        return isEmpty(jobId) ? null : new StringKey(jobId + ":" + userKey);
    }

    private static void checkJobs() {

        // ping clients with active(EXECUTING) job to keep the connection alive
        getRunningJobs().stream().filter(fi -> fi != null && fi.getPhase() == EXECUTING)     // all running jobs
                .map(fi -> fi.getMeta().getUserKey() + ":" + fi.getMeta().getEventConnId()).distinct()       // get distinct list of userKey and connId
                .forEach(client -> {                                                    // ensure it gets pinged only once, regardless of the number of jobs it has.
                    String[] ownerConnId = client.split(":");
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
                    removeLocalJob(jobInfo);
                } else {
                    updateClient(jobInfo);        // update jobInfo to client
                }
            });
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
//  inner classes
//====================================================================


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
        List<JobInfo> jobs = getAllJobs();
        jobs.forEach(job -> {
            CacheKey k = cacheKey(job);
            if (!job.getMeta().isMonitored() && job.getEndTime().plus(1, ChronoUnit.HOURS).isBefore(Instant.now())) {
                LOG.info("Removing non-monitored job: " + k);
                allJobInfos.remove(k);      // remove non-monitored job after 1 hour
            } else if (!CLEANUP_PHASES_EXCLUDES.contains(job.getPhase()) && job.getEndTime().plus(JOB_EXPIRY_HOURS, ChronoUnit.HOURS).isBefore(Instant.now())) {
                LOG.info("Removing expired job: " + k);
                allJobInfos.remove(k);
            }
        });
    }

    /**
     * This serializer is used to serialize JobInfo objects for storage in the cache.
     * Instead of using the default Java serialization, it uses a JSON string.
     */
    private static class JobInfoSerializer implements DistribMapCache.Serializer<JobInfo> {

        public String serialize(Object obj) {
            if (obj instanceof JobInfo jobInfo) {
                return toJson(jobInfo);
            }
            return null;
        }

        public JobInfo deserialize(String str) throws Exception{
            if (str != null) {
                return toJobInfo((JSONObject) new JSONParser().parse(str));
            }
            return null;
        }
    }


//====================================================================
//  Utilities: adhoc use cases
//====================================================================

    /**
     * Migrate all Redis keys that do not have userKey in the key to the new format.
     * @return the number of keys migrated
     */
    public static int migrateRedisKeys() {
        // For keys without userKey, we need to migrate them to the new format where userKey is appended to the jobId.
        int count = 0;
        for(CacheKey key : allJobInfos.getKeys()) {
            if (key instanceof StringKey sk && !sk.getUniqueString().contains(":")) { // has userKey in the key
                JobInfo jobInfo = allJobInfos.get(key);
                if (jobInfo != null) {
                    allJobInfos.put(cacheKey(jobInfo), jobInfo);
                    allJobInfos.remove(key); // remove old key
                    count++;
                }
            }
        }
        return count;
    }
}
