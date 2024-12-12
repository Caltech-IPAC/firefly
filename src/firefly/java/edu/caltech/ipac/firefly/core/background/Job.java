package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static edu.caltech.ipac.firefly.core.background.JobInfo.Phase.ABORTED;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.combineErrorMsg;

/**
 * Date: 9/29/21
 *
 * @author loi
 * @version : $
 */
public interface Job extends Callable<String> {

    enum Type {SEARCH, UWS, PACKAGE}

    String getJobId();

    void setJobId(String id);

    Worker getWorker();

    void setWorker(Worker worker);

    void setParams(SrvParam params);

    SrvParam getParams();

    Type getType();

    void runAs(RequestOwner ro);

    String run() throws Exception;

    default JobInfo getJobInfo() {return JobManager.getJobInfo(getJobId()); }

    default void setPhase(JobInfo.Phase phase) { setIf(getJobInfo(), v -> v.setPhase(phase)); }

    default void setError(int code, String msg) {
        setIf(getJobInfo(), v -> v.setError(new JobInfo.Error(code, msg)));
    }

    /**
     * Update the progress of the Job.
     * @param desc
     */
    default void progressDesc(String desc) {
        setIf(getJobInfo(), v -> v.setProgressDesc(desc));
    }

    default void progress(int progress) { setIf(getJobInfo(), v -> v.setProgress(progress)); }

    default void progress(int progress, String desc) {
        setIf(getJobInfo(), v -> {
            v.setProgress(progress);
            v.setProgressDesc(desc);
        });
    }

    default void addResult(JobInfo.Result result) {
        setIf(getJobInfo(), v -> v.addResult(result));
    }

    default String call() {

        setPhase(JobInfo.Phase.EXECUTING);
        JobManager.logJobInfo(getJobInfo());

        try {
            String results = run();
            setPhase(JobInfo.Phase.COMPLETED);
            JobManager.logJobInfo(getJobInfo());
            return results;
        } catch (InterruptedException | DataAccessException.Aborted e) {
            setPhase(ABORTED);
            JobManager.logJobInfo(getJobInfo());
        } catch (Exception e) {
            String msg = combineErrorMsg(e.getMessage(), e.getCause() == null ? null : e.getCause().getMessage());
            setError(500, msg);
            JobManager.logJobInfo(getJobInfo());
            Logger.getLogger().error(e);
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
        default void onAbort() {}
        default void onComplete() {}
    }

//====================================================================
//
//====================================================================

    /**
     * If JobInfo is not null, apply the update, then immediately send
     * update notifications
     * @param f
     */
    static void setIf(JobInfo info, Consumer<JobInfo> f){
        if (info != null) {
            f.accept(info);
            JobManager.sendUpdate(info);
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
