/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.vispush;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 1:04 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class VisPushJob {

    private static final Map<String,VisPushJob> jobs= new HashMap<String, VisPushJob>(23);

    public static VisPushJob getJob(String id) { return jobs.get(id); }

    public static String makeNewJob(String bid) {
        PushWorker worker= new PushWorker();
        BackgroundEnv.BackgroundProcessor processor=
                new BackgroundEnv.BackgroundProcessor(worker,
                                                      "Push Job",
                                                      "from push",
                                                      ServerContext.getRequestOwner(),bid );
        return BackgroundEnv.backgroundProcess(1, processor).getID();
    }

    public static boolean pushFits(String id, WebPlotRequest wpr) {
        BackgroundInfoCacher pi= new BackgroundInfoCacher(id);
        BackgroundStatus bgStat= pi.getStatus();
        boolean success= false;
        if (bgStat!=null) {
            bgStat.addPushData(wpr.toString(), BackgroundStatus.PushType.WEB_PLOT_REQUEST);
            pi.setStatus(bgStat);
            success= true;
        }
        return success;
    }

    public static boolean pushRegion(String id, String fileName) {
        BackgroundInfoCacher pi= new BackgroundInfoCacher(id);
        BackgroundStatus bgStat= pi.getStatus();
        boolean success= false;
        if (bgStat!=null) {
            bgStat.addPushData(fileName, BackgroundStatus.PushType.REGION_FILE_NAME);
            pi.setStatus(bgStat);
            success= true;
        }
        return success;
    }

    private static class PushWorker implements BackgroundEnv.Worker {

        public PushWorker() {
        }

        public BackgroundStatus work(BackgroundEnv.BackgroundProcessor p)  throws Exception {
            BackgroundStatus bgStat= new BackgroundStatus(p.getBID(), BackgroundStatus.BgType.PERSISTENT,
                                                          BackgroundState.WAITING);
            return bgStat;
        }
    }
}
