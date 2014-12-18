package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.Widget;

/**
 * User: roby
 * Date: Oct 13, 2010
 * Time: 12:21:04 PM
 */
public interface InputFieldContainer {
    void setVisible(boolean visible);
    void clearLabel(InputField f);
    void addLabel(InputField f);
    Widget getWidget();
}

