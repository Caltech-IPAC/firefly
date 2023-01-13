/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.api;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.core.background.JobManager;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static edu.caltech.ipac.firefly.data.ServerParams.JOB_ID;
import static edu.caltech.ipac.util.StringUtils.*;
import static edu.caltech.ipac.firefly.core.background.JobManager.toJson;

/**
 * Follows UWS pattern for async job processing.
 *
 * /CmdSrv/async                        # no parameter to list all jobs for current user. {jobs: [{job-url}]}
 * /CmdSrv/async?cmd=xxx                # submit job and then redirect to /CmdSrv/async/{id} to return JobInfo
 * /CmdSrv/async/{id}                   # return JobInfo for the given id
 * /CmdSrv/async/{id}?cmd=xxx           # run the command associated with this job, i.e. setEmail, resendEmail, etc.
 * /CmdSrv/async/{id}/phase?PHASE=ABORT # abort this job.  RUN is not needed because we run it immediately upon submit
 * /CmdSrv/async/{id}/results           # return an array of url(s)
 * /CmdSrv/async/{id}/results/result    # return table result from a SEARCH job
 *
 * @author loi
 * @version : $
 */
public class Async extends BaseHttpServlet {

    static private final Logger.LoggerImpl logger = Logger.getLogger();


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        String[] path = isEmpty(req.getPathInfo()) ? new String[0] : req.getPathInfo().substring(1).split("/");
        String jobId = path.length > 0 ? path[0] : null;
        String action = path.length > 1 ? path[1] : null;
        String result = path.length > 2 ? path[2] : null;
        SrvParam params = new SrvParam(req.getParameterMap());

        try {
            if (jobId == null) {
                if (params.size() == 0) {
                    // list all jobs for current user;          /CmdSrv/async
                    listUserJob(res);
                } else {
                    // submit job with given cmd parameters;    /CmdSrv/async?cmd=xxx
                    submitJob(res, params);
                }
            } else {
                if (action == null) {
                    if (params.size() == 0) {
                        // get JobInfo;                         /CmdSrv/async/{id}
                        getJobInfo(res, jobId);
                    } else if (params.getCommandKey() != null){
                        // run a command on this job;           /CmdSrv/async/{id}?cmd=xxx
                        updateJob(res, params, jobId);
                    }
                } else {
                    if (action.equals("phase")) {
                        // update job's phase;                  /CmdSrv/async/{id}/phase?PHASE=ABORT
                        updateJobPhase(req, res, jobId);
                    } else if (action.equals("results") && result == null) {
                        // list job's results                   /CmdSrv/async/{id}/results
                        listResults(res, jobId);
                    } else if (action.equals("results") && result.equals("result")) {
                        // return CMD results                   /CmdSrv/async/{id}/results/result
                        getCmdResult(res, jobId);
                    } else {
                        sendErrorResponse(404, new JobInfo(jobId), "path not found", res);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e);
            sendErrorResponse(500, JobManager.getJobInfo(jobId), e.getMessage(), res);
        }
    }

//====================================================================
//
//====================================================================

    private static void listUserJob(HttpServletResponse res) throws Exception {
        // list all jobs for current user; /CmdSrv/async
        List<JobInfo> list = JobManager.list();
        sendResponse(JobManager.toJsonJobList(list), res);
    }

    private static void submitJob(HttpServletResponse res, SrvParam params) throws Exception {
        Job job = ServerCommandAccess.getCmdJob(params);
        if (job != null) {
            JobInfo info = JobManager.submit(job);
            res.setHeader("Location", getAsyncUrl() + info.getJobId());
            res.setStatus(301);
        } else {
            sendErrorResponse(404, null, "Command not found: " + params.getCommandKey(), res);
        }
    }

    private static void getJobInfo(HttpServletResponse res, String jobId) throws Exception {
        sendResponse(toJson(JobManager.getJobInfo(jobId)), res);
    }

    private static void updateJob(HttpServletResponse res, SrvParam params, String jobId) throws Exception {
        params.setParam(JOB_ID, jobId);
        Job job = ServerCommandAccess.getCmdJob(params);
        if (job != null) {
            String json = job.run();
            sendResponse(json, res);
        } else {
            sendErrorResponse(404, new JobInfo(jobId), "Job not found: " + jobId, res);
        }
    }

    private static void updateJobPhase(HttpServletRequest req, HttpServletResponse res, String jobId) throws Exception {
        String phase = req.getParameter("PHASE");
        if (String.valueOf(phase).equals("ABORT")) {
            JobInfo fi = JobManager.abort(jobId, "Abort by user");
            sendResponse(toJson(fi), res);
        }
    }

    private static void listResults(HttpServletResponse res, String jobId) throws Exception {
        String json = JobManager.results(jobId);
        sendResponse(json, res);
    }

    private static void getCmdResult(HttpServletResponse res, String jobId) throws Exception {
        JobInfo info = JobManager.getJobInfo(jobId);
        SrvParam params = info.getSrvParams();
        Job job = ServerCommandAccess.getCmdJob(params);
        if (job != null && job.getType() == Job.Type.SEARCH) {
            job.setJobId(info.getJobId());
            String json = job.run();
            sendResponse(json, res);
        }
    }

    private static void sendErrorResponse(int code, JobInfo info, String message, HttpServletResponse res)  throws Exception {
        if (info == null) {
            info = new JobInfo("NULL");
        }
        res.setStatus(code);
        info.setError(new JobInfo.Error(code, message));
        sendResponse(toJson(info), res);
    }

    private static void sendResponse(String json, HttpServletResponse res) throws Exception {
        json = json + "\n";
        res.setContentType("application/json");
        res.setContentLength(json.length());
        ServletOutputStream out = res.getOutputStream();
        out.write(json.getBytes());
        out.close();
    }

    static public String getAsyncUrl() {
        RequestOwner ro = ServerContext.getRequestOwner();
        String spath = ro.getRequestAgent().getServletPath();     // from config =>  /CmdSrv/async
        spath = spath == null ? "" : spath.substring(1);
        return ro.getBaseUrl() + spath + "/";
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
