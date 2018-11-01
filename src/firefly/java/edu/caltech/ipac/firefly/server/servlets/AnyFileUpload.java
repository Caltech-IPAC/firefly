/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.firefly.server.ws.WsServerCommands;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Date: Feb 16, 2011
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class AnyFileUpload extends BaseHttpServlet {
    private static final String DEST_PARAM  = "dest";
    private static final String PRELOAD_PARAM = "preload";
    private static final String FILE_TYPE = "type";
    private static final String FILE_NAME = "filename";
    private static final String CACHE_KEY = "cacheKey";
    private static final String WORKSPACE_PUT = "workspacePut";
    private enum FileType {FITS, TABLE, REGION, XML, UNKNOWN, PNG}

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        doFileUpload(req, res);
    }

    public static void doFileUpload(HttpServletRequest req, HttpServletResponse res) throws Exception {
        UploadFileInfo fi = null;
        File uf;
        File destDir = null;
        String ext;
        String fileName = null;
        String rPathInfo = "";
        String fileType;
        boolean fileAnalysis;
        String overrideCacheKey;
        String specifiedFileName;
        FileType fType = null;
        FileInfo urlDownloadInfo;
        FileItemStream file = null;
        SrvParam sp = new SrvParam(req.getParameterMap());

        if (! ServletFileUpload.isMultipartContent(req)) { // not file uploaded, just move file from server to workspace
            String wsCmd = sp.getOptional("wsCmd");

            if (wsCmd == null) {
                sendReturnMsg(res, 400, "Is not a Multipart request. Request rejected.", "");
            }

            StopWatch.getInstance().start("Upload File");

            fileAnalysis = sp.getOptionalBoolean("fileAnalysis", false);
            fileType = sp.getOptional(FILE_TYPE);
            overrideCacheKey = sp.getOptional(CACHE_KEY);
            WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
            rPathInfo = WsServerCommands.utils.upload(params1);
            // file in server uploaded from workspace
            uf = ServerContext.convertToFile(rPathInfo);
            fileName = params1.getRelPath().substring((params1.getRelPath().lastIndexOf("/")+1));
            fi = new UploadFileInfo(rPathInfo, uf, fileName, null);
            destDir = uf.getParentFile();
            ext = resolveExt(fileName, fileType);
            fType = resolveType(fileType, ext, null);

        } else { // upload file and put on server and optionally put in workspace as well
            StopWatch.getInstance().start("Upload File");

            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(req);
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
                        url = value;
                    }
                }
            }
            sp.addParams(params);

            String dest = sp.getOptional(DEST_PARAM);
//            String preload = sp.getOptional(PRELOAD_PARAM);
            overrideCacheKey = sp.getOptional(CACHE_KEY);
            fileType = sp.getOptional(FILE_TYPE);
            specifiedFileName = sp.getOptional(FILE_NAME);
            fileAnalysis = sp.getOptionalBoolean("fileAnalysis", false);

            if (file != null || url != null) {
                if (file != null) {
                    fileName = specifiedFileName!=null ? specifiedFileName : file.getName();
                } else {
                    int idx = url.lastIndexOf('/');
                    fileName = (idx >= 0) ? url.substring(idx + 1) : url;
                }
                //Check for filename max chars:
                fileName = fileName.length() > 255 ? fileName.substring(fileName.length() - 255) : fileName;
                ext = resolveExt(fileName, fileType);
                fType = resolveType(fileType, ext, (file != null ? file.getContentType() : null));
                destDir = resolveDestDir();
                uf = File.createTempFile("upload_", ext, destDir); // other parts of system depend on file name starting with "upload_"
                if (file != null) {
                    InputStream inStream = new BufferedInputStream(file.openStream(), IpacTableUtil.FILE_IO_BUFFER_SIZE);
                    FileUtil.writeToFile(inStream, uf);
                } else {
                    urlDownloadInfo = URLDownload.getDataToFile(new URL(url), uf);

                    if (urlDownloadInfo != null && !(urlDownloadInfo.getResponseCodeMsg().equals("OK"))) {
                        throw new Exception("invalid upload from URL: " + urlDownloadInfo.getResponseCodeMsg());
                    }
                }

                rPathInfo = ServerContext.replaceWithPrefix(uf);
                fi = new UploadFileInfo(rPathInfo, uf, fileName, (file != null ? file.getContentType() : null));


                /// -- check if going all the way to workspace
                if (sp.getOptionalBoolean(WORKSPACE_PUT, false)) {
                    WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
                    WsResponse wsRes= WsServerCommands.utils.putFile(params1 ,uf );

                }
            }
        }

        JSONObject analysisResult = null;
        if (fi != null) {
            if (fileAnalysis) {
                analysisResult = createAnalysisResult(fi);
            }

            if (fType != null && fType == FileType.TABLE) {
                uf = File.createTempFile("upload_", ".tbl", destDir); // cleaned ipac file.
                rPathInfo = ServerContext.replaceWithPrefix(uf);
                DataGroup dg = TableUtil.readAnyFormat(fi.getFile(), 0);
                IpacTableWriter.save(uf, dg);
                fi = new UploadFileInfo(rPathInfo, uf, fileName, (file != null ? file.getContentType() : null));
            }

            String fileCacheKey = overrideCacheKey != null ? overrideCacheKey : rPathInfo;
            UserCache.getInstance().put(new StringKey(fileCacheKey), fi);

            if (analysisResult != null) {
                String resultS = analysisResult.toJSONString();
                String fFormat = (String) analysisResult.get("fileFormat");

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
        String analysisSummary;

        JSONObject analysisResult = new JSONObject();
        File f = fi.getFile();
        long size = f.length();

        TableUtil.Format fileFormat = TableUtil.guessFormat(f);
        DataGroup dgAnalysis = TableUtil.readAnyFormatHeader(f, fileFormat);
        if (dgAnalysis != null) {
            analysisSummary = dgAnalysis.getTitle();
            if (!analysisSummary.contains("invalid")) {
                analysisModel = toJsonAnalysisTableModel(dgAnalysis, fileFormat, size);
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

    private static File resolveDestDir() throws FileNotFoundException {
        File destDir = ServerContext.getUploadDir();
        if (!destDir.exists()) throw new FileNotFoundException("Destination path does not exists: " + destDir.getPath());
        return destDir;
    }


    private static String resolveExt(String fileName, String fType) {
        String ext = StringUtils.isEmpty(fileName) ? "" : "." + FileUtil.getExtension(fileName);
        if (StringUtils.isEmpty(ext)) {
            ext =  fType==null ? ".tmp" : "." + fType.toLowerCase();
        }
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

//    private boolean resolvePreload(String preload, FileType fileType) {
//        if (StringUtils.isEmpty(preload)) {
//            return fileType == FileType.FITS;
//        } else {
//            return Boolean.parseBoolean(preload);
//        }
//    }

    private static JSONObject toJsonAnalysisTableModel(DataGroup dg, TableUtil.Format ff, long size ) {
        JSONObject tableModel = new JSONObject();
        JSONObject tableData = JsonTableUtil.toJsonTableData(dg);
        String tblId =  "UPLOAD_ANALYSIS";

        tableModel.put("tableData", tableData);
        tableModel.put("tbl_id", tblId);
        tableModel.put("title", dg.getTitle());
        tableModel.put("totalRows", dg.values().size());
        tableModel.put("fileFormat", ff.toString());
        tableModel.put("highlightedRow", 0);
        tableModel.put("size", size);

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

