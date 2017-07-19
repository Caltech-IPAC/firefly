/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.ptf;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static edu.caltech.ipac.firefly.server.query.ptf.PtfFileRetrieve.IBE_HOST;

/**
 * Created by IntelliJ IDEA. User: wmi Date: Feb 15, 2011 Time: 1:40:11 PM To change this template use File | Settings |
 * File Templates.
 */


@SearchProcessorImpl(id = "PtfProcimsFileRetrieve")
public class PtfProcimsFileRetrieve extends URLFileInfoProcessor {

    // example: http://irsadev.ipac.caltech.edu:9006/data/ptf/dev/process/{pfilename}?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_l1(String baseUrl, String baseFile, String lon, String lat, String size) {
        String url = baseUrl + baseFile;
        url += "?center=" + lon + "," + lat + "&size=" + size;
        url += "&gzip=" + baseFile.endsWith("gz");

        return url;
    }

    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("host") != null ? sr.getSafeParam("host") : IBE_HOST;
        String schema = sr.getSafeParam("schema");
        String table = sr.getSafeParam("table");

        return QueryUtil.makeUrlBase(host) + "/data/ptf/" + schema + "/" + table + "/";
    }

    private static URL getIbeURL(ServerRequest sr, boolean doCutOut) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);

        String pidStr = sr.getSafeParam("pid");
        long pid = Long.parseLong(pidStr);

        String baseFile = sr.getSafeParam("pfilename");
        if (baseFile == null) {
            try {
                baseFile = new PtfIbeResolver().getListPfilenames(new long[]{pid})[0];
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (doCutOut) {
            // look for ra_obj returned by moving object search
            String subLon = sr.getSafeParam("ra_obj");
            if (StringUtils.isEmpty(subLon)) {
                // next look for in_ra returned IBE
                subLon = sr.getSafeParam("in_ra");
                if (StringUtils.isEmpty(subLon)) {
                    // all else fails, try using crval1
                    subLon = sr.getSafeParam("crval1");
                }
            }

            // look for dec_obj returned by moving object search
            String subLat = sr.getSafeParam("dec_obj");
            if (StringUtils.isEmpty(subLat)) {
                // next look for in_dec retuened by IBE
                subLat = sr.getSafeParam("in_dec");
                if (StringUtils.isEmpty(subLat)) {
                    // all else fails, try using crval2
                    subLat = sr.getSafeParam("crval2");
                }
            }

            String subSize = sr.getSafeParam("subsize");

            return new URL(createCutoutURLString_l1(baseUrl, baseFile, subLon, subLat, subSize));
        } else {
            return new URL(baseUrl + baseFile);
        }

    }

    public URL getURL(ServerRequest sr) throws MalformedURLException {
        if (sr.containsParam("subsize")) {
            return getIbeURL(sr, true);
        } else {
            return getIbeURL(sr, false);
        }
    }

    @Override
    protected boolean identityAware() {
        return true;
    }
}
