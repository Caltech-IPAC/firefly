package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static edu.caltech.ipac.firefly.server.util.Logger.getLogger;

/**
 * Created by zhang on 11/17/16.
 *
 */

@SearchProcessorImpl(id = "LSSTDownload")
public class LSSTFileGroupProcessor  extends FileGroupsProcessor {
    public static final String LSST_FILESYSTEM_BASEPATH = AppProperties.getProperty("lsst.filesystem_basepath");
    private  Logger.LoggerImpl logger = getLogger();
    private static  long  SIZE = 30611520;
    @Override
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }



    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {

        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());


        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();

        long fgSize = 0;


        // values = folder or flat
        Set<String> zipFiles = new HashSet<String>();
        String zipType = request.getParam("zipType");
        boolean zipFolders = true;
        if (zipType != null && zipType.equalsIgnoreCase("flat")) {
            zipFolders = false;
        }

        String basePath = LSST_FILESYSTEM_BASEPATH;
        String baseFileName = request.getParam(DownloadRequest.BASE_FILE_NAME);

        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        basePath +=  "/"+baseFileName+"/";

        String fileName = basePath + baseFileName+new Integer( selectedRows.size()).toString();


        boolean isDeepCoadd = baseFileName.equalsIgnoreCase("deepCoad")? true:false;



        String[] sccdCols = {"run",  "camcol", "field", "filterName"};
        String[] deeoCoaddCols={"tract", "patch", "filterName"};
        String[] columns= isDeepCoadd?deeoCoaddCols:sccdCols;

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, columns);
        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();

        for (int rowIdx : selectedRows) {

            String outFile = ServerContext.getTempWorkDir() + "/" + fileName;

            FileInfo fileInfo = getDataFileInfo(dgData, rowIdx, isDeepCoadd, outFile);

            if (zipFolders) {
                 String zipName = fileInfo.getExternalName();
                if(!zipFiles.contains(zipName)){
                    zipFiles.add(zipName);
                }
            } else {
                String[]  zipNameArrays = fileInfo.getExternalName().split("/");
                String zipName = zipNameArrays[zipNameArrays.length-2];
                if(!zipFiles.contains(zipName)) {
                    zipFiles.add(zipName);
                }
             }
            fiArr.add(fileInfo );
            fgSize += outFile.length();
        }



        FileGroup fg = new FileGroup(fiArr, ServerContext.getTempWorkDir(), fgSize, "LSST Download Files");
        fgArr.add(fg);

        return fgArr;
    }


    private FileInfo getFileInfo(ServerRequest request){

            SearchManager sm = new SearchManager();
            try {

                LSSTImageSearch lsstImage = (LSSTImageSearch) sm.getProcessor("LSSTImageSearch");
                return lsstImage.getData(request);
            } catch (Exception e) {
                e.getStackTrace();
            }
            return null;
     }

    private FileInfo getDataFileInfo( IpacTableParser.MappedData dgData, int rowIdx,  boolean isDeepCoadd, String outFile) throws MalformedURLException {

        FileInfo fileInfo;
        URL url;
        ServerRequest svr = new ServerRequest();
        if (isDeepCoadd){
            String tract = (String) dgData.get(rowIdx,"tract");
            String patch = (String) dgData.get(rowIdx,"patch");
            String filterName =(String) dgData.get(rowIdx,"filterName");
            svr.setParam("tract", tract);
            svr.setParam("patch", patch);
            svr.setParam("filterName", filterName);
            url = LSSTImageSearch.createURLForDeepCoadd(tract, patch, filterName);
        }
        else{
            String run =  (String) dgData.get(rowIdx,"run");
            String camcol =  (String) dgData.get(rowIdx,"camcol");
            String field =  (String) dgData.get(rowIdx,"field");
            String filterName =  (String) dgData.get(rowIdx,"filterName");

            svr.setParam("run", run);
            svr.setParam("camcol", camcol);
            svr.setParam("field", field);
            svr.setParam("filterName", filterName);
            url =LSSTImageSearch.createURLForScienceCCD(run, camcol,field, filterName);
        }
        //search to see if the file is already in the work area.
        fileInfo = getFileInfo(svr);
        if (fileInfo==null) {
            fileInfo = new FileInfo(url.toString(), outFile, SIZE );
        }
        return  fileInfo;
    }

    private URL getDataURL( IpacTableParser.MappedData dgData, int rowIdx,  boolean isDeepCoadd) throws MalformedURLException {
        if (isDeepCoadd){
            String tract = (String) dgData.get(rowIdx,"tract");
            String patch = (String) dgData.get(rowIdx,"patch");
            String filterName =(String) dgData.get(rowIdx,"filterName");

            return LSSTImageSearch.createURLForDeepCoadd(tract, patch, filterName);

        }

        else{
            String run =  (String) dgData.get(rowIdx,"run");
            String camcol =  (String) dgData.get(rowIdx,"camcol");
            String field =  (String) dgData.get(rowIdx,"field");
            String filterName =  (String) dgData.get(rowIdx,"filterName");

            return LSSTImageSearch.createURLForScienceCCD(run, camcol,field, filterName);
        }
    }
    private String getDataUROldL(String baseURL, IpacTableParser.MappedData dgData, int rowIdx,String[] columns){



        StringBuffer sb = new  StringBuffer();
        for (int i=0; i<columns.length; i++){
            String val = ((String) dgData.get(rowIdx, columns[i])).trim();
            if (i==columns.length-1) {
                sb.append(columns[i] + "=" + val );
            }
            else {
                sb.append(columns[i] + "=" + val + "&");
            }
        }

        return  baseURL+sb.toString();
    }
}
