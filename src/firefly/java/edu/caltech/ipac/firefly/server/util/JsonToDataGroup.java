package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * Sample json:
 * {
 *   "results":[
 *      [
 *         8404051561545737,
 *         125230127
 *      ],
 *      [
 *         8404051561545738,
 *         125230127
 *      ]
 *   ],
 *   "metadata":{
 *      "columnDefs":[
 *         {
 *            "name":"deepForcedSourceId",
 *            "ipacType":"long"
 *         },
 *         {
 *            "name":"scienceCcdExposureId",
 *            "ipacType":"long"
 *         }
 *      ]
 *   }
 * }
 *
 *
 * @author tatianag
 */
public class JsonToDataGroup {

    //private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public static DataGroup parse (File jsonFile) throws IOException {

        JSONObject json = null;
         try {
             // this method is not intended for large files
             byte[] encoded = Files.readAllBytes(Paths.get(jsonFile.getPath()));
             String contents =  new String(encoded, StandardCharsets.UTF_8);

             json = (JSONObject) JSONValue.parse(contents);
             //System.out.print(json);
         } catch (Exception e) {
             e.printStackTrace();
         }

        if (json != null) {

            if ( json.get("exception") != null) {
                throw new IOException("Error in JSON return: "+json.get("message")+" - see "+jsonFile.getPath());
            }

            Object metadata = json.get("metadata");
            List<String> colNames = new ArrayList<String>();
            if (metadata != null && metadata instanceof Map) {
                Object columnDefsLst = ((Map)metadata).get("columnDefs");
                if (columnDefsLst instanceof List) {
                    for (Object def : (List)columnDefsLst) {
                        if (def instanceof Map) {
                            colNames.add(((Map)def).get("name").toString());
                        } else {
                            colNames.add("f"+colNames.size());
                        }
                    }
                }
            }

            Object rows = json.get("results");
            if (rows != null && rows instanceof List) {
                List<DataType> columns = new ArrayList<DataType>();
                if (((List)rows).size() > 0) {
                    Object firstRow = ((List) rows).get(0);

                    if (firstRow instanceof List) {
                        int ctr = 1;
                        String colName;
                        for (Object value : (List) firstRow) {
                            colName = ctr <= colNames.size() ? colNames.get(ctr - 1) : "fld" + ctr;
                            columns.add(new DataType(colName, value.getClass()));
                            ctr++;
                        }
                    }
                }
                DataGroup dg = new DataGroup(null, columns);
                dg.beginBulkUpdate();
                for (Object row : (List) rows) {
                    if (row instanceof List) {
                        DataObject dObj = new DataObject(dg);
                        int idx = 0;
                        for (Object value : (List)row) {
                            dObj.setDataElement(columns.get(idx),value);
                            idx++;
                        }
                        dg.add(dObj);
                    }
                }
                dg.endBulkUpdate();
                dg.shrinkToFitData();
                return dg;
            }
        }
        return null;
     }

}
