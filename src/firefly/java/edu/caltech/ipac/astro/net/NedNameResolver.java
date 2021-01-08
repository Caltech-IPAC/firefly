/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * NED name resolver. Rewritten in 2021 for new NED service.
 * @see <a href="https://ned.ipac.caltech.edu/Documents/Guides/Interface/ObjectLookup">NED API Reference</a>
 */
public class NedNameResolver {
    private static final String SERVER = AppProperties.getProperty("ned.host", "https://ned.ipac.caltech.edu");
    private static final String NED_URL_STR= SERVER + "/srs/ObjectLookup";
    private static final String POST_TEMPLATE = "{\"name\":{\"v\":\"%s\"}}";

    public static ResolveResult resolveName(String objName) throws FailedRequestException {
        try {
            Map<String,String> postData= CollectionUtil.stringMap("json",String.format(POST_TEMPLATE, objName));
            String jsonStr= URLDownload.getDataFromURL(new URL(NED_URL_STR),postData , null, null).getResultAsString();
            JsonHelper helper= JsonHelper.parse(jsonStr);
            long resultCode= helper.getValue(0L, "ResultCode");
            if (resultCode != 2 && resultCode != 3) throw makeEx(objName, "resultCode="+resultCode,null);
            double ra= helper.getValue(Double.NaN, "Preferred", "Position", "RA");
            double dec= helper.getValue(Double.NaN, "Preferred", "Position", "Dec");
            if (checkNaN(ra,dec)) throw makeEx(objName,"ra or dec is not parsable",null);
            return new ResolveResult(Resolver.NED, objName, new ResolvedWorldPt(ra, dec,objName,Resolver.NED));
        } catch (MalformedURLException e) {
            throw makeEx(objName, "Could not build NED URL: " + NED_URL_STR,e);
        } catch (IllegalArgumentException e) {
            throw makeEx(objName, "Unexpected result data- not JSON",e);
        }
    }

    private static FailedRequestException makeEx(String objName, String detailStr, Exception e) {
        return new FailedRequestException("NED did not find the object: "+ objName,detailStr, e);
    }
    private static boolean checkNaN(double ra,double dec) {return Double.isNaN(ra) || Double.isNaN(dec);}

}