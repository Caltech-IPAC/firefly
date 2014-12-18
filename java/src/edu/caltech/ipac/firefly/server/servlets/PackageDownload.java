package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.packagedata.Packager;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * @author tatianag
 * @version $Id: PackageDownload.java,v 1.8 2011/02/11 23:06:31 roby Exp $
 */
@Deprecated
public class PackageDownload extends BaseHttpServlet {

    public final static String PARAM_ID = AppProperties.getProperty("download.servlet.param.packageid", "id");
    public final static String PARAM_IDX = AppProperties.getProperty("download.servlet.param.packageidx", "idx");
    public final static String PARAM_NAME = AppProperties.getProperty("download.servlet.param.packagename", "name");

    public static int BUFF_SIZE = AppProperties.getIntProperty("download.buffsize", 4096);

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        Cache cache= getCache();
        StringKey statusKey= new StringKey(req.getQueryString());
        cache.put(statusKey, SearchServices.DownloadProgress.STARTING);
        String packageId = req.getParameter(PARAM_ID);
        String partIdx = req.getParameter(PARAM_IDX);
        String browserFileName = req.getParameter(PARAM_NAME);
        ClientLog.message("Retrieving package id: " + packageId + "/" + partIdx);
        if (packageId == null || packageId.length() == 0) {
            throw  new IllegalArgumentException(PARAM_ID+" is a required argument");
        }
        if (partIdx == null || partIdx.length() == 0) partIdx = "0";


        BufferedOutputStream buffOS = null;
        BufferedInputStream buffIS = null;

        try {
            File fileName = Packager.getZipFile(packageId, partIdx);
            if (browserFileName == null) browserFileName = fileName.getName();
            if (!browserFileName.endsWith("zip")) browserFileName+=".zip";

            // get the file and stream it to caller
            FileInputStream fIS = new FileInputStream(fileName);
            buffIS = new BufferedInputStream(fIS);

            buffOS = new BufferedOutputStream(res.getOutputStream());

            res.setContentType("application/zip");
            res.setHeader("Content-disposition",
                    "attachment; filename=" +
                            browserFileName);


            long fileLength= fileName.length();
            if(fileLength <= Integer.MAX_VALUE) res.setContentLength((int)fileLength);
            else                                res.addHeader("Content-Length", fileLength+"");

            int bytesRead;
            byte buffer[] = new byte[BUFF_SIZE];
            cache.put(statusKey, SearchServices.DownloadProgress.WORKING);
            while ((bytesRead = buffIS.read(buffer)) != -1) {
                buffOS.write(buffer, 0, bytesRead);
            }
            cache.put(statusKey, SearchServices.DownloadProgress.DONE);
        } catch (IOException e) {
            cache.put(statusKey, SearchServices.DownloadProgress.FAIL);
            throw e;
        } finally {
            if (buffOS!= null) try {buffOS.close();} catch (Exception e) {ClientLog.warning("Failed to close: "+e.getMessage());}
            if (buffIS!=null) try {buffIS.close();}  catch (Exception e) {ClientLog.warning("Failed to close: "+e.getMessage());}
        }
    }


    public static Cache getCache() {
        return UserCache.getInstance();
    }
}
