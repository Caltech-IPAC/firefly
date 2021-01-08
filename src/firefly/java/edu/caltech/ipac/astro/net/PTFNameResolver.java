/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;


/**
 * @author Xiuqin Wu, based on Trey Roby's CoordConvert
 */
public class PTFNameResolver {

    private static final String CGI_CMD="http://ptf.caltech.edu/cgi-bin/ptf/transient/name_radec.cgi?name=";
    private final static String UNAME= "irsaquery";
    private final static String PWD= "iptf333";
    private final static String authStringEnc = Base64.encode(UNAME + ":" + PWD);
    private final static Map<String,String> reqHeaders= Collections.singletonMap("Authorization", "Basic " + authStringEnc);

    public static ResolveResult resolveName(String objName) throws  FailedRequestException {
        String obj= null;
        String urlStr= CGI_CMD+objName;
        try {
            obj= URLDownload.getDataFromURL(new URL(urlStr), null, null, reqHeaders).getResultAsString();
            if (obj.endsWith("\n")) obj= obj.substring(0,obj.indexOf("\n"));

            String[] sAry= obj.split(" +", 3);
            if (sAry.length!=3) throw new FailedRequestException("Object not found", "server returned bad data: "+ obj);

            ResolvedWorldPt wp= new ResolvedWorldPt(Double.parseDouble(sAry[1]), Double.parseDouble(sAry[2]),
                    objName, Resolver.PTF);
            return new ResolveResult(Resolver.PTF, objName, wp);
        } catch (MalformedURLException e) {
            throw new FailedRequestException("bad url: " + urlStr);
        } catch (NumberFormatException e) {
            throw new FailedRequestException("Object not found", "server returned bad data: " + obj);
        }
    }
}
