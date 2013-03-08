package edu.caltech.ipac.firefly.ui.panels;
/**
 * User: roby
 * Date: 9/16/11
 * Time: 11:15 AM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;


/**
 * @author Trey Roby
 */
public class BackButton extends Composite {

    private HorizontalPanel _hp= new HorizontalPanel();
    private HTML buttonText= new HTML("Back");

    public BackButton(String desc) {

        initWidget(_hp);


        buttonText.setText(desc);
        IconCreator ic= IconCreator.Creator.getInstance();

        Image start= new Image(ic.getBackButtonStart());
        Image end= new Image(ic.getBackButtonEnd());
//        AbsolutePanel middlePanel= new AbsolutePanel();
        buttonText.setStyleName("title-color");
        GwtUtil.setStyles(buttonText, "background", "url(" + ic.getBackButtonMiddle().getURL() + ") top left repeat-x",
                                     "lineHeight", "27px",
                                     "fontSize", "11pt");

//        middlePanel.add(buttonText, 2, 8);

        _hp.add(start);
        _hp.add(buttonText);
        _hp.add(end);

//        _hp.setHeight("35px");
        buttonText.setHeight("30px");
        GwtUtil.setStyle(_hp, "cursor", "pointer");


    }

    public void setDesc(String desc) {
        buttonText.setHTML("<span style=\"white-space:nowrap;\" >"+desc+"</span>");
    }
    public void setHTML(String desc) {
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return _hp.addDomHandler(handler, ClickEvent.getType());
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
 a*/
