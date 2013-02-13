package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableMeta;

import java.util.List;

/**
 * @author loi
 * $Id: SearchServices.java,v 1.12 2012/06/16 00:21:53 loi Exp $
 */
public interface SearchServices extends RemoteService {


    RawDataSet getRawDataSet(TableServerRequest request) throws RPCException;
    FileStatus getFileStatus(String filePath) throws RPCException;
    BackgroundReport packageRequest(DownloadRequest dataRequest) throws RPCException;
    BackgroundReport submitBackgroundSearch(TableServerRequest request, Request clientRequest, int waitMillis) throws RPCException;
    RawDataSet getEnumValues(String filePath) throws RPCException;


    BackgroundReport getStatus(String id);
    boolean cancel(String id);
    boolean cleanup(String id);
    SearchServices.DownloadProgress getDownloadProgress(String fileKey);
    boolean setEmail(String id, String email);
    boolean setEmail(List<String> idList, String email);
    boolean setAttribute(String id, BackgroundReport.JobAttributes attribute);
    boolean setAttribute(List<String> idList, BackgroundReport.JobAttributes attribute);
    String getEmail(String id);
    boolean resendEmail(List<String> idList, String email);
    String createDownloadScript(String id,
                                        String fname,
                                        String dataSource,
                                        List<BackgroundReport.ScriptAttributes> attributes);


    /**
     * returns a list of values from the given file path.
     * for security reason, the path is validated before the file is read.
     * @param filePath  path to the file
     * @param rows      rows of the data to collect
     * @param colName   only values from this column
     * @return
     * @throws RPCException
     */
    List<String> getDataFileValues(String filePath, List<Integer> rows, String colName) throws RPCException;

    /**
     * Utility/Convenience class.
     * Use SearchServices.App.getInstance() to access static instance of SearchServicesAsync
     */
    public static class App  extends ServiceLocator<SearchServicesAsync>{

        private static final App locator = new App();


        private App() {
            super("rpc/FireFly_SearchServices");
        }

        protected SearchServicesAsync createService() {

            NetworkMode mode= Application.getInstance().getNetworkMode();
            SearchServicesAsync retval= null;
            switch (mode) {
                case RPC:    retval= (SearchServicesAsync) GWT.create(SearchServices.class); break;
                case WORKER: retval= new SearchServicesJson(false); break;
                case JSONP:  retval= new SearchServicesJson(true); break;
            }
            return retval;
        }

        public static SearchServicesAsync getInstance() {
            return locator.getService();
        }

    }

    enum DownloadProgress { STARTING, WORKING, DONE, UNKNOWN, FAIL}
}
