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
import edu.caltech.ipac.util.StringUtils;
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

@SearchProcessorImpl(id = "LSSTFileGroupProcessor")
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


        // do folder or flat
        Set<String> zipFiles = new HashSet<String>();
        String zipType = request.getParam("zipType");
        boolean zipFolders = true;
        if (zipType != null && zipType.equalsIgnoreCase("flat")) {
            zipFolders = false;
        }

        //String basePath = LSST_FILESYSTEM_BASEPATH;
        //basePath = basePath!=null? basePath :"";
        String baseFileName = request.getParam(DownloadRequest.BASE_FILE_NAME);
        String basePath  = baseFileName ;


        boolean isDeepCoadd = baseFileName.equalsIgnoreCase("deepCoadd")? true:false;


        String[] sccdCols = { "scienceCcdExposureId","run",  "camcol", "field", "filterName"};
        String[] deeoCoaddCols={"deepCoaddId","tract", "patch", "filterName"};
        String[] columns= isDeepCoadd?deeoCoaddCols:sccdCols;

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, columns);
        ArrayList<FileInfo> fiArr = new ArrayList<>();

        for (int rowIdx : selectedRows) {

            String fileName = getFileName(isDeepCoadd, dgData,rowIdx);

            String urlStr = getDataURLString(dgData, rowIdx, isDeepCoadd);

            String extFileName =zipFolders? baseFileName + "/"+ fileName : fileName;

            if(!zipFiles.contains(urlStr)){
                zipFiles.add(urlStr);
            }

            FileInfo fileInfo =  new FileInfo(urlStr, extFileName, SIZE );
            fiArr.add(fileInfo );

            //fgSize += new File(extFileName).length();  //fileInfo.getSizeInBytes();
        }



        FileGroup fg = new FileGroup(fiArr, ServerContext.getTempWorkDir(), 0, "LSST Download Files");
        fgArr.add(fg);

        return fgArr;
    }

    private String getFileName(boolean isDeepCoadd, IpacTableParser.MappedData dgData, int rowIdx){
        String filterName =(String) dgData.get(rowIdx,"filterName");
        String id="";
        if (isDeepCoadd) {
            id =  dgData.get(rowIdx,"deepCoaddId").toString();

        }
        else{
            id =  dgData.get(rowIdx,"scienceCcdExposureId").toString();
        }
        return  id +"-"+filterName+".fits";

    }
    private  String  getDataURLString( IpacTableParser.MappedData dgData, int rowIdx,  boolean isDeepCoadd) throws MalformedURLException {

        ServerRequest svr = new ServerRequest();
        if (isDeepCoadd){
            Long tract = (Long) dgData.get(rowIdx,"tract");
            String patch = (String) dgData.get(rowIdx,"patch");
            String filterName =(String) dgData.get(rowIdx,"filterName");
            svr.setParam("tract", tract.toString());
            svr.setParam("patch", patch);
            svr.setParam("filterName", filterName);
            return LSSTImageSearch.createURLForDeepCoadd(tract.toString(), patch, filterName);
        }
        else{
            Long run =  (Long) dgData.get(rowIdx,"run");
            Integer camcol =  (Integer)dgData.get(rowIdx,"camcol");
            Long field =  (Long) dgData.get(rowIdx,"field");
            String filterName =  (String) dgData.get(rowIdx,"filterName");

            svr.setParam("run", run.toString());
            svr.setParam("camcol", camcol.toString());
            svr.setParam("field", field.toString());
            svr.setParam("filterName", filterName);
            return LSSTImageSearch.createURLForScienceCCD(run.toString(), camcol.toString(),field.toString(), filterName);
        }

    }


}
