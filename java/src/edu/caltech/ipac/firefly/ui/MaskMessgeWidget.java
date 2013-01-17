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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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