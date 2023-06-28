package edu.caltech.ipac.firefly.server.query.ztf;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by wmi
 * on 8/16/17
 * edu.caltech.ipac.hydra.server.query
 */


@SearchProcessorImpl(id = "ZtfRefimsFileRetrieve")
public class ZtfRefimsFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String ZTF_DATA_RETRIEVAL_TYPE = AppProperties.getProperty("ztf.data_retrieval_type");  // url or filesystem
    public static final String ZTF_FILESYSTEM_BASEPATH = AppProperties.getProperty("ztf.filesystem_basepath");

    public static enum FILE_TYPE {
        REFIMAGE, REFCOV, REFUNC, REFSEXCAT, REFPSFCAT, REFIMLOG, REFLOG
    }

    public static String createCutoutURLString_l2(String baseUrl,String field,String filtercode,String ccdid,String qid,FILE_TYPE type,String lon,String lat,String size) {
        String url = baseUrl;
        if (!url.endsWith("/")){
            url += "/";
        }

        url += getFilePath_ref(field,filtercode,ccdid,qid,type);
        url += "?center=" + lon + "," + lat + "&size=" + size;

        return url;

    }

    // example: https://irsadev.ipac.caltech.edu/ibe/data/ztf/products/ref/908/field908008/zr/ccd01/q3/
    //ref/<fff>/field<field>/<filtercode>/ccd<ccdid>/q<qid>/ztf_<field>_<filtercode>_c<ccdid>_q<qid>_<ptype>
    public static String createFilepath_l2(String field, String filtercode, String ccdid, String qid, FILE_TYPE type) {
        return  getFilePath_ref(field,filtercode,ccdid,qid, type);

    }
    
    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("host") != null ? sr.getSafeParam("host") : ZtfFileRetrieve.IBE_HOST;
        String schemaGroup = sr.getSafeParam("schemaGroup")!= null ? sr.getSafeParam("schemaGroup"):"ztf";
        String schema = sr.getSafeParam("schema");
        String table = sr.getSafeParam("SearchType");

        return QueryUtil.makeUrlBase(host) + "/data/" + schemaGroup + "/" + schema + "/" + table + "/";
    }

    private static String getFilePath_ref(String field, String filtercode, String ccdid, String qid,FILE_TYPE type){

        String formatccdid = ("00" + ccdid).substring(ccdid.length());
        String formatfield =  ("000000" + field).substring(field.length());
        String fff = formatfield.substring(0,3);
        String refbaseDir = fff + "/" + "field" + formatfield + "/" + filtercode +"/" + "ccd" +formatccdid +"/" + "q" + qid +"/";
        String refbaseFile = refbaseDir + "ztf_" + formatfield + "_" + filtercode +"_c" + formatccdid + "_q" + qid;

        if (type == FILE_TYPE.REFIMAGE) {
            refbaseFile += ZtfRequest.REFIMAGE;
        } else if (type == FILE_TYPE.REFCOV) {
            refbaseFile += ZtfRequest.REFCOV;
        } else if (type == FILE_TYPE.REFPSFCAT) {
            refbaseFile += ZtfRequest.REFPSFRFCAT;
        } else if (type == FILE_TYPE.REFSEXCAT) {
            refbaseFile += ZtfRequest.REFSEXRDCAT;
        } else if (type == FILE_TYPE.REFUNC) {
            refbaseFile += ZtfRequest.REFUNC;
        } else if (type == FILE_TYPE.REFIMLOG) {
            refbaseFile += ZtfRequest.REFIMLOG;
        } else if (type == FILE_TYPE.REFLOG) {
            refbaseFile += ZtfRequest.REFLOG;
        }

        return refbaseFile;
    }


    private static URL getIbeURL(ServerRequest sr, boolean doCutOut) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String field = sr.getSafeParam("field");
        String filtercode = sr.getSafeParam("filtercode");
        String ccdid = sr.getSafeParam("ccdid");
        String qid = sr.getSafeParam("qid");

        String baseFile = getFilePath_ref(field,filtercode,ccdid, qid, FILE_TYPE.REFIMAGE);

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

            return new URL(createCutoutURLString_l2(baseUrl,field,filtercode,ccdid,qid, FILE_TYPE.REFIMAGE,subLon, subLat, subSize));
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


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
