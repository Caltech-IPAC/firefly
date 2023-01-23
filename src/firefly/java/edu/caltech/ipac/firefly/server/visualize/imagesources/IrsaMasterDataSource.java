/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;


import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.util.download.FailedRequestException;
import org.apache.commons.csv.CSVFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry.PARAMS;
import org.apache.commons.io.FileUtils;

import static edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterData.makeJsonObj;

/**
 * @author Trey Roby
 * 10/05/17 LZ modified and added some methods
 */
public class IrsaMasterDataSource implements ImageMasterDataSourceType {
    private final static String masterTable = "/edu/caltech/ipac/firefly/resources/irsa-image-master-table.csv";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String IRSA_MASTER_TABLE = AppProperties.getProperty("irsa.mastertable.location",masterTable);



    @Override
    public List<ImageMasterDataEntry> getImageMasterData() {
        try {

            return createDataList(IRSA_MASTER_TABLE);

        } catch (FailedRequestException  | IOException e) {
            LOG.info("Reading IRSA Master table failed");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private HashMap<String, String> getParameterMaps(){

        HashMap<String, String> paramMaps = new HashMap<>();
        paramMaps.put("apiType", "Service");
        paramMaps.put("surveyKey", "SurveyKey");
        paramMaps.put(PARAMS.WAVEBAND_ID.getKey(), "SurveyKeyBand");
        paramMaps.put("title", "title");
        paramMaps.put("filter", "filter");
        return paramMaps;
    }


    private boolean validateMasterTable(DataGroup inDg,  PARAMS[] parameters){

        for (int i=0; i<parameters.length; i++){
            if (!inDg.containsKey(parameters[i].getKey())){
                LOG.info("The master table does not contains "+parameters[i].getKey());
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
     public List<ImageMasterDataEntry> createDataList(String masterTable) throws Exception {
         List<ImageMasterDataEntry>  retList= new ArrayList();
         DataGroup inDg = getDataFromMasterTable(masterTable);
         List<DataObject> dataRows = inDg.values();
         Map<String, String> paramMaps=getParameterMaps();
         PARAMS[] parameters = ImageMasterDataEntry.PARAMS.values();

         ArrayList<String> imageIdList=new ArrayList<>();
         if (!validateMasterTable(inDg, parameters)){
             throw new Exception("The column names do not match the required parameter names");
         }
         for(DataObject row : dataRows) {
             ImageMasterDataEntry entry = new ImageMasterDataEntry();
             for (int i=0; i<parameters.length; i++){

                     Object obj = row.getDataElement(parameters[i].getKey());
                     String val = obj != null ? String.valueOf(obj) : null;

                     //make a temporary imageId
                     if (val==null && parameters[i].getKey().equals("imageId")){
                         val = String.valueOf(row.getDataElement(PARAMS.MISSION_ID.getKey()))
                                 +String.valueOf(row.getDataElement("surveyKey"))
                                 +String.valueOf(row.getDataElement(PARAMS.WAVEBAND_ID.getKey()));
                         if( imageIdList.contains(val)){
                             val+="_"+i;
                         }
                         imageIdList.add(val);
                     }//end making imageId

                     entry.set(parameters[i], val);
             }
             entry.setPlotRequestParams(getMappedPlotRequestParam(row, paramMaps));
             entry.setDefaultColorParams(getMappedColors(row));

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
                     //Because the checkbox options label are passed from the title, we need an image title label different to distinguish between 2 different same instrument/band
                     // Build 'intitle' label for UI image display info with plot request title and acronym
                     if(key.equalsIgnoreCase(PARAMS.TITLE.getKey())){
                         params.put(paramMaps.get(key) , String.valueOf(row.getDataElement(PARAMS.ACRONYM.getKey())));//+" "+String.valueOf(row.getDataElement(PARAMS.WAVEBAND_ID.getKey())));
                     }else{
                         params.put(paramMaps.get(key),  String.valueOf(row.getDataElement(key)) );
                     }
                     break;
                 }
             }
         }
         //For IRSA, type is SERVICE for now.
         params.put("type",  "SERVICE");
         return params;
     }

     private HashMap<String, String> getMappedColors(DataObject row){
         HashMap<String, String> colorMap = new HashMap<>();
         Object colorObj = row.getDataElement("defaultRgbColor");
         if (colorObj!=null) {
             String[] rgbColors = colorObj.toString().split(";");
             String[] rgbColorKeys = {"red", "green", "blue"};
             for (int i = 0; i < 3; i++) {
                 String imageId = String.valueOf(row.getDataElement(PARAMS.MISSION_ID.getKey()))
                         + String.valueOf(row.getDataElement("surveyKey"))
                         + rgbColors[i];
                 colorMap.put(rgbColorKeys[i], imageId);
             }
         }
         return colorMap;
     }

     DataGroup getDataFromMasterTable(String masterTableName) throws IOException, FailedRequestException {

         InputStream inf= IrsaMasterDataSource.class.getResourceAsStream(masterTableName);
         DataGroup dg = DsvTableIO.parse(inf, CSVFormat.DEFAULT);
         return dg;
     }

     public static void main(String[] args) throws Exception {
       IrsaMasterDataSource s = new IrsaMasterDataSource(){
           @Override
           DataGroup getDataFromMasterTable(String masterTableName) throws IOException, FailedRequestException {
               FileInputStream inf = FileUtils.openInputStream(new File(masterTableName));//
               DataGroup dg = DsvTableIO.parse(inf, CSVFormat.DEFAULT);
               return dg;
           }
       };
         List<ImageMasterDataEntry> dataList = s.createDataList("/hydra/cm/firefly/src/firefly/java/edu/caltech/ipac/firefly/resources/irsa-image-master-table.csv");
//         ImageMasterDataEntry o = (ImageMasterDataEntry) dataList.get(0);
         for(ImageMasterDataEntry o:dataList){
            System.out.println(makeJsonObj(o.getDataMap()));
         }

         ExternalMasterDataSource e = new ExternalMasterDataSource();
         List<ImageMasterDataEntry> imageMasterData = e.getImageMasterData();
         for(ImageMasterDataEntry o:imageMasterData){
             System.out.println(makeJsonObj(o.getDataMap()));
         }
     }

}
