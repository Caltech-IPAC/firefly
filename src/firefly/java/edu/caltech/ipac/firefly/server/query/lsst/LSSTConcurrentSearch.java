package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by zhang on 4/4/17.
 * This class is served as a concurrent search module that can be extended to do a parallel search.
 * For any given number of threads, and the input files, it will do a concurrent search and then returns
 * a DataGroup object.
 *
 */
public class LSSTConcurrentSearch extends LSSTCataLogSearch {
    private int timeout  = Integer.parseInt(AppProperties.getProperty("lsst.database.timeoutLimit", "180"));

    public DataGroup doConcurrentSearch(TableServerRequest request, int nThread) throws ExecutionException, InterruptedException, IOException, EndUserException, TimeoutException, CloneNotSupportedException {

        //process the input table and read it to DataGroup.  First check if the file name is exist.
        if (!request.containsParam("filename")){
            throw new EndUserException("The request does not contain a input file name", null);
        }
        String filename= request.getParam("filename");
        DataGroup inDg = getInDataGroup(filename);
        //Read the data group to the DataObject array
        DataObject[] rows = inDg.values().toArray(new DataObject[0]);

        //Start the parallel search
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
        //process the results stored in the Future object array
        /*DM-9964 : TODO this is a temporary solution until the meta server is up
         Since the meta server is not up, the meta data is obtained from the column names.
         Thus, when the search result returns an empty data, the table can not be displayed due to
         the missing meta data.  To overcome this problem, when the search result is empty, set the
         DataObject to null.  In catalog search, the exception will be thrown to let users know
         that no data is found.  In the multi-object search, the empty result is skipped so that
         it can continue to search other data in the input file.
         */
        ArrayList<DataGroup> dgArrayList = new ArrayList<>();
        for (int i=0;i<list.size(); i++){
            // dataGroupArray[i]= list.get(i).get();
            try {
                if (list.get(i).get(timeout, TimeUnit.SECONDS) != null) {
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
        DataGroup[] dataGroupArray= dgArrayList.toArray(new DataGroup[0]);
        String catalog = request.getParam("table_name");
        return  dataGroupArrayToDataGroup(dataGroupArray,inDg, catalog);
    }

    /**
     * This method uploads the input file according to the filename.  The filename is passed from UI and is stored in the
     * temporary directory.
     *
     * @param filename
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     * @throws EndUserException
     */
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

    /**
     * This method terminates the thread in case is is running none stop.
     * @param executor
     */
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
     * This method creates a DataType array which includes the data type in the search results and the data types in
     * the input file.  The distance as a new data type is added.
     *
     * @param dg
     * @param inDg
     * @return
     * @throws CloneNotSupportedException
     */
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
            //NOTE, use new DataType(inTypes[i]..) to prevent from inTypes are modified.
            DataType dType = new DataType (inTypes[i].getKeyName(), inTypes[i].getDataType());
            dTypeArray.add(dType);
        }

        return  dTypeArray.toArray(new DataType[0]);
    }

    /**
     * This method is to compute the distance between the search target to the search result's (ra,dec).
     * @param row
     * @param center
     * @param catalog
     * @return
     */
    private double computeDistance(DataObject row, WorldPt center, String catalog){

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

    /**
     * This method is to compute the search target for each pair of (ra, dec)
     * @param inDg
     * @return
     */
    private WorldPt[] getCenterArray(DataGroup inDg ){
        DataObject[] rows =inDg.values().toArray(new DataObject[0]);
        WorldPt[] cArray = new WorldPt[rows.length];
        for (int i=0; i<rows.length; i++){
            cArray[i]=new WorldPt( (Double) rows[i].getDataElement("ra"), (Double) rows[i].getDataElement("dec"));

        }
        return cArray;
    }

    /**
     * This class is a java concurrent callable object.  It works as a worker.
     */
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
            /*
              If the major is in the input file, use the major as the radius.
              The major in the input file means semi-major and its unit is arcsec.  So it needs to be converted to degree.
              The unit in Radius passed is in degree.
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

        }

    }
}
