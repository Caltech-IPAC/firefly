package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;


/**
 * User: roby
 * Date: Dec 15, 2008
 * Time: 1:15:52 PM
 */
public class GWTLabelShape extends Shape<GWTCanvas> {

    private Label _label;

    public GWTLabelShape(Label label) {
        _label= label;
    }

    public void draw(GWTCanvas surfaceWidget) {

    }

    public Label getLabel() { return _label; }

}