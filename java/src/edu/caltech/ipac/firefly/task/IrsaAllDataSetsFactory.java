/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
public class IrsaAllDataSetsFactory implements DataSetInfoFactory {


    private List<AsyncCallback<List<DataSetInfo>>> responseList = new ArrayList<AsyncCallback<List<DataSetInfo>>>(3);
    private List<DataSetInfo> dataSetInfoList = null;
    private DataSet originalDataSet= null;
    private boolean running= false;
    private static IrsaAllDataSetsFactory instance= null;


    public static IrsaAllDataSetsFactory getInstance() {
        if (instance==null) instance= new IrsaAllDataSetsFactory();
        return instance;
    }

    private IrsaAllDataSetsFactory() {};

    public void getAllDataSets(Widget w, AsyncCallback<List<DataSetInfo>> response) {
        if (dataSetInfoList !=null) {
            if (response!=null) response.onSuccess(dataSetInfoList);
        }
        else {
            if (response!=null) responseList.add(response);
            if (!running) {
                new GetData(w).start();
            }
        }
    }

    public boolean isAllDataSetsRetrieved() { return dataSetInfoList!=null;}
    public List<DataSetInfo> getAllDataSetsImmediate() { return dataSetInfoList; }
    public DataSet getOriginalDataSet() { return originalDataSet; }




    private class GetData extends ServerTask<RawDataSet> {

        public void start() {
            running= true;
            super.start();
        }

        public GetData() { super();  }
        private GetData(Widget w) { super(w,"Loading Data...", false); }

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

        private List<DataSetInfo> convertToDataSetInfo(DataSet ds) {
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

        private void addTestData(List<DataSetInfo> dsList) {
            for(DataSetInfo dsInfo : dsList) {
                String d= dsInfo.getUserDesc();

                if (d.equals("SPITZER") ) {
                    Set<SpacialType> stSet= new HashSet<SpacialType>(10);
                    stSet.add(SpacialType.Cone);
                    addImageData(dsInfo, stSet);
                }

                else if (d.equals("2MASS") || d.equals("WISE") ) {
                    Set<SpacialType> stSet= new HashSet<SpacialType>(10);
                    stSet.add(SpacialType.IbeSingleImage);
                    stSet.add(SpacialType.IbeMultiTableUpload);
                    addImageData(dsInfo, stSet);
                }

//            if (d.equals("SPITZER")) {
//                dsInfo.setSpectrumProjInfo(new Object());
//            }
            }
        }

        private void addImageData(DataSetInfo dsInfo, Set<SpacialType> stSet) {
//        stSet.add(SpacialType.Cone);
//        stSet.add(SpacialType.Box);
//        stSet.add(SpacialType.IbeSingleImage);
//        stSet.add(SpacialType.MultiTableUpload);
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

}

