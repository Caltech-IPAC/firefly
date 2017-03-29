package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
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
    private int timeout  = Integer.parseInt(AppProperties.getProperty("lsst.database.timeoutLimit", "180"));

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        try {
            String catalog = request.getParam("table_name");
            String filename= request.getParam("filename");
            DataGroup inDg = getInDataGroup(filename);
            DataGroup[] dataGroupArray =  getSearchResult(request, inDg);
            DataGroup dg =  dataGroupArrayToDataGroup(dataGroupArray,inDg, catalog);
            dg.shrinkToFitData();
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg);
            return  outFile;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }

    }

    private DataGroup getInDataGroup(String filename) throws ExecutionException, InterruptedException, IOException, EndUserException {
        boolean badParam = true;
        if (!StringUtils.isEmpty(filename)) {
            File uploadFile = ServerContext.convertToFile(filename);
            if (uploadFile.canRead()) {
                //can read .tbl or .csv file
                return  DataGroupReader.readAnyFormat(uploadFile);
            }
        }
        if (badParam) {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + filename);
        }
        return null;
    }

    private void terminateThreads( ExecutorService executor){

        try {
            LOGGER.info("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            LOGGER.info("tasks interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                LOGGER.info("cancel non-finished tasks");
            }
            executor.shutdownNow();
            LOGGER.info("shutdown finished");
        }
    }
    /**
     * This method is calling the concurrent worker to do the search.  it depends on the thread, nThread, to create the
     * workers.
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     */
   private  DataGroup[]  getSearchResult(TableServerRequest request, DataGroup inDg) throws ExecutionException, InterruptedException, IOException, EndUserException, TimeoutException {


       DataObject[] rows = inDg.values().toArray(new DataObject[0]);

        int nThread = request.containsParam("nThread")? Integer.parseInt(request.getParam("nThread")) :2;
        ExecutorService executor =  Executors.newFixedThreadPool(nThread);
        List<Future<DataGroup>> list = new ArrayList<>();


        for (int i=0; i<rows.length; i++){
            try {
                SearchOneRowCallableThread worker = new SearchOneRowCallableThread(rows[i], request);
                Future<DataGroup> submit = executor.submit(worker );

                while (!submit.isDone()) {
                }
                list.add(submit);

            } catch (RejectedExecutionException re) {
               re.printStackTrace();
            }
        }


       // DataGroup[] dataGroupArray = new DataGroup[rows.length];
       //DM-9964 : TODO this is a temporary solution until the meta server is up
       ArrayList<DataGroup> dgArrayList = new ArrayList<>();
       for (int i=0;i<list.size(); i++){
           // dataGroupArray[i]= list.get(i).get();
           try {
               if (list.get(i).get(timeout, TimeUnit.SECONDS) != null) {
                //   if (list.get(i).get() != null) {
                   dgArrayList.add(list.get(i).get());
               }
           }
           catch (InterruptedException e) {
               e.printStackTrace();
           } catch (ExecutionException e) {
               e.printStackTrace();
           }
        }
       // return dataGroupArray;

       //terminate java executor if it exists
       terminateThreads(executor);

       return dgArrayList.toArray(new DataGroup[0]);

    }

    /**
     * This method is to test if the row with the same id is already in the DataGroup.
     * It will remove the duplications
     * @param dg
     * @param inDg
     * @return
     * @throws CloneNotSupportedException
     */
