/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;

/**
 * This is the default layout of a GWT application.  This class should be overriden if this layout
 * does not fit the requirement.
 *
 * This manager uses a DockPanel as its main panel.
 * The top panel contains the menu toolbar.
 * The center panel is hidden behind a ScrollPanel.
 *
 * Date: Nov 1, 2007
 *
 * @author Trey
 * @version $Id: MaskMessgeWidget.java,v 1.4 2010/11/09 23:13:00 roby Exp $
 */
public class MaskMessgeWidget extends Composite {

    private static final String DEF_MESSAGE= "Loading...";
    private HTML status= new HTML(DEF_MESSAGE);
    private AbsolutePanel statusHolder;
    private Panel hpanel= new HorizontalPanel();

    public MaskMessgeWidget(boolean asMask) {
       this(null, asMask);
    }

    public MaskMessgeWidget(String text, boolean asMask) {
        makeContent(asMask);
        setHTML(text);
        initWidget(statusHolder);
    }

    public void setHTML(String text) {
        if (text==null) text= DEF_MESSAGE;
        status.setHTML(text);
    }

//====================================================================

    private void makeContent(boolean over) {
        status.setText(DEF_MESSAGE);
        status.setStyleName("firefly-mask-msg");
        status.addStyleName("normal-text");
        status.addStyleName("firefly-mask-msg-alone");
        hpanel.add(status);


        statusHolder = new AbsolutePanel();
        statusHolder.add(hpanel);
        statusHolder.setStyleName("firefly-mask");

        if (over) statusHolder.addStyleName("firefly-mask-overeverything");
        statusHolder.addStyleName("standard-border");
    }
}
