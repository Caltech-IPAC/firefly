/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.ptf;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import static edu.caltech.ipac.firefly.server.query.ptf.PtfFileRetrieve.IBE_HOST;

/**
 * Created by IntelliJ IDEA. User: wmi Date: OCT.4,2013
 * <p/>
 * To change this template use File | Settings | File Templates.
 */


@SearchProcessorImpl(id = "PtfRefimsFileRetrieve")
public class PtfRefimsFileRetrieve extends URLFileInfoProcessor {

    // example: http://irsadev.ipac.caltech.edu:6001/data/ptf/dev_refims/ptf_ref_img/ptffiled/f#/c#/filename?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_l2(String baseUrl, String baseFile, String lon, String lat, String size) {
        String url = baseUrl + baseFile;
        url += "?center=" + lon + "," + lat;
        if (!StringUtils.isEmpty(size)) {
            url += "&size=" + size;
        }
        url += "&gzip=" + baseFile.endsWith("gz");

        return url;
    }

    // example: http://irsadev:6001/search/ptf/dev_refims/ptf_ref_img?
    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("host") != null ? sr.getSafeParam("host") : IBE_HOST;
        String schemaGroup = sr.getSafeParam("schemaGroup");
        String schema = sr.getSafeParam("schema");
        String table = sr.getSafeParam("table");

        return QueryUtil.makeUrlBase(host) + "/data/" + schemaGroup + "/" + schema + "/" + table + "/";
    }


    private static URL getIbeURL(ServerRequest sr, boolean doCutOut) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String filename = sr.getSafeParam("filename");
        //String fieldId = sr.getSafeParam("ptffield");
        String fieldId = filename.split("_")[1];
        String fieldDir = fieldId.substring(0, 4);
        String filterId = sr.getSafeParam("fid");
        String ccdId = sr.getSafeParam("ccdid");

        String baseFile = fieldDir + "/" + fieldId + "/f" + filterId + "/c" + ccdId + "/" + filename;

        if (doCutOut) {
            // look for ra_obj returned by moving object search
            String subLon = sr.getSafeParam("ra_obj");
            if (StringUtils.isEmpty(subLon)) {
                // next look for in_ra returned IBE
                subLon = sr.getSafeParam("in_ra");
                if (StringUtils.isEmpty(subLon)) {
                    // all else fails, try using crval1
                    subLon = sr.getSafeParam("ra");
                }
            }

            // look for dec_obj returned by moving object search
            String subLat = sr.getSafeParam("dec_obj");
            if (StringUtils.isEmpty(subLat)) {
                // next look for in_dec retuened by IBE
                subLat = sr.getSafeParam("in_dec");
                if (StringUtils.isEmpty(subLat)) {
                    // all else fails, try using crval2
                    subLat = sr.getSafeParam("dec");
                }
            }

            String subSize = sr.getSafeParam("subsize");

            return new URL(createCutoutURLString_l2(baseUrl, baseFile, subLon, subLat, subSize));
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
