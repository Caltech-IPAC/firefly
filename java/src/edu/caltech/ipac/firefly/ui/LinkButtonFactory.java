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

