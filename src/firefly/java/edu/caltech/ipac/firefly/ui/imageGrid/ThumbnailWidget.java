/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Aug 9, 2010
 * Time: 6:27:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThumbnailWidget extends VerticalPanel implements HasClickHandlers,
    HasDoubleClickHandlers, HasMouseOutHandlers, HasMouseOverHandlers {
    private static final String FULL_SIZE = "100%";
    private static final String DEFAULT_WIDTH = "134px";
    private static final String DEFAULT_HEIGHT = "154px";
    private HTML label = new HTML();
    private String backgroundColor = "#ddd";
    private boolean requiresResize = false;
    private int itemsPerRow = 0;
    public ThumbnailWidget(String title, String url) {
        this(title, url, "#ddd");
    }

    public ThumbnailWidget(String title, String url, String background) {
        this(title, url, background, false, 0);
    }
    public ThumbnailWidget(String title, String url, String background, boolean requiresResize, int itemsPerRow) {
        super();
        this.requiresResize = requiresResize;
        this.itemsPerRow = itemsPerRow;
        Image image = new Image();
        if (requiresResize) {
            this.setCellHeight(image, FULL_SIZE);
            this.setCellWidth(image, FULL_SIZE);
            GwtUtil.setStyle(image, "maxWidth",FULL_SIZE);
            GwtUtil.setStyle(image, "maxHeight",FULL_SIZE);
        } else {
            this.setCellHeight(image, "122px");
            this.setCellWidth(image, "122px");
            GwtUtil.setStyle(image, "maxWidth","122px");
            GwtUtil.setStyle(image, "maxHeight","122px");
        }
        this.setHorizontalAlignment(ALIGN_CENTER);
        this.setVerticalAlignment(ALIGN_MIDDLE);
        backgroundColor=background;
        //GwtUtil.setStyle(this, "backgroundColor", backgroundColor);
        //GwtUtil.setStyle(this, "border", "1px solid "+backgroundColor);

        if (title.length() > 70) title = title.substring(0,66).trim()+"...";
        label.setHTML(title);
        image.setUrl(url);

        GwtUtil.setStyle(label, "fontSize", "100%");
        GwtUtil.setStyle(label, "textAlign", "left");
        GwtUtil.setStyle(label, "fontWeight", "bold");
        GwtUtil.setStyle(label, "margin", "5px");
        this.add(label);
        this.add(image);

        if (requiresResize) {
            this.setSize(FULL_SIZE, FULL_SIZE);
            GwtUtil.setStyle(this, "maxWidth",FULL_SIZE);
            GwtUtil.setStyle(this, "maxHeight",FULL_SIZE);
        } else {
            this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            GwtUtil.setStyle(this, "maxWidth",DEFAULT_WIDTH);
            GwtUtil.setStyle(this, "maxHeight",DEFAULT_HEIGHT);
        }

        GwtUtil.setStyle(this, "cssFloat","left");
        GwtUtil.setStyle(this, "styleFloat","left");
    }

    public String getLabelHTML() {
        return label.getHTML();
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
        return addDomHandler(handler, MouseOverEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return addDomHandler(handler, MouseOutEvent.getType());
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
         return addDomHandler(handler, DoubleClickEvent.getType());
    }
}
