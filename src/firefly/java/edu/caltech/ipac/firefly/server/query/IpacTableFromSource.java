/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


@SearchProcessorImpl(id = "IpacTableFromSource")
public class IpacTableFromSource extends IpacTablePartProcessor {

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String source = request.getParam(ServerParams.SOURCE);
        String altSource = request.getParam(ServerParams.ALT_SOURCE);
        boolean isFixedLength = request.getBooleanParam(TableServerRequest.FIXED_LENGTH, true);
        if (StringUtils.isEmpty(source)) {
            String processor = request.getParam("processor");
            if (StringUtils.isEmpty(processor)) {
                throw new DataAccessException("Required parameter 'source' is not given.");
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
            source = fi.getInternalFilename();
        }

        File inf = getSourceFile(source, request);
        if (inf == null) {
            inf = getSourceFile(altSource, request);
        }

        if (inf == null) {
            throw new DataAccessException("Unable to read the source[alt_source] file:" + source + (StringUtils.isEmpty(altSource) ? "" : " [" + altSource + "]") );
        }

        DataGroupReader.Format format = DataGroupReader.guessFormat(inf);

        if ( !VisContext.isFileInPath(inf) ) {
            throw new SecurityException("Access is not permitted.");
        }

        if (format == DataGroupReader.Format.IPACTABLE && isFixedLength) {
            // file is already in ipac table format
        } else {
            if ( format != DataGroupReader.Format.UNKNOWN) {
                // format is unknown.. convert it into ipac table format
                DataGroup dg = DataGroupReader.readAnyFormat(inf);
                inf = createFile(request, ".tbl");
                DataGroupWriter.write(inf, dg, 0);
            } else {
                throw new DataAccessException("Source file has an unknown format:" + source);
            }
        }

        return inf;
    }

    /**
     * resolve the file given a 'source' string.  it could be a local path, or a url.
     * if it's a url, download it into the application's workarea
     * @param source
     * @param request
     * @return
     */
    private File getSourceFile(String source, TableServerRequest request) {
        File inf = null;
        try {
            URL url = makeUrl(source);
            if (url == null) {
                inf = new File(source);
//                if (f.canRead()) {
//                    inf = createFile(request, ".tbl");
//                    FileUtils.copyFile(f, inf);
//                }
            } else {
                HttpURLConnection conn = (HttpURLConnection) URLDownload.makeConnection(url);
                int rcode = conn.getResponseCode();
                if (rcode >= 200 && rcode < 400) {
                    String sfname = URLDownload.getSugestedFileName(conn);
                    if (sfname == null) {
                        sfname = url.getPath();
                    }
                    String ext = sfname == null ? null : FileUtil.getExtension(sfname);
                    ext = StringUtils.isEmpty(ext) ? ".ul" : "." + ext;
                    inf = createFile(request, ext);
                    URLDownload.getDataToFile(conn, inf, null, false, true, true, Long.MAX_VALUE);
                }
            }
        } catch (Exception ex) {
            inf = null;
        }
        if (inf != null && inf.canRead()) {
            return inf;
        }

        return null;
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        for (Param p : request.getParams()) {
            if (request.isInputParam(p.getName())) {
                defaults.setAttribute(p.getName(), p.getValue());
            }
        }
        UserCatalogQuery.addCatalogMeta(defaults,columns,request);
    }

    private URL makeUrl(String source) {
        try {
            return new URL(source);
        } catch (MalformedURLException e) {
            return null;
        }
    }

}