/*
    private boolean dataObjetExist(DataGroup dg, DataObject row){

        boolean exist =false;
        Comparator<DataObject> comparator =new Comparator<DataObject>() {
            public int compare(DataObject dataObject, DataObject dataObject1) {
                return ((Long) dataObject.getDataElement("id")).compareTo((Long) dataObject1.getDataElement("id"));
            }
        };
        for (int i=0; i<dg.size(); i++){
            if (comparator.compare(row, dg.get(i))==0){
                exist = true;
                break;
            }
        }
        return exist;
    }*/

    private DataType[] getDataType(DataGroup dg, DataGroup inDg) throws CloneNotSupportedException {


        DataType[] inDataTypes=inDg.getDataDefinitions();
        ArrayList<DataType> dTypeArray = new ArrayList<>();
        //dTypeArray.add(new DataType("target", WorldPt.class));
        dTypeArray.add(new DataType("distance",  Double.class));

        for (int i=0; i<inDataTypes.length; i++){
            DataType dType = (DataType) inDataTypes[i].clone();
            dType.setKeyName(inDataTypes[i].getKeyName()+"_01");
            dTypeArray.add(dType);
        }

        DataType[] inTypes = dg.getDataDefinitions();
        for (int i=0; i<dg.getDataDefinitions().length; i++){
            DataType dType = new DataType (inTypes[i].getKeyName(), inTypes[i].getDataType());
            dTypeArray.add(dType);
        }

        return  dTypeArray.toArray(new DataType[0]);
    }
    private double computeDistance( DataObject row, WorldPt center, String catalog){

        WorldPt point = new WorldPt((Double) row.getDataElement(getRA(catalog)),(Double) row.getDataElement(getDEC(catalog) ) );

        return  VisUtil.computeDistance(center, point);
    }


    /**
     * This method joins all the DataGroup Array to one DataGroup
     * @param dgArray
     * @return
     */
    private DataGroup dataGroupArrayToDataGroup(DataGroup[] dgArray,DataGroup inDg, String catalog) throws CloneNotSupportedException {


        WorldPt[] centers =getCenterArray(inDg);
        DataType[] inTypes = inDg.getDataDefinitions();

        DataType[] joinedDataTypes = getDataType( dgArray[0], inDg);

        int index = inTypes.length+1;

        DataGroup joinedResult = new DataGroup("joinedDataGroup", joinedDataTypes);

        for (int i=0; i<dgArray.length; i++){
            for (int j=0; j<dgArray[i].size(); j++){
                if (dgArray[i].size()==0) continue;
                DataObject row =dgArray[i].get(j);
                DataObject dataObject = new DataObject(joinedResult);
                double distance = computeDistance(row, centers[i], catalog);
                //dataObject.setDataElement(joinedDataTypes[0], centers[i]);
                dataObject.setDataElement(joinedDataTypes[0], new Double(distance));

                //add the data columns from the input table
                for (int ii=0; ii<inTypes.length; ii++){
                    dataObject.setDataElement(joinedDataTypes[ii+1], inDg.get(i).getDataElement(inTypes[ii]));
                }
                //add the data from the searching result table
                for (int k=0; k<row.size(); k++) {
                    dataObject.setDataElement(joinedDataTypes[k+index], row.getDataElement(joinedDataTypes[k]) );
                }
                joinedResult.add(dataObject);

            }
        }
        joinedResult.shrinkToFitData();
        return joinedResult;
    }
    private WorldPt[] getCenterArray(DataGroup inDg ){
        DataObject[] rows =inDg.values().toArray(new DataObject[0]);
        WorldPt[] cArray = new WorldPt[rows.length];
        for (int i=0; i<rows.length; i++){
            cArray[i]=new WorldPt( (Double) rows[i].getDataElement("ra"), (Double) rows[i].getDataElement("dec"));

        }
        return cArray;
    }


    class SearchOneRowCallableThread implements Callable<DataGroup> {

        private DataObject row;
        private String tableName;
        private String radius;
        private String  constraints;
        private String columnNames;
        private String catTable;
        SearchOneRowCallableThread(DataObject dataObject,TableServerRequest request){
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
            row=dataObject;


        }
        String getMethod(){
            String major =  row.containsKey("major") && row.getDataElement("major")!=null? row.getDataElement("major").toString():"";
            String ratio =  row.containsKey("ratio") && row.getDataElement("ratio")!=null?row.getDataElement("ratio").toString():"";

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

            req.setParam("UserTargetWorldPt", row.getDataElement("ra").toString()+";"+row.getDataElement("dec").toString());
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
                    req.setParam("ratio", row.getDataElement("ratio").toString());
                     String angle =  row.containsKey("angle")?row.getDataElement("angle").toString():"";
                    if (angle==null || angle.length()==0){
                        angle="0";
                    }
                    req.setParam("posang", angle);
                    req.setParam("SearchMethod", "ELIPTICAL");
                    break;
                default:
                    throw new DataAccessException(method + "is not supported");


            }
            DataGroup dg =  getDataFromURL(req);
            dg.shrinkToFitData();
            return dg;

           /* SearchManager sm = new SearchManager();
            try {
                DataGroupPart dgp = sm.getDataGroup(req);
                //return dgp.getData();
                DataGroup dg = dgp.getData();
                return dg;
            } catch (Exception e) {
                throw new DataAccessException("Unable to get metadata", e);
            }
*/


        }

    }
}
