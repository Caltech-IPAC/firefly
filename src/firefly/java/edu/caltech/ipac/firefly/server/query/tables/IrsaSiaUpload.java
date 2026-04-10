package edu.caltech.ipac.firefly.server.query.tables;
/**
 * User: roby
 * Date: 4/7/26
 * Time: 2:39 PM
 */


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.core.Util.Try;
import static edu.caltech.ipac.util.CollectionUtil.isEmpty;

/**
 * @author Trey Roby
 *
 */
@SearchProcessorImpl(id = "SiaExtUpload")
public class IrsaSiaUpload extends EmbeddedDbProcessor {
    private static final List<String> ignore= List.of("uploadSiaExtParams", "COLLECTION", "siaUrl", "RequestClass",
            "tbl_id", "ffSessionId", "accessURL", "id", "circleSize");

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // get and validate params
        var urlStr= req.getParam("accessURL");
        URL url= Try.it( () -> new URI(urlStr).toURL()).get();
        if (url==null) throw new DataAccessException("a valid accessURL is required");

        var uploadParams= Try.it(() -> (JSONObject)new JSONParser().parse(req.getParam("uploadSiaExtParams"))).get();
        if (uploadParams==null) throw new DataAccessException("uploadSiaExtParams is required");

        var lonCol= (String)uploadParams.get("lonCol");
        var latCol= (String)uploadParams.get("latCol");
        var fileStr= (String)uploadParams.get("serverFile");
        var circleSize= req.getParam("circleSize");
        if (lonCol==null || latCol==null || fileStr==null || circleSize==null)  {
            throw new DataAccessException("lonCol, latCol, circleSize and serverFile is required");
        }
        updateJob(ji -> {
            ji.getAux().setJobUrl(urlStr);
            ji.getAux().setTitle("SIAv2 Upload");
        });


        // create SIA service upload file from loaded file
        var file= ServerContext.convertToFile(fileStr);
        var uploadFile= Try.it( () -> File.createTempFile(file.getName(),".tbl",file.getParentFile())).get();
        try {
            var indg= TableUtil.readAnyFormat(file);
            DataType[] cols = new DataType[] {indg.getDataDefintion(lonCol), indg.getDataDefintion(latCol)};
            var uploadDg= new DataGroup("upload", cols);
            for(int i=0;(i<indg.size());i++) {
                uploadDg.setData(lonCol,i,indg.getData(lonCol,i));
                uploadDg.setData(latCol,i,indg.getData(latCol,i));
                uploadDg.setSize(i+1);
            }
            IpacTableWriter.save(uploadFile, uploadDg);
        } catch (IOException e) {
            throw new DataAccessException("Error reading upload file ", e);
        }

        // build SIA parameters, call service, make DataGroup
        try {
            var collection= Try.it(() -> (JSONArray)new JSONParser().parse(req.getParam("COLLECTION"))).get();
            var postData= new HashMap<String,Object>( Map.of(
                    "POS",       String.format("CIRCLE my_table.%s my_table.%s %s", lonCol, latCol, circleSize),
                    "UPLOAD",    "my_table,param:table.tbl",
                    "table.tbl", FileUtil.readFile(uploadFile)
            ) );
            if (!isEmpty(collection)) postData.put("collection", collection);

            req.getParams().stream()
                    .filter(p -> !ignore.contains(p.getName()))
                    .forEach( p -> postData.put(p.getName(), p.getValue()));
            

            var searchResult= File.createTempFile("sia-upload-",".xml",ServerContext.getTempWorkDir());
            FileInfo fi= URLDownload.getDataToFileUsingPost(url,postData,null, null, searchResult, null,0);
            if (!fi.isOK()) throw new DataAccessException(fi.getResponseCodeMsg());
            var resultDg= TableUtil.readAnyFormat(searchResult);
            setJobResults(searchResult);
            return resultDg;
        } catch (IOException | FailedRequestException e) {
            throw new DataAccessException(e);
        }
    }
}
