/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.FitsRead;
import nom.tam.fits.Fits;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.input.TeeInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

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
    private enum FileType {FITS, TABLE, REGION, UNKNOWN}


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        String dest = req.getParameter(DEST_PARAM);
        String preload = req.getParameter(PRELOAD_PARAM);
        String overrideCacheKey= req.getParameter(CACHE_KEY);
        String fileType= req.getParameter(FILE_TYPE);

        if (! ServletFileUpload.isMultipartContent(req)) {
            sendReturnMsg(res, 400, "Is not a Multi-Part request. Request aborted.", "");
        }

        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter = upload.getItemIterator(req);
        while (iter.hasNext()) {
            FileItemStream item = iter.next();

            if (!item.isFormField()) {
                String fileName = item.getName();
                InputStream inStream = item.openStream();
                String ext = resolveExt(fileName);
                FileType fType = resolveType(fileType, fileName);
                File destDir = resolveDestDir(dest, fType);
                boolean doPreload = resolvePreload(preload, fType);

                File uf = File.createTempFile("upload_", ext, destDir);
                String rPathInfo = ServerContext.replaceWithPrefix(uf);

                UploadFileInfo fi= new UploadFileInfo(rPathInfo,uf,fileName,item.getContentType());
                String fileCacheKey= overrideCacheKey!=null ? overrideCacheKey : rPathInfo;
                UserCache.getInstance().put(new StringKey(fileCacheKey), fi);

                if (doPreload && fType == FileType.FITS) {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(uf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
                    TeeInputStream tee = new TeeInputStream(inStream, bos);
                    final Fits fits = new Fits(tee);
                    FitsRead[] frAry = FitsRead.createFitsReadArray(fits);
                    CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM).put(new StringKey(uf.getAbsoluteFile()), frAry);
                    FileUtil.silentClose(bos);
                } else {
                    FileUtil.writeToFile(inStream, uf);
                }
                sendReturnMsg(res, 200, null, fileCacheKey);
                Counters.getInstance().increment(Counters.Category.Upload, fi.getContentType());
                return;
            }
        }
    }

    private File resolveDestDir(String dest, FileType fType) throws FileNotFoundException {
        File destDir = ServerContext.getTempWorkDir();
        if (!StringUtils.isEmpty(dest)) {
            destDir = ServerContext.convertToFile(dest);
        } else if (fType == FileType.FITS) {
            destDir = ServerContext.getVisCacheDir();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination path does not exists: " + destDir.getPath());
        }
        return destDir;
    }


    private String resolveExt(String fileName) {
        String ext = StringUtils.isEmpty(fileName) ? "" : FileUtil.getExtension(fileName);
        ext = StringUtils.isEmpty(ext) ? ".tmp" : "." + ext;
        return ext;
    }

    private FileType resolveType(String fileType, String fileExtension) {
        FileType ftype = FileType.UNKNOWN;
        try {
            ftype = FileType.valueOf(fileType);
        } catch (Exception e) {
            if (!StringUtils.isEmpty(fileExtension)) {
                if (fileExtension.equalsIgnoreCase(".fits")) {
                    ftype = FileType.FITS;
                } else if (fileExtension.matches("\\.tbl|\\.csv|\\.tsv")) {
                    ftype = FileType.TABLE;
                } else if (fileExtension.matches(".reg")) {
                    ftype = FileType.REGION;
                }
            }
        }
        return ftype;
    }

    private boolean resolvePreload(String preload, FileType fileType) {
        if (StringUtils.isEmpty(preload)) {
            return fileType == FileType.FITS;
        } else {
            return true;
        }
    }

}

