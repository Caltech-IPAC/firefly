package edu.caltech.ipac.firefly.server.query.lsst;


import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.List;

/**
 * Created by zhang on 10/12/16.
 * This search processor is searching the MetaData (or Data Definition from DAX database, then save to a
 * IpacTable file.
 */
@SearchProcessorImpl(id = "LSSTMetaSearch")
public class LSSTMetaSearch  extends IpacTablePartProcessor{
     private static final Logger.LoggerImpl _log = Logger.getLogger();


    /* private  static TableMeta getMeta(TableServerRequest request) throws IOException, ParseException {
        //TODO return the search data and then process it to a TableMeta data
         return null;
     }*/

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

/*
        String ddTable = request.getParam(CatalogRequest.CATALOG);
         output file will be in json format
        File file = createFile(request, ".json");*/


        File file = new File(request.getParam("table_path")+request.getParam("table_name")+".json");
        try {
            DataGroup dg = getMetaData(file);
            File outFile = createFile(request, ".tbl");
            dg.shrinkToFitData();
            DataGroupWriter.write(outFile, dg, 0);
            return outFile;

        }
        catch (ParseException e){
            _log.error("load table failed");
            e.getStackTrace();
        }
        return null;

    }


     private DataType[] getDataType(JSONObject firstRow){
         String[] colNames = (String[]) firstRow.keySet().toArray(new String[0]);
         DataType[] dataTypes = new DataType[colNames.length+2];//add unit and descriptions
         for (int i=0; i<colNames.length; i++){
             dataTypes[i] = new DataType(colNames[i],  (new String()).getClass());

         }

         dataTypes[colNames.length] =new DataType("Unit",  (new String()).getClass());
         dataTypes[colNames.length+1] =new DataType("Description",  (new String()).getClass());
         return dataTypes;

     }
    /**
     * This method reads the json file from DAX and process it. The output is a DataGroup of the Meta data
     * @param file
     * @return
     * @throws IOException
     * @throws ParseException
     */

    private DataGroup getMetaData(File file) throws IOException, ParseException {

        JSONParser parser = new JSONParser();

        Object obj = parser.parse(new FileReader(file ));
        JSONArray metaData = (JSONArray) obj;


        DataType[] dataTypes = getDataType( (JSONObject) metaData.get(0) );
        DataGroup dg = new DataGroup(file.getName(), dataTypes);

        for (int i=0; i<metaData.size(); i++){
            JSONObject value = (JSONObject) metaData.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataTypes.length-2; j++){
                row.setDataElement(dataTypes[j], value.get(dataTypes[j].getKeyName()));
            }
            row.setDataElement(dataTypes[dataTypes.length-2], "dummyUnit1");
            row.setDataElement(dataTypes[dataTypes.length-1], "description of "+value.get(dataTypes[0].getKeyName() ) );
            dg.add(row);
        }

        return dg;
    }



    @Override
    protected String getFilePrefix(TableServerRequest request) {
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            return request.getRequestId();
        } else {
            return catTable+"-dd-";
        }

    }
    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

    }

   File loadDataFileDummy(TableServerRequest request) throws IOException, DataAccessException {


           try {
               File file = new File(request.getParam("inFileName"));
               DataGroup dg = getMetaData(file);
              // File outFile = createFile(request, ".json");
               File oFile = new File(request.getParam("outFileName"));
               dg.shrinkToFitData();
               DataGroupWriter.write(oFile, dg, 0);
               return oFile;
           }
           catch (ParseException e){
               e.getStackTrace();
           }

           return null;
   }


    public static void main(String[] args) throws IOException, ParseException, DataAccessException {

        String jsonFileName = "RunDeepSourceDD";

        String dataPath = "/hydra/cm/firefly_test_data/DAXTestData/";

        TableServerRequest request = new TableServerRequest("DummyDD");
        request.setParam("inFileName",dataPath +jsonFileName+".json" );
        String outFileName = dataPath + "output_" + jsonFileName+".tbl";
        request.setParam("outFileName", outFileName);
        LSSTMetaSearch lsstMeta = new LSSTMetaSearch();


        File file = lsstMeta.loadDataFileDummy(request);
        System.out.println("done" + file.getAbsolutePath());



    }
}
