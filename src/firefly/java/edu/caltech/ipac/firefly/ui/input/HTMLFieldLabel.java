/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author tatianag
 *         $Id: HTMLFieldLabel.java,v 1.2 2011/03/09 18:24:39 schimms Exp $
 */

public class HTMLFieldLabel implements FieldLabel.Mutable {

    private HTML _label= new HTML();

    public HTMLFieldLabel(String text, String tip) {
        if (!StringUtils.isEmpty(text)) {
            setText(text);
        }
        setTip(tip);
    }


    public Widget getWidget() { return _label; }
    public void setText(String txt) { _label.setHTML(txt+":"); }
    public void setTip(String tip) { _label.setTitle(tip); }

    public void setVisible(boolean v) { _label.setVisible(v); }
}
