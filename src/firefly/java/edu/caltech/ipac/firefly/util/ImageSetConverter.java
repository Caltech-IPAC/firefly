package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.FailedRequestException;
import org.apache.commons.csv.CSVFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;


/**
 * Created by zhang on 9/27/17.
 * This class will read in the ImageSet information table (maybe from an API) and
 * the convert to a JSONObject.
 */
public class ImageSetConverter {
    private  final String masterTablePath = "/edu/caltech/ipac/firefly/resources/";
    private  final String irsaMasterTableName = "irsa-master-table.csv";
    private  final String lsstMasterTableName = "lsst-master-table.csv";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private JSONArray imageSetToJSONArray =new JSONArray();
    private static HashMap<String, String> paramMaps = new HashMap<>();

    public ImageSetConverter(String whichProject) throws IOException, FailedRequestException {

        //define the key mappings between the master table to the json file
        paramMaps.put("apiType", "Service");
        paramMaps.put("dataProductId", "surveyKey");
        paramMaps.put("waveBandId", "surveyKeyBand");
        paramMaps.put("waveBandDesc", "title");
        paramMaps.put("filter", "filter");



        String masterTable=null;
        switch (whichProject.toLowerCase()){
            case "irsa":
                masterTable= new String(masterTablePath+irsaMasterTableName);
                break;
            case "lsst":
                masterTable= new String(masterTablePath+lsstMasterTableName);
                break;
        }
        imageSetToJSONArray = createImageJSONArray(masterTable);
        //test the result
     /*   try {
            writeJSONArrayToFile(imageSetToJSONArray,  "/Users/zhang/IRSA_Dev/testingData/irsa-master-table.json");
        }
        catch(Exception e){
            e.printStackTrace();
        }*/
    }

    /**
     * This method reads in the master-table and then create a JSONArray based on the table data.
     * @return
     * @throws IOException
     * @throws FailedRequestException
     */
    private JSONArray createImageJSONArray(String masterTable) throws IOException, FailedRequestException {
        DataGroup inDg = getDataFromMasterTable(masterTable);
        DataType[] dataTypes = inDg.getDataDefinitions();
        List<DataObject> dataRows = inDg.values();


        for (int i=0; i<dataRows.size(); i++){
            imageSetToJSONArray.add(dataObjectToJSONObject(dataRows.get(i),dataTypes));
        }

        LOG.info("JSONArray is created successfully");


        return imageSetToJSONArray;
    }

    /**
     * This method finds the plotRequestParams.  It uses predefined map to map the data in the master table.
     * For example, the apiType in master table means Service for the plotRequest parameter.
     * @param row
     * @param dataTypes
     * @return
     */
    private HashMap<String, String> getMappedPlotRequestParam(DataObject row, DataType[] dataTypes){

        HashMap<String, String> params = new HashMap<>();

        String[] plotParamKeys =  paramMaps.keySet().toArray(new String[0]);
        for (int i=0; i<plotParamKeys.length; i++) {
            String key= plotParamKeys[i];
            for (int j = 0; j < row.size(); j++) {
                String colName = dataTypes[j].getKeyName();
                if(colName.equalsIgnoreCase(key)){
                    params.put( paramMaps.get(key), row.getFormatedData(dataTypes[j]));
                    break;
                }
            }

        }
        return params;
    }

    /**
     * This method process each row data in the master-table.  Each row makes one JSONObject. All rows make
     * an JSONArray of the size = number of rows
     * @param row
     * @param dataTypes
     * @return
     */
    private JSONObject dataObjectToJSONObject(DataObject row,  DataType[] dataTypes){
        JSONObject jsonObject = new JSONObject();


        for (int i=0; i<dataTypes.length; i++){
            Class cls = dataTypes[i].getDataType();
            Object obj = row.getDataElement(dataTypes[i]);
            if (cls==null){
              jsonObject.put(dataTypes[i].getKeyName(),  obj);
              continue;
            }

            switch (cls.getSimpleName().toLowerCase()){
                case "string":
                    String val = obj==null? null:((String) obj).trim();

                    if (dataTypes[i].getKeyName().equalsIgnoreCase("missionLabel (project)")){
                        jsonObject.put("project",  val);
                    }
                    else if ( dataTypes[i].getKeyName().equalsIgnoreCase("dataProductLabel") ){
                        jsonObject.put("subProject",  val);
                     }
                    else if ( dataTypes[i].getKeyName().equalsIgnoreCase("wavebandDesc") ){
                        jsonObject.put("title",  val);
                    }
                    else if ( dataTypes[i].getKeyName().equalsIgnoreCase("name") ){
                        jsonObject.put("JSON DM-12001",  val);
                    }
                    else {
                        jsonObject.put(dataTypes[i].getKeyName(),  val);
                    }
                    break;
                case "float":
                    float fval = obj==null? 0.1f:  ((Float)obj).floatValue();
                    jsonObject.put(dataTypes[i].getKeyName(), fval);
                    break;
                case "integer":
                    int ival = obj==null? 0:  ((Integer)obj).intValue();
                    jsonObject.put(dataTypes[i].getKeyName(), ival);
                    break;
                case "double":
                    double dval = obj==null? 0.1:  ((Double)obj).doubleValue();
                    jsonObject.put(dataTypes[i].getKeyName(), dval );
                    break;

                default:
                  jsonObject.put(dataTypes[i].getKeyName(),  obj);
                  break;
           }


        }

        HashMap<String, String> params = getMappedPlotRequestParam(row,dataTypes);
        String[] keys = params.keySet().toArray(new String[0]);
        JSONObject plotReqParams= new JSONObject();
        plotReqParams.put("type", "SERVICE");
        for(int i=0; i<params.size(); i++){
            plotReqParams.put(keys[i], params.get(keys[i]));
        }
        jsonObject.put( "plotRequestParams",plotReqParams );
        return jsonObject;

    }
    DataGroup getDataFromMasterTable(String masterTableName) throws IOException, FailedRequestException {


        InputStream inf= ImageSetConverter.class.getResourceAsStream(masterTableName);
        DataGroup dg = DsvToDataGroup.parse(inf, CSVFormat.DEFAULT);
        return dg;

    }

    public  JSONArray getImageSetToJSONArray(){

        return imageSetToJSONArray;
    }


    public static void writeJSONArrayToFile(JSONArray jarray, String outJsonFile) throws Exception {

        JSONObject obj = new JSONObject();
        obj.put("Result array", jarray);

        // try-with-resources statement based on post comment below :)
        try (FileWriter file = new FileWriter(outJsonFile)) {
            file.write(obj.toJSONString());
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + obj);
        }
    }
}