package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
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
import java.util.List;

/**
 * Created by zhang on 10/12/16.
 * This search processor is returning column metadata for a specified database table.
 * The data come from metaserv, see http://dm.lsst.org/dax_metaserv/api.html
 * Database table name should consist of 3 parts: logical database name, schema, table name.
 * Example: W13_sdss_v2.sdss_stripe82_01.RunDeepSource
 */
@SearchProcessorImpl(id = "LSSTMetaSearch",
        params =
                {@ParamDoc(name="table_name", desc="database table to query")})

public class LSSTMetaSearch  extends IpacTablePartProcessor{
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    //set default timeout to 30 seconds
    private int timeout  = AppProperties.getIntProperty("lsst.database.timeoutLimit" , 30);

    private DataGroup  getDataFromURL(TableServerRequest request) throws Exception {

        String dbTable = LSSTQuery.getDBTableNameFromRequest(request);
        String[] parts = dbTable.split("\\.");
        if (parts.length != 3) {
            throw new DataAccessException("Unsupported table name: "+dbTable);
        }
        if (parts[0].contains("/")) {
            throw new DataAccessException("Unable to retrieve metadata for "+dbTable);
        }

        //use fully specified path, which includes logical database, schema, and table name
        //ex. URL http://lsst-qserv-dax01:5000/meta/v1/db/W13_sdss_v2/sdss_stripe82_01/tables/RunDeepForcedSource
        String url = LSSTQuery.getMetaservURL() + URLEncoder.encode(parts[0], "UTF-8") + "/" +
                URLEncoder.encode(parts[1], "UTF-8") + "/tables/"  +
                URLEncoder.encode(parts[2], "UTF-8");
        _log.briefDebug("Getting metadata: " + url);

        File file = createFile(request, ".json");

        HttpServiceInput inputs = HttpServiceInput.createWithCredential(LSSTQuery.getMetaservURL());
        inputs.setHeader("Accept", "application/json");

        long cTime = System.currentTimeMillis();
        FileInfo fileData = URLDownload.getDataToFileUsingPost(new URL(url),null, inputs.getCookies(), inputs.getHeaders(), file, null, timeout);
        _log.briefDebug("Metadata call took " + (System.currentTimeMillis() - cTime) + "ms");

        if (fileData.getResponseCode() >= 400) {
            String err = LSSTQuery.getErrorMessageFromFile(file);
            throw new DataAccessException("[DAX] " + (err == null ? fileData.getResponseCodeMsg() : err));
        }
        return getMetaData(file);
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        DataGroup dg;
        try {
            dg = getDataFromURL(request);
            File outFile = createFile(request, ".tbl");
            IpacTableWriter.save(outFile, dg);
            return outFile;

        } catch (IOException | DataAccessException ee) {
            throw ee;
        } catch (Exception e) {
            _log.error("load table failed: "+e.getMessage());
            throw new DataAccessException(e.getMessage(), e);
        }

    }

    /**
     * This method reads the json file from DAX and process it. The output is a DataGroup of the Meta data
     * @param file json file
     * @return DataGroup  DataGroup of the Meta data
     * @throws IOException on file read error
     * @throws ParseException on json parse error
     */

    private DataGroup getMetaData(File file) throws IOException, ParseException {

        JSONParser parser = new JSONParser();

        JSONObject obj = ( JSONObject) parser.parse(new FileReader(file ));

        JSONObject result = (JSONObject) obj.get("result");
        if (result == null) {
            _log.error("No result entry in "+file.getAbsolutePath());
            return new DataGroup("No result", new DataType[0]); // should not happen
        }
        JSONArray columns = (JSONArray) (result).get("columns");

        //"name":"coord_ra","datatype":"double","ucd":"pos.eq.ra","unit":"deg","tableName":"RunDeepSource"
        String [] fields = new String[]{"name", "unit", "description", "datatype", "ucd", "nullable"};
        DataType[] dataTypes = new DataType[fields.length];

        for (int i=0; i<fields.length; i++){
            dataTypes[i] = new DataType(fields[i], String.class);
        }
        DataGroup dg = new DataGroup(file.getName(), dataTypes);

        Object o;
        for (Object c : columns){

            DataObject row = new DataObject(dg);
            for (int i=0; i<fields.length; i++){
                o = ((JSONObject)c).get(fields[i]);
                if (o != null) {
                    row.setDataElement(dataTypes[i], o.toString());
                }
            }
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

}
