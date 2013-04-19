package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.WebUtil;

import java.util.Map;


/**
 * Date: Feb 11, 2009
 *
 * @author loi
 * @version $Id: PagingDataSetLoader.java,v 1.23 2012/10/26 14:39:18 tatianag Exp $
 */
public abstract class PagingDataSetLoader extends AbstractLoader<TableDataView> {
    private TableServerRequest request;
    boolean isStandAlone = false;

    protected PagingDataSetLoader(TableServerRequest request) {
        this.request = request;
//        setPageSize(request.getPageSize());
//        setFilters(request.getFilters());
//        setSortInfo(request.getSortInfo());
    }

    /**
     *
     * @param req
     * @param callback
     */
    public void getData(TableServerRequest req, final AsyncCallback<TableDataView> callback) {

        AsyncCallback<RawDataSet> cb = new AsyncCallback<RawDataSet>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(RawDataSet result) {
                DataSet dataset = DataSetParser.parse(result);
                callback.onSuccess(dataset);
            }
        };
        doLoadData(req, cb);
    }

    public void load(int offset, int pageSize, final AsyncCallback<TableDataView> callback) {

        request.setStartIndex(offset);
        request.setPageSize(pageSize);
        request.setFilters(getFilters());
        request.setSortInfo(getSortInfo());

        AsyncCallback<TableDataView> cb = new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(TableDataView result) {
                if(getCurrentData() == null) {
                    setCurrentData(result);
                }
                TableDataView tv = getCurrentData();
                tv.setModel(result.getModel());
                tv.setTotalRows(result.getTotalRows());
                tv.setStartingIdx(result.getStartingIdx());

                if (result.getMeta() != null) {
                    Map<String, String> attribs = tv.getMeta().getAttributes();
                    tv.setMeta(result.getMeta());
                    if (attribs != null) {
                        for(String k : attribs.keySet()) {
                            tv.getMeta().setAttribute(k, attribs.get(k));
                        }
                    }

                }
                onLoad(result);
                callback.onSuccess(result);
            }
        };
        getData(request, cb);
    }

    public TableServerRequest getRequest() {
        return request;
    }

    @Override
    public void onLoad(TableDataView result) {
        // do not replace the tableview
    }

    public String getSourceUrl() {
        // sets start index to 0, page size to infinity
        return WebUtil.getTableSourceUrl(request);
    }

    protected abstract void doLoadData(TableServerRequest request, AsyncCallback<RawDataSet> callback);

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