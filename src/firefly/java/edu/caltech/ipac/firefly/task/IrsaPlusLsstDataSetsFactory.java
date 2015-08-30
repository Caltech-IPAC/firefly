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
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.catalog.Catagory;
import edu.caltech.ipac.firefly.ui.catalog.Catalog;
import edu.caltech.ipac.firefly.ui.catalog.CatalogData;
import edu.caltech.ipac.firefly.ui.catalog.Proj;
import edu.caltech.ipac.firefly.util.DataSetParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.caltech.ipac.firefly.data.SpacialType.*;

/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */

/**
 * @author Trey Roby
 */
public class IrsaPlusLsstDataSetsFactory implements DataSetInfoFactory {


    private List<AsyncCallback<List<DataSetInfo>>> responseList = new ArrayList<AsyncCallback<List<DataSetInfo>>>(3);
    private List<DataSetInfo> dataSetInfoList = null;
    private DataSet originalDataSet= null;
    private boolean running= false;
    private static IrsaPlusLsstDataSetsFactory instance= null;


    public static IrsaPlusLsstDataSetsFactory getInstance() {
        if (instance==null) instance= new IrsaPlusLsstDataSetsFactory();
        return instance;
    }

    private IrsaPlusLsstDataSetsFactory() {};

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
            addTestData(retList,ds);
            return retList;
        }

        private void addTestData(List<DataSetInfo> dsList, DataSet ds) {



            // Add Image data support for some projects

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
            }

            // Add test LSST data support
           List<BaseTableData.RowData> lsstCatalogList= Arrays.asList(
                    /*
                    Catalog.makeTableRow("lsst","lsst-all","Science CCD Exposure", "SERVER","Science_Ccd_Exposure",
                                         "46","1998219","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","Deep Coadd", "SERVER","DeepCoadd",
                                         "34","5022","7200","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","Deep Source", "SERVER","DeepSource",
                                         "55","14670597","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","Deep Forced Source", "SERVER","DeepForcedSource",
                                         "59","3870141868","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","AvgForcedPhot", "SERVER","AvgForcedPhot",
                                         "28","14667816","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","AvgForcedPhotYearly", "SERVER","AvgForcedPhotYearly",
                                         "23","12615828948","1800","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","RefObject", "SERVER","RefObject",
                                         "30","15855006","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                    Catalog.makeTableRow("lsst","lsst-all","Science_Ccd_Exposure_coadd_r", "SERVER","Science_Ccd_Exposure_coadd_r",
                                         "41","666399","3600","","", "LSSTCatalogQuery","LSSTCatalogDD")
                                         */
                   // from SMM:
                   // - ignore all the tables named run_XXX, since the corresponding XXX view is the same thing
                   // with the addition of column aliases.
                   // - ignore deepCoadd_det and deepCoadd_mergeDet - they don’t seem to contain much other
                   // than an ID and position, and the RA/Dec columns (coord_ra, coord_dec) are always NULL.

                   // from smm_bremerton  table that have coord_ra, coord_dec columns
/*
                   for f in 'smm_bremerton.deepCoadd_forced_src' 'smm_bremerton.deepCoadd_meas' 'smm_bremerton.deepCoadd_ref' 'smm_bremerton.icSrc' 'smm_bremerton.src' 'sdss_dr9.fink_v5b'; do
                       curl "http://localhost:8661/db/v0/query?sql=SELECT+COUNT(*)+FROM+$f";echo $f
                   done
*/
                   Catalog.makeTableRow("lsst","lsst-all","src table", "SERVER","smm_bremerton.src",
                           "0","2731622","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                   Catalog.makeTableRow("lsst","lsst-all","icSrc table", "SERVER","smm_bremerton.icSrc",
                           "0","543128","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                   Catalog.makeTableRow("lsst","lsst-all","deepCoadd_ref from MergeMeasurementsTask", "SERVER","smm_bremerton.deepCoadd_ref",
                           "0","10983","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
//                    Catalog.makeTableRow("lsst","lsst-all","deepCoadd_mergeDet table", "SERVER","smm_bremerton.deepCoadd_mergeDet",
//                                         "0","0","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                   Catalog.makeTableRow("lsst","lsst-all","deepCoadd_meas from MeasureMergedCoaddSourcesTask", "SERVER","smm_bremerton.deepCoadd_meas",
                           "0","32949","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                   Catalog.makeTableRow("lsst","lsst-all","deepCoadd_forced_src table", "SERVER","smm_bremerton.deepCoadd_forced_src",
                           "0","32949","1800","","", "LSSTCatalogQuery","LSSTCatalogDD"),
//                    Catalog.makeTableRow("lsst","lsst-all","deepCoadd_det table", "SERVER","smm_bremerton.deepCoadd_det",
//                                         "0","0","3600","","", "LSSTCatalogQuery","LSSTCatalogDD"),
                   // from sdss_dr9
                   Catalog.makeTableRow("lsst","lsst-all","Reference catalog (sdss_dr9.fink_v5b)", "SERVER","sdss_dr9.fink_v5b",
                           "0","116324519","3600","","", "LSSTCatalogQuery","LSSTCatalogDD")


           );

            DataSetInfo dsInfo= new DataSetInfo("LSST", "LSST");
            Proj proj= new Proj("LSST");
            Catagory catagory= new Catagory("lsst-all");
            proj.addCatagory(catagory);
            TableData td= ds.getModel();
            for(BaseTableData.RowData row : lsstCatalogList) {
                catagory.addCatalog(new Catalog(row));
                td.addRow(row);
            }
            Set<SpacialType> set= new HashSet<SpacialType>(Arrays.asList(Cone, Box));
            dsInfo.setCatData(proj,set);
            dsList.add(dsInfo);

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

