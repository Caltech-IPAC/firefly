/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;


import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.download.FailedRequestException;
import org.apache.commons.csv.CSVFormat;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class IrsaMasterDataSource implements ImageMasterDataSourceType {
    private final String masterTable = "/edu/caltech/ipac/firefly/resources/irsa-master-table.csv";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();



    @Override
    public List<ImageMasterDataEntry> getImageMasterData() {
        try {

            return createDataList(masterTable);

        } catch (FailedRequestException  | IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private HashMap<String, String> getParameterMaps(){

        HashMap<String, String> paramMaps = new HashMap<>();
        paramMaps.put("ProjectTypeDesc", "type");

        paramMaps.put("apiType", "Service");
        paramMaps.put("dataProductId", "surveyKey");
        paramMaps.put("waveBandId", "surveyKeyBand");
        paramMaps.put("waveBandDesc", "title");
        paramMaps.put("filter", "filter");
        return paramMaps;
    }
    private String[] getDefinedParameters() throws IOException, FailedRequestException{
        Field[] fields = IrsaMasterTableParameters.class.getDeclaredFields();
        String[] keys = new String[fields.length];

        for (int i=0; i<fields.length; i++){
            try {
                keys[i] = (String) fields[i].get(null);
            }
            catch (IllegalAccessException x) {
                continue; // not a publicly accessible field
            } catch (NullPointerException x) {
                continue; // non-static field
            }
        }
        return keys;
    }

    private boolean validateMasterTable(DataGroup inDg,  String[] parameterKeys){
        for (int i=0; i<parameterKeys.length; i++){
            if (!inDg.containsKey(parameterKeys[i])){
                return false;
            }
        }
        return true;
    }
      /**
       * This method reads in the master-table and then create a Map List based on the table data.
       * @return
       * @throws IOException
       * @throws FailedRequestException
       */
     public List createDataList(String masterTable) throws Exception {
         List<ImageMasterDataEntry>  retList= new ArrayList();
         DataGroup inDg = getDataFromMasterTable(masterTable);
         List<DataObject> dataRows = inDg.values();
         Map<String, String> paramMaps=getParameterMaps();
         String[] parameterKeys = getDefinedParameters();
         if (!validateMasterTable(inDg, parameterKeys)){
             throw new Exception("The column names do not match the required parameter names");
         }
         for(DataObject row : dataRows) {
             ImageMasterDataEntry entry = new ImageMasterDataEntry();
             for (int i=0; i<parameterKeys.length; i++){
                 Object obj = row.getDataElement(parameterKeys[i]);
                 String val = obj!=null?String.valueOf(obj):null;
                 entry.set(parameterKeys[i], val );

             }
             entry.setPlotRequestParams(getMappedPlotRequestParam(row, paramMaps));

             retList.add( entry);
         }
         return retList;
     }



     /**
      * This method finds the plotRequestParams.  It uses predefined map to map the data in the master table.
      * For example, the apiType in master table means Service for the plotRequest parameter.
      * @param row
      * @return
      */
     private HashMap<String, String> getMappedPlotRequestParam(DataObject row,Map<String, String>paramMaps){

         HashMap<String, String> params = new HashMap<>();

         String[] plotParamKeys =  paramMaps.keySet().toArray(new String[0]);
         for (int i=0; i<plotParamKeys.length; i++) {
             String key= plotParamKeys[i];
             for (int j = 0; j < row.size(); j++) {
                 if(row.containsKey(key)){
                     params.put(paramMaps.get(key),  String.valueOf(row.getDataElement(key)) );
                     break;
                 }
             }
         }
         return params;
     }

     DataGroup getDataFromMasterTable(String masterTableName) throws IOException, FailedRequestException {

         InputStream inf= IrsaMasterDataSource.class.getResourceAsStream(masterTableName);
         DataGroup dg = DsvToDataGroup.parse(inf, CSVFormat.DEFAULT);
         return dg;
     }

}
