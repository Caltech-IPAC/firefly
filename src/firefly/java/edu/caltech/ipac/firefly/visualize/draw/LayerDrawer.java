/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 8/26/15
 * Time: 2:46 PM
 */


/**
 * @author Trey Roby
 */
public interface LayerDrawer  {

    void setVisible(boolean v);
    boolean isVisible();
    boolean hasData();
    DrawingDef getDrawingDef();
    void setDrawingDef(DrawingDef def);
    boolean getSupportsRegions();
    boolean isImageOverlay();


}
