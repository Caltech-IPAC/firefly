package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.JobManager.sendUpdate;
import static edu.caltech.ipac.firefly.core.background.JobManager.updateJobInfo;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.combineErrorMsg;
import static java.util.Optional.ofNullable;

/**
 * Date: 9/29/21
 *
 * @author loi
 * @version : $
 */
public interface Job extends Callable<String> {

    enum Type {SEARCH, UWS, TAP, PACKAGE, SCRIPT}

    String getJobId();

    void setJobId(String id);

    Worker getWorker();

    void onStart(Worker worker);

    void setParams(SrvParam params);

    SrvParam getParams();

    Type getType();

    void runAs(RequestOwner ro);

    String run() throws Exception;

    default void updateManagedStatus(Consumer<JobInfo> func) {
        if (getWorker() != null && !getWorker().isSelfManaged()) {
            updateJobInfo(getJobId(), func);
        }
    }

    default String call() {
        try {
            updateJobInfo(getJobId(), ji -> {
                ji.setStartTime(Instant.now());
                ji.getMeta().setProgress(0);
            });
            String results = run();
            // the worker is set in onStart().
            getWorker().onComplete();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Job was aborted");
            updateManagedStatus(ji -> {
                ji.setPhase(JobInfo.Phase.COMPLETED);
                ji.getMeta().setProgress(100, "");
            });
            return results;
        } catch (InterruptedException | DataAccessException.Aborted e) {
            updateJobInfo(getJobId(), ji -> {
                ji.setPhase(JobInfo.Phase.ABORTED);
                ji.getMeta().setProgress(100, "Job was aborted");
            });
            getWorker().onAbort();
        } catch (Exception e) {
            updateManagedStatus(ji -> {
                String msg = combineErrorMsg(e.getMessage(), e.getCause() == null ? null : e.getCause().getMessage());
                ji.setError(new JobInfo.Error(500, msg));
            });
            Logger.getLogger().error(e);
        } finally {
            sendUpdate(getJobId(), ji -> {
                ji.setEndTime(Instant.now());
                ji.getMeta().setProgress(100);
            });
        }
        return null;
    }

    /**
     * @return the result of this job
     */
    default String getResult() {
        try {
            return String.valueOf(run());
        } catch (Exception e) {
            return null;
        }
    }


//====================================================================
//
//====================================================================

    interface Worker {
        void setJob(Job job);
        Job getJob();
        default String getLabel(){ return ""; }
        default Type getType() {return Type.SEARCH;}
        default String getSvcId() {return "IRSA";}
        default void onAbort() {}
        default void onComplete() {}

        default boolean isSelfManaged() { return false; }

        /*
         * Update and publish job status only if the worker is running as a job(async).
         */
        default void sendJobUpdate(Consumer<JobInfo> func) {
            ifNotNull(getJob()).apply(j -> {
                sendUpdate(j.getJobId(), func);
            });
        }

        /* update only, no publish */
        default void updateJob(Consumer<JobInfo> func) {
            ifNotNull(getJob()).apply(j -> {
                updateJobInfo(j.getJobId(), func);
            });
        }

    }

}
