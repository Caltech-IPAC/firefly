/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Label;


/**
 * User: roby
 * Date: Dec 15, 2008
 * Time: 1:15:52 PM
 */
public class CanvasLabelShape {

    private Label _label;

    public CanvasLabelShape(Label label) {
        _label= label;
    }

    public Label getLabel() { return _label; }

}