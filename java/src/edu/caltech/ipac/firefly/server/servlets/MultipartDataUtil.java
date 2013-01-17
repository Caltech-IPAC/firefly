package edu.caltech.ipac.firefly.server.servlets;
/**
 * User: roby
 * Date: 3/16/12
 * Time: 1:45 PM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartData;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class MultipartDataUtil {

    public static MultiPartData handleRequest(HttpServletRequest req) throws Exception {
        return handleRequest(new StringKey("MultiPartHandler", System.currentTimeMillis()), req);
    }

    public static MultiPartData handleRequest(StringKey key, HttpServletRequest req) throws Exception {

            // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List /* FileItem */ items = upload.parseRequest(req);

        MultiPartData data = new MultiPartData(key);


        // Process the uploaded items
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (item.isFormField()) {
                String name = item.getFieldName();
                String value = item.getString();
                data.addParam(name, value);
            } else {
                String fieldName = item.getFieldName();
                String fileName = item.getName();
                String contentType = item.getContentType();
                File uf = new File(ServerContext.getTempWorkDir(), System.currentTimeMillis() + ".upload");
                item.write(uf);
                data.addFile(fieldName, uf, fileName, contentType);
                StringKey fileKey= new StringKey(fileName, System.currentTimeMillis());
                CacheManager.getCache(Cache.TYPE_TEMP_FILE).put(fileKey, uf);
            }
        }
        return data;
    }





}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
