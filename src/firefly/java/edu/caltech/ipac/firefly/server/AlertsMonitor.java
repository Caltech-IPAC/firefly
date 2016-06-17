package edu.caltech.ipac.firefly.server;

import com.google.gwt.media.dom.client.TimeRanges;
import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Date: 6/16/16
 *
 * @author loi
 * @version $Id: $
 */
public class AlertsMonitor {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String ALERTS_DIR = AppProperties.getProperty("alerts.dir", "/hydra/alerts/");
    private static final File alertDir = new File(ALERTS_DIR);
    private static Alert alerts = new Alert(null, "", true);;
    private static Timer timer;

    /**
     * check for alerts
     * @param forceSend  if true, send alerts regardless
     */
    public static void checkAlerts(boolean forceSend) {
        long lastMod = alerts == null ? 0 : alerts.getLastModDate();
        if (alertDir.lastModified() > lastMod) {
            File[] files = alertDir.listFiles(file -> file.isFile() && !file.isHidden());
            alerts = new Alert(null, "", true);
            if (files != null && files.length > 0) {
                // found alerts
                alerts.setLastModDate(alertDir.lastModified());
                for (File f : files) {
                    try {
                        if (!StringUtils.isEmpty(alerts.getMsg())) {
                            alerts.setMsg(alerts.getMsg() + " <br> ");
                        }
                        alerts.setMsg(alerts.getMsg() + FileUtil.readFile(f));
                    } catch (IOException e) {
                        LOG.error(e, "Error reading alerts.");
                    }
                }
            }
            sendAlerts(alerts);
        } else if (alerts != null && forceSend) {
            sendAlerts(alerts);
        }
    }

    public static void startMonitor() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer("AlertsMonitor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                checkAlerts(false);
            }
        }, 0, 1000 * 10);  // check for update every 10 seconds
    }

    private static void sendAlerts(Alert alerts) {
        FluxAction action = new FluxAction("app_data.setAlerts");       // AppDataCntlr.SET_ALERTS
        action.setValue(alerts.getMsg(), "msg");
        ServerEventManager.fireAction(action, ServerEvent.Scope.WORLD);
    }

    public static void main(String[] args) {
        AlertsMonitor.startMonitor();
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
