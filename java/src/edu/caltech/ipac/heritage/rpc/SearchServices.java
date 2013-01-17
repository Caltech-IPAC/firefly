package edu.caltech.ipac.heritage.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.rpc.ServiceLocator;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.heritage.data.entity.IRSInfoData;
import edu.caltech.ipac.visualize.plot.CoveragePolygons;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.util.List;
import java.util.Map;

/**
 * Date: Sep 26, 2008
 *
 * @author loi
 * @version $Id: SearchServices.java,v 1.32 2010/07/09 05:56:23 balandra Exp $
 */
public interface SearchServices extends RemoteService {

    Map<String, String> getAbstractInfo(int progId) throws RPCException;

    List<String> getObservers() throws RPCException;

    public CoveragePolygons getAorCoverage(int requestID);

    List<Integer> getBcdIds(String pbcdFile, List<Integer> fileRowIndices) throws RPCException ;

    public IRSInfoData getIRSFileInfo(PlotState request, ImageWorkSpacePt inIpt) throws RPCException;

    /**
     * Utility/Convenience class. Use SearchServices.App.getInstance() to access static instance of SearchServicesAsync
     */
    public static class App extends ServiceLocator<SearchServicesAsync> {
        private static final App locator = new App();

        private App() {
            super("rpc/SearchServices");
        }

        protected SearchServicesAsync createService() {
            return (SearchServicesAsync) GWT.create(SearchServices.class);
        }

        public static SearchServicesAsync getInstance() {
            return locator.getService();
        }
    }
}
