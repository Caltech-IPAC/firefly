/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;

import java.util.List;

/**
 * Date: Feb 11, 2009
 *
 * @author loi
 * @version $Id: Loader.java,v 1.5 2010/04/13 16:59:25 roby Exp $
 */
public interface Loader <Data> {
    public static final String SYS_FILTER_CHAR = "#";

    List<String> getFilters();
    void setFilters(List<String> filters);

    int getPageSize();
    void setPageSize(int pageSize);

    void setSortInfo(SortInfo sortInfo);
    SortInfo getSortInfo();

    /**
     * get data backed by this data source, but do not save state.
     * @param req
     * @param callback
     */
    void getData(TableServerRequest req, final AsyncCallback<Data> callback);

    void load(int offset, int pageSize, AsyncCallback<Data> callback);

    void onLoad(Data result);

    String getSourceUrl();

    Data getCurrentData();
    void setCurrentData(Data data);

    TableServerRequest getRequest();

}
