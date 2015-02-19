/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * EXPERIMENTAL - not used
 * @author Trey
 * @version $Id: WebPlotServlet.java,v 1.3 2012/03/12 18:04:41 roby Exp $
 */
public class WebPlotServlet extends BaseHttpServlet {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List /* FileItem */ items = upload.parseRequest(req);


        Map<String,String> params= new HashMap<String, String>(10);


        // Process the uploaded items
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (item.isFormField()) {
                String name = item.getFieldName();
                String value = item.getString();
                params.put(name, value);
            } else {
                String fieldName = item.getFieldName();
                try {
                    File uf = new File(ServerContext.getTempWorkDir(), System.currentTimeMillis() + ".upload");
                    item.write(uf);
                    params.put(fieldName,uf.getPath());

                } catch (Exception e) {
                    sendReturnMsg(res, 500, e.getMessage(), null);
                    return;
                }
            }
        }



//        String result= ServerCommandAccess.doCommand(params);


//        sendReturnMsg(res, 200, "results", result);


// test code..
//        if (doTest) {
//            MultiPartPostBuilder builder = new MultiPartPostBuilder(
//                                    "http://localhost:8080/applications/Spitzer/SHA/servlet/Firefly_MultiPartHandler");
//            builder.addParam("dummy1", "boo1");
//            builder.addParam("dummy2", "boo2");
//            builder.addParam("dummy3", "boo3");
//            for(UploadFileInfo fi : data.getFiles()) {
//                builder.addFile(fi.getPname(), fi.getFile());
//            }
//            StringWriter sw = new StringWriter();
//            MultiPartPostBuilder.MultiPartRespnse pres = builder.post(sw);
//            LOG.briefDebug("uploaded status: " + pres.getStatusMsg());
//            LOG.debug("uploaded response: " + sw.toString());
//        }
    }
}
