package edu.caltech.ipac.firefly.ui.imageGrid;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasErrorHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.TableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageCountChangeHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PageLoadHandler;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureEvent;
import edu.caltech.ipac.firefly.ui.imageGrid.event.PagingFailureHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 10, 2010
 * Time: 3:54:26 PM
 * To change this template use File | Settings | File Templates.
 */
//todo: change all DOM.setStyleAttribute() statements to CSS

public class BasicPagingImageGrid<RowType> extends ScrollPanel{
    private FlowPanel flowpanel = new FlowPanel();
    private MouseOverHandler mouseOverHandler = null;
    private MouseOutHandler mouseOutHandler = null;
    private ClickHandler clickHandler=null;
    private DoubleClickHandler doubleClickHandler=null;
    private ErrorHandler errorHandler=null;
    private MutableTableModel<TableData.Row> tableModel;
    /**
    * The current visible page.
    */
    private int currentPage = -1;
    private TableModelHelper.Request lastRequest = null;
    private boolean isPageLoading;
    private int oldPageCount;
    private List<TableData.Row> rowValues = new ArrayList<TableData.Row>();
    private ImageGridPanel imageGridPanel;
    /**
    * The number of rows per page. If the number of rows per page is equal to the
    * number of rows, paging is disabled because only one page exists.
    */
    private int pageSize = 0;
    /**
    * The callback that handles page requests.
    */
    private TableModel.Callback<TableData.Row> pagingCallback = new TableModel.Callback<TableData.Row>() {
        public void onFailure(Throwable caught) {
            isPageLoading = false;
            fireEvent(new PagingFailureEvent(caught));
        }

        public void onRowsReady(TableModelHelper.Request request, TableModelHelper.Response<TableData.Row> response) {
            if (lastRequest == request) {
                setData(request.getStartRow(), response.getRowValues());
                lastRequest = null;
            }
        }
    };
    public BasicPagingImageGrid(ImageGridPanel imageGridPanel, MutableTableModel<TableData.Row> tableModel,
                   TableDataView tableDataView) {
        this.imageGridPanel = imageGridPanel;
        this.tableModel = tableModel;
        super.setSize("100%", "100%");
        flowpanel.setSize("100%", "100%");
        super.add(flowpanel);
        DOM.setStyleAttribute(flowpanel.getElement(), "backgroundColor", "#ddd");
        DOM.setStyleAttribute(this.getElement(), "backgroundColor", "#ddd");
    }

    public void clear() {
        flowpanel.clear();
    }

    public void add(Widget widget) {
        if (widget instanceof HasClickHandlers && clickHandler!=null) {
            ((HasClickHandlers) widget).addClickHandler(clickHandler);
        }
        if (widget instanceof HasDoubleClickHandlers && doubleClickHandler!=null) {
            ((HasDoubleClickHandlers) widget).addDoubleClickHandler(doubleClickHandler);
        }
        if (widget instanceof HasErrorHandlers && errorHandler!=null) {
            ((HasErrorHandlers) widget).addErrorHandler(errorHandler);
        }
        if (widget instanceof HasMouseOutHandlers && mouseOutHandler!=null) {
            ((HasMouseOutHandlers) widget).addMouseOutHandler(mouseOutHandler);
        }
        if (widget instanceof HasMouseOverHandlers && mouseOverHandler!=null) {
            ((HasMouseOverHandlers) widget).addMouseOverHandler(mouseOverHandler);
        }
        flowpanel.add(widget);
    }

