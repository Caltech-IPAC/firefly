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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.util.DataSetParser;

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
    public static final String DEFAULT_STYLENAME = "toolbar";

    private PagingPanel pagingBar;
    private TablePanel table;
    private FlexTable mainPanel;
    private HorizontalPanel addtlButtons;
    private CheckFileStatusTimer timer;
    private boolean gotEnums = false;

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
        table.getLoader().setPageSize(ps);
        table.getTable().setPageSize(ps);
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
//        table.mask();
    }

    protected void onPageLoad() {
//        table.unmask();
        if (table.getDataset() != null) {
            setStatusMsg();
            if (!table.getDataset().getMeta().isLoaded() && timer == null) {
                timer = new CheckFileStatusTimer();
                timer.scheduleRepeating(1500);
            } else {
                onLoadCompleted();
            }
        }
    }

    private void onLoadCompleted() {
        try {
            if (gotEnums) return;

            gotEnums = true;
            SearchServices.App.getInstance().getEnumValues(table.getDataset().getMeta().getSource(),
                    new AsyncCallback<RawDataSet>() {
                        public void onFailure(Throwable throwable) {
                            //do nothing
                        }
                        public void onSuccess(RawDataSet rawDataSet) {
                            TableDataView ds = table.getDataset();
                            DataSet enums = DataSetParser.parse(rawDataSet);
                            for(TableDataView.Column c : enums.getColumns()) {
                                if (c.getEnums() != null && c.getEnums().length > 0) {
                                    TableDataView.Column fc = ds.findColumn(c.getName());
                                    if (fc != null) {
                                        fc.setEnums(c.getEnums());
                                    }
                                }
                            }
                            table.getTable().updateHeaderTable(true);
                        }
                    });
        } catch (RPCException e) {
            e.printStackTrace();
            //do nothing.
        }
    }
//====================================================================
//
//====================================================================

    public void setIsLoading(boolean flg) {
        pagingBar.setIsLoading(flg);
    }

    private void setStatusMsg() {
        int totalRows = table.getDataset().getTotalRows();
        boolean isLoaded = table.getDataset().getMeta().isLoaded();
        int startIdx = table.getTable().getAbsoluteFirstRowIndex()+1;
        startIdx = startIdx < 0 ? 0 : startIdx;
        int endIdx = table.getTable().getAbsoluteLastRowIndex()+1;
        endIdx = endIdx < 0 ? 0 : endIdx;
        pagingBar.setStatus("(" + startIdx +
                " - " + endIdx + " of " + totalRows + (isLoaded ? ")" : "+)"));
    }

    private class CheckFileStatusTimer extends Timer {

        public void run() {
            SearchServices.App.getInstance().getFileStatus(table.getDataset().getMeta().getSource(),
                    new AsyncCallback<FileStatus>(){
                        public void onFailure(Throwable caught) {
                            CheckFileStatusTimer.this.cancel();
                            timer = null;
                        }
                        public void onSuccess(FileStatus result) {
                            boolean isLoaded = !result.getState().equals(FileStatus.State.INPROGRESS);
                            table.getDataset().setTotalRows(result.getRowCount());
                            table.getDataset().getMeta().setIsLoaded(isLoaded);
                            table.getTable().getTableModel().setRowCount(result.getRowCount());
                            table.updateTableStatus();
                            setStatusMsg();
                            if (isLoaded) {
                                CheckFileStatusTimer.this.cancel();
                                timer = null;
                                onLoadCompleted();
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
