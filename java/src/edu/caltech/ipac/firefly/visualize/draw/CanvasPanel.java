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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
