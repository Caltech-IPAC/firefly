/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;
/**
 * User: roby
 * Date: 12/18/12
 * Time: 10:13 AM
 */


import edu.caltech.ipac.firefly.visualize.WebPlot;

/**
 * @author Trey Roby
 */
public class TaskUtils {

    public static void copyImportantAttributes(WebPlot oldPlot, WebPlot newPlot) {
        Object o;
        if (oldPlot.containsAttributeKey(WebPlot.FIXED_TARGET)) {
            o= oldPlot.getAttribute(WebPlot.FIXED_TARGET);
            newPlot.setAttribute(WebPlot.FIXED_TARGET, o);
        }
        if (oldPlot.containsAttributeKey(WebPlot.MOVING_TARGET_CTX_ATTR)) {
            o= oldPlot.getAttribute(WebPlot.MOVING_TARGET_CTX_ATTR);
            newPlot.setAttribute(WebPlot.MOVING_TARGET_CTX_ATTR, o);
        }
        if (oldPlot.containsAttributeKey(WebPlot.UNIQUE_KEY)) {
            o= oldPlot.getAttribute(WebPlot.UNIQUE_KEY);
            newPlot.setAttribute(WebPlot.UNIQUE_KEY, o);
        }
    }


}

