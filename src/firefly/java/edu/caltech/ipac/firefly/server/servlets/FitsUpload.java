/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;


/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: FitsUpload.java,v 1.19 2011/10/25 05:23:50 roby Exp $
 */
public class FitsUpload extends BaseHttpServlet {

    private static final String _nameBase="upload";
    private static final String _fitsNameExt=".fits";
    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        File dir= ServerContext.getVisUploadDir();
        File uploadedFile= getUniqueName(dir);

        String overrideKey= req.getParameter("cacheKey");

        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List /* FileItem */ items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator iter = items.iterator();
        FileItem item= null;
        if  (iter.hasNext()) {
            item = (FileItem) iter.next();

            if (!item.isFormField()) {
                try {
                    item.write(uploadedFile);
                } catch (Exception e) {
                    sendReturnMsg(res, 500, e.getMessage(), null);
                    return;
                }

            }
        }

        if (item==null) {
            sendReturnMsg(res, 500, "Could not find a upload file", null);
            return;
        }


        if (FileUtil.isGZipFile(uploadedFile)) {
            File uploadedFileZiped= new File(uploadedFile.getPath() + "." + FileUtil.GZ);
            uploadedFile.renameTo(uploadedFileZiped);
            FileUtil.gUnzipFile(uploadedFileZiped, uploadedFile, (int)FileUtil.MEG);
        }

        PrintWriter resultOut = res.getWriter();
        String retFile= ServerContext.replaceWithPrefix(uploadedFile);
        UploadFileInfo fi= new UploadFileInfo(retFile,uploadedFile,item.getName(),item.getContentType());
        String fileCacheKey= overrideKey!=null ? overrideKey : retFile;
        UserCache.getInstance().put(new StringKey(fileCacheKey), fi);
        resultOut.println(fileCacheKey);
        String size= StringUtils.getSizeAsString(uploadedFile.length(),true);
        Logger.info("Successfully uploaded file: "+uploadedFile.getPath(),
                    "Size: "+ size);
        Logger.stats(Logger.VIS_LOGGER,"Fits Upload", "fsize", (double)uploadedFile.length()/StringUtils.MEG, "bytes", size);
    }


    private static File getUniqueName(File dir) {
        File f;
        try {
            f= File.createTempFile(_nameBase, _fitsNameExt, dir);
        } catch (IOException e) {
            f= null;
        }
        return f;
    }

}
