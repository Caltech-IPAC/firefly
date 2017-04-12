package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ConcurrentSearchUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.WorldPt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;



/**
 * Created by zhang on 3/16/17.
 * The LSSTMultiObjectSearch reads in the .tbl or .csv input file and then does a parallel search.  The search results
 * is an array of DataGroup.  The array of DataGroup is combined to one big data group.  The redundancy  is nto removed.
 *
 * The input file can be a .tbl or .csv format.
 * 1. If the input table contains only ra and dec, the radius in the UI will be used, the search type is cone.
 * 2. If the input table contains ra, dec, major,  the major will be used as a radius, the search type is cone.
 * 3. If the input table contains ra, dec, major, ratio, the major is used as the semi-major and the serach type is
 *  elliptical. Since there is no angle column, the angle will be 0.
 * 4. If the input table contains ra, dec, major, ratio, angle, the search type is elliptical and the angle will be used.
 * NOTE:
 * The unit for ra, dec and angle is in degree and the major is in arcsec, major means semi-major
 */

@SearchProcessorImpl(id = "LSSTMultiObjectSearch")
public class LSSTMultiObjectSearch extends LSSTCataLogSearch{

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        try {

            DataGroup dg =  getSearchResult(request, request.getParam("filename"));
            dg.shrinkToFitData();
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg);
            return  outFile;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }

    }

    private DataGroup getSearchResult(TableServerRequest request, String filename) throws ExecutionException, InterruptedException, IOException, EndUserException, TimeoutException, CloneNotSupportedException {
        int nThread = request.containsParam("nThread")? Integer.parseInt(request.getParam("nThread")) :2;

        DataGroup inDg = ConcurrentSearchUtil.getInDataGroup(filename);
        DataObject[] rows = inDg.values().toArray(new DataObject[0]);

        Callable<DataGroup>[] workers = new SearchOneRowCallableThread[rows.length];
        for (int i=0; i<rows.length; i++){
            workers[i] = new SearchOneRowCallableThread(rows[i], request);
        }

        String tableName = request.getParam("table_name");
       return ConcurrentSearchUtil.doSearch(workers, nThread, inDg, getRA(tableName), getDEC(tableName));

    }

    /**
     * This is a Java concurrent callable class which starts a parallel threads to get a work done.
     * Each callable return a future object which contains the search result.  In this case, the
     * future object is a DataGroup.
     */
    class SearchOneRowCallableThread implements Callable<DataGroup> {

        private DataObject inRow;
        private String tableName;
        private String radius;
        private String  constraints;
        private String columnNames;
        private String catTable;

        SearchOneRowCallableThread(DataObject dataObject,TableServerRequest request) throws InterruptedException, ExecutionException, EndUserException, IOException {
            tableName = request.getParam("table_name");

            constraints = request.getParam(CatalogRequest.CONSTRAINTS);
            columnNames = request.getParam(CatalogRequest.SELECTED_COLUMNS);
            catTable = request.getParam(CatalogRequest.CATALOG);
            /*If the major is in the input file, use the major/2 as the radius.  Since the major is in
              archsec convert the major to degree
            */
            radius = dataObject.containsKey("major") && dataObject.getDataElement("major")!=null?
                  String.valueOf(Double.parseDouble(dataObject.getDataElement("major").toString())/3600.0)
                  :request.getParam("radius");
            inRow =dataObject;

        }
        String getMethod(){
            String major =  inRow.containsKey("major") && inRow.getDataElement("major")!=null? inRow.getDataElement("major").toString():"";
            String ratio =  inRow.containsKey("ratio") && inRow.getDataElement("ratio")!=null? inRow.getDataElement("ratio").toString():"";

            //Search a circular area around each of two positions.
            if (ratio.length()==0){
                return "CONE";
            }
            //Search an elliptical area around each of two positions.
            if ( major.length()!=0  && ratio.length()!=0){
                return "ELIPTICAL";
            }

            return "NotDefined";

        }


        @Override
        public DataGroup call() throws Exception {

            TableServerRequest req = new TableServerRequest("LSSTCataLogSearch");

            req.setParam("UserTargetWorldPt", inRow.getDataElement("ra").toString()+";"+ inRow.getDataElement("dec").toString());
            req.setParam(CatalogRequest.RADIUS, radius);
            req.setParam("table_name", tableName);
            req.setParam(CatalogRequest.CONSTRAINTS, constraints);
            req.setParam(CatalogRequest.CATALOG, catTable);
            req.setParam(CatalogRequest.SELECTED_COLUMNS, columnNames);
            String method = getMethod();
            switch (method){
                case  "CONE":
                    req.setParam("SearchMethod", "CONE");
                     break;
                case "ELIPTICAL":
                    req.setParam("ratio", inRow.getDataElement("ratio").toString());
                     String angle =  inRow.containsKey("angle")? inRow.getDataElement("angle").toString():"";
                    if (angle==null || angle.length()==0){
                        angle="0";
                    }
                    req.setParam("posang", angle);
                    req.setParam("SearchMethod", "ELIPTICAL");
                    break;
                default:
                    throw new DataAccessException(method + "is not supported");
            }

            DataGroup dg = getDataFromURL(req);
                    //ConcurrentSearchUtil.addInputToOutputDataGroup(getDataFromURL(req), inRow, getRA(tableName), getDEC(tableName));
            dg.shrinkToFitData();
            return dg;



        }

    }
}
