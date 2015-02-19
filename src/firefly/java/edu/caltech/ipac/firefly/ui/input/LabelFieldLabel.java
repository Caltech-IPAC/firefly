/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Trey Roby
 * $Id: LabelFieldLabel.java,v 1.2 2010/10/13 20:39:56 roby Exp $
 */
public class LabelFieldLabel implements FieldLabel.Mutable {
        private Label _label= new Label();
        public Widget getWidget() { return _label; }
        public void setText(String txt) { _label.setText(txt+":"); }
        public void setTip(String tip) { _label.setTitle(tip); }
        public void setVisible(boolean v) { _label.setVisible(v); }
}
