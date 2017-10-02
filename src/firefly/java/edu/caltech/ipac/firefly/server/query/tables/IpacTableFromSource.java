/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


@SearchProcessorImpl(id = "IpacTableFromSource")
public class IpacTableFromSource extends IpacTablePartProcessor {
    public static final String TBL_TYPE = "tblType";
    public static final String TYPE_CATALOG = "catalog";
    public static final String URL_CHECK_FOR_NEWER = WebPlotRequest.URL_CHECK_FOR_NEWER;
    //public static final String TBL_INDEX = TableServerRequest.TBL_INDEX;     // the table to show if it's a multi-table file.
    private static final String SEARCH_REQUEST = "searchRequest";


    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String source = request.getParam(ServerParams.SOURCE);
        String altSource = request.getParam(ServerParams.ALT_SOURCE);
        String processor = request.getParam("processor");
        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        boolean checkForUpdates = request.getBooleanParam(URL_CHECK_FOR_NEWER, true);

        if (StringUtils.isEmpty(source) && processor != null) {
            return getByProcessor(processor, request);
        } else if (searchRequestJson != null) {
            // wrapping search request is useful to hide filters of the wrapped search request
            return SearchRequestUtils.fileFromSearchRequest(searchRequestJson);
        } else {
            // get source by source key
            File inf = getSourceFile(source, request, checkForUpdates);
            if (inf == null) {
                inf = getSourceFile(altSource, request, checkForUpdates);
            }

            if (inf == null) {
                throw new DataAccessException("Unable to read the source[alt_source] file:" + source + (StringUtils.isEmpty(altSource) ? "" : " [" + altSource + "]") );
            }

            if ( !ServerContext.isFileInPath(inf) ) {
                throw new SecurityException("Access is not permitted.");
            }
            return inf;
        }
    }

    private File getByProcessor(String processor, TableServerRequest request) throws DataAccessException {
        if (StringUtils.isEmpty(processor)) {
            throw new DataAccessException("Required parameter 'processor' is not given.");
        }
        TableServerRequest sReq = new TableServerRequest(processor, request);
        FileInfo fi = new SearchManager().getFileInfo(sReq);
        if (fi == null) {
            throw new DataAccessException("Unable to get file location info");
        }
        if (fi.getInternalFilename()== null) {
            throw new DataAccessException("File not available");
        }
        if (!fi.hasAccess()) {
            throw new SecurityException("Access is not permitted.");
        }
        return fi.getFile();
    }

    @Override
    public boolean doCache() {
        return false;
    }

    /**
     * resolve the file given a 'source' string.  it could be a local path, or a url.
     * if it's a url, download it into the application's workarea
     * @param source source file
     * @param request table request
     * @param checkForUpdates
     * @return file
     */
    private File getSourceFile(String source, TableServerRequest request, boolean checkForUpdates) {
        if (source == null) return null;
        try {
            URL url = makeUrl(source);
            if (url == null) {
                File f = ServerContext.convertToFile(source);
                if (f == null || !f.canRead()) return  null;

                StringKey key = new StringKey(getUniqueID(request), f.lastModified());
                File cached  = (File) getCache().get(key);
                if (cached != null) return cached;    // it's cached.. return it.

                File res = convertToIpacTable(f, request);
                getCache().put(key, res);
                return res;
            } else {
                StringKey key = new StringKey(getUniqueID(request), url);
                File res  = (File) getCache().get(key);

                String ext = FileUtil.getExtension(url.getPath());
                ext = StringUtils.isEmpty(ext) ? ".ul" : "." + ext;
                File nFile = createFile(request, ext);

                HttpURLConnection conn = (HttpURLConnection) URLDownload.makeConnection(url);
                if (res == null) {
                    URLDownload.getDataToFile(conn, nFile, null, false, true, false, Long.MAX_VALUE);
                    res = convertToIpacTable(nFile, request);
                    getCache().put(key, res);
                } else if (checkForUpdates) {
                    FileUtil.writeStringToFile(nFile, "workaround");
                    nFile.setLastModified(res.lastModified());
                    FileInfo finfo = URLDownload.getDataToFile(conn, nFile, null, false, true, true, Long.MAX_VALUE);
                    if (finfo.getResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
                        res = convertToIpacTable(nFile, request);
                        getCache().put(key, res);
                    }
                }
                return res;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        String type = request.getParam(TBL_TYPE);
        if (type == null || type.equals(TYPE_CATALOG)) {
            UserCatalogQuery.addCatalogMeta(defaults,columns,request);
        }
    }

    private URL makeUrl(String source) {
        try {
            return new URL(source);
        } catch (MalformedURLException e) {
            return null;
        }
    }

}

