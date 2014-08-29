package edu.caltech.ipac.firefly.task;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.ImageIntersectionType;
import edu.caltech.ipac.firefly.data.MasterCatalogRequest;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.catalog.CatalogData;
import edu.caltech.ipac.firefly.ui.catalog.Proj;
import edu.caltech.ipac.firefly.util.DataSetParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */

/**
 * @author Trey Roby
 */
public class IrsaAllDataSetsTask extends ServerTask<RawDataSet> {


    private static List<AsyncCallback<List<DataSetInfo>>> responseList = new ArrayList<AsyncCallback<List<DataSetInfo>>>(3);
    private static List<DataSetInfo> dataSetInfoList = null;
    private static DataSet originalDataSet= null;
    private static boolean running= false;

    public static void getIrsaAllDataSets(Widget w, AsyncCallback <List<DataSetInfo>> response) {
        if (dataSetInfoList !=null) {
            if (response!=null) response.onSuccess(dataSetInfoList);
        }
        else {
            if (response!=null) responseList.add(response);
            if (!running) {
                new IrsaAllDataSetsTask(w).start();
            }
        }
    }

    public static boolean isIrsaAllDataSetsRetrieved() { return dataSetInfoList!=null;}
    public static List<DataSetInfo> getIrsaAllDataSetsImmediate() { return dataSetInfoList; }
    public static DataSet getOriginalDataSet() { return originalDataSet; }


    public void start() {
        running= true;
        super.start();
    }

    public IrsaAllDataSetsTask() { super();  }
    private IrsaAllDataSetsTask(Widget w) { super(w,"Loading Data...", false); }


    @Override
    public void onSuccess(RawDataSet rawDataSet) {
        running= false;
        if (rawDataSet!=null) {
            DataSet ds = DataSetParser.parse(rawDataSet);
            dataSetInfoList= convertToDataSetInfo(ds);
            originalDataSet= ds;
            for(AsyncCallback<List<DataSetInfo>> async : responseList) {
                async.onSuccess(dataSetInfoList);
            }
        }
    }

    @Override
    protected void onFailure(Throwable caught) {
        dataSetInfoList= null;
        running= false;
        for(AsyncCallback<List<DataSetInfo>> async : responseList) {
            async.onFailure(caught);
        }
    }

    @Override
    public void doTask(AsyncCallback <RawDataSet> passAlong) {
        if (dataSetInfoList==null) {
            SearchServicesAsync serv = SearchServices.App.getInstance();
            MasterCatalogRequest req = new MasterCatalogRequest();
            serv.getRawDataSet(req, passAlong);
        }
        else {
            passAlong.onSuccess(null);
        }
    }

    private static List<DataSetInfo> convertToDataSetInfo(DataSet ds) {
        CatalogData cData= new CatalogData(ds);
        List<DataSetInfo> retList= new ArrayList<DataSetInfo>(cData.getProjects().size());
        for(Proj proj  : cData.getProjects()) {
            DataSetInfo dsInfo= new DataSetInfo(proj.getShortProjName(), proj.getShortProjName());
            dsInfo.setCatData(proj);
            retList.add(dsInfo);
        }
        addTestData(retList); //todo remove, only for testing
        return retList;
    }

    private static void addTestData(List<DataSetInfo> dsList) {
        for(DataSetInfo dsInfo : dsList) {
            String d= dsInfo.getUserDesc();

            if (d.equals("2MASS") ||  d.equals("SPITZER") || d.equals("WISE") ) {
                addDummyImageData(dsInfo);
            }
//            if (d.equals("SPITZER")) {
//                dsInfo.setSpectrumProjInfo(new Object());
//            }
        }
    }

    private static void addDummyImageData(DataSetInfo dsInfo) {
        Set<SpacialType> stSet= new HashSet<SpacialType>(10);
        stSet.add(SpacialType.Cone);
        stSet.add(SpacialType.Box);
        stSet.add(SpacialType.MultiTableUpload);
//        stSet.add(SpacialType.MultiPrevSearch);
//        stSet.add(SpacialType.MultiPoints);

        Set<ImageIntersectionType> iiSet= new HashSet<ImageIntersectionType>(10);
        iiSet.add(ImageIntersectionType.ImageContainsTarget);
        iiSet.add(ImageIntersectionType.ImageCoversSearchRegion);
        iiSet.add(ImageIntersectionType.SearchRegionEnclosesImage);
        iiSet.add(ImageIntersectionType.ImageTouchesSearchRegion);
        dsInfo.setImageProjInfo(new Object(), stSet, iiSet,new Object());
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
