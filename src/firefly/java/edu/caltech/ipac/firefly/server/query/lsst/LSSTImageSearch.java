package edu.caltech.ipac.firefly.server.query.lsst;


import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.URLDownload;
import java.io.*;
import java.net.URL;


/**
 * Created by zhang on 10/12/16.
 * This search processor is searching the MetaData (or Data Definition from DAX database, then save to a
 * IpacTable file.
 */
@SearchProcessorImpl(id = "LSSTImageSearch")
/**
 * Created by zhang on 11/3/16.
 */
public class LSSTImageSearch extends BaseFileInfoProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static String DAX_URL="http://lsst-qserv-dax01.ncsa.illinois.edu:5000/image/v0/";


    /**
     * This method is using deepCoaddId to search the image.  It worked fine.  Comment it for now.  If it does not needed,
     * it will be deleted.
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    private FileInfo loadDataByDeepCoaddId(ServerRequest request) throws IOException, DataAccessException {

        String deepCoaddId = request.getParam("deepCoaddId");
        String url = DAX_URL+"deepCoadd/id?id="+deepCoaddId;

        String fname = request.getParam("plotId")==null? "lsstDeepCoadd": request.getParam("plotId")+request.getParam( "filterId");

        File outfile = makeOutputFile(fname);

        try {

            FileData fileData  = URLDownload.getDataToFile(new URL(url), outfile);
            return new FileInfo(fileData.getFile().getAbsolutePath(), fname, outfile.length());

        } catch (FailedRequestException e) {
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }

    }

    @Override
    protected FileInfo loadData(ServerRequest request) throws IOException, DataAccessException {

        if (request.getParam("deepCoaddId")!=null){
           return loadDataByDeepCoaddId(request);
        }
        else {
            return loadDataBySetOfIds(request);
        }
    }

    /**
     * This method uses a set of fields to search for image
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    private FileInfo loadDataBySetOfIds(ServerRequest request)throws IOException, DataAccessException {
        _log.info("getting the parameters out from the request");
        String run = request.getParam("run");
        String camcol = request.getParam("camcol");
        String field = request.getParam("field");
        String filter = request.getParam("filterName");

        _log.info("create URL");
        String url = DAX_URL+"calexp/ids?run="+run+"&camcol="+camcol+"&field="+field+"&filter="+filter;

        //use the plotId as name
        String fname = request.getParam("plotId")==null? "lsstScienceCcd": request.getParam("plotId")+request.getParam( "filterId");

        File outfile = makeOutputFile(fname);

        try {

            _log.info("download data from the DAX");
            FileData fileData  = URLDownload.getDataToFile(new URL(url), outfile);

            return new FileInfo(fileData.getFile().getAbsolutePath(), fname, outfile.length());

        } catch (FailedRequestException e) {
            _log.error("download data is failed");
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }
    }

    File makeOutputFile(String fname)  throws IOException {

        if (fname.contains(".tbl")) {
            return File.createTempFile("LSSTFileRetrieve-", "-" + fname, ServerContext.getTempWorkDir());
        } else {

           return File.createTempFile(fname + "-", "", ServerContext.getVisCacheDir());
        }
    }


    @Override
    public boolean doCache() {
        return true;
    }

    /**
     * Test the processor in main program.
     * @param args
     * @throws IOException
     * @throws DataAccessException
     */
    public static void main(String[] args) throws IOException, DataAccessException {
        ServerRequest request = new ServerRequest();
        request.setParam("table_name", "Science_Ccd_Exposure");
        request.setParam("run", "3325");
        request.setParam("camcol", "1");
        request.setParam("filterId", "4");
        request.setParam("field", "171");
        request.setParam("filename", "test");
        LSSTImageSearch imageSearch = new LSSTImageSearch();
        FileInfo fileInfo = imageSearch.loadData(request);


    }
}