/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.gen2.table.event.client.PageChangeEvent;
import com.google.gwt.gen2.table.event.client.PageChangeHandler;
import com.google.gwt.gen2.table.event.client.PageCountChangeEvent;
import com.google.gwt.gen2.table.event.client.PageCountChangeHandler;
import com.google.gwt.gen2.table.event.client.PageLoadEvent;
import com.google.gwt.gen2.table.event.client.PageLoadHandler;
import com.google.gwt.gen2.table.event.client.PagingFailureEvent;
import com.google.gwt.gen2.table.event.client.PagingFailureHandler;
import com.google.gwt.gen2.table.client.PagingScrollTable;
import com.google.gwt.gen2.table.client.PagingOptions;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;

/**
 * This is a customized version of GWT's PagingOptions class.
 * <p/>
 * A panel that wraps a {@link com.google.gwt.gen2.table.client.PagingScrollTable} and includes options to manipulate
 * the page.
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <p/>
 * <ul class="css">
 * <p/>
 * <li>.gwt-PagingOptions { applied to the entire widget }</li>
 * <p/>
 * <li>.gwt-PagingOptions .errorMessage { applied to the error message }</li>
 * <p/>
 * <li>.pagingOptionsFirstPage { the first page button }</li>
 * <p/>
 * <li>.pagingOptionsLastPage { the last page button }</li>
 * <p/>
 * <li>.pagingOptionsNextPage { the next page button }</li>
 * <p/>
 * <li>.pagingOptionsPreviousPage { the previous page button }</li>
 * <p/>
 * </ul>
 */
public class PagingPanel extends Composite {

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

    private Label status;

    /**
     * The table being affected.
     */
    private PagingScrollTable<?> table;

    /**
     * Constructor.
     *
     * @param table the table being ad
     */
    public PagingPanel(PagingScrollTable<?> table) {
        this(table, GWT.<PagingOptions.PagingOptionsImages>create(PagingOptions.PagingOptionsImages.class));
    }

    /**
     * Constructor.
     *
     * @param table  the table being affected
     * @param images the images to use
     */
    public PagingPanel(PagingScrollTable<?> table, PagingOptions.PagingOptionsImages images) {
        this.table = table;

        // Create the main widget
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

        // display status
        status = new Label();
        status.addStyleName("status");
        status.setWordWrap(false);

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
        hPanel.add(status);
        hPanel.add(createSpacer());
        hPanel.add(loadingImage);
        hPanel.add(errorLabel);

        // Add handlers to the table
        table.addPageLoadHandler(new PageLoadHandler() {
            public void onPageLoad(PageLoadEvent event) {
                setIsLoading(false);
            }
        });
        table.addPageChangeHandler(new PageChangeHandler() {
            public void onPageChange(PageChangeEvent event) {
                curPageBox.setText((event.getNewPage() + 1) + "");
                setIsLoading(true);
            }
        });
        table.addPagingFailureHandler(new PagingFailureHandler() {
            public void onPagingFailure(PagingFailureEvent event) {
                loadingImage.setVisible(false);
                errorLabel.setHTML(event.getException().getMessage());
            }
        });
        table.addPageCountChangeHandler(new PageCountChangeHandler() {
            public void onPageCountChange(PageCountChangeEvent event) {
                setPageCount(event.getNewPageCount());
            }
        });
        setPageCount(table.getPageCount());
    }

    public void setStatus(String msg) {
        status.setText(msg);
    }

    public void setIsLoading(boolean flag) {
        if (flag) {
            loadingImage.setVisible(true);
            errorLabel.setHTML("");

        } else {
            loadingImage.setVisible(false);
            errorLabel.setHTML("");
        }
    }

    /**
     * @return the {@link com.google.gwt.gen2.table.client.PagingScrollTable}.
     */
    public PagingScrollTable<?> getPagingScrollTable() {
        return table;
    }

    /**
     * Create a widget that can be used to add space.
     *
     * @return a spacer widget
     */
    protected Widget createSpacer() {
        return new HTML("&nbsp;");
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

                int keyCode = event.getNativeEvent().getKeyCode();
                char charCode = event.getCharCode();
                if (keyCode == KeyCodes.KEY_ENTER) {
                    PagingPanel.this.table.gotoPage(getPagingBoxValue(), false);
                } else if (charCode != 0 && !Character.isDigit(charCode)) {
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
    private void createPageButtons(PagingOptions.PagingOptionsImages images) {
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
                    table.gotoFirstPage();
                } else if (source == lastImage) {
                    table.gotoLastPage();
                } else if (source == nextImage) {
                    table.gotoNextPage();
                } else if (source == prevImage) {
                    table.gotoPreviousPage();
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
     * Get the value of in the page box. If the value is invalid, it will be set to 1 automatically.
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