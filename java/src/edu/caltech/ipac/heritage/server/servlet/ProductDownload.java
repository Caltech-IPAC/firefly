package edu.caltech.ipac.heritage.server.servlet;


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.heritage.server.persistence.FileInfoDao;
import edu.caltech.ipac.heritage.server.persistence.HeritageSecurityModule;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.ThrowableUtil;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 *
 *
 * <BR> Copyright (C) 1999-2003 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged. <BR>
 * @version $Id: ProductDownload.java,v 1.11 2012/10/19 23:02:33 tatianag Exp $
 */
public class ProductDownload extends HttpServlet {
    private static final String PARAM__ID = "ID";
    private static final String PARAM__DATASET = "DATASET";
    private static final String PARAM__OPTIONS = "OPTIONS";

    //public final static String SERVICE_PATH = AppProperties.getProperty("service.url.common", "ServicePath");

    enum Options {anc1, anc2, cals}

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final Logger.LoggerImpl STATS_LOG = Logger.getLogger(Logger.DOWNLOAD_LOGGER);


    /**
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        log("BEGIN: init()");


        log("END: init()");
    }

    /**
     * The servlet should accept id and type
     *      id (integer) can be either bcdid for level1 or pbcdid for level2
     *      type is the type of the download
     *          "level2"     post-bcd product (post-bcd fits or spectra)
     *          "level1"     bcd product (bcd fits or spectra)
     * @param req  http request
     * @param resp http response
     * @throws java.io.IOException on IO error
     * @throws javax.servlet.ServletException on servlet error
     */
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        log("BEGIN: processRequest()");

        log("Servlet queryString = " + req.getQueryString());

        int id;

        Map paramMap = req.getParameterMap();

