/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.net.URLParms;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Simbad name resolver. Rewritten in 2021 for new simbad Tap.
 * @see <a href="http://simbad.u-strasbg.fr/simbad/sim-tap">Simbad Tap Example tag</a>
 */
public class SimbadNameResolver {
    private static final String SERVER = AppProperties.getProperty("simbad.host", "https://simbad.u-strasbg.fr");
    private static final String SIMBAD_URL_STR= SERVER + "/simbad/sim-tap/sync";
    private static final String RESOLVE_ADQL = "SELECT RA,DEC,main_id,otype_txt,sp_type,plx_value FROM basic JOIN ident ON oidref = oid WHERE id = '%s';";
    private static final String MAG_ADQL = "SELECT B, V, R ,I , J from allfluxes JOIN ident USING(oidref) WHERE id = '%s'";

    public static ResolveResult resolveName(String objName) throws FailedRequestException {
        Map<String,String> tapParams= CollectionUtil.stringMap( "request", "doQuery", "format", "json", "lang", "adql");
        Map<String,String> resolveData= CollectionUtil.stringMap(tapParams, "query", String.format(RESOLVE_ADQL, URLParms.encode(objName)) );
        Map<String,String> magData= CollectionUtil.stringMap(tapParams, "query", String.format(MAG_ADQL,URLParms.encode(objName)) );
        try {
            URL url= new URL(SIMBAD_URL_STR);
            String resolveJsonStr= URLDownload.getDataFromURL(url,resolveData , null, null).getResultAsString();
            JsonHelper json= JsonHelper.parse(resolveJsonStr);
            double ra = getRow0DoubleValue(json, 0,Double.NaN);
            double dec = getRow0DoubleValue(json, 1, Double.NaN);
            if (checkNaN(ra,dec)) throw makeEx(objName,"ra or dec is not parsable",null);
            ResolveResult sa= new ResolveResult(Resolver.Simbad, objName, new ResolvedWorldPt(ra, dec,objName,Resolver.Simbad));
            sa.setFormalName(getRow0StringValue(json, 2, ""));
            sa.setType(getRow0StringValue(json, 3, ""));
            sa.setSpectralType(getRow0StringValue(json, 4, ""));
            sa.setParallax(getRow0DoubleValue(json, 5, Double.NaN));

            try {
                String magJsonStr= URLDownload.getDataFromURL(url,magData , null, null).getResultAsString();
                json= JsonHelper.parse(magJsonStr);
                sa.setBMagnitude(getRow0DoubleValue(json, 0, Double.NaN));
                sa.setVMagnitude(getRow0DoubleValue(json, 1, Double.NaN));
            } catch (Exception e) { // we not not that concerned if this fails
                sa.setBMagnitude(Double.NaN);
                sa.setVMagnitude(Double.NaN);
            }
            return sa;
        } catch (MalformedURLException e) {
            throw makeEx(objName, "Could not build Simbad URL: " + SIMBAD_URL_STR,e);
        } catch (IllegalArgumentException e) {
            throw makeEx(objName, "Unexpected result data- not JSON",e);
        } catch (IndexOutOfBoundsException|ClassCastException|NullPointerException e) {
            throw makeEx(objName, "No results found",e);
        }
    }

    private static FailedRequestException makeEx(String objName, String detailStr, Exception e) {
        return new FailedRequestException("Simbad did not find the object: "+ objName,detailStr, e);
    }
    
    private static boolean checkNaN(double ra,double dec) {return Double.isNaN(ra) || Double.isNaN(dec);}

    private static double getRow0DoubleValue(JsonHelper h, int cIdx, double def) {
        Object vObj= h.getValue("", "data", "0", cIdx+"");
        String vStr= vObj.toString();
        if (vStr.equals("")) return def;
        try {
            return Double.parseDouble(vStr);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String getRow0StringValue(JsonHelper h, int cIdx, String def) {
        return h.getValue(def, "data", "0", cIdx+"");
    }

}