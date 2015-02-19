/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.FormUtil;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureHandler;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 5:13:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingToolbar extends Composite {
    public static final String DEFAULT_STYLENAME = "toolbar";

    private ImageGridPanel imageGridPanel;
    private FlexTable mainPanel;
    private SimpleInputField pageSize;
    private PagingOptions pagingBar;
    private Label status;
    private HorizontalPanel addtlButtons;
    private CheckFileStatusTimer timer;
    private boolean doPageReload = true;

    public PagingToolbar(ImageGridPanel imageGridPanel) {
        this.imageGridPanel = imageGridPanel;

        // Create the main widget
        mainPanel = new FlexTable();
        initWidget(mainPanel);
        setStyleName(DEFAULT_STYLENAME);
        FlexTable.FlexCellFormatter formatter = mainPanel.getFlexCellFormatter();

        // paging controls
        pagingBar = new PagingOptions(this.imageGridPanel, new Images());
        mainPanel.setWidget(0, 0, pagingBar);
        formatter.setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);

        // display status
        status = new Label();
        status.addStyleName("status");
        status.setWordWrap(false);
        mainPanel.setWidget(0, 2, status);
        formatter.setVerticalAlignment(0, 2, HasVerticalAlignment.ALIGN_MIDDLE);
        formatter.setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT);

        // display status
        addtlButtons = new HorizontalPanel();
        mainPanel.setWidget(0, 1, addtlButtons);
        formatter.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
        formatter.setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);

        // page size field  TOTO: this is using GXT lib..  remove when we remove GXT
        pageSize = SimpleInputField.createByProp("TablePanel.pagesize");
        addtlButtons.add(pageSize);
        pageSize.setValue(imageGridPanel.getDataModel().getPageSize()+"");

        pageSize.getField().addValueChangeHandler(new ValueChangeHandler<String>(){
                public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                        if (doPageReload) {
                            if (pageSize.validate()) {
                                reloadPageSize(pageSize.getField());
                            }
                        }
                    }
                });
//TODO: need to add this into InputField
//        pageSize.addListener(Events.KeyPress, new EnterKeyListener(){
//                public void doAction(FieldEvent event) {
//                    if (pageSize.isValid()) {
//                        FocusImpl.getFocusImplForWidget().blur(pageSize.getElement());
//                    }
//                }
//            });


        // Add handlers to the table
        this.imageGridPanel.getImageGrid().addPageLoadHandler(new PageLoadHandler() {
          public void onPageLoad(PageLoadEvent event) {
              PagingToolbar.this.onPageLoad();
//              pageSize.setValue(PagingToolbar.this.table.getTable().getPageSize()+"");
          }
        });
        this.imageGridPanel.getImageGrid().addPageChangeHandler(new PageChangeHandler() {
          public void onPageChange(PageChangeEvent event) {
              PagingToolbar.this.onPageChange();
          }
        });
        this.imageGridPanel.getImageGrid().addPagingFailureHandler(new PagingFailureHandler() {
          public void onPagingFailure(PagingFailureEvent event) {
              PagingToolbar.this.onPagingFailure();
          }
        });
        this.imageGridPanel.getImageGrid().addPageCountChangeHandler(new PageCountChangeHandler() {
          public void onPageCountChange(PageCountChangeEvent event) {
              PagingToolbar.this.onPageCountChange();
          }
        });

        onPageLoad();
    }


    private void reloadPageSize(InputField ps) {
        imageGridPanel.getImageGrid().setPageSize(FormUtil.getIntValue(ps));
    }

    public void addButton(Button button) {
        addtlButtons.add(button);
    }

    protected void onPageCountChange() {
        setStatusMsg();
//        table.mask();
    }

    protected void onPagingFailure() {
//        table.unmask();
    }

    protected void onPageChange() {
        setStatusMsg();
//        table.mask();
    }

    protected void onPageLoad() {
//        table.unmask();
        if (imageGridPanel.getDataModel().getCurrentData() != null) {
            //setStatusMsg();
            if (!imageGridPanel.getDataModel().getCurrentData().getMeta().isLoaded() && timer == null) {
                timer = new CheckFileStatusTimer();
                timer.scheduleRepeating(1500);
            }
            setStatusMsg();
        }
    }

//====================================================================
//
//====================================================================

    private void setStatusMsg() {
        int totalRows = imageGridPanel.getDataModel().getTotalRows();
        boolean isLoaded = imageGridPanel.getDataModel().getCurrentData().getMeta().isLoaded();
        int startIdx = imageGridPanel.getImageGrid().getAbsoluteFirstRowIndex()+1;
        int endIdx = imageGridPanel.getImageGrid().getAbsoluteLastRowIndex()+1;
        status.setText("Displaying " + startIdx +
                " - " + endIdx + " of " + totalRows + (isLoaded ? "" : "+"));
        doPageReload = false;
        pageSize.setValue(String.valueOf(imageGridPanel.getImageGrid().getPageSize()));
        doPageReload = true;
    }

    private class CheckFileStatusTimer extends Timer {

        public void run() {
            SearchServices.App.getInstance().getFileStatus(imageGridPanel.getDataModel().getCurrentData().getMeta().getSource(),
                new AsyncCallback<FileStatus>(){
                    public void onFailure(Throwable caught) {
                        CheckFileStatusTimer.this.cancel();
                        timer = null;
                    }
                    public void onSuccess(FileStatus result) {
                        boolean isLoaded = !result.getState().equals(FileStatus.State.INPROGRESS);
                        imageGridPanel.getDataModel().getCurrentData().setTotalRows(result.getRowCount());
                        imageGridPanel.getDataModel().getCurrentData().getMeta().setIsLoaded(isLoaded);
                        imageGridPanel.getImageGrid().getTableModel().setRowCount(result.getRowCount());
                        imageGridPanel.updateTableStatus();
                        setStatusMsg();
                        if (isLoaded) {
                            CheckFileStatusTimer.this.cancel();
                            timer = null;
                        }
                    }
                });
        }
    }



    public static class Images implements PagingOptions.PagingOptionsImages {

        public AbstractImagePrototype pagingOptionsFirstPage() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getFirstPage());
        }

        public AbstractImagePrototype pagingOptionsLastPage() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getLastPage());
        }

        public AbstractImagePrototype pagingOptionsNextPage() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getNextPage());
        }

        public AbstractImagePrototype pagingOptionsPrevPage() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getPrevPage());
        }
    }
}

