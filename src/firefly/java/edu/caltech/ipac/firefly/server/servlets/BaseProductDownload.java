/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/20/13
 * Time: 6:29 PM
 * To change this template use File | Settings | File Templates.
 * testing urls:

 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=DSS&dss_bands=poss2ukstu_ir&mode=accessUrl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=DSS&dss_bands=poss2ukstu_ir&mode=accessWithAnc1Url
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=DSS&dss_bands=poss2ukstu_ir&mode=fitsurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=large&sources=DSS&dss_bands=poss2ukstu_ir&mode=jpgurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=small&sources=DSS&dss_bands=poss2ukstu_ir&mode=shrunkjpgurl

 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=SDSS&sdss_bands=u&mode=accessUrl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=SDSS&sdss_bands=u&mode=accessWithAnc1Url
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=SDSS&sdss_bands=u&mode=fitsurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=large&sources=SDSS&sdss_bands=u&mode=jpgurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=small&sources=SDSS&sdss_bands=u&mode=shrunkjpgurl

 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=twomass&twomass_bands=k&mode=accessUrl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=twomass&twomass_bands=k&mode=accessWithAnc1Url
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=twomass&twomass_bands=k&mode=fitsurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=large&sources=twomass&twomass_bands=k&mode=jpgurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=small&sources=twomass&twomass_bands=k&mode=shrunkjpgurl

 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=IRIS&iras_bands=12&mode=accessUrl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=IRIS&iras_bands=12&mode=accessWithAnc1Url
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=IRIS&iras_bands=12&mode=fitsurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=large&sources=IRIS&iras_bands=12&mode=jpgurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=small&sources=IRIS&iras_bands=12&mode=shrunkjpgurl

 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=WISE&wise_bands=4&mode=accessUrl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=WISE&wise_bands=4&mode=accessWithAnc1Url
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=medium&sources=WISE&wise_bands=4&mode=fitsurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=large&sources=WISE&wise_bands=4&mode=jpgurl
 http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=148.88822&DEC=69.06529&SIZE=0.5&subsize=0.5&thumbnail_size=small&sources=WISE&wise_bands=4&mode=shrunkjpgurl

 */
