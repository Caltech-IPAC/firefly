package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.DataSetParser;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */


/**
 * @author Trey Roby
 */
public class DataSetTask extends ServerTask<RawDataSet> {

    private final TableServerRequest _req;
    private final AsyncCallback<DataSet> _cb;

    public static DataSetTask getData(TableServerRequest req,
                                      AsyncCallback<DataSet> cb) {
        DataSetTask task= new DataSetTask(req,cb);
        task.start();
        return task;
    }

    private DataSetTask( TableServerRequest req,
                         AsyncCallback<DataSet> cb) {
        super();
        _req= req;
        _cb= cb;
    }

    @Override
    public void onSuccess(RawDataSet rawDataSet) {
        DataSet ds= DataSetParser.parse(rawDataSet);
        _cb.onSuccess(ds);
    }


    @Override
    public void doTask(AsyncCallback<RawDataSet> passAlong) {
        SearchServicesAsync  serv= SearchServices.App.getInstance();
        serv.getRawDataSet(_req, passAlong);
    }



}