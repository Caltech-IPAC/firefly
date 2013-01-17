package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.user.client.ui.Label;


/**
 * User: roby
 * Date: Dec 15, 2008
 * Time: 1:15:52 PM
 */
public class CanvasLabelShape extends Shape<Context2d> {

    private Label _label;

    public CanvasLabelShape(Label label) {
        _label= label;
    }

    public void draw(Context2d ctx) {  }

    public Label getLabel() { return _label; }

}