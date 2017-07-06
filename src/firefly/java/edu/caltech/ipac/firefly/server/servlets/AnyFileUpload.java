/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.FileInfo;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import org.json.simple.JSONObject;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.net.URL;

/**
 * Date: Feb 16, 2011
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class AnyFileUpload extends BaseHttpServlet {
    private static final Logger.LoggerImpl _LOG = Logger.getLogger();
    public static final String DEST_PARAM  = "dest";
    public static final String PRELOAD_PARAM = "preload";
    public static final String FILE_TYPE = "type";
    public static final String CACHE_KEY = "cacheKey";
    private enum FileType {FITS, TABLE, REGION, XML, UNKNOWN}

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        doFileUpload(req, res);
    }

    public static void doFileUpload(HttpServletRequest req, HttpServletResponse res) throws Exception {

        if (! ServletFileUpload.isMultipartContent(req)) {
            sendReturnMsg(res, 400, "Is not a Multipart request. Request rejected.", "");
        }
        StopWatch.getInstance().start("Upload File");

        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter = upload.getItemIterator(req);
        FileItemStream file = null;
        String url = null;

        HashMap<String, String> params = new HashMap<>();

        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            if (!item.isFormField()) {
                file = item;
                // file should be the last param.  param after file will be ignored.
                break;
            } else {
                String name = item.getFieldName();
                String value = FileUtil.readFile(item.openStream());
                params.put(name, value);
                if (name.equals("URL")) {
                    url = new String(value);
                }
            }
        }

        String dest = getParam(DEST_PARAM, params, req);
        String preload = getParam(PRELOAD_PARAM, params, req);
        String overrideCacheKey= getParam(CACHE_KEY, params, req);
        String fileType= getParam(FILE_TYPE, params, req);
        String fileAnalysis = getParam("fileAnalysis", params, req);

        if (file != null || url != null) {
            UploadFileInfo fi;
            String ext;
            File destDir;
            FileType fType;
            File uf;
            String fileName;
            FileInfo urlDownloadInfo = null;

            if (file != null) {
                fileName = file.getName();
            } else {
                int idx = url.lastIndexOf('/');
                fileName = (idx >= 0) ? url.substring(idx + 1) : new String(url);
            }
            ext = resolveExt(fileName);
            fType = resolveType(fileType, ext, (file != null ? file.getContentType() : null));
            destDir = resolveDestDir(dest, fType);
            uf = File.createTempFile("upload_", ext, destDir); // other parts of system depend on file name starting with "upload_"
            if (file != null) {
                InputStream inStream = new BufferedInputStream(file.openStream(), IpacTableUtil.FILE_IO_BUFFER_SIZE);
                FileUtil.writeToFile(inStream, uf);
            } else {
                urlDownloadInfo = URLDownload.getDataToFile(new URL(url), uf);
            }

            String rPathInfo = ServerContext.replaceWithPrefix(uf);
            fi = new UploadFileInfo(rPathInfo, uf, fileName, (file != null ? file.getContentType() : null));

            JSONObject analysisResult = null;
            if (fileAnalysis != null && fileAnalysis.equalsIgnoreCase("true")) {
                if (url != null && urlDownloadInfo != null &&
                                !(urlDownloadInfo.getResponseCodeMsg().equals("OK"))) {
                    throw new Exception("invalid upload from URL: " + urlDownloadInfo.getResponseCodeMsg());
                }

                analysisResult = createAnalysisResult(fi);
            }

            if (fType == FileType.TABLE) {
                uf = File.createTempFile("upload_", ".tbl", destDir); // cleaned ipac file.
                rPathInfo = ServerContext.replaceWithPrefix(uf);
                DataGroup dg = DataGroupReader.readAnyFormat(fi.getFile(), 0);
                DataGroupWriter.write(new DataGroupWriter.IpacTableHandler(uf, dg));
                fi = new UploadFileInfo(rPathInfo, uf, fileName, (file != null ? file.getContentType() : null));
            }
            String fileCacheKey = overrideCacheKey != null ? overrideCacheKey : rPathInfo;
            UserCache.getInstance().put(new StringKey(fileCacheKey), fi);

            if (analysisResult != null) {
                String resultS = analysisResult.toJSONString();
                String fFormat = (String)analysisResult.get("fileFormat");

                if (!StringUtils.isEmpty(resultS)) {
                    fileCacheKey = fileCacheKey + "::" + fFormat + "::" + resultS;
                }
            }

            sendReturnMsg(res, 200, null, fileCacheKey);

            Counters.getInstance().increment(Counters.Category.Upload, fi.getContentType());

        }

        StopWatch.getInstance().printLog("Upload File");
    }


    private static JSONObject createAnalysisResult(UploadFileInfo fi) throws Exception {
        JSONObject analysisModel = null;
        DataGroupReader.Format fileFormat = null;
        DataGroup dgAnalysis = null;
        String analysisSummary = "";

        JSONObject analysisResult = new JSONObject();

        fileFormat = DataGroupReader.guessFormat(fi.getFile());
        dgAnalysis = DataGroupReader.readAnyFormatHeader(fi.getFile(), fileFormat);
        if (dgAnalysis != null) {
            analysisSummary = dgAnalysis.getTitle();
            if (!analysisSummary.contains("invalid")) {
                analysisModel = toJsonAnalysisTableModel(dgAnalysis, fileFormat);
            }
        } else {
            analysisSummary = "invalid " + fileFormat.toString() + " file";
        }

        if (analysisSummary.startsWith("invalid")) {
            throw new Exception(analysisSummary);
        }

        analysisResult.put("status", 200);
        analysisResult.put("message", "");
        analysisResult.put("fileCacheKey", fi.getPname());
        analysisResult.put("analysisSummary", analysisSummary);
        analysisResult.put("fileFormat", fileFormat.toString());
        if (analysisModel != null) {
            analysisResult.put("analysisModel", analysisModel);
        }

        return analysisResult;
    }

    private static String getParam(String key, HashMap<String, String> params, HttpServletRequest req) {
        if (key == null) return null;
        if (params.containsKey(key)) {
            return params.get(key);
        } else {
            return req.getParameter(key);
        }
    }

    private static File resolveDestDir(String dest, FileType fType) throws FileNotFoundException {
        File destDir = ServerContext.getTempWorkDir();
/*
        removed.. this writes temp file into the source directory.  may be readonly.  why was it needed before?
        not sure of its history.  leaving comment as a reminder in case it breaks something else.
        if (!StringUtils.isEmpty(dest)) {
            destDir = ServerContext.convertToFile(dest);
        } else
*/
        if (fType == FileType.FITS) {
            destDir = ServerContext.getVisCacheDir();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination path does not exists: " + destDir.getPath());
        }
        return destDir;
    }


    private static String resolveExt(String fileName) {
        String ext = StringUtils.isEmpty(fileName) ? "" : FileUtil.getExtension(fileName);
        ext = StringUtils.isEmpty(ext) ? ".tmp" : "." + ext;
        return ext;
    }

    private static FileType resolveType(String fileType, String fileExtension, String contentType) {
        FileType ftype = FileType.UNKNOWN;
        try {
            ftype = FileType.valueOf(fileType);
        } catch (Exception e) {
            if (!StringUtils.isEmpty(fileExtension)) {
                if (fileExtension.equalsIgnoreCase(".fits")) {
                    ftype = FileType.FITS;
                } else if (fileExtension.matches("\\.tbl|\\.csv|\\.tsv")) {
                    ftype = FileType.TABLE;
                } else if (fileExtension.matches("\\.reg")) {
                    ftype = FileType.REGION;
                } else if (fileExtension.matches("\\.xml|\\.vot")) {
                    ftype = FileType.XML;
                }
            } else {
                // guess using contentType
                if (!StringUtils.isEmpty(contentType)) {
                    if (contentType.matches("image/fits|application/fits|image/fits")) {
                        ftype = FileType.FITS;
                    }
                }
            }
        }
        return ftype;
    }

    private boolean resolvePreload(String preload, FileType fileType) {
        if (StringUtils.isEmpty(preload)) {
            return fileType == FileType.FITS;
        } else {
            return Boolean.parseBoolean(preload);
        }
    }

    private static JSONObject toJsonAnalysisTableModel(DataGroup dg, DataGroupReader.Format ff ) {
        JSONObject tableModel = new JSONObject();
        JSONObject tableData = JsonTableUtil.toJsonTableData(dg, null);
        String tblId =  "UPLOAD_ANALYSIS";

        tableModel.put("tableData", tableData);
        tableModel.put("tbl_id", tblId);
        tableModel.put("title", dg.getTitle());
        tableModel.put("totalRows", dg.values().size());
        tableModel.put("fileFormat", ff.toString());
        tableModel.put("highlightedRow", 0);

        JSONObject tableMeta = new JSONObject();
        Iterator<Entry<String, DataGroup.Attribute>> attributes = dg.getAttributes().entrySet().iterator();

        while( attributes.hasNext() ) {
            Map.Entry<String, DataGroup.Attribute> entry = attributes.next();
            DataGroup.Attribute att = entry.getValue();

            tableMeta.put(att.getKey(), att.getValue());
        }

        tableModel.put("tableMeta", tableMeta);
        return tableModel;
    }

}

