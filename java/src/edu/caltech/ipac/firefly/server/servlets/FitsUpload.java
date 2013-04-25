package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
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

        File dir= VisContext.getVisUploadDir();
        File uploadedFile= getUniqueName(dir);


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
        String retval= VisContext.replaceWithPrefix(uploadedFile);
        UploadFileInfo fi= new UploadFileInfo(retval,uploadedFile,item.getName(),item.getContentType());
        CacheManager.getCache(Cache.TYPE_HTTP_SESSION).put(new StringKey(retval),fi);
        resultOut.println(retval);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