    public void addClickHandler(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    public void addDoubleClickHandler(DoubleClickHandler doubleClickHandler) {
        this.doubleClickHandler = doubleClickHandler;
    }

    public void addMouseOverHandler(MouseOverHandler mouseOverHandler) {
        this.mouseOverHandler= mouseOverHandler;
    }

    public void addMouseOutHandler(MouseOutHandler mouseOutHandler) {
        this.mouseOutHandler= mouseOutHandler;
    }

    public void addErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public HandlerRegistration addPageChangeHandler(PageChangeHandler handler) {
        return addHandler(handler, PageChangeEvent.TYPE);
    }

    public HandlerRegistration addPageCountChangeHandler(
        PageCountChangeHandler handler) {
        return addHandler(handler, PageCountChangeEvent.TYPE);
    }

    public HandlerRegistration addPageLoadHandler(PageLoadHandler handler) {
        return addHandler(handler, PageLoadEvent.TYPE);
    }

    public HandlerRegistration addPagingFailureHandler(
        PagingFailureHandler handler) {
        return addHandler(handler, PagingFailureEvent.TYPE);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    /**
    * @return the absolute index of the first visible row
    */
    public int getAbsoluteFirstRowIndex() {
        return currentPage * pageSize;
    }

    /**
    * @return the absolute index of the last visible row
    */
    public int getAbsoluteLastRowIndex() {
        if (tableModel.getRowCount() < 0) {
            // Unknown row count, so just return based on current page
            return (currentPage + 1) * pageSize - 1;
        } else if (pageSize == 0) {
            // Only one page, so return row count
            return tableModel.getRowCount() - 1;
        }
        return Math.min(tableModel.getRowCount(), (currentPage + 1) * pageSize) - 1;
    }

    /**
    * @return the table model
    */
    public TableModel<TableData.Row> getTableModel() {
        return tableModel;
    }

    public Panel getDisplay() {
        return this;
    }

    public int getSize() {
        return flowpanel.getWidgetCount();
    }

    public void setPageSize(int pageSize) {
        pageSize = Math.max(0, pageSize);
        this.pageSize = pageSize;

        int pageCount = getPageCount();
        if (pageCount != oldPageCount) {
            fireEvent(new PageCountChangeEvent(oldPageCount, pageCount));
            oldPageCount = pageCount;
        }

        // Reset the page
        if (currentPage >= 0) {
            gotoPage(currentPage, true);
        }
    }

    /**
    * @return the number of pages, or -1 if not known
    */
    public int getPageCount() {
        if (pageSize < 1) {
            return 1;
        } else {
            int numDataRows = tableModel.getRowCount();
            if (numDataRows < 0) {
                return -1;
            }
            return (int) Math.ceil(numDataRows / (pageSize + 0.0));
        }
    }

    /**
    * @return the number of rows per page
    */
    public int getPageSize() {
        return pageSize;
    }

    /**
    * Go to the first page.
    */
    public void gotoFirstPage() {
        gotoPage(0, false);
    }

    /**
    * Go to the last page. If the number of pages is not known, this method is
    * ignored.
    */
    public void gotoLastPage() {
        if (getPageCount() >= 0) {
            gotoPage(getPageCount(), false);
        }
    }

    /**
    * Go to the previous page.
    */
    public void gotoPreviousPage() {
        gotoPage(currentPage - 1, false);
    }

    /**
    * Go to the next page.
    */
    public void gotoNextPage() {
        gotoPage(currentPage + 1, false);
    }

    public void gotoPage(int page, boolean forced) {

        int oldPage = currentPage;
        int numPages = getPageCount();
        if (numPages >= 0) {
            currentPage = Math.max(0, Math.min(page, numPages - 1));
        } else {
            currentPage = page;
        }

        if (currentPage != oldPage || forced) {
            isPageLoading = true;

            fireEvent(new PageChangeEvent(oldPage, currentPage));

            int firstRow = getAbsoluteFirstRowIndex();
            int lastRow = pageSize == 0 ? tableModel.getRowCount() : pageSize;
            lastRequest = new TableModelHelper.Request(firstRow, lastRow, null);
            tableModel.requestRows(lastRequest, pagingCallback);
        }
    }

  /**
   * Get the value associated with a row.
   *
   * @param row the row index
   * @return the value associated with the row
   */
  public TableData.Row getRowValue(int row) {
    if (rowValues.size() <= row) {
      return null;
    }
    return rowValues.get(row);
  }


  /**
   * Get the list of row values associated with the table.
   *
   * @return the list of row value
   */
  protected List<TableData.Row> getRowValues() {
    return rowValues;
  }

  /**
   * Associate a row in the table with a value.
   *
   * @param row the row index
   * @param value the value to associate
   */
  public void setRowValue(int row, TableData.Row value) {
    // Make sure the list can fit the row
    for (int i = rowValues.size(); i <= row; i++) {
      rowValues.add(null);
    }

    // Set the row value
    rowValues.set(row, value);

    // Render the new row value
    refreshRow(row);
  }

   /**
   * Set a block of data. This method is used when responding to data requests.
   *
   * This method takes an iterator of iterators, where each iterator represents
   * one row of data starting with the first row.
   *
   * @param firstRow the row index that the rows iterator starts with
   * @param rows the values associated with each row
   */
  protected void setData(int firstRow, Iterator<TableData.Row> rows) {
    rowValues = new ArrayList<TableData.Row>();
    if (rows != null && rows.hasNext()) {
      // Get an iterator over the visible rows
      int firstVisibleRow = getAbsoluteFirstRowIndex();
      int lastVisibleRow = getAbsoluteLastRowIndex();
      Iterator<TableData.Row> visibleIter = new VisibleRowsIterator(rows, firstRow,
          firstVisibleRow, lastVisibleRow);

      // Set the row values
        this.clear();
      while (visibleIter.hasNext()) {
          rowValues.add(visibleIter.next());
      }

      // Render the rows
      renderRows();

    } else {
      //setEmptyTableWidgetVisible(true);
    }

    // Fire page loaded event
    onDataTableRendered();
  }

    protected void renderRows() {
        flowpanel.clear();
        for (TableData.Row row: rowValues) {
            this.add(imageGridPanel.makeCellWidget(row));
        }
    }
  /**
   * Called when the data table has finished rendering.
   */
  protected void onDataTableRendered() {
    // Refresh the headers if needed

    // Select rows

    // Update the UI of the table
    isPageLoading = false;
    fireEvent(new PageLoadEvent(currentPage));
  }


  /**
   * Refresh a single row in the table.
   *
   * @param rowIndex the index of the row
   */
  private void refreshRow(int rowIndex) {
    final TableData.Row rowValue = getRowValue(rowIndex);
      /*
    Iterator<RowType> singleIterator = new Iterator<RowType>() {
      private boolean nextCalled = false;

      public boolean hasNext() {
        return !nextCalled;
      }

      public RowType next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        nextCalled = true;
        return rowValue;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
    tableDefinition.renderRows(rowIndex, singleIterator, rowView);
    */
  }

    /**
   * An iterator over the visible rows in an iterator over many rows.
   */
  private class VisibleRowsIterator implements Iterator<TableData.Row> {
    /**
     * The iterator of row data.
     */
    private Iterator<TableData.Row> rows;

    /**
     * The current row of the rows iterator.
     */
    private int curRow;

    /**
     * The last visible row in the grid.
     */
    private int lastVisibleRow;

    /**
     * Constructor.
     *
     * @param rows the iterator over row data
     * @param firstRow the first absolute row of the rows iterator
     * @param firstVisibleRow the first visible row in this grid
     * @param lastVisibleRow the last visible row in this grid
     */
    public VisibleRowsIterator(Iterator<TableData.Row> rows, int firstRow,
        int firstVisibleRow, int lastVisibleRow) {
      this.curRow = firstRow;
      this.lastVisibleRow = lastVisibleRow;

      // Iterate up to the first row
      while (curRow < firstVisibleRow && rows.hasNext()) {
        rows.next();
        curRow++;
      }
      this.rows = rows;
    }

    public boolean hasNext() {
      return (curRow <= lastVisibleRow && rows.hasNext());
    }

    public TableData.Row next() {
      // Check that the next row exists
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return rows.next();
    }

    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}

