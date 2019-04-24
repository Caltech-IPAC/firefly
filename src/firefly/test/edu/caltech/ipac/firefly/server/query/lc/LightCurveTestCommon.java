/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * [April-23-19]
 * Author: L.Z.
 * This class contains common variables and methods to be shared by IrsaLightCurveHandler and LightCurveProcessorTest.
 *
 */

package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.*;



public class LightCurveTestCommon extends ConfigTest {

    static PeriodogramAPIRequest periodogramAPIRequestForUI;
    static PeriodogramAPIRequest periodogramAPIRequestHasFilterForUI;

    static PeriodogramAPIRequest periodogramAPIRequestForURL;


    static String uploadFileName ="212027909.tbl";
    static File inputTbl = FileLoader.resolveFile(LightCurveProcessorTest.class, uploadFileName);
    static String url = "http://web.ipac.caltech.edu/staff/ejoliet/demo/AllWISE-MEP-m82-2targets-10arsecs.tbl";
    static String uploadFileInURL ="private static String url";
    static Map<String, String> expectedHttpInputParamsForUI = new HashMap<>();
    static Map<String, String> expectedHttpInputParamsForURL = new HashMap<>();
    static Map<String, String> reqParamsCommon = new LinkedHashMap<>();



    public LightCurveTestCommon(){
        reqParamsCommon.put("RequestClass" ,"ServerRequest");
        reqParamsCommon.put("id","IpacTableFromSource");
        reqParamsCommon.put("tblType","notACatalog");
        reqParamsCommon.put("tbl_id", "raw_table");

        String[] reqValues = new String[]{"x=BJD", "y=FLUX", "peaks=10", "alg=ls",
                "step_method=fixedf"};
        for (int i=0; i<reqValues.length; i++){
            String[] keyVal = reqValues[i].split("=");
            expectedHttpInputParamsForUI.put(keyVal[0], keyVal[1]);
        }


        String[] reqValuesURL = new String[]{"x=mjd", "y=w1mpro_ep", "peaks=10", "alg=ls",
                "step_method=fixedf"};
        for (int i=0; i<reqValues.length; i++){
            String[] keyVal = reqValuesURL[i].split("=");
            expectedHttpInputParamsForURL.put(keyVal[0], keyVal[1]);
        }

        periodogramAPIRequestForUI =  makePeriodogramAPIRequest(reqValues, inputTbl.getAbsolutePath(), "ASC,\"BJD\"", uploadFileName, null);
        periodogramAPIRequestForURL =  makePeriodogramAPIRequest(reqValuesURL, url, "ASC,\"mjd\"", uploadFileInURL, null);
        periodogramAPIRequestHasFilterForUI=makePeriodogramAPIRequest(reqValues, inputTbl.getAbsolutePath(), "ASC,\"BJD\"", uploadFileName, "BJD>3270");

        periodogramAPIRequestForUI.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name());
        periodogramAPIRequestForURL.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name());
        periodogramAPIRequestHasFilterForUI.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name());

    }


    private static JSONObject getJsonObj(String src, String sortInfo, String uploadFileName, String filters){
        JSONObject json= new JSONObject(reqParamsCommon);
        json.put("uploadFileName",  uploadFileName);//"212027909.tbl");
        json.put("sortInfo" , sortInfo);
        json.put("source",src);
        if (filters!=null){
            json.put("filters", filters);
        }
        return json;
    }

    private static PeriodogramAPIRequest makePeriodogramAPIRequest(String[] reqValues, String src, String sortInfo, String uploadFileName, String filters){
        PeriodogramAPIRequest request = new PeriodogramAPIRequest();
        for (String item : reqValues) {
            String[] keyValPair = item.split("=");
            request.setParam(keyValPair[0], keyValPair[1]);
        }
        JSONObject json= getJsonObj(src, sortInfo, uploadFileName,filters);

        //set lc source
        request.setLcSource(json.toJSONString());
        request.setParam("table_name", LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name());
        return request;
    }

    DataGroup extractTblFrom(File votableResult, LightCurveHandler.RESULT_TABLES_IDX resultTable) {
        try {
            DataGroup[] dataGroups = VoTableReader.voToDataGroups(votableResult.getAbsolutePath());

            return dataGroups[resultTable.ordinal()];
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }
}
