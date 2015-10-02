/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 10/2/15
 * Time: 11:29 AM
 */


/**
 * @author Trey Roby
 * Object to hold defaults for drawing a group of objects.
 */
public class DrawingDef {

    public static final String COLOR_HIGHLIGHTED_PT = "00aaff";
    public static final String COLOR_PT_1 = "ff0000"; // red
    public static final String COLOR_PT_2 = "00ff00"; //green
    public static final String COLOR_PT_3 = "pink";  // pink
    public static final String COLOR_PT_4 = "00a8ff"; //blue
    public static final String COLOR_PT_5 =  "990099"; //purple
    public static final String COLOR_PT_6 = "ff8000"; //orange

    public static final String COLOR_DRAW_1 = "ff0000";
    public static final String COLOR_DRAW_2 = "5500ff";

    private final String defColor;

    public static final String COLOR_SELECTED_PT = "ffff00";

    public DrawingDef(String defColor) {
        this.defColor= defColor;
    }

    public String getDefColor() { return defColor; }
}
