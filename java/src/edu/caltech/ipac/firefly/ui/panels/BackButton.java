/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
        buttonText.setHTML("<span style=\"white-space:nowrap; line-height: 27px; font-size: 11Pt;\" >"+desc+"</span>");
    }
    public void setHTML(String desc) {
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return _hp.addDomHandler(handler, ClickEvent.getType());
    }

}

