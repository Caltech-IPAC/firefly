package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.server.ServerContext;
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
 * Created by zhang on 4/11/17.
 * This is a utility class which can be used for concurrent object search
 */
public class ConcurrentSearchUtil {
    private static int timeout  = Integer.parseInt(AppProperties.getProperty("lsst.database.timeoutLimit", "180"));

    /**
     * This method does the multi-thread search based on the input Callable array and number of thread.
     *
     * @param workers - a Callable object array
     * @param nThread - number of threads to be started
     * @param inDg - the input DataGroup
     * @param raCol - the ra column name in the search result table
     * @param decCol - the decl column name in the search result table
     * @return
     * @throws TimeoutException
     * @throws CloneNotSupportedException
     */

    public static DataGroup doSearch( Callable<DataGroup>[] workers, int nThread, DataGroup inDg,
                 String raCol, String decCol) throws TimeoutException, CloneNotSupportedException {


        ExecutorService executor =  Executors.newFixedThreadPool(nThread);
        List<Future<DataGroup>> list = new ArrayList<>();
        for (int i=0; i<workers.length; i++){
            try{
                Future<DataGroup> submit = executor.submit(workers[i] );
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

        //terminate java executor if it exists
        terminateThreads(executor);

        //add the input column data each data group in the array and then combine the data group array to one data group
        return joinDataGroupArrayToDataGroup(dgArrayList.toArray(new DataGroup[0]), inDg, raCol, decCol);

    }

    /**
     * This method terminates the threads.  It provents from running the threads forever
     * @param executor
     */
    private static void terminateThreads( ExecutorService executor){

        try {

            Logger.info("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Logger.info("tasks interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                Logger.info("cancel non-finished tasks");
            }
            executor.shutdownNow();
            Logger.info("shutdown finished");
        }
    }

    /**
     * This method creates a DataType array which contains the data type definitions from the input DataGroup and
     * the data type definitions in the searched DataGroup.
     * @param dg
     * @param inDataTypes
     * @return
     * @throws CloneNotSupportedException
     */
   private static  DataType[] getJoinedDataTypes(DataGroup dg, DataType[] inDataTypes) throws CloneNotSupportedException {


        ArrayList<DataType> dTypeArray = new ArrayList<>();
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

    /**
     * This method calculates the array of center in the input DataGroup.
     * @param inDg
     * @return
     */
    private static WorldPt[] getCenterArray(DataGroup inDg ){
        DataObject[] rows =inDg.values().toArray(new DataObject[0]);
        WorldPt[] cArray = new WorldPt[rows.length];
        for (int i=0; i<rows.length; i++){
            cArray[i]=new WorldPt( (Double) rows[i].getDataElement("ra"), (Double) rows[i].getDataElement("dec"));

        }
        return cArray;
    }

    /**
     * This method creates a final output DataGroup.  To create the final DataGroup:
     *    1. for each DataGroup in the array, add the distance and the input column information
     *    2. join the DataGroup array to one DataGroup.
     * @param dgArray
     * @param inDg
     * @param raCol
     * @param decCol
     * @return
     * @throws CloneNotSupportedException
     */
    private static DataGroup joinDataGroupArrayToDataGroup(DataGroup[] dgArray, DataGroup inDg,
                               String raCol, String decCol) throws CloneNotSupportedException {


        WorldPt[] centers =getCenterArray(inDg);
        DataType[] inTypes = inDg.getDataDefinitions();

        DataType[] joinedDataTypes = getJoinedDataTypes( dgArray[0], inTypes);

        int index = inTypes.length+1;

        DataGroup joinedResult = new DataGroup(null, joinedDataTypes);

        ////join the DataGroup array
        for (int i=0; i<dgArray.length; i++){
            ////add the distance and the input columns
            for (int j=0; j<dgArray[i].size(); j++){
                if (dgArray[i].size()==0) continue;
                //get the row DataObject in the search result of dgArray[i]
                DataObject rowDataObject =dgArray[i].get(j);

                //create a new DataObject bounded to the final DataGroup
                DataObject dataObject = new DataObject(joinedResult);
                //dataObject.setDataElement(joinedDataTypes[0], centers[i]);
                //add the distance column to the dataObject
                dataObject.setDataElement(joinedDataTypes[0],computeDistance(rowDataObject, centers[i], raCol, decCol));

                //add the data columns from the input table
                for (int ii=0; ii<inTypes.length; ii++){
                    dataObject.setDataElement(joinedDataTypes[ii+1], inDg.get(i).getDataElement(inTypes[ii]));
                }
                //add the data from the searching result
                for (int k=0; k<rowDataObject.size(); k++) {
                    dataObject.setDataElement(joinedDataTypes[k+index], rowDataObject.getDataElement(joinedDataTypes[k]) );
                }
                joinedResult.add(dataObject);

            }
        }
        joinedResult.shrinkToFitData();
        return joinedResult;
    }

    /**
     * This method loads the input DataGroup for the given filename
     * @param filename
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     * @throws EndUserException
     */
    public static DataGroup getInDataGroup(String filename) throws ExecutionException, InterruptedException, IOException,
            EndUserException {
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
     * This method computes the distance for reach (ra,dec) pair.
     * @param row - searched DataObject
     * @param center - WorldPt, the center calculated based on the input (ra, dec)
     * @param raCol - the name of ra column
     * @param decCol - the name of the dec column
     * @return
     */
    private static double computeDistance( DataObject row, WorldPt center, String raCol, String decCol){

        WorldPt point = new WorldPt((Double) row.getDataElement(raCol),(Double) row.getDataElement(decCol ) );
        return  VisUtil.computeDistance(center, point);
    }

}
