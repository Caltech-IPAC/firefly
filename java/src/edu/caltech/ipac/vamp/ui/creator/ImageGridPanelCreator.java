package edu.caltech.ipac.vamp.ui.creator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasErrorHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.creator.ImageGridPrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableCreator;

import edu.caltech.ipac.firefly.ui.imageGrid.ImageGridPanel;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Apr 28, 2010
 * Time: 7:17:36 PM
 * To change this template use File | Settings | File Templates.
 */
//todo: change all DOM.setStyleAttribute() statements to CSS
public class ImageGridPanelCreator implements PrimaryTableCreator {
    public static final String TITLE = "Title";
    public static final String SHORT_DESC = "ShortDesc";
    public static final String THUMBNAIL_TITLE_FIELD = "ThumbnailTitle";
    public static final String THUMBNAIL_URL_FIELD = "ThumbnailURL";
    public static final String POPUP_CAPTION_FIELD = "PopUpCaption";
    public static final String POPUP_TITLE_FIELD = "PopUpTitle";
    public static final String POPUP_URL_FIELD = "PopUpURL";

    public ImageGridPrimaryDisplay create(TableServerRequest req, Map<String, String> params) {
        String title = params.get(TITLE);
        String desc = params.get(SHORT_DESC);

        BaseTableConfig<TableServerRequest> config =
                                new BaseTableConfig<TableServerRequest>(req, title, desc);

        ImageGridPanel panel = makeImageGridPanel(req.getRequestId(), config.getLoader());

        panel.setTitle(title);
        panel.setShortDesc(desc);
        panel.setName(req.getRequestId());

        return new ImageGridPrimaryDisplay(panel);
    }

    private ImageGridPanel makeImageGridPanel(String name, Loader<TableDataView> loader) {
        return new ImageGridPanel(name, loader) {
            public Widget makeCellWidget(TableData.Row row) {
                return new ThumbnailWidget(getThumbnailTitle(row), getThumbnailURL(row));
            }
            public void handleDoubleClick(TableData.Row row) {
                ImageGridPanelCreator.makePreviewPopupPane(row).show(); 
            }
        };
    }

    //------------------------ Private Static Methods ------------------------
    private static PopupPane makePreviewPopupPane(TableData.Row row) {
        PopupPane popupPane = new PopupPane("PopUp");
        popupPane.setHeader(getThumbnailTitle(row).replace("<em>","").replace("</em>",""));
        VerticalPanel vp = new VerticalPanel();
        final Image previewImage = new Image(getPopupImageURL(row));
        final HTML caption = new HTML(getPopUpCaption(row));

        caption.setWidth("320px");

        previewImage.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent ev) {
                caption.setWidth(previewImage.getWidth()+"px");
            }
        });
        DOM.setStyleAttribute(vp.getElement(), "marginTop", "4px");
        DOM.setStyleAttribute(vp.getElement(), "marginBottom", "4px");
        DOM.setStyleAttribute(vp.getElement(), "marginRight", "4px");
        DOM.setStyleAttribute(vp.getElement(), "marginLeft", "4px");

        vp.setCellHorizontalAlignment(previewImage, HasHorizontalAlignment.ALIGN_CENTER);
        vp.setCellVerticalAlignment(previewImage, HasVerticalAlignment.ALIGN_MIDDLE);
        vp.add(previewImage);
        vp.add(caption);

        popupPane.setWidget(vp);

        return popupPane;
    }

    private static String getPopUpCaption(TableData.Row row) {
        return String.valueOf(row.getValue("headline"));
    }

    private static String getPopupImageURL(TableData.Row row) {
        return getThumbnailURL(row).replace("thumbnails","images").replace(".jpg","_Ti.jpg");
    }

    // --------------- Define ImageGridPanel Thumbnail URL and title ---------------
    private static String getThumbnailTitle(TableData.Row row) {
        String title;
        String avmId;
        avmId = String.valueOf(row.getValue("avm_id"));
        title = avmId+": "+String.valueOf(row.getValue("title"));
        return title;
    }

    private static String getThumbnailURL(TableData.Row row) {
        String url;
        String avmId;
        avmId = String.valueOf(row.getValue("avm_id"));
        url = "../vamp_files/thumbnails/"+avmId+".jpg";
        return url;
    }

    // ---------------------------- private class ----------------------------
    private static class ThumbnailWidget extends VerticalPanel implements HasClickHandlers,
            HasErrorHandlers, HasDoubleClickHandlers, HasMouseOutHandlers,HasMouseOverHandlers {
        private HTML label = new HTML();
        private Image image = new Image();

        public ThumbnailWidget(String title, String url) {
            super();

            this.setCellHeight(image, "128px");
            this.setCellWidth(image, "128px");
            this.setHorizontalAlignment(ALIGN_CENTER);
            this.setVerticalAlignment(ALIGN_MIDDLE);
            DOM.setStyleAttribute(this.getElement(), "backgroundColor", "#ddd");
            DOM.setStyleAttribute(this.getElement(), "border", "1px solid #ddd");

            if (title.length() > 70) title = title.substring(0,66).trim()+"...";
            label.setHTML(title);
            image.setUrl(url);

            DOM.setStyleAttribute(label.getElement(), "fontSize", "100%");
            DOM.setStyleAttribute(label.getElement(), "textAlign", "center");

            this.add(image);
            this.add(label);

            this.setSize("140px", "180px");
            DOM.setStyleAttribute(this.getElement(), "maxWidth","140px");
            DOM.setStyleAttribute(this.getElement(), "maxHeight","180px");

            DOM.setStyleAttribute(this.getElement(), "cssFloat","left");
            DOM.setStyleAttribute(this.getElement(), "styleFloat","left");
        }

        public HandlerRegistration addErrorHandler(ErrorHandler errorHandler) {
            return image.addErrorHandler(errorHandler);
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
