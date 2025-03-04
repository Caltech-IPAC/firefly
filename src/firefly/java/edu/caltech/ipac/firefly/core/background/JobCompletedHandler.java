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
        JobCompletedEvent ev = JobCompletedEvent.fromMsg(msg);
        if (ev == null) return;

        JobInfo jobInfo = JobUtil.fromMsg(ev);
        String host = jobInfo.getAuxData().getRefHost();
        if (host != null && host.equals(JobUtil.hostName())) {
            processEvent(ev, jobInfo);
        }
    }
}
