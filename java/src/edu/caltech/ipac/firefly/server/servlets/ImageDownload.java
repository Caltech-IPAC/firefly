package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: ImageDownload.java,v 1.15 2012/10/11 20:47:59 roby Exp $
 */
public class ImageDownload extends BaseHttpServlet {

    public static final boolean ENABLE_CACHE= true;
    public static final String TYPE_TILE= "tile";
    public static final String TYPE_FULL= "full";
    public static final String TYPE_THUMBNAIL= "thumbnail";
    public static final String TYPE_ANY = "any";
    private static final int MAX_AGE= 86400; // 1 day in seconds
    private static final SimpleDateFormat _dateFormatter=new SimpleDateFormat(URLDownload.PATTERN_RFC1123);

    static {
        _dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String xStr= req.getParameter("x");
        String yStr= req.getParameter("y");
        String type= req.getParameter("type");
        String widthStr= req.getParameter("width");
        String heightStr= req.getParameter("height");
        String stateStr= req.getParameter("state");
        PlotState state= PlotState.parse(stateStr);

        if (type==null) type= TYPE_ANY;
        try {

            String fname= getFileName(req);
            if (fname!=null) {
                String mimeType = this.getServletContext().getMimeType(fname);
                res.addHeader("content-type", mimeType);
            }

            ServletOutputStream out= res.getOutputStream();

            if (type.equals(TYPE_TILE)) {
                if (isNonMatch(fname,req)) {
                    int x= Integer.parseInt(xStr);
                    int y= Integer.parseInt(yStr);
                    int width= Integer.parseInt(widthStr);
                    int height= Integer.parseInt(heightStr);
                    File outputFile= PlotServUtils.createImageFile(fname,state,x,y,width,height);
                    insertCacheHeaders(res,outputFile.lastModified()+"");
                    FileUtil.writeFileToStream(outputFile,out);
                }
                else {
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            }
            else if (type.equals(TYPE_THUMBNAIL)) {
                if (isNonMatch(fname,req)) {
                    File outputFile= PlotServUtils.createImageThumbnail(fname,state, null);
                    insertCacheHeaders(res,outputFile.lastModified()+"");
                    FileUtil.writeFileToStream(outputFile,out);
                }
                else {
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            }
            else if (type.equals(TYPE_FULL)) {
                res.setHeader("Content-Disposition", "attachment; filename=fits-image.png");
                PlotServUtils.writeFullImageFileToStream(out,state, null);
            }
            else {
                File f= VisContext.convertToFile(fname);
                FileUtil.writeFileToStream(f,out);
            }
        } catch (IOException e) {
            throw new ServletException(e.toString(),e);
        }
    }

    private void insertCacheHeaders(HttpServletResponse res, String etag) {
        if (ENABLE_CACHE) {
            res.addIntHeader("Max-Age", MAX_AGE);
            res.addHeader("Cache-Control", "max-age="+MAX_AGE+", public, must-revalidate");
            if (etag!=null) res.addHeader("ETag", etag);
        }
    }

    private static String getFileName(HttpServletRequest req) {
        String inFname= req.getParameter("file");
        String fname= inFname;
        if (inFname!=null) {
            fname= inFname.replace("<pre>", "");
            fname= fname.replace("</pre>", "");
        }
        return fname;
    }

    private static boolean isNonMatch(String fname, HttpServletRequest req) {
        boolean retval= true;
        String etag= req.getHeader("ETag");
        if (etag!=null) {
            long tileModTime= PlotServUtils.getTileModTime(fname);
            if (tileModTime>0) {
                retval= !etag.equals(tileModTime+"");
            }
        }
        return retval;
    }


    @Override
    protected long getLastModified(HttpServletRequest req) {
        if (ENABLE_CACHE) {
            String type= req.getParameter("type");
            long retval= -1;
            if (type!=null && (type.equals(TYPE_TILE) || type.equals(TYPE_THUMBNAIL))) {
                String fname= getFileName(req);
                retval= PlotServUtils.getTileModTime(fname);
                if (retval<0) retval= System.currentTimeMillis();
                retval= (retval/1000)*1000;
            }
            return retval;
        }
        else {
            return super.getLastModified(req);
        }
    }



//    public static void putLastModified(HttpServletResponse res, long modTime) {
//        res.addHeader("Last-Modified", _dateFormatter.format(new Date(modTime)));
//    }

    // some saved code for doing last modified
//                long modSince= PropertyStringDownload.getModifiedSince(req);
//                long tileModTime= PlotServUtils.getTileModTime(fname)-2000;
//                if (modSince>0 && tileModTime>0 && modSince>=tileModTime) {
//                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
//                    return;
//                }
//                else {
//                    long modTime= PlotServUtils.writeImageFileToStream(fname,out,state, x,y,width,height);
//                    putLastModified(res,modTime);
//                }




}

