/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.PagingOptions;
import com.google.gwt.gen2.table.event.client.PageChangeEvent;
import com.google.gwt.gen2.table.event.client.PageChangeHandler;
import com.google.gwt.gen2.table.event.client.PageCountChangeEvent;
import com.google.gwt.gen2.table.event.client.PageCountChangeHandler;
import com.google.gwt.gen2.table.event.client.PageLoadEvent;
import com.google.gwt.gen2.table.event.client.PageLoadHandler;
import com.google.gwt.gen2.table.event.client.PagingFailureEvent;
import com.google.gwt.gen2.table.event.client.PagingFailureHandler;
import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;

/**
 * A paging toobar built on top of gwt-incubator's PagingOptions
 * to provide additional information.
 *
 * <h3>CSS Style Rules</h3>
 *
 * <ul class="css">
 * <li>.pagingToolbar { applied to the entire widget }</li>
 * <li>.gwt-PagingOptions { applied to the paging widget }</li>
 * <li>.gwt-PagingOptions .errorMessage { applied to the error message }</li>
 * <li>.pagingOptionsFirstPage { the first page button }</li>timer
 * <li>.pagingOptionsLastPage { the last page button }</li>
 * <li>.pagingOptionsNextPage { the next page button }</li>
 * <li>.pagingOptionsPreviousPage { the previous page button }</li>
 * </ul>
 *
 * @author loi
 * @version $Id: PagingToolbar.java,v 1.21 2012/06/16 00:21:53 loi Exp $
 */
public class PagingToolbar extends Composite {
    public static final String DEFAULT_STYLENAME = "firefly-toolbar";

    private PagingPanel pagingBar;
    private TablePanel table;
    private FlexTable mainPanel;
    private HorizontalPanel addtlButtons;

    public PagingToolbar(TablePanel table) {
        this.table = table;

        // Create the main widget
        mainPanel = new FlexTable();
        initWidget(mainPanel);
        setStyleName(DEFAULT_STYLENAME);
        FlexTable.FlexCellFormatter formatter = mainPanel.getFlexCellFormatter();

        // paging controls
        pagingBar = new PagingPanel(table.getTable(), new Images());
        mainPanel.setWidget(0, 0, pagingBar);
        formatter.setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
//        formatter.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);

        // display status
        addtlButtons = new HorizontalPanel();
        mainPanel.setWidget(0, 1, addtlButtons);
        formatter.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
//        formatter.setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);


        // Add handlers to the table
        table.getTable().addPageLoadHandler(new PageLoadHandler() {
          public void onPageLoad(PageLoadEvent event) {
              PagingToolbar.this.onPageLoad();
//              pageSize.setValue(PagingToolbar.this.table.getTable().getPageSize()+"");
          }
        });
        table.getTable().addPageChangeHandler(new PageChangeHandler() {
          public void onPageChange(PageChangeEvent event) {
              PagingToolbar.this.onPageChange();
          }
        });
        table.getTable().addPagingFailureHandler(new PagingFailureHandler() {
          public void onPagingFailure(PagingFailureEvent event) {
              PagingToolbar.this.onPagingFailure();
          }
        });
        table.getTable().addPageCountChangeHandler(new PageCountChangeHandler() {
          public void onPageCountChange(PageCountChangeEvent event) {
              PagingToolbar.this.onPageCountChange();
          }
        });

        onPageLoad();
    }

    public void reloadPageSize(int ps) {
        table.getDataModel().setPageSize(ps);
        table.getTable().setPageSize(ps);
    }

    public void addButton(Button button) {
        addtlButtons.add(button);
    }

    protected void onPageCountChange() {
        updateStatusMsg();
//        table.mask();
    }

    protected void onPagingFailure() {
//        table.unmask();
    }

    protected void onPageChange() {
//        table.mask();
    }

    protected void onPageLoad() {
//        table.unmask();
        updateStatusMsg();
    }

//====================================================================
//
//====================================================================

    public void setIsLoading(boolean flg) {
        pagingBar.setIsLoading(flg);
    }

    public void updateStatusMsg() {
        if (table == null || table.getDataset() == null) return ;

        int totalRows = table.getDataset().getTotalRows();
        boolean isLoaded = table.getDataset().getMeta().isLoaded();
        int startIdx = table.getTable().getAbsoluteFirstRowIndex()+1;
        startIdx = startIdx < 0 ? 0 : startIdx;
        int endIdx = table.getTable().getAbsoluteLastRowIndex()+1;
        endIdx = endIdx < 0 ? 0 : endIdx;
        pagingBar.setStatus("(" + startIdx +
                " - " + endIdx + " of " + totalRows + (isLoaded ? ")" : "+)"));
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
