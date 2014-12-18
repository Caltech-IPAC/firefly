package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Feb 11, 2009
 *
 * @author loi
 * @version $Id: AbstractLoader.java,v 1.7 2009/11/06 01:24:26 loi Exp $
 */
public abstract class AbstractLoader<Data> implements Loader<Data> {

    public static final String SORT_COLS_CHANGED = "sortCols";
    public static final String FILTER_CHANGED = "filter";

    private Data curData;
    private int pageSize = Preferences.getInt("TablePageSize", Application.getInstance().getProperties().getIntProperty("AbstractLoader.PageSize", 50));
    private ArrayList<String> filters = new ArrayList<String>();
    private SortInfo sortInfo;
//    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return return all of the filters used by this loader
     */
    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        getFilters().clear();
        if (filters != null) {
            getFilters().addAll(filters);
        }
//        pcs.firePropertyChange(new PropertyChangeEvent(this, FILTER_CHANGED, null, filters));
    }

    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfo = sortInfo;
//        pcs.firePropertyChange(new PropertyChangeEvent(this, SORT_COLS_CHANGED, null, filters));
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }

    public void getData(TableServerRequest req, final AsyncCallback<Data> callback) {
        callback.onSuccess(getCurrentData());
    }

    public void load(int offset, int pageSize, AsyncCallback<Data> dataAsyncCallback) {
    }

    public void onLoad(Data result) {
        curData = result;
    }

    public void setCurrentData(Data curData) {
        this.curData = curData;
    }

    public Data getCurrentData() {
        return curData;
    }
}
