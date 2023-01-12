/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;

/**
 * A base class which extends ServCommand function into a Job/Worker async processing
 *
 * @author loi
 * @version : $
 */
public abstract class ServCmdJob extends ServCommand implements Job {

    private String jobId;           // jobId may be null when it's not executing as a Job.
    private Worker worker;
    private SrvParam params;
    private RequestOwner reqOwner;

    public void setParams(SrvParam params) {
        this.params = params;
    }

    /**
     * Implements Job's run function.  This is only called when this command is executed as a Job.
     * @return the results of executing this command which is json string.
     * @throws Exception
     */
    public String run() throws Exception {
        if (reqOwner != null) {
            ServerContext.getRequestOwner().setTo(reqOwner);
        } else {
            ServerContext.clearRequestOwner();
        }
        return doCommand(params);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
        getJobInfo().setParams(params.flatten());
    }

    public Worker getWorker() {
        return worker;
    }

    public SrvParam getParams() {
        return params;
    }

    public void setWorker(Worker worker) {
        if (jobId != null) {
            this.worker = worker;
            worker.setJob(this);
            JobInfo info = getJobInfo();
            info.setType(worker.getType());
            info.setLabel(worker.getLabel());
        }
    }

    public void runAs(RequestOwner reqOwner) {
        try {
            this.reqOwner = (RequestOwner) reqOwner.clone();
        } catch (CloneNotSupportedException e) {
            // ignore.. should not happen
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
