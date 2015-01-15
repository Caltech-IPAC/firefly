/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 9/26/12
 * Time: 4:15 PM
 */


import edu.caltech.ipac.firefly.commands.LayerCmd;
import edu.caltech.ipac.firefly.visualize.AllPlots;

/**
 * @author Trey Roby
 */
@Deprecated
public class AlertLayerPopup {

    public static final int MAX_ALERT_CNT=2;
    public static int _turnOnCount = 0;
    public static int _attentionChanges= 0;
    public static boolean _alertOn;
    public static boolean _dialogVisible= false;

    public static void setAlert(boolean alert) {
        if (!_dialogVisible) {
            if (alert) _turnOnCount++;
            else if (_turnOnCount >0)      _turnOnCount--;
            boolean turnAlertOn= _turnOnCount >0;
            if (!_alertOn && turnAlertOn) {
                _attentionChanges++;
            }
            getLayerCmd().setAttention(_attentionChanges<=MAX_ALERT_CNT && turnAlertOn);
            _alertOn= turnAlertOn;
        }
    }

    public static void setLayerDialogVisibleStatus(boolean v) {
        _dialogVisible= v;
        AlertLayerPopup.killAlert();
    }

    private static void killAlert() {
        _turnOnCount = 0;
        _alertOn= false;
        getLayerCmd().setAttention(false);
    }

    private static LayerCmd getLayerCmd() {
        return (LayerCmd) AllPlots.getInstance().getCommand(LayerCmd.CommandName);
    }


}

