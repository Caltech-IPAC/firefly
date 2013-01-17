package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.WebPropertyLoader;
import edu.caltech.ipac.firefly.util.BrowserInfo;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: PropertyStringDownload.java,v 1.9 2012/08/23 20:30:48 roby Exp $
 */
public class PropertyStringDownload extends BaseHttpServlet {

    private static final Date _modDate=new Date();
    private static final String _modDateStr=new SimpleDateFormat(URLDownload.PATTERN_RFC1123).format(_modDate);
    private static final long _modDateTime= _modDate.getTime()-1000; //move back 1 second from actual time, this deals with a partial seconds
    private static final SimpleDateFormat _dateParser=new SimpleDateFormat();

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String s = WebPropertyLoader.getAllPropertiesAsString();

        long modSince= getModifiedSince(req);
        if (modSince>0 && modSince>=_modDateTime) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }



//        res.addHeader("Cache-Control", "max-age=10368000");
        res.addHeader("content-length", s.length() + "");
        res.addHeader("Last-Modified", _modDateStr);

        String doJsonPStr = req.getParameter(ServerParams.DO_JSONP);
        boolean doJsonP = (doJsonPStr != null) ? doJsonP = Boolean.parseBoolean(doJsonPStr) : false;


        if (doJsonP) {
            BrowserInfo b= new BrowserInfo(req.getHeader("user-agent"));
            if ((b.isIE() && b.getMajorVersion()<=9)) {
                res.setContentType("text/plain");
            }
            else {
                res.setContentType("application/javascript");
            }
            String callback = req.getParameter("callback");
            String sMod = s.replace("\n", "--NL--");
            sMod = StringUtils.escapeQuotes(sMod);
            if (callback != null) {
                String jsonpStr = callback + "({" +
                        "\"success\" :  \"" + true + "\"," +
                        "\"data\" :  \"" + sMod + "\"" +
                        "});";

                res.setContentType("text/html");
                res.setContentLength(jsonpStr.length());
                ServletOutputStream out = res.getOutputStream();
                out.write(jsonpStr.getBytes());
                out.close();
            }

        } else {
            res.addHeader("content-type", "text/plain");
            FileUtil.writeStringToStream(s, res.getOutputStream());
        }


    }


    public static long getModifiedSince(HttpServletRequest req) {
        String dateStr=req.getHeader("If-Modified-Since");
        long retval= 0;
        if (dateStr!=null) {
            for(String format : URLDownload.DEFAULT_PATTERNS) {
                _dateParser.applyPattern(format);
                try {
                    retval= _dateParser.parse(dateStr).getTime();
                    break;
                } catch (ParseException e) {
                    retval= 0L;
                }
            }
        }
        return retval;
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
