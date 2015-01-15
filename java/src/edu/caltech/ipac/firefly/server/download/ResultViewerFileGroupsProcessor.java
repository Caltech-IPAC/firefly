/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.caltech.ipac.firefly.ui.creator.CommonParams.DataSource;


@SearchProcessorImpl(id = "resultViewerDownload")
public class ResultViewerFileGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();



    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        Set<String> zipFiles = new HashSet<String>();

        Collection<Integer> selectedRows = request.getSelectedRows();

        ServerRequest searchR= request.getSearchRequest();
        SearchManager man= new SearchManager();
        SearchProcessor processor= man.getProcessor(searchR.getRequestId());
        DataGroupPart primaryData= (DataGroupPart)processor.getData(searchR);
        TableMeta meta= QueryUtil.getRawDataSet(primaryData).getMeta();
        processor.prepareTableMeta(meta, Collections.unmodifiableList(primaryData.getTableDef().getCols()), searchR);
        DataGroup dataGroup= primaryData.getData();



        List<FileInfo> retList= new ArrayList<FileInfo>();

        String srcStr= meta.getAttribute(CommonParams.PREVIEW_SOURCE_HEADER);
        DataSource dataSource;
        try {
            dataSource= Enum.valueOf(CommonParams.DataSource.class, srcStr);
        } catch (Exception e) {
           throw new DataAccessException("could not determine source of the fits file data");
        }
        String dataColumn= null;

        if (dataSource==DataSource.FILE || dataSource==DataSource.URL) {
            dataColumn= meta.getAttribute(CommonParams.PREVIEW_COLUMN_HEADER);
            if (dataColumn==null) {
                throw new DataAccessException("determine data source column");
            }
        }
        String fileProcessorID= null;
        if (dataSource==DataSource.REQUEST) {
            fileProcessorID= meta.getAttribute(CommonParams.SEARCH_PROCESSOR_ID);
            if (fileProcessorID==null) {
                throw new DataAccessException("determine data search processor");
            }
        }

        FileInfo fi;
        DataObject data;
        for (int rowIdx : selectedRows) {
            data= dataGroup.get(rowIdx);
            if (data!=null) {
                if (dataSource==DataSource.REQUEST) {
                    fi= makeFileInfoForRequest(data, fileProcessorID,meta, rowIdx);
                }
                else  {
                    fi= makeFileInfoForNonRequest(data, dataSource,dataColumn, rowIdx);
                }
                if (fi!=null) retList.add(fi);
            }
            else {
                logger.warn("Could not find data row #" + rowIdx);
            }
        }


        List<FileGroup> retval= new ArrayList<FileGroup>(1);
        retval.add(new FileGroup(retList,null,0,"Result Viewer"));
        return retval;
    }



    private FileInfo makeFileInfoForRequest(DataObject data,
                                            String id,
                                            TableMeta meta,
                                            int rowIdx) {

        FileInfo fi= null;
        ServerRequest sr= new ServerRequest(id);

        DataType dtAry[]= data.getDataDefinitions();
        for(DataType dt : dtAry) {
            Object v= data.getDataElement(dt.getKeyName());
            if (v!=null)  sr.setSafeParam(dt.getKeyName(), v.toString());
        }


        for(Map.Entry<String,String> entry : meta.getAttributes().entrySet()) {
            sr.setSafeParam(entry.getKey(),entry.getValue());
        }

        WebPlotRequest wpReq= WebPlotRequest.makeProcessorRequest(sr,"dummy title");
        FileRetriever retrieve= FileRetrieverFactory.getRetriever(wpReq);
        if (retrieve!=null) {
            try {
                FileData fileData = retrieve.getFile(wpReq);
                File f= fileData.getFile();
                fi= new FileInfo(f.getPath(), f.getName(), f.length());
            } catch (Exception e) {
                fi= null;
                logger.warn(e,"Could not retrieve file for row index: " + rowIdx);
            }
        }
        else {
            logger.warn("Could not a file retriever for row index: " + rowIdx);
        }

        return fi;
    }



    private FileInfo makeFileInfoForNonRequest(DataObject data,
                                               DataSource dataSource,
                                               String dataColumn,
                                               int rowIdx) {
        FileInfo fi= null;
        String fname= (String)data.getDataElement(dataColumn);
        if (fname!=null) {
            if (dataSource==DataSource.URL) {
                int i = fname.lastIndexOf('/');
                String extName= fname;
                if (i > 0 &&  i < fname.length() - 1) {
                    extName = fname.substring(i+1);
                }
                fi= new FileInfo(fname, extName ,0);
            }
            else if (dataSource==DataSource.FILE) {
                File f= new File(fname);
                if (f.canRead()) {
                    fi= new FileInfo(f.getPath(), f.getName(), f.length());
                }
                else {
                    logger.warn("Could not find file dataSource at row #" + rowIdx + " file: " + f.getPath());
                }
            }
        }
        else {
            logger.warn("Could not find dataSource at row #" + rowIdx);
        }
        return fi;
    }

}


