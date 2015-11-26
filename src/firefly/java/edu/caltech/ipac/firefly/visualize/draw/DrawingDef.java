/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 10/2/15
 * Time: 11:29 AM
 */


import edu.caltech.ipac.util.ComparisonUtil;

/**
 * @author Trey Roby
 * Object to hold defaults for drawing a group of objects.
 */
public class DrawingDef {

	// FIXME: those are not DS9 colors, hence problem when saved in DS9 regions format file
    public static final String COLOR_HIGHLIGHTED_PT = "00aaff";
    public static final String COLOR_PT_1 = "ff0000"; // red
    public static final String COLOR_PT_2 = "00ff00"; //green
    public static final String COLOR_PT_3 = "pink";  // pink
    public static final String COLOR_PT_4 = "00a8ff"; //blue
    public static final String COLOR_PT_5 =  "990099"; //purple
    public static final String COLOR_PT_6 = "ff8000"; //orange

    public static final String COLOR_DRAW_1 = "ff0000";
    public static final String COLOR_DRAW_2 = "5500ff";

    private String defColor;

    public static final String COLOR_SELECTED_PT = "ffff00";

    public DrawingDef(String defColor) {
        this.defColor= defColor;
    }

    public void setDefColor(String color) { this.defColor= color; }

    public String getDefColor() { return defColor; }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o!=null && o instanceof DrawingDef) {
            DrawingDef d= (DrawingDef)o;
            retval= ComparisonUtil.equals(d.getDefColor(),getDefColor());
        }
        return retval;
    }

    public DrawingDef makeCopy() {
        DrawingDef def= new DrawingDef(this.defColor);
        // in future, set other stuff here, line width, rotation, offset, etc
        return def;
    }
}
