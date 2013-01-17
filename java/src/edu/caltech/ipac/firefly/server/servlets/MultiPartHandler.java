package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartData;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * This servlet take a multipart request and extract the files and parameters from it.
 * The data are stored in the UserCache as a MultiPartData.
 * The key is returned to the caller(HttpResponse).
 * The files are stored in a temporary work directory backed by
 * the TYPE_TEMP_FILE cache... which remove the older file(s) if it exceeds a
 * pre-configured number.  So, you do not need to handle the deletion of the
 * uploaded files.  But, there's a chance that the files are no longer there,
 * after a long period of time.
 *
 * Date: July 27, 2010
 *
 * @author loi
 * @version $Id: MultiPartHandler.java,v 1.3 2012/03/23 19:12:37 roby Exp $
 */
public class MultiPartHandler extends BaseHttpServlet {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        try {
            MultiPartData data= MultipartDataUtil.handleRequest(req);
            UserCache.getInstance().put(data.getCacheKey(), data);
            LOG.info("Multipart request processed.",
                     "File(s) uploaded: " + StringUtils.toString(data.getFiles()),
                     "Form parameters : " + data.getParams());
            sendReturnMsg(res, 200, "uploaded successfully", data.getCacheKey().toString());
        } catch (Exception e) {
            sendReturnMsg(res, 500, e.getMessage(), null);
        }

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
