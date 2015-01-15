/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.util.WebAssert;


/**
 * User: roby
 * Date: Feb 24, 2009
 * Time: 1:27:27 PM
 */


/**
 * @author Trey Roby
 */
public class CanvasPanel extends Composite {

    private final Canvas canvas;
    private final AbsolutePanel panel = new AbsolutePanel();

//======================================================================
//----------------------- Constructors ---------------------------------
//====================================================================

    public CanvasPanel() {
        canvas= Canvas.createIfSupported();
        WebAssert.argTst(canvas != null, "Canvas is not supported with this browser");
        panel.add(canvas,0,0);
        initWidget(panel);
    }

    public static boolean isSupported() { return Canvas.isSupported();  }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public Canvas getCanvas() { return canvas; }

    public void addLabel(Label label,int x, int y) { panel.add(label,x,y); }

    public void removeLabel(Label label) { panel.remove(label); }

    @Override
    public void setPixelSize(int width, int height) {
        super.setPixelSize(width, height);
        canvas.setCoordinateSpaceWidth(width);
        canvas.setCoordinateSpaceHeight(height);
    }
}

