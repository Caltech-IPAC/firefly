package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

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
 * @author loi
 * @version $Id: DefaultWorkingWidget.java,v 1.12 2010/09/27 22:23:44 loi Exp $
 */
public class DefaultWorkingWidget extends Composite {

    private static final String DEF_MESSAGE= "Loading...";
    private Label status= new Label(DEF_MESSAGE);
    private AbsolutePanel statusHolder;
    private Image icon = new Image(GwtUtil.LOADING_ICON_URL);
    private Panel hpanel= new HorizontalPanel();

//    public DefaultWorkingWidget() {
//       this((ButtonInfo)null);
//    }
//
    public DefaultWorkingWidget(ClickHandler cancelHandler) {
        this(cancelHandler == null ? null : new CancelButton(cancelHandler));
    }

    public DefaultWorkingWidget(ButtonInfo... buttons) {
        makeContent(buttons);
        initWidget(statusHolder);
    }

    public void setText(String text) {
        if (text==null) text= DEF_MESSAGE;
        status.setText(text);
    }



//====================================================================

    private void makeContent(ButtonInfo... buttons) {
        Label sep = new Label();
        sep.setWidth("1px");
        status.setText(DEF_MESSAGE);
        status.setStyleName("firefly-mask-msg");
        status.addStyleName("normal-text");
        status.addStyleName("firefly-mask-msg-working");
        icon.setStyleName("firefly-mask-icon");

        hpanel.add(icon);
        hpanel.add(sep);

        if (buttons!=null) {
            icon.setStyleName("firefly-mask-icon-pad-with-cancel");
            Panel vpanel= new VerticalPanel();
            vpanel.add(status);
            vpanel.add(GwtUtil.getFiller(0, 5));

            HorizontalPanel bpanel = new HorizontalPanel();
            vpanel.add(bpanel);
            for (ButtonInfo bi : buttons) {
                if (bi != null) {
                    Button b = GwtUtil.makeButton(bi.getLabel(), bi.getDesc(), bi.getClickHandler());
                    bi.setButton(b);
                    SimplePanel bholder= new SimplePanel();
                    bholder.setWidget(b);
                    bholder.setStyleName("firefly-mask-cancel");
                    bpanel.add(GwtUtil.getFiller(5, 0));
                    bpanel.add(bholder);
                }
            }
            hpanel.add(vpanel);
        }
        else {
//            cancel= GwtUtil.makeButton("Cancel", "Click to Cancel", null);
            icon.setStyleName("firefly-mask-icon-pad-no-cancel");
            hpanel.add(status);
        }


        statusHolder = new AbsolutePanel();
        statusHolder.add(hpanel);
        statusHolder.setStyleName("firefly-mask");
        statusHolder.addStyleName("firefly-mask-overeverything");
        statusHolder.addStyleName("standard-border");
    }

    public static class CancelButton extends ButtonInfo {
        public CancelButton(ClickHandler clickHandler) {
            super(clickHandler, "Cancel", "Click to Cancel");
        }
    }

    public static class ButtonInfo {
        ClickHandler clickHandler;
        String label;
        String desc;
        Button button;

        public ButtonInfo(ClickHandler clickHandler, String label, String desc) {
            this.clickHandler = clickHandler;
            this.label = label;
            this.desc = desc;
        }

        public ClickHandler getClickHandler() {
            return clickHandler;
        }

        public String getLabel() {
            return label;
        }

        public String getDesc() {
            return desc;
        }

        public Button getButton() {
            return button;
        }

        void setButton(Button button) {
            this.button = button;
        }
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
