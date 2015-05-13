/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.FitsRead;
import nom.tam.fits.Fits;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.input.TeeInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        String dest = req.getParameter(DEST_PARAM);
        boolean doPreload = req.getParameterMap().containsKey(PRELOAD_PARAM);
        File destDir = ServerContext.getTempWorkDir();
        String overrideKey= req.getParameter("cacheKey");

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List /* FileItem */ items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (!item.isFormField()) {
                String fileName = item.getName();
                String ext = StringUtils.isEmpty(fileName) ? "" : FileUtil.getExtension(fileName);
                ext = StringUtils.isEmpty(ext) ? ".tmp" : "." + ext;

                // creating destDir
                if (!StringUtils.isEmpty(dest)) {
                    destDir = VisContext.convertToFile(dest);
                } else if (ext.equals(".fits")) {
                    destDir = VisContext.getVisCacheDir();
                }
                if (!destDir.exists()) {
                    sendReturnMsg(res, 400, "Destination path does not exists: " + dest, "");
                }

                try {
                    final File uf = File.createTempFile("upload_", ext, destDir);
                    String retFName = VisContext.replaceWithPrefix(uf);
                    UploadFileInfo fi= new UploadFileInfo(retFName,uf,item.getName(),item.getContentType());
                    String fileCacheKey= overrideKey!=null ? overrideKey : retFName;
                    UserCache.getInstance().put(new StringKey(fileCacheKey), fi);

                    if (doPreload) {
                        if (ext.equals(".fits")) {
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(uf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
                            TeeInputStream tee = new TeeInputStream(item.getInputStream(), bos);
                            final Fits fits = new Fits(tee);
                            FitsRead[] frAry = FitsRead.createFitsReadArray(fits);
                            CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM).put(new StringKey(uf.getAbsoluteFile()), frAry);
                            FileUtil.silentClose(bos);
                        } else {
                            item.write(uf);
                        }
                    } else {
                        item.write(uf);
                    }

                    sendReturnMsg(res, 200, null, fileCacheKey);
                    Counters.getInstance().increment(Counters.Category.Upload, fi.getContentType());
                } catch (Exception e) {
                    sendReturnMsg(res, 500, e.getMessage(), null);
                    return;
                }
            }
        }


    }


}

