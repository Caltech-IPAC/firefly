/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureHandler;


/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 26, 2010
 * Time: 4:41:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingOptions extends Composite {

    public static interface PagingOptionsImages extends ClientBundle {
        /**
        * An image used to navigate to the first page.
        *
        * @return a prototype of this image
        */
        AbstractImagePrototype pagingOptionsFirstPage();

        /**
        * An image used to navigate to the last page.
        *
        * @return a prototype of this image
        */
        AbstractImagePrototype pagingOptionsLastPage();

        /**
        * An image used to navigate to the next page.
        *
        * @return a prototype of this image
        */
        AbstractImagePrototype pagingOptionsNextPage();

        /**
        * An image used to navigate to the previous page.
        *
        * @return a prototype of this image
        */
        AbstractImagePrototype pagingOptionsPrevPage();
    }

    /**
    * The default style name.
    */
    public static final String DEFAULT_STYLENAME = "gwt-PagingOptions";
    public static final String STYLENAME_PREFIX = "pagingOptions";

    /**
    * The label used to display errors.
    */
    private HTML errorLabel;

    /**
    * Goto first page button.
    */
    private Image firstImage;

    /**
    * Goto last page button.
    */
    private Image lastImage;

    /**
    * The loading image.
    */
    private Image loadingImage;

    /**
    * Goto next page button.
    */
    private Image nextImage;

    /**
    * The HTML field that contains the number of pages.
    */
    private HTML numPagesLabel;

    /**
    * Goto previous page button.
    */
    private Image prevImage;

    /**
    * The box where the user can select the current page.
    */
    private TextBox curPageBox = new TextBox();

    private ImageGridPanel imageGridPanel;

    public PagingOptions(ImageGridPanel imageGridPanel, PagingOptionsImages images) {
        this.imageGridPanel = imageGridPanel;
        HorizontalPanel hPanel = new HorizontalPanel();
        initWidget(hPanel);
        hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        setStyleName(DEFAULT_STYLENAME);

        // Create the paging image buttons
        createPageButtons(images);

        // Create the current page box
        createCurPageBox();

        // Create the page count label
        numPagesLabel = new HTML();

        // Create the loading image
        loadingImage = new Image(GWT.getModuleBaseURL() + "scrollTableLoading.gif");
        loadingImage.setVisible(false);

        // Create the error label
        errorLabel = new HTML();
        errorLabel.setStylePrimaryName("errorMessage");

        // Add the widgets to the panel
        hPanel.add(createSpacer());
        hPanel.add(firstImage);
        hPanel.add(createSpacer());
        hPanel.add(prevImage);
        hPanel.add(createSpacer());
        hPanel.add(curPageBox);
        hPanel.add(createSpacer());
        hPanel.add(numPagesLabel);
        hPanel.add(createSpacer());
        hPanel.add(nextImage);
        hPanel.add(createSpacer());
        hPanel.add(lastImage);
        hPanel.add(createSpacer());
        hPanel.add(loadingImage);
        hPanel.add(errorLabel);

        // Add handlers to the table
        this.imageGridPanel.getImageGrid().addPageLoadHandler(new PageLoadHandler() {
            public void onPageLoad(PageLoadEvent event) {
                loadingImage.setVisible(false);
                errorLabel.setHTML("");
            }
        });
        this.imageGridPanel.getImageGrid().addPageChangeHandler(new PageChangeHandler() {
            public void onPageChange(PageChangeEvent event) {
                curPageBox.setText((event.getNewPage() + 1) + "");
                loadingImage.setVisible(true);
                errorLabel.setHTML("");
            }
        });
        this.imageGridPanel.getImageGrid().addPagingFailureHandler(new PagingFailureHandler() {
            public void onPagingFailure(PagingFailureEvent event) {
                loadingImage.setVisible(false);
                errorLabel.setHTML(event.getException().getMessage());
            }
        });
        this.imageGridPanel.getImageGrid().addPageCountChangeHandler(new PageCountChangeHandler() {
            public void onPageCountChange(PageCountChangeEvent event) {
                setPageCount(event.getNewPageCount());
            }
        });
        setPageCount(this.imageGridPanel.getImageGrid().getPageCount());
    }

    /**
    * Create a widget that can be used to add space.
    *
    * @return a spacer widget
    */
    protected Widget createSpacer() {
        return new HTML("&nbsp;&nbsp;");
    }

    /**
    * Create a box that holds the current page.
    */
    private void createCurPageBox() {
        // Setup the widget
        curPageBox.setWidth("3em");
        curPageBox.setText("1");
        curPageBox.setTextAlignment(TextBoxBase.ALIGN_RIGHT);

        // Disallow non-numeric pages
        KeyPressHandler handler = new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                char charCode = event.getCharCode();
                if (charCode == KeyCodes.KEY_ENTER) {
                    PagingOptions.this.imageGridPanel.getImageGrid().gotoPage(getPagingBoxValue(), false);
                } else if (!Character.isDigit(charCode)
                        && (charCode != KeyCodes.KEY_TAB)
                        && (charCode != KeyCodes.KEY_BACKSPACE)
                        && (charCode != KeyCodes.KEY_DELETE)
                        && (charCode != KeyCodes.KEY_ENTER)
                        && (charCode != KeyCodes.KEY_HOME)
                        && (charCode != KeyCodes.KEY_END)
                        && (charCode != KeyCodes.KEY_LEFT) && (charCode != KeyCodes.KEY_UP)
                        && (charCode != KeyCodes.KEY_RIGHT)
                        && (charCode != KeyCodes.KEY_DOWN)) {
                    curPageBox.cancelKey();
                }
            }
        };

        // Add the handler
        curPageBox.addKeyPressHandler(handler);
    }

    /**
    * Create a paging image buttons.
    *
    * @param images the images to use
    */
    private void createPageButtons(PagingOptionsImages images) {
        // Create the images
        firstImage = images.pagingOptionsFirstPage().createImage();
        firstImage.addStyleName(STYLENAME_PREFIX + "FirstPage");
        prevImage = images.pagingOptionsPrevPage().createImage();
        prevImage.addStyleName(STYLENAME_PREFIX + "PreviousPage");
        nextImage = images.pagingOptionsNextPage().createImage();
        nextImage.addStyleName(STYLENAME_PREFIX + "NextPage");
        lastImage = images.pagingOptionsLastPage().createImage();
        lastImage.addStyleName(STYLENAME_PREFIX + "LastPage");

        // Create the listener
        ClickHandler handler = new ClickHandler() {
            public void onClick(ClickEvent event) {
                Object source = event.getSource();
                if (source == firstImage) {
                    imageGridPanel.getImageGrid().gotoFirstPage();
                } else if (source == lastImage) {
                    imageGridPanel.getImageGrid().gotoLastPage();
                } else if (source == nextImage) {
                    imageGridPanel.getImageGrid().gotoNextPage();
                } else if (source == prevImage) {
                    imageGridPanel.getImageGrid().gotoPreviousPage();
                }
            }
        };

        // Add the listener to each image
        firstImage.addClickHandler(handler);
        prevImage.addClickHandler(handler);
        nextImage.addClickHandler(handler);
        lastImage.addClickHandler(handler);
    }

    /**
    * Get the value of in the page box. If the value is invalid, it will be set
    * to 1 automatically.
    *
    * @return the value in the page box
    */
    private int getPagingBoxValue() {
        int page = 0;
        try {
            page = Integer.parseInt(curPageBox.getText()) - 1;
        } catch (NumberFormatException e) {
            // This will catch an empty box
            curPageBox.setText("1");
        }

        // Replace values less than 1
        if (page < 1) {
            curPageBox.setText("1");
            page = 0;
        }

        // Return the 0 based page, not the 1 based visible value
        return page;
    }

    /**
    * Set the page count.
    *
    * @param pageCount the current page count
    */
    private void setPageCount(int pageCount) {
        if (pageCount < 0) {
            numPagesLabel.setHTML("");
            lastImage.setVisible(false);
        } else {
            numPagesLabel.setHTML("of&nbsp;&nbsp;" + pageCount);
            numPagesLabel.setVisible(true);
            lastImage.setVisible(true);
        }
    }
}
