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

