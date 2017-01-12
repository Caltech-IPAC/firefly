package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

/**
 * Temp way of getting dd tables using show columns
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "LSSTCatalogDD", params =
        {@ParamDoc(name=CatalogRequest.CATALOG, desc="catalog table to query")
        })
public class QueryLSSTCatalogDD extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log= Logger.getLogger();

    private static String DATA_ACCESS_URI = AppProperties.getProperty("lsst.dataAccess.uri", "lsst.dataAccess.uri");
    //private static String DATABASE = AppProperties.getProperty("lsst.dataAccess.db", "lsst.dataAccess.db");

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {



        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            throw new RuntimeException(CatalogRequest.CATALOG + " parameter is required");
        }

        //String sql = "select name, description, type, unit, IF(notNull>0,\"y\",\"n\") as sel from md_Column where tableId in (select tableId from md_Table where name='"+catTable+"')";
        String sql = "SHOW COLUMNS FROM "+catTable;  //http://localhost:8661/db/v0/query?sql=SHOW+COLUMNS+FROM+DeepSource+IN+DC_W13_Stripe82

        try {
            long cTime = System.currentTimeMillis();
            _log.briefDebug("Executing SQL query: " + sql);
            String url = DATA_ACCESS_URI+"sql="+ URLEncoder.encode(sql, "UTF-8");

            URLConnection uc = URLDownload.makeConnection(new URL(url));
            uc.setRequestProperty("Accept", "text/plain");
            File file = createFile(request, ".json");
            URLDownload.getDataToFile(uc, file, null);
            _log.briefDebug("SHOW COLUMNS took " + (System.currentTimeMillis() - cTime) + "ms");

            DataGroup dg = JsonToDataGroup.parse(file);
            if (dg != null) {
                DataType[] defs = dg.getDataDefinitions();
                DataType nameType = defs[0];
                DataType typeType = defs[1];
                DataType nullType = defs[2];

                DataType nameTypeR = (DataType)nameType.clone();
                nameTypeR.setKeyName("name");
                DataType typeTypeR = (DataType)typeType.clone();
                typeTypeR.setKeyName("type");
                DataType selTypeR = new DataType("sel", String.class);


                DataType[] columns = new DataType[]{
                        nameTypeR,
                        new DataType("description", String.class),
                        typeTypeR,
                        new DataType("units", String.class),
                        selTypeR
                };
                DataGroup toReturn = new DataGroup(catTable + "-dd", columns);
                DataObject dObjReturn;
                for (DataObject dObj : dg) {
                    dObjReturn = new DataObject(dg);
                    dObjReturn.setDataElement(nameTypeR, dObj.getDataElement(nameType));
                    dObjReturn.setDataElement(typeTypeR, dObj.getDataElement(typeType));
                    dObjReturn.setDataElement(selTypeR, dObj.getDataElement(nullType).equals("YES") ? "y" : "n");

                    toReturn.add(dObjReturn);
                }
                toReturn.shrinkToFitData();
                File inf = createFile(request, ".tbl");
                DataGroupWriter.write(inf, toReturn);
                return inf;
            } else {
                return null;
            }

        } catch (Exception e) {
            _log.error(e);
            throw new DataAccessException("Query failed: "+e.getMessage());
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
        //meta.setAttribute("col.dbtype.Visibility", "hide");
    }

}
