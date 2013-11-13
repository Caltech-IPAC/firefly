package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 11/13/13
 * Time: 10:05 AM
 */


import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class LinkButtonFactory {

    private final String linkButtonStyle;
    private final String mouseOverStyle;
    private final String mouseOffStyle;

    public LinkButtonFactory(String linkButtonStyle, String mouseOverStyle, String mouseOffStyle) {
        this.linkButtonStyle= linkButtonStyle;
        this.mouseOverStyle= mouseOverStyle;
        this.mouseOffStyle= mouseOffStyle;
    }

    public Widget makeLinkButton(String prop, ClickHandler handler) {
        String name = WebProp.getName(prop);
        String tip = WebProp.getTip(prop);
        return makeLinkButton(name, tip, handler);
    }

    public Label makeLinkButton(String text,
                                       String tip,
                                       ClickHandler handler) {
        final Label link = new Label(text);
        link.setTitle(tip);
        if (handler!=null) link.addClickHandler(handler);
        makeIntoLinkButton(link);
        return link;

    }


    public Widget makeLinkIcon(String iconUrl, String text, String tip, ClickHandler handler) {
        HorizontalPanel hp = new HorizontalPanel();
        Image image = new Image(iconUrl);
        image.setHeight("16px");
        makeIntoLinkButton(image);
        hp.add(image);
        if (!StringUtils.isEmpty(text)) {
            Label label = new Label(text);
            if (tip != null) {
                label.setTitle(tip);
            }
            label.addClickHandler(handler);
            makeIntoLinkButton(label);
            hp.add(GwtUtil.getFiller(3, 1));
            hp.add(label);
        }
        if (tip != null) {
            image.setTitle(tip);
        }
        image.addClickHandler(handler);
        return hp;
    }

    public void makeIntoLinkButton(final Widget... link ) {
        makeIntoLinkButton(linkButtonStyle,mouseOverStyle, mouseOffStyle, link);
    }

    public void makeIntoLinkButton(final String linkButtonStyle,
                                   final String mouseOverStyle,
                                   final String mouseOffStyle,
                                   final Widget... link ) {

        MouseOverHandler mOver= new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                for(Widget w : link) {
                    if (mouseOffStyle!=null) w.removeStyleName(mouseOffStyle);
                    if (mouseOverStyle!=null) w.addStyleName(mouseOverStyle);
                }
            }
        };

        MouseOutHandler mOut= new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                for(Widget w : link) {
                    if (mouseOverStyle!=null) w.removeStyleName(mouseOverStyle);
                    if (mouseOffStyle!=null) w.addStyleName(mouseOffStyle);
                }
            }
        };


        for(Widget w : link) {
            if (linkButtonStyle!=null) w.addStyleName(linkButtonStyle);
            if (mouseOffStyle!=null) w.addStyleName(mouseOffStyle);

            if (w instanceof HasAllMouseHandlers) {
                HasAllMouseHandlers ol = (HasAllMouseHandlers) w;
                ol.addMouseOverHandler(mOver);
                ol.addMouseOutHandler(mOut);
            }
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
 */
