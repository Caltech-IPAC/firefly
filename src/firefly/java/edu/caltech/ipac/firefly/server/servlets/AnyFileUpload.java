/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.ws.WsServerCommands;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Date: Feb 16, 2011
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class AnyFileUpload extends BaseHttpServlet {
    private static final String FILE_NAME = "filename";
    private static final String CACHE_KEY = "cacheKey";
    private static final String WORKSPACE_PUT = "workspacePut";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        doFileUpload(req, res);
    }

    public static void doFileUpload(HttpServletRequest req, HttpServletResponse res) throws Exception {

        StopWatch.getInstance().start("doFileUpload");

        FileItemStream uploadedItem = null;

        // processes the parameters...
        SrvParam sp = new SrvParam(req.getParameterMap());

        if (ServletFileUpload.isMultipartContent(req)) {        // this is a multipart request.. extract params from parts
            ServletFileUpload upload = new ServletFileUpload( new DiskFileItemFactory(64 * 1024, ServerContext.getUploadDir()));        // set factory to raise in-memory usage
            for (FileItemIterator iter = upload.getItemIterator(req); iter.hasNext(); ) {
                FileItemStream item = iter.next();
                if (item.isFormField()) {
                    sp.setParam(item.getFieldName(), FileUtil.readFile(item.openStream()));
                } else {
                    uploadedItem = item;
                    break;
                    // file should be the last param.  param after file will be ignored.
                }
            }
        }

        // handle upload file request.. results in saved as an UploadFileInfo
        UploadFileInfo fileInfo = null;

        String wsCmd = sp.getOptional("wsCmd");
        String fromUrl = sp.getOptional("URL");

        if (wsCmd != null) {
            // from workspace.. get the file using workspace api
            fileInfo = getFileFromWorkspace(sp);
        } else if (fromUrl != null) {
            // from a URL.. get it
            int idx = fromUrl.lastIndexOf('/');
            String fname = (idx >= 0) ? fromUrl.substring(idx + 1) : fromUrl;
            File tmpFile = File.createTempFile("upload_", fname, ServerContext.getUploadDir());
            FileInfo status = URLDownload.getDataToFile(new URL(fromUrl), tmpFile);
            if (status != null && !(status.getResponseCodeMsg().equals("OK"))) {
                throw new Exception("invalid upload from URL: " + status.getResponseCodeMsg());
            }
            fileInfo = new UploadFileInfo(ServerContext.replaceWithPrefix(tmpFile), tmpFile, fname, null);

        } else if (uploadedItem != null) {
            // it's a stream from multipart.. write it to disk
            String name = uploadedItem.getName();
            File tmpFile = File.createTempFile("upload_", "_" + name, ServerContext.getUploadDir());
            FileUtil.writeToFile(uploadedItem.openStream(), tmpFile);
            fileInfo = new UploadFileInfo(ServerContext.replaceWithPrefix(tmpFile), tmpFile, name, uploadedItem.getContentType());
        }

        /// -- check if going all the way to workspace
        if (sp.getOptionalBoolean(WORKSPACE_PUT, false)) {
            WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
            WsServerCommands.utils.putFile(params1 ,fileInfo.getFile() );
        }

        // modify file name if requested
        StringUtils.applyIfNotEmpty(sp.getOptional(FILE_NAME), fileInfo::setFileName);

        // save info in a cache for downstream use
        String fileCacheKey = sp.getOptional(CACHE_KEY);
        fileCacheKey = fileCacheKey == null ? fileInfo.getPname() : fileCacheKey;
        UserCache.getInstance().put(new StringKey(fileCacheKey), fileInfo);

        // returns the fileCacheKey
        String returnVal = fileCacheKey;

        String doAnalysis = sp.getOptional("fileAnalysis");
        if (doAnalysis != null && !doAnalysis.toLowerCase().equals("false")) {
            StopWatch.getInstance().start("doAnalysis");

            FileAnalysis.ReportType  reportType = getReportType(doAnalysis);
            FileAnalysis.Report report = FileAnalysis.analyze(fileInfo.getFile(), reportType);
            report.setFileName(fileInfo.getFileName());
            returnVal = returnVal + "::" + FileAnalysis.toJsonString(report);   // appends the report to the end of the returned String

            StopWatch.getInstance().printLog("doAnalysis");
        }

        sendReturnMsg(res, 200, null, returnVal);
        Counters.getInstance().increment(Counters.Category.Upload, fileInfo.getContentType());

        StopWatch.getInstance().printLog("doFileUpload");
    }

//====================================================================
//
//====================================================================
    private static FileAnalysis.ReportType getReportType(String type) {
        try {
            return FileAnalysis.ReportType.valueOf(type);
        } catch (Exception e) {
            return FileAnalysis.ReportType.Details;
        }

    }

    private static UploadFileInfo getFileFromWorkspace(SrvParam sp) throws IOException, FailedRequestException {
        WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
        String rPathInfo = WsServerCommands.utils.upload(params1);      // it's called upload, but it's actually pulling the data from workspace and saving it here.
        File uf = ServerContext.convertToFile(rPathInfo);
        String fileName = params1.getRelPath().substring((params1.getRelPath().lastIndexOf("/") + 1));
        return new UploadFileInfo(rPathInfo, uf, fileName, null);
    }


}

