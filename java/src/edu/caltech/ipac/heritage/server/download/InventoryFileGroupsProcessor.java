package edu.caltech.ipac.heritage.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.heritage.data.entity.download.InventoryDownloadRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;

import java.io.File;
import java.io.IOException;
//import java.net.URL;
//import java.net.URLConnection;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author tatianag
 *         $Id: InventoryFileGroupsProcessor.java,v 1.5 2011/03/25 22:41:49 tatianag Exp $
 */
@SearchProcessorImpl(id ="inventoryDownload")
public class InventoryFileGroupsProcessor extends FileGroupsProcessor {

   public static final String INVENTORY_BASE_PREFIX = AppProperties.getProperty("download.inventory.base");
   private static final Logger.LoggerImpl log= Logger.getLogger();

   public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert(request instanceof InventoryDownloadRequest);
        try {
            return  computeFileGroup((InventoryDownloadRequest)request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(InventoryDownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        Collection<Integer> selectedRows= request.getSelectedRows();
        boolean includeRelated= request.includeRelated();
        String setid = request.getSearchRequest().getParam("set");
        if (setid == null) setid="Requested";
        TableServerRequest searchRequest = request.getSearchRequest();
        searchRequest.setPageSize(0);
        DataGroupPart dgp = new SearchManager().getDataGroup(searchRequest);

        ArrayList<String> pathCols = new ArrayList<String>();
        ArrayList<String> relatedCols = new ArrayList<String>();
        String colName;
        for (DataType dt : dgp.getData().getDataDefinitions()) {
            colName = dt.getKeyName();
            if (colName.equals("fname") ||
                    colName.equals("coverage") ||
                    colName.equals("uncertainty") ||
                    colName.equals("mask")) {
                pathCols.add(colName);
            }
            if (colName.endsWith("_u") || colName.endsWith("_U")) {
                relatedCols.add(colName);
            }
        }

        // if primary columns are not found get all files referenced by path columns
        boolean checkExt = false;
        if (!includeRelated && pathCols.size() < 1) {
            includeRelated = true;
            checkExt = true;
        }
        if (includeRelated) {
            pathCols.addAll(relatedCols);
        }

        if (pathCols.size() < 1) {
            throw new DataAccessException("Unable to get path columns from "+dgp.getTableDef().getSource());
        }

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, pathCols.toArray(new String[1]));

        ArrayList<String> paths = new ArrayList<String>();
        String path;
        for(int rowIdx : selectedRows) {
            for(String col : pathCols) {
                path = (String)dgData.get(rowIdx, col);
                if (path != null && !path.equals("none")) {
                    if (path.contains(";")) {
                        String [] cpaths = Pattern.compile(";").split(path);
                        paths.addAll(Arrays.asList(cpaths));
                    } else if (!checkExt || (path.contains("fits") || path.contains("tbl")) ) {        
                        paths.add(path);
                    }
                }
            }
        }
        return doComputeFileGroup(paths, setid);

    }

    private List<FileGroup> doComputeFileGroup(ArrayList<String> paths, String desc) throws IOException {
        List<FileGroup> fgs = new ArrayList<FileGroup>(1);
        Set<FileInfo> fi= new LinkedHashSet<FileInfo>();
        long fsize;
        long totalSize = 0;
        File baseDir = null;
        int defFileSize = 20000;
        if (INVENTORY_BASE_PREFIX.contains("://")) {
            fsize = 0;
            for(String path : paths) {
                String urlPath = INVENTORY_BASE_PREFIX+File.separator+path;
                /**
                URLConnection conn = null;
                try {
                    URL url = new URL(urlPath);
                    conn = url.openConnection();
                    fsize = conn.getContentLength();
                    if(fsize < 0) {
                        log.warn("Could not determine file size for "+urlPath);
                        fsize=defFileSize;
                    }
                } catch (Exception e) {
                    log.error(e, "Unable to get info for product at url "+urlPath);
                    fsize=defFileSize;
                } finally {
                    if (conn != null) {
                        try {
                            conn.getInputStream().close();
                        } catch (Exception e) { log.error(e, "Unable to close url connection"); }
                    }
                }
                totalSize += fsize;
                */
                fi.add(new FileInfo(urlPath, path, fsize));
            }
        } else {
            baseDir = new File(INVENTORY_BASE_PREFIX);

            File f;
            for(String path : paths) {
                f = new File(baseDir, path);
                if (f.exists()) {
                    fsize = f.length();
                } else {
                    log.warn("File does not exist: "+f.getAbsolutePath());
                    fsize = defFileSize;
                }
                totalSize += fsize;
                fi.add(new FileInfo(path, path, fsize));

            }
        }

        FileGroup fg = new FileGroup(fi, baseDir, totalSize, desc);
        fgs.add(fg);
        return fgs;
    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
