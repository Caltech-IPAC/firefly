/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.core.background.JobManager.JobCompletedEvent;
import edu.caltech.ipac.firefly.messaging.Message;
import edu.caltech.ipac.firefly.messaging.Subscriber;

/**
 * Date: 2/19/25
 *
 * @author loi
 * @version : $
 */
public interface JobCompletedHandler extends Subscriber {

    void processEvent(JobCompletedEvent ev, JobInfo jobInfo);

    @Override
    default void onMessage(Message msg) {
        if (!JobCompletedEvent.isJobCompletedEvent(msg)) return;

        JobInfo jobInfo = JobManager.JobEvent.getJobInfo(msg);
        String host = jobInfo.getMeta().getRunHost();
        if (host != null && host.equals(JobUtil.hostName())) {
            processEvent(new JobCompletedEvent(jobInfo), jobInfo);
        }
    }
}
