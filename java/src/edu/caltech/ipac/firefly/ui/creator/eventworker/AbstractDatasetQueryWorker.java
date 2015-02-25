/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Aug 27, 2010
 * Time: 2:58:50 PM
 */


/**
 * @author Trey Roby
 */
public abstract class AbstractDatasetQueryWorker<T> extends BaseEventWorker<T> {
    private List<String> argCols;
    private List<String> argsFromOriginalRequest;
    private List<String> headerParams;
    private List<Param> extraParams = new ArrayList<Param>(5);
    private TableServerRequest _lastReq = null;
    private DataSet _lastResults = null;
    private TableData.Row lastHLRow;

    public AbstractDatasetQueryWorker() {
        super(DatasetQueryCreator.DATASET_QUERY);
    }

    public void insertCommonArgs(Map<String, String> params) {
        setParams(params);
        if (params.containsKey(ID)) setID(params.get(ID));
        setEvents(StringUtils.asList(getParam(CommonParams.EVENTS_PARAM), ","));
        setArgCols(StringUtils.asList(getParam(CommonParams.ARG_COLS), ","));
        setHeaderParams(StringUtils.asList(getParam(CommonParams.ARG_HEADERS), ","));
        setQuerySources(StringUtils.asList(getParam(QUERY_SOURCE), ","));
        setExtraParams(GwtUtil.parseParams(params.get(CommonParams.EXTRA_PARAMS)));
        setDesc(getParam(CommonParams.TITLE));
    }

    public void setArgCols(List<String> argCols) {
        this.argCols = argCols;
    }

    public void setArgsFromOriginalRequest(List<String> argsFromOriginalRequest) {
        this.argsFromOriginalRequest = argsFromOriginalRequest;
    }

    public void setHeaderParams(List<String> headerParams) {
        this.headerParams = (headerParams == null) ? Collections.<String>emptyList() : headerParams;
    }

    public void setExtraParams(List<Param> extraParams) {
        this.extraParams.clear();
        if (extraParams != null) this.extraParams.addAll(extraParams);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) lastHLRow = null;
        super.setEnabled(enabled);
    }

    @Override
    protected boolean useEvent(WebEvent ev) {
        boolean retval= true;
        if ((ev.getSource() instanceof TablePanel)) {
            TablePanel t= (TablePanel) ev.getSource();
            retval= getQuerySources().contains(t.getName());
        }
        return retval;
    }

    public void handleEvent(WebEvent ev) {
        if ((ev.getSource() instanceof TablePanel)) {
            TablePanel table = (TablePanel) ev.getSource();
            if (!this.getQuerySources().contains(table.getName())) {
                return;
            }

            // all requirements satisfied... proceed
            TableData.Row hlRow = table.getTable().getHighlightedRow();

            if (hlRow == null) {
                lastHLRow = null;
                handleResults(null);
            } else {

                TableData.Row cHLRow = hlRow;

                if (cHLRow == null || (lastHLRow != null && lastHLRow.equals(cHLRow))) {
                    // don't do anything if it's the same row
                    return;
                }

                lastHLRow = cHLRow;

                TableServerRequest req = makeRequest();
                if (argCols == null) {
                    for (TableDataView.Column c : table.getDataset().getColumns()) {
                        req.setSafeParam(c.getName(), String.valueOf(hlRow.getValue(c.getName())));
                    }
                } else {
                    for (String s : argCols) {
                        req.setSafeParam(s, String.valueOf(hlRow.getValue(s)));
                    }
                }


                if (argsFromOriginalRequest != null) {
                    TableServerRequest originalReq= table.getDataModel().getRequest();
                    for (String s : argsFromOriginalRequest) {
                        if (originalReq.containsParam(s)) {
                            req.setSafeParam(s, originalReq.getParam(s));
                        }
                    }
                }

                TableMeta meta = table.getDataset().getMeta();
                if (headerParams!=null) {
                    for (String key : headerParams) {
                        if (meta.contains(key)) {
                            req.setSafeParam(key, meta.getAttribute(key));
                        }
                    }
                }

                if (req.equals(_lastReq)) {
                    processResults(_lastResults);
                } else {
                    callServer(req);
                }
            }
        } else if (ev.getData() instanceof TableServerRequest) {
            callServerWithRequest((TableServerRequest) ev.getData());
        }
    }

    protected TableServerRequest getLastRequest() { return _lastReq; }

    protected void callServerWithRequest(TableServerRequest searchReq) {
        TableServerRequest req = makeRequest();
        //_lastReq = req;
        ArrayList<Param> prms = new ArrayList<Param>(searchReq.getParams());
        prms.remove(new Param(TableServerRequest.ID_KEY, null));
        prms.remove(new Param(TableServerRequest.FILTERS, null));
        prms.remove(new Param(TableServerRequest.SORT_INFO, null));
        prms.remove(new Param(TableServerRequest.PAGE_SIZE, null));
        prms.remove(new Param(TableServerRequest.START_IDX, null));
        req.setParams(prms);
        if (_lastReq!=null && req.equals(_lastReq)) {
            processResults(_lastResults);
        } else {
            callServer(req);
        }
    }

    protected void callServerWithParams(Map<String, String> params) {
        TableServerRequest req = makeRequest();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            req.setSafeParam(entry.getKey(), entry.getValue());
        }
        _lastReq = req;
        callServer(req);
    }

    protected TableServerRequest makeRequest() {
        String searchId = getParam(CommonParams.SEARCH_PROCESSOR_ID);
        int pageSize = StringUtils.getInt(getParam(CommonParams.PAGE_SIZE), 10000);
        TableServerRequest req = new TableServerRequest(searchId);
        req.setPageSize(pageSize);
        req.setStartIndex(0);
        for (Param p : extraParams) req.setParam(p);
        return req;
    }

    private void processResults(final DataSet data) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                handleResults(convertResult(data));
            }
        });


    }

    private void callServer(final TableServerRequest req) {
        ServerTask task = new ServerTask<RawDataSet>() {
            public void onSuccess(RawDataSet result) {
                DataSet data = DataSetParser.parse(result);
                _lastResults = data;
                processResults(data);
            }

            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                SearchServices.App.getInstance().getRawDataSet(req, passAlong);
            }
        };
        task.start();
    }


    public abstract T convertResult(DataSet dataset);
}

