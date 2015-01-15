/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.util.download.URLDownload;
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

        boolean saOnly= false;
        String saOnlyStr = req.getParameter("saOnly");
        if (saOnlyStr!=null) {
            saOnly= Boolean.parseBoolean(saOnlyStr);
        }

        String s =  saOnly ? WebPropertyLoader.getServerAccessiblePropertiesAsString() :
                             WebPropertyLoader.getAllPropertiesAsString();

        long modSince= getModifiedSince(req);
        if (modSince>0 && modSince>=_modDateTime) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }



//        res.addHeader("Cache-Control", "max-age=10368000");
        if (!saOnly) {
            res.addHeader("content-length", s.length() + "");
            res.addHeader("Last-Modified", _modDateStr);
        }

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
