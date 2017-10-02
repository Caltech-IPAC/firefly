package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.FailedRequestException;
import org.apache.commons.csv.CSVFormat;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    private static HashMap<String, String> paramMaps = new HashMap<>();
    List<ImageMasterDataEntry>  retList= new ArrayList();

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
        retList=createDataList(masterTable);

    }

    /**
     * This method reads in the master-table and then create a Map List based on the table data.
     * @return
     * @throws IOException
     * @throws FailedRequestException
     */
    public List createDataList(String masterTable) throws IOException, FailedRequestException {

        DataGroup inDg = getDataFromMasterTable(masterTable);
        DataType[] dataTypes = inDg.getDataDefinitions();
        List<DataObject> dataRows = inDg.values();


        for(DataObject obj : dataRows) {
            Map<String,Object> map= new HashMap<>();
            map.putAll(dataObjectToMap(obj,dataTypes ));
            retList.add( ImageMasterDataEntry.makeFromMap(map));
        }
        return retList;
    }

    /**
     * This method process each row data in the master-table.  Each row makes one JSONObject. All rows make
     * an JSONArray of the size = number of rows
     * @param row
     * @param dataTypes
     * @return
     */
    private Map<String, Object> dataObjectToMap(DataObject row, DataType[] dataTypes) {
        Map<String, Object> mapData = new HashMap<>();

        for (int i = 0; i < dataTypes.length; i++) {
            Class cls = dataTypes[i].getDataType();
            Object obj = row.getDataElement(dataTypes[i]);
            if (cls == null) {
                mapData.put(dataTypes[i].getKeyName(), obj);
                continue;
            }

            switch (cls.getSimpleName().toLowerCase()) {
                case "string":
                    String val = obj == null ? null : ((String) obj).trim();

                    if (dataTypes[i].getKeyName().equalsIgnoreCase("missionLabel (project)")) {
                        mapData.put("project", val);
                    } else if (dataTypes[i].getKeyName().equalsIgnoreCase("dataProductLabel")) {
                        mapData.put("subProject", val);
                    } else if (dataTypes[i].getKeyName().equalsIgnoreCase("wavebandDesc")) {
                        mapData.put("title", val);
                    } else if (dataTypes[i].getKeyName().equalsIgnoreCase("name")) {
                        mapData.put("JSON DM-12001", val);
                    } else {
                        mapData.put(dataTypes[i].getKeyName(), val);
                    }
                    break;
                case "float":
                    float fval = obj == null ? 0.1f : ((Float) obj).floatValue();
                    mapData.put(dataTypes[i].getKeyName(), fval);
                    break;
                case "integer":
                    int ival = obj == null ? 0 : ((Integer) obj).intValue();
                    mapData.put(dataTypes[i].getKeyName(), ival);
                    break;
                case "double":
                    double dval = obj == null ? 0.1 : ((Double) obj).doubleValue();
                    mapData.put(dataTypes[i].getKeyName(), dval);
                    break;

                default:
                    mapData.put(dataTypes[i].getKeyName(), obj);
                    break;
            }


        }

        HashMap<String, String> params = getMappedPlotRequestParam(row, dataTypes);
        String[] keys = params.keySet().toArray(new String[0]);
        JSONObject plotReqParams = new JSONObject();
        plotReqParams.put("type", "SERVICE");
        for (int i = 0; i < params.size(); i++) {
            plotReqParams.put(keys[i], params.get(keys[i]));
        }
        mapData.put("plotRequestParams", plotReqParams);
        return mapData;


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



    DataGroup getDataFromMasterTable(String masterTableName) throws IOException, FailedRequestException {


        InputStream inf= ImageSetConverter.class.getResourceAsStream(masterTableName);
        DataGroup dg = DsvToDataGroup.parse(inf, CSVFormat.DEFAULT);
        return dg;

    }



    public List getDataList(){
        return retList;
    }

}