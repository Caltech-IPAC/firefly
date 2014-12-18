package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

/**
 * User: roby
 * Date: Feb 24, 2009
 * Time: 1:27:27 PM
 */


/**
 * @author Trey Roby
 */
public class GWTCanvasPanel extends Composite {

    private final GWTCanvas _gwtSurface= new GWTCanvas();
    private final AbsolutePanel _panel= new AbsolutePanel();

//======================================================================
//----------------------- Constructors ---------------------------------
//====================================================================

    public GWTCanvasPanel() {
        _panel.add(_gwtSurface,0,0);
        initWidget(_panel);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public GWTCanvas getCanvas() { return _gwtSurface; }

    public void addLabel(Label label,int x, int y) { _panel.add(label,x,y); }

    public void removeLabel(Label label) { _panel.remove(label); }

    @Override
    public void setPixelSize(int width, int height) {
        super.setPixelSize(width, height);
        _gwtSurface.setPixelSize(width,height);
    }
}

