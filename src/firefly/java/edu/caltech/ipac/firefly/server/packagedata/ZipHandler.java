/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * @author tatianag
 * @version $Id: ZipHandler.java,v 1.48 2012/08/08 18:30:49 roby Exp $
 */
public class ZipHandler {

    public final static String DOWNLOAD_SERVLET_PATH = "servlet/Download";
    public final static int COMPRESSION_LEVEL = AppProperties.getIntProperty("download.compression.level", 1);
    private static Logger.LoggerImpl logger = Logger.getLogger();


    public static String makeDownloadUrl(File f, String suggestedName) {
        String fileStr = ServerContext.replaceWithPrefix(f);
        try {
            fileStr = URLEncoder.encode(fileStr, "UTF-8");
            suggestedName = suggestedName == null ? "DownloadPackage" : suggestedName;
            if (f.getName().contains("_")) {        // has multiple idx
                suggestedName += "-part" + f.getName().split("_")[1];
            }
            suggestedName = URLEncoder.encode(suggestedName, "UTF-8");
        } catch (Exception e) {/*ignore*/}

        return ServerContext.getRequestOwner().getBaseUrl() + DOWNLOAD_SERVLET_PATH + "?" +
                AnyFileDownload.FILE_PARAM + "=" + fileStr + "&" +
                AnyFileDownload.RETURN_PARAM + "=" + suggestedName + "&" +
                AnyFileDownload.LOG_PARAM + "=true&";
    }

    public static File getZipFile(String jobId, int packageIdx) {
        File stagingDir = ServerContext.getStageWorkDir();
        String fname = String.format("%s%s.zip", jobId, (packageIdx > 0 ? "_" + packageIdx : ""));
        return new File(stagingDir, fname);

    }


    /**
     *
     * @param zout      ZipOutputStream to add to
     * @param zipEntryFileName the name of the file for the zip entry
     * @param fi        file to add
     * @param baseDir   base path of the added file
     * @return          the number of bytes added
     * @throws Exception
     */
    public static long addZipEntry(ZipOutputStream zout, String zipEntryFileName, FileInfo fi, File baseDir) throws Exception {

        if (!fi.hasAccess()) {
            throw new AccessDeniedException("");
        }

        // construct and add zip entry
        InputStream is = null;
        BufferedInputStream bis = null;
        ZipEntry zipEntry = null;
        long totalBytes = 0;

        try {

            is = getInputStream(fi.getInternalFilename(), fi, baseDir);
            String zipEntryComment = "(" + fi.getSizeInBytes() + "b) ";
            String filename = zipEntryFileName;

            int inBufSize = 4096;
            if (filename != null && FileUtil.isExtension(filename, FileUtil.GZ)) {
                InputStream decompressedIs = null;
                try {// try to uncompress the data
                    decompressedIs = new GZIPInputStream(is);  // for uncompressed data, throw an exception
                    bis = new BufferedInputStream(decompressedIs);
                } catch (Exception e) {
                    FileUtil.silentClose(decompressedIs);
                    bis = new BufferedInputStream(is, inBufSize);
                }
            } else {
                bis = new BufferedInputStream(is, inBufSize);
            }
            // remove .gz, if exists - filename or url stream are all going to be uncompressed at this point
            if (FileUtil.isExtension(filename, FileUtil.GZ)) {
                filename = filename.substring(0, filename.length() - 3);
            }

            zipEntry = new ZipEntry(filename);
            zipEntry.setComment(zipEntryComment);
            zout.putNextEntry(zipEntry);

            int read;
            byte[] buffer = new byte[inBufSize];
            while ((read = bis.read(buffer)) != -1) {
                zout.write(buffer, 0, read);
                totalBytes += read;
            }
        } catch (ZipException ze) {
            String zipError = ze.getMessage();

            // ignore duplicate entry errors
            if (zipError.startsWith("duplicate entry:")) {
                Logger.info("Packaging warning: " + zipError);
            } else {
                String error = "Failed packaging " + zipEntryFileName + " - " + zipError;
                Logger.error(error);
                throw new Exception(fi.getExternalName());
            }

        } catch (Exception e) {
            String error = "Failed packaging " + zipEntryFileName + " - " + e.getMessage();
            Logger.error(error);
            throw new Exception(fi.getExternalName());

        } finally {
            FileUtil.silentClose(bis);
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    logger.warn("Can not close input stream: " + e.getMessage());
                }
            }
            if (zipEntry != null) {
                try {
                    zout.closeEntry();
                } catch (Exception e) {
                    logger.warn("can not close zip entry: " + e.getMessage());
                }
            }
        }
        return totalBytes;
    }


    static public void addReadmeZipEntry(ZipOutputStream zout, String msg) {

        try {
            ZipEntry zipEntry = new ZipEntry("README");
            zipEntry.setComment("Info about package");
            zout.putNextEntry(zipEntry);
            PrintWriter pw = new PrintWriter(zout);
            pw.println(msg);
            pw.flush();
        } catch (IOException e) {
            logger.warn("Error writing readme file:" + e.getMessage());
        } finally {
            try {
                zout.closeEntry();
            } catch (Exception e) {
                logger.warn("can not close readme zip entry: " + e.getMessage());
            }
        }
    }

    /**
     * @param filename (filename can be url)
     * @param fi       file info pbject
     * @return input stream handle
     * @throws IOException on error
     */
    private static InputStream getInputStream(String filename, FileInfo fi, File baseDir) throws IOException {
        InputStream is;
        if (filename.contains("://")) {
            try {
                URL url = new URL(filename);
                Map<String, String> cookies = fi.getRequestInfo() != null ? fi.getRequestInfo().getCookies() : null;
                Map<String, String> headers = fi.getRequestInfo() != null ? fi.getRequestInfo().getHeaders() : null;
                URLConnection uc = URLDownload.makeConnection(url, cookies, headers);
                uc.setRequestProperty("Accept", "text/plain");

                if (fi.hasFileNameResolver()) {
                    String suggestedFilename = URLDownload.getSugestedFileName(uc);
                    fi.setExternalName(fi.resolveFileName(suggestedFilename));
                }

                is = uc.getInputStream();

            } catch (MalformedURLException e) {
                logger.error(e);

                File fileToRead;
                if (baseDir == null) {
                    fileToRead = new File(filename);
                } else {
                    fileToRead = new File(baseDir, filename);
                }

                is = new FileInputStream(fileToRead);
            }

        } else {
            File fileToRead;
            if (baseDir == null) {
                fileToRead = new File(filename);
            } else {
                fileToRead = new File(baseDir, filename);
            }

            is = new FileInputStream(fileToRead);
        }

        return is;
    }
}
