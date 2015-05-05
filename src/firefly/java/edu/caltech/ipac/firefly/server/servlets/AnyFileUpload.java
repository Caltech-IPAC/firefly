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
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Feb 16, 2011
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class AnyFileUpload extends BaseHttpServlet {
    private static final Logger.LoggerImpl _LOG = Logger.getLogger();
    public static final String DEST_PARAM  = "dest";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String dest = req.getParameter(DEST_PARAM);
        File destDir = ServerContext.getTempWorkDir();
        if (!StringUtils.isEmpty(dest)) {
            destDir = ServerContext.convertToFile(dest);
        }
        String overrideKey= req.getParameter("cacheKey");

        if (!destDir.exists()) {
            sendReturnMsg(res, 400, "Destination path does not exists: " + dest, "");
        }

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
                try {
                    File uf = File.createTempFile("upload_", ext, destDir);
                    item.write(uf);
                    String retFName = ServerContext.replaceWithPrefix(uf);
                    UploadFileInfo fi= new UploadFileInfo(retFName,uf,item.getName(),item.getContentType());
                    String fileCacheKey= overrideKey!=null ? overrideKey : retFName;
                    UserCache.getInstance().put(new StringKey(fileCacheKey), fi);
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

