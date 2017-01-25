package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import static edu.caltech.ipac.firefly.server.util.Logger.getLogger;

/**
 * Created by zhang on 11/17/16.
 *
 *
 */

@SearchProcessorImpl(id = "LSSTFileGroupProcessor")
public class LSSTFileGroupProcessor  extends FileGroupsProcessor {
    //leave this line here in case we are going to use the property file later
    //public static final String LSST_FILESYSTEM_BASEPATH = AppProperties.getProperty("lsst.filesystem_basepath");
    private  Logger.LoggerImpl logger = getLogger();
    private static  long  DEEP_COADD_SIZE  = 44196480;
    private static  long  SCIENCE_CCD_SIZE = 44196480;
    @Override
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            logger.info("compute file groups");
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("failed at computing file groups");
            throw new DataAccessException(e.getMessage());
        }
    }



    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {

        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileGroup> fgArr = new ArrayList<>();

        // do folder or flat
        Set<String> zipFiles = new HashSet<>();
        String zipType = request.getParam("zipType");
        boolean zipFolders = true;
        if (zipType != null && zipType.equalsIgnoreCase("flat")) {
            zipFolders = false;
        }


        String baseFileName = request.getParam(DownloadRequest.BASE_FILE_NAME);
        boolean isDeepCoadd = baseFileName.equalsIgnoreCase("deepCoadd")? true:false;


        String[] sccdCols = { "scienceCcdExposureId","run",  "camcol", "field", "filterName"};
        String[] deeoCoaddCols={"deepCoaddId","tract", "patch", "filterName"};
        String[] columns= isDeepCoadd?deeoCoaddCols:sccdCols;

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, columns);
        ArrayList<FileInfo> fiArr = new ArrayList<>();

        long size = isDeepCoadd? DEEP_COADD_SIZE:SCIENCE_CCD_SIZE;
        for (int rowIdx : selectedRows) {

            String fileName = getFileName(isDeepCoadd, dgData,rowIdx);

            String urlStr = getDataURLString(dgData, rowIdx, isDeepCoadd);

            String extFileName =zipFolders? baseFileName + "/"+ fileName : fileName;

            if(!zipFiles.contains(urlStr)){
                zipFiles.add(urlStr);
            }

            FileInfo fileInfo =  new FileInfo(urlStr, extFileName, size );
            fiArr.add(fileInfo );

        }



        FileGroup fg = new FileGroup(fiArr, ServerContext.getTempWorkDir(), 0, "LSST Download Files");
        fgArr.add(fg);

        return fgArr;
    }

    private String getFileName(boolean isDeepCoadd, IpacTableParser.MappedData dgData, int rowIdx){
        String filterName =(String) dgData.get(rowIdx,"filterName");
        if (isDeepCoadd) {
            return  dgData.get(rowIdx,"deepCoaddId").toString()+"-"+filterName+".fits";

        }
        else{
           return dgData.get(rowIdx,"scienceCcdExposureId").toString()+"-"+filterName+".fits";
        }


    }
    private  String  getDataURLString( IpacTableParser.MappedData dgData, int rowIdx,  boolean isDeepCoadd) throws MalformedURLException {


        if (isDeepCoadd){
            Long tract = (Long) dgData.get(rowIdx,"tract");
            String patch = (String) dgData.get(rowIdx,"patch");
            String filterName =(String) dgData.get(rowIdx,"filterName");

            return LSSTImageSearch.createURLForDeepCoadd(tract.toString(), patch, filterName);
        }
        else{
            Long run =  (Long) dgData.get(rowIdx,"run");
            Integer camcol =  (Integer)dgData.get(rowIdx,"camcol");
            Long field =  (Long) dgData.get(rowIdx,"field");
            String filterName =  (String) dgData.get(rowIdx,"filterName");

            return LSSTImageSearch.createURLForScienceCCD(run.toString(), camcol.toString(),field.toString(), filterName);
        }

    }


}
