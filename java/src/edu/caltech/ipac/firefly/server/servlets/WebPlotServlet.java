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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