        if(!paramMap.containsKey(PARAM__ID)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "MISSING PARAMETER: "+PARAM__ID+" is required to process this request.");
            return;
        }
        else {
            try {
                id = Integer.parseInt(((String[])paramMap.get(PARAM__ID))[0]);
            } catch  (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "INVALID PARAMETER: integer id is required to process this request.");
                return;
            }
        }

        if(!paramMap.containsKey(PARAM__DATASET)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "MISSING PARAMETER: "+ PARAM__DATASET +" (level1 or level2) is required to process this request.");
            return;
        }
        String dataset = ((String[])paramMap.get(PARAM__DATASET))[0];

        boolean level1;
        if (dataset.toLowerCase().endsWith("level1")) {
            level1 = true;
        } else if (dataset.toLowerCase().endsWith("level2")) {
            level1 = false;
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "BAD PARAMETER: "+ PARAM__DATASET +" must be either level1 or level2.");
            return;

        }        

        List<String> options;
        if(paramMap.containsKey(PARAM__OPTIONS)) {
            String optionsParam = ((String[])paramMap.get(PARAM__OPTIONS))[0];
            options = Arrays.asList(Pattern.compile(",").split(optionsParam));
        } else {
            options = new ArrayList<String>();
        }
        try {
            if (options.size() < 1) {
                sendSingleProduct(resp, id, level1);
            } else {
                sendZip(resp, id, level1, options);
            }

        } catch (Throwable e) {
            log("Unexpected Exception: "+e.getMessage());
            log(ThrowableUtil.getStackTraceAsString(e));
        }
        finally {
            log("END: processRequest()");
        }
    }


    private void sendSingleProduct(HttpServletResponse resp, int id, boolean level1) throws IOException {
        String sql;

        SimpleJdbcTemplate openedJdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);


        if(level1) {
            sql = "select heritagefilename as heritagefilename, externalname as externalname, reqkey as reqkey from bcdproducts where bcdid= "+id+"; ";
        }
        else {
            sql = "select heritagefilename as heritagefilename, externalname as externalname, reqkey as reqkey from postbcdproducts where pbcdid= "+id+"; ";
        }
        log(sql);

        String fileName;
        String extName;
        Integer reqkey;
        try {
            Map hm = openedJdbc.queryForMap(sql, new HashMap());
            fileName = (String)hm.get("heritagefilename");
            extName = (String)hm.get("externalname");
            reqkey = (Integer)hm.get("reqkey");
        } catch (Exception e) {
            log("Unable to get file for "+PARAM__ID+" "+id, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "BAD PARAMETER: unable to get file for "+PARAM__ID+" "+id);
            return;
        }

        if (!HeritageSecurityModule.checkHasAccess(reqkey.toString())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "BAD PARAMETER: access to "+PARAM__ID+" "+id+" is restricted.");
            return;
        }

        if (fileName == null || extName == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "NO DATA: unable to get product with "+PARAM__ID+" "+id);
            return;
        }
        DataInputStream dis;

        // output streams - will be set after it's clear that at least 1 file is available
        BufferedOutputStream buffOS = null;

        long fSize;

        String mimeType;
        String encoding;
        try {

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
            } else if (extName.endsWith(".tbl") || extName.endsWith(".txt") || extName.endsWith(".log")) {
                mimeType = "text/plain";
                inline = true;
            } else {
                mimeType = "application/octet-stream";
            }

            File f = getFile(fileName);

            fSize = f.length();
            if (fSize == 0) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: unable to get "+extName);
                return;

            }
            // now we know that at least one file is available
            // commit to HttpResponse

            buffOS = new BufferedOutputStream(resp.getOutputStream());
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream  bis = new BufferedInputStream(fis);
            dis  = new DataInputStream (bis);

            resp.setContentType(mimeType);
            if (encoding != null) {
                resp.setHeader("Content-encoding", encoding);
            }
            String fSizeS = Long.toString(fSize);
            int fSizeInt = Integer.parseInt(fSizeS);
            String baseName = (new File(extName)).getName();
            resp.setContentLength(fSizeInt);
            resp.setHeader("Content-disposition",
                    (inline ? "inline" : "attachment")+"; filename=" +
                            baseName);



            // copy file from the input steam to servlet output stream
            getFileFromStream(dis, buffOS, fSize);


        } catch (Exception fse) {
            if (buffOS == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to retrieve file " + extName);
            }
            log("ERROR: Unable to retrieve");
            log(ThrowableUtil.getStackTraceAsString(fse));
        } finally {

            if (buffOS != null) {
                try {
                    buffOS.close();
                } catch (Exception e) {
                    log("ERROR: Can not close output stream. "+e.getMessage());
                }
            }
        }

    }

    private void sendZip(HttpServletResponse resp, int id, boolean level1, List<String> options) throws IOException {
        boolean zipWithAnc1 = options.contains(Options.anc1.name());
        boolean zipWithAnc2 = options.contains(Options.anc2.name());
        boolean zipWithCals = options.contains(Options.cals.name());
        if (!zipWithAnc1 && !zipWithAnc2 && !zipWithCals) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid options");
            return;
        }


        ArrayList<Integer> idList = new ArrayList<Integer>();
        idList.add(id);
        DataSource ds;
        try {
            ds = JdbcFactory.getDataSource(DbInstance.archive);
        } catch (Exception e) {
            log("Unable to get data source connection", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Data access error");
            return;

        }
        Connection conn = null;
        Set<FileInfo> fiSet = null;
        Set<FileInfo> aSet;
        try {
            conn = DataSourceUtils.getConnection(ds);
            if (level1) {
                // zip bcd and related products
                if (zipWithAnc1) {fiSet = FileInfoDao.getBcdFileInfo(idList, conn);}
                if (zipWithAnc2) {
                    aSet = FileInfoDao.getBcdAncilFileInfo(idList, conn);
                    if (fiSet == null) { fiSet = aSet; }
                    else { fiSet.addAll(aSet); }
                }
                if (zipWithCals) {
                    aSet = FileInfoDao.getBcdCalFileInfo(idList, conn);
                    if (fiSet == null) { fiSet = aSet; }
                    else { fiSet.addAll(aSet); }
                }
            } else {
                if (zipWithAnc1) {fiSet = FileInfoDao.getPbcdFileInfo(idList, conn);}
                if (zipWithAnc2) {
                    aSet = FileInfoDao.getPbcdAncilFileInfo(idList, conn);
                    if (fiSet == null) { fiSet = aSet; }
                    else { fiSet.addAll(aSet); }
                }
                if (zipWithCals) {
                    aSet = FileInfoDao.getPbcdCalFileInfo(idList, conn);
                    if (fiSet == null) { fiSet = aSet; }
                    else { fiSet.addAll(aSet); }
                }
            }
        } catch (Exception e) {
            log("Unable to get file info", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Data access error");
            return;
        } finally {
            if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
        }

        if (fiSet == null || fiSet.size() < 1) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Nothing to package");
            return;            
        }

        ZipOutputStream zout = new ZipOutputStream(resp.getOutputStream());
        zout.setComment("Source: Spitzer Heritage Archive ["+(level1 ? "bcd=":"postbcd="+id)+"]");
        zout.setMethod(ZipOutputStream.DEFLATED);

       // set HTTP header fields
        resp.setContentType("application/zip");
        resp.setHeader("Content-disposition",
                       "attachment"+"; filename="+(level1 ? "bcd-"+id:"postbcd-"+id)+".zip");

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
            ZipEntry zipEntry = new ZipEntry((level1 ? "bcd-":"postbcd-")+id+"-README.txt");
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

    public static String getUrl(boolean level1, int id) {
        return getUrl(level1, id, null);
    }

    public static String getUrl(boolean level1, int id, List<Options> options) {
        String encodedDataset = level1 ? "level1": "level2";
        //try {
        //    encodedDataset = URLEncoder.encode("ivo://irsa.ipac/spitzer."+(level1 ? "level1": "level2"), "UTF-8");
        //} catch (UnsupportedEncodingException e) {
        //    encodedDataset = level1 ? "level1": "level2";
        //}
        String url = ServerContext.getRequestOwner().getBaseUrl()+"servlet/ProductDownload?"+
                    PARAM__DATASET+"="+encodedDataset + "&" +
                    PARAM__ID+"="+id;

        if (options != null && options.size()>0) {
            String optionsVal = null;
            for (Options o : options) {
                if (optionsVal == null) {
                    optionsVal = o.name();
                } else {
                    optionsVal += ","+o.name();
                }
            }
            if (optionsVal != null) {
                try {
                    String encoded = URLEncoder.encode(optionsVal, "UTF-8");
                    url += "&"+PARAM__OPTIONS+"="+encoded;
                } catch (UnsupportedEncodingException e) {
                    LOG.error(e);
                }
            }
        }
        return url;
    }

    public static File getFile(String name) {
        return new File(name);

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


    /**
     */
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     */
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        processRequest(httpServletRequest, httpServletResponse);
    }

    @Override
    public void log(String msg) {
        LOG.briefDebug(msg);
    }

    @Override
    public void log(String msg, Throwable e) {
        LOG.error(e, msg);
    }

    private static void logActivity(long bytes) {
        String logStr= "Stream_File: " +
                " size: " +  FileUtil.getSizeAsString(bytes) +
                ", bytes: " + bytes;
        LOG.briefInfo(logStr);
        STATS_LOG.stats("Stream_File", "fsize(MB)", (double)bytes/StringUtils.MEG, "bytes", bytes);
    }

}

