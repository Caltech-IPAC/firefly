package edu.caltech.ipac.heritage.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
 * @version $Id: SearchServicesAsync.java,v 1.27 2010/06/30 21:53:43 roby Exp $
 */
public interface SearchServicesAsync {


    void getAbstractInfo(int progId, AsyncCallback<Map<String, String>> async);

    void getObservers(AsyncCallback<List<String>> async);

    void getAorCoverage(int requestID, AsyncCallback<CoveragePolygons> async);

    void getBcdIds(String pbcdFile, List<Integer> fileRowIndices, AsyncCallback<List<Integer>> async);

    void getIRSFileInfo(PlotState request, ImageWorkSpacePt inIpt, AsyncCallback<IRSInfoData> async);
}
