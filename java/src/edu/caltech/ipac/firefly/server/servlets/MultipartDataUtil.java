/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

