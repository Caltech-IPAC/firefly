package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhang on 10/12/16.
 * This search processor is searching the MetaData (or Data Definition from DAX database, then save to a
 * IpacTable file.
 */
@SearchProcessorImpl(id = "LSSTMetaSearch",
        params =
                {@ParamDoc(name=CatalogRequest.CATALOG, desc="catalog table to query")})

public class LSSTMetaSearch  extends IpacTablePartProcessor{
     private static final Logger.LoggerImpl _log = Logger.getLogger();
     private static final String PORT = "5000";
     private static final String HOST = AppProperties.getProperty("lsst.dd.hostname","lsst-qserv-dax01.ncsa.illinois.edu");
    //TODO how to handle the database name??
    // private static final String DATABASE_NAME =AppProperties.getProperty("lsst.database" , "gapon_sdss_stripe92_patch366_0");
    //private static final String DATABASE_NAME =AppProperties.getProperty("lsst.database" , "");
    //set default timeout to 30seconds
    int timeout  = new Integer( AppProperties.getProperty("lsst.database.timeoutLimit" , "30")).intValue();

    static  String getErrorMessageFromFile(File file) throws IOException, ParseException {
        JSONParser parser = new JSONParser();

        JSONObject obj = ( JSONObject) parser.parse(new FileReader(file ));
        String message = obj.get("message").toString();
        String error = obj.get("error").toString();
        if (error != null && message != null) {
            return message.contains(error) ? message : error + " " + message;
        } else {
            return error;
        }
    }

    private DataGroup  getDataFromURL(TableServerRequest request) throws Exception {

         String sql = "query=" + URLEncoder.encode(buildSqlQueryString(request),"UTF-8");

         long cTime = System.currentTimeMillis();
         _log.briefDebug("Executing SQL query: " + sql);
         String url = "http://"+HOST +":"+PORT+"/db/v0/tap/sync";

         File file = createFile(request, ".json");
         Map<String, String> requestHeader=new HashMap<>();
         requestHeader.put("Accept", "application/json");

         FileInfo fileData = URLDownload.getDataToFileUsingPost(new URL(url),sql,null,  requestHeader, file, null,timeout);
         if (fileData.getResponseCode()>=500) {
            // throw new DataAccessException("ERROR:" + sql + ";"+ getErrorMessageFromFile(file));
             throw new DataAccessException("[DAX] "+ getErrorMessageFromFile(file));
         }
         DataGroup dg =  getMetaData(file);
         _log.briefDebug("SHOW COLUMNS took " + (System.currentTimeMillis() - cTime) + "ms");
         return dg;


    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        DataGroup dg;
        try {
            dg = getDataFromURL(request);
            File outFile = createFile(request, ".tbl");
            dg.shrinkToFitData();
            DataGroupWriter.write(outFile, dg);
            return outFile;

        } catch (IOException | DataAccessException ee) {
            throw ee;
        } catch (Exception e) {
            _log.error("load table failed: "+e.getMessage());
            throw new DataAccessException(e.getMessage(), e);
        }

    }

    /**
     * This method processes the input JSONArray and then return a DataType array.
     * Since there is no unit data,this method fakes the unit and description.
     *
     * @param metaData meta data
     * @return DataType[] data type with fake unit and description
     */
    private DataType[] getDataType(JSONArray metaData){
        DataType[] dataTypes = new DataType[metaData.size()+2];//add unit and descriptions
        for (int i=0; i<metaData.size(); i++){
            JSONObject element = (JSONObject) metaData.get(i);
            dataTypes[i] = new DataType(element.get("name").toString(),  element.get("datatype").getClass());
        }
        dataTypes[metaData.size()] =new DataType("Unit",  (new String()).getClass());
        dataTypes[metaData.size()+1] =new DataType("Description",  (new String()).getClass());
        return dataTypes;

    }

    /**
     * This method reads the json file from DAX and process it. The output is a DataGroup of the Meta data
     * @param file json file
     * @return DataGroup  DataGroup of the Meta data
     * @throws IOException
     * @throws ParseException
     */

    private DataGroup getMetaData(File file) throws IOException, ParseException {

        JSONParser parser = new JSONParser();

        JSONObject obj = ( JSONObject) parser.parse(new FileReader(file ));

        JSONArray metaData = (JSONArray) ( (JSONObject) ( (JSONObject)( (JSONObject) obj.get("result")).get("table")).get("metadata")).get("elements");

        DataType[] dataTypes = getDataType( metaData );
        JSONArray  data = (JSONArray) ( (JSONObject)( (JSONObject) obj.get("result")).get("table")).get("data");

        DataGroup dg = new DataGroup(file.getName(), dataTypes);

        for (int i=0; i<data.size(); i++){

            DataObject row = new DataObject(dg);
            for (int j=0; j<dataTypes.length-2; j++){
                row.setDataElement(dataTypes[j], ( (JSONArray)data.get(i)).get(j));
            }
            row.setDataElement(dataTypes[dataTypes.length-2], "dummyUnit1");
            row.setDataElement(dataTypes[dataTypes.length-1], "description of "+ ((JSONArray)data.get(i)).get(0).toString());
            dg.add(row);
        }

        return dg;
    }


    String buildSqlQueryString(TableServerRequest request) throws Exception {
        String dbTable = String.join(".", LSSTQuery.getDBTableNameFromRequest(request));

        String sql = "SHOW COLUMNS FROM " + dbTable + ";";
        if (dbTable.length() <= 0) {
            throw new EndUserException("Error: Search without table name will cause database hanging!!!", sql);
        } else {
            return sql;
        }
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

}
