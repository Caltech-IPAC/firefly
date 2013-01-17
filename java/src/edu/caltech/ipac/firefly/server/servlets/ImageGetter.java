package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @deprecated
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: ImageGetter.java,v 1.2 2011/02/02 01:11:11 roby Exp $
 */
@Deprecated
public class ImageGetter extends BaseHttpServlet {
    private File dirRoot;
    private boolean isInit = false;

    @Override
    @Deprecated
    public void init(ServletConfig servletConfig) throws ServletException {
        String root = servletConfig.getInitParameter("Image.Root");
        if (root != null) {
            dirRoot = new File(root);
            if (dirRoot.isDirectory() && dirRoot.canRead()) {
                isInit = true;
                return;
            }
        }
        Logger.error("ImageGetter is not initialized.  Root directory is not defined.");
    }

    @Deprecated
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (!isInit) {
            throw new ServletException("Servlet not initialized correctly.");
        }

        String fname = req.getParameter("file");

        if(fname == null) {
            throw new ServletException("Missing parameter");
        }

        String mimeType = FileUtil.getExtension(fname);

        if (mimeType == null || !mimeType.matches("jpg|jpeg|gif|png")) {
            throw new ServletException("File requested is not supported");
        }

        res.addHeader("content-type", "image/"+ mimeType);

        File imageFile = new File(dirRoot, fname);
        if (!imageFile.canRead()) {
            throw new ServletException("File does not exist");
        }

        FileUtil.writeFileToStream(imageFile, res.getOutputStream());
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