public class BaseProductDownload extends BaseHttpServlet {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String DOWNLOAD = "download";
    private static final String QUERY = "query";
    private static final Logger.LoggerImpl STATS_LOG = Logger.getLogger(Logger.DOWNLOAD_LOGGER);
    public static final String BASE_PRODUCT_DOWNLOAD="BaseProductDownload";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LOG.debug("Query string", req.getQueryString());
        Map origParamMap = req.getParameterMap();
        Map<String, String> paramMap = new HashMap<String,String>();
        // parameters could be upper or lower case
        for (Object p : origParamMap.keySet()) {
            if (p instanceof String) {
                paramMap.put((String)p, (((String[])origParamMap.get(p))[0]).trim());
            }
        }
        try {
            DownloadRequest downloadRequest = new DownloadRequest(getRequest(paramMap),"","");
            if (paramMap.containsKey("file_type"))downloadRequest.setParam("file_type", paramMap.get("file_type"));
            downloadRequest.getSearchRequest().setParam(BASE_PRODUCT_DOWNLOAD);
            downloadRequest.getSearchRequest().setPageSize(Integer.MAX_VALUE);

            List<FileGroup>  data = (List<FileGroup>)
                    SearchProcessorFactory.getProcessor(paramMap.get(DOWNLOAD)).getData(downloadRequest);
            //sendTable(res, paramMap, dgPart);
            int size = data.get(0).getSize();
            if (size<1) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: no file returned. "+ downloadRequest);
            } else if (size==1) {
                sendSingleProduct(data.get(0).getFileInfo(0), res);
            } else {
                String fname = ("FinderChartFiles_"+req.getParameter("RA")+"+"+req.getParameter("DEC")
                        +"_"+req.getParameter("SIZE")).replaceAll("\\+\\-","\\-");
                sendZip(fname, data.get(0), res);
            }
        } catch (Exception e) {
            LOG.error(e);

        }

    }

    private void sendSingleProduct(FileInfo fi, HttpServletResponse res) throws IOException {
        String extName = fi.getExternalName();
        String mimeType;
        String encoding;

        // check the type of the file
        encoding = null;
        boolean inline = false;
        if (extName.endsWith(".H")) {
            mimeType = "image/fits";
            encoding = "x-H";
        } else if (extName.endsWith(".fits")) {
            mimeType = "image/fits";
        } else if (extName.endsWith(".gif")) {
            mimeType = "image/gif";
            inline = true;
        } else if (extName.endsWith(".jpg")) {
            mimeType = "image/jpeg";
            inline = true;
        } else if (extName.endsWith(".png")) {
            mimeType = "image/png";
            inline = true;
        } else if (extName.endsWith(".tbl") || extName.endsWith(".txt") || extName.endsWith(".log")) {
            mimeType = "text/plain";
            inline = true;
        } else {
            mimeType = "application/octet-stream";
        }

        File f = new File(fi.getInternalFilename());

        long fSize = f.length();
        if (fSize == 0) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: unable to get "+extName);
            return;
        }
        // now we know that at least one file is available
        // commit to HttpResponse

        BufferedOutputStream buffOS = new BufferedOutputStream(res.getOutputStream());
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DataInputStream dis  = new DataInputStream(bis);

        res.setContentType(mimeType);
        if (encoding != null) {
            res.setHeader("Content-encoding", encoding);
        }
        String fSizeS = Long.toString(fSize);
        int fSizeInt = Integer.parseInt(fSizeS);
        String baseName = (new File(extName)).getName();
        res.setContentLength(fSizeInt);
        res.setHeader("Content-disposition",
                (inline ? "inline" : "attachment")+"; filename=" +
                        baseName);



        // copy file from the input steam to servlet output stream
        getFileFromStream(dis, buffOS, fSize);
    }

    private void sendZip(String fname, FileGroup fiSet, HttpServletResponse resp) throws IOException{
        ZipOutputStream zout = new ZipOutputStream(resp.getOutputStream());
        zout.setComment("Source:");
        zout.setMethod(ZipOutputStream.DEFLATED);

       // set HTTP header fields
        resp.setContentType("application/zip");
        resp.setHeader("Content-disposition", "attachment"+"; filename="+fname+".zip");

        List<FileInfo> failed = null;
        for (FileInfo fi : fiSet) {
            File f = new File(fi.getInternalFilename());
            if (f.exists() && f.canRead()) {
                try {
                    ZipEntry zipEntry = new ZipEntry(fi.getExternalName());
                    zout.putNextEntry(zipEntry);
                    FileInputStream fis = new FileInputStream(fi.getInternalFilename());
                    BufferedInputStream  bis = new BufferedInputStream(fis);
                    DataInputStream dis  = new DataInputStream (bis);
                    getFileFromStream(dis, zout, f.length());
                    zout.closeEntry();
                } catch (Exception e) {
                    log("Failed to package: "+fi.getInternalFilename(), e);
                    failed.add(fi);
                }
            } else {
                log("Can not access file "+f.getAbsolutePath());
                if (failed == null) failed = new ArrayList<FileInfo>();
                failed.add(fi);
            }
        }
        // report failures
        if (failed != null) {
            ZipEntry zipEntry = new ZipEntry("README.txt");
            zout.putNextEntry(zipEntry);
            PrintWriter pw = new PrintWriter(zout);
            pw.println("\nErrors were encountered when packaging "+failed.size()+" files: \n");
            for (FileInfo fi : failed) {
                pw.println(fi.getExternalName());
            }
            pw.flush();
            zout.closeEntry();
        }
        zout.close();
    }

    private void getFileFromStream(InputStream is, OutputStream os, long filesize)
        throws IOException {
        int inBufSize = 1024;
        byte[] data = new byte[inBufSize];
        long bytesToRead = filesize;
        int retVal;

        try {
            while (bytesToRead > 0) {
                /**
                 *  Casting of long to int here is OK. The if statement
                 *  guarantees that by the time we fall into the case where
                 *  we're doing the casting bytesToRead will be "castable"
                 */
                if (inBufSize < bytesToRead) retVal = is.read(data,0,inBufSize);
                else retVal = is.read(data,0,(int)bytesToRead);
                if (retVal < 1)
                    throw new IOException ("Unexpected EOF from network peer.");
                bytesToRead -= retVal;
                os.write(data,0,retVal);
                os.flush();
            }
            logActivity(filesize);
        } catch (IOException e) {
            log("[ProductDownload] IOException ["+e.getMessage()+"]");
            log("[ProductDownload] Flush remaining data from peer");
            try {
                while (bytesToRead > 0) {
                    if (inBufSize < bytesToRead) retVal = is.read(data,0,inBufSize);
                    else retVal = is.read(data,0,(int)bytesToRead);
                    if (retVal < 1) break;
                    bytesToRead -= retVal;
                }
            } catch (Exception ee) {
                log("ERROR: "+ee.getMessage()+" while trying to flush remaining data from FTZ");
            }
            throw e;
        }
    }

    private TableServerRequest getRequest(Map<String, String> paramMap) {
        TableServerRequest searchReq = new TableServerRequest(paramMap.get(QUERY));
        for (Map.Entry<String, String> e: paramMap.entrySet()) {
            searchReq.setParam(e.getKey(),e.getValue());
        }
        return searchReq;
    }

    private static void logActivity(long bytes) {
        String logStr= "Stream_File: " +
                " size: " +  FileUtil.getSizeAsString(bytes) +
                ", bytes: " + bytes;
        LOG.briefInfo(logStr);
        STATS_LOG.stats("Stream_File", "fsize(MB)", (double)bytes/ StringUtils.MEG, "bytes", bytes);
    }

}
