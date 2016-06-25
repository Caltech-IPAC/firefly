package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.Alert;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;

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
    private static Alert alerts = null;
    private static Timer timer;

    /**
     * check for alerts
     * @param forceSend  if true, send alerts regardless
     */
    public static void checkAlerts(boolean forceSend) {
        long lastMod = alerts == null ? 0 : alerts.getLastModDate();
        File[] files = alertDir.listFiles(file -> file.isFile() && !file.isHidden());
        long mod = Math.max(lastMod, alertDir.lastModified());
        String msg = "";
        if (files != null && files.length > 0) {
            // found alerts
            for (File f : files) {
                mod = Math.max(mod, f.lastModified());
                try {
                    if (msg.length() > 0) {
                        msg += " <br> ";
                    }
                    msg += FileUtil.readFile(f);
                } catch (IOException e) {
                    LOG.error(e, "Error reading alerts.");
                }
            }
        }
        if (mod > lastMod ) {
            alerts = new Alert(msg, mod);
            sendAlerts(alerts);
        } else  if (forceSend && alerts != null) {
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
