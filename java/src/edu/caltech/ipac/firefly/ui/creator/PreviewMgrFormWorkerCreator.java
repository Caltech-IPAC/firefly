/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorkerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.PreviewMgrFormWorker;

import java.util.Map;


public class PreviewMgrFormWorkerCreator implements FormEventWorkerCreator {
    public FormEventWorker create(Map<String, String> params) {
        return new PreviewMgrFormWorker();
    }
}

