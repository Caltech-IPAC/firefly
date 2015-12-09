/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provide the ability to query Simbad using Sesame HTTP-GET access.
 * For more information: http://cdsweb.u-strasbg.fr/doc/sesame.htx
 *
 *  Below is a sample query output
 * <p/>
 * # hip123	#Q-00001
 * #=Sc=Simbad (CDS, via client/server):    1   105ms
 * %@ @239810
 * %C.0 *
 * %J 0.39989237 +72.23661432 = 00 01 35.974  +72 14 11.81
 * %J.E [3.34 2.73 90] A 2007A&A...474..653V
 * %P -43.84 3.28 [0.38 0.31 0] A 2007A&A...474..653V
 * %V v -13.32 A [0.22] 2005A&A...430..165F
 * %X 5.91 [0.42] A 2007A&A...474..653V
 * %S K0 D ~                   =0.0001F400.0000.0000000000000000
 * %M.B 8.37 [0.02]  D 2000A&A...355L..27H
 * %M.V 7.21 [0.01]  D 2000A&A...355L..27H
 * %M.J 5.224 [0.020]  C 2003yCat.2246....0C
 * %M.H 4.789 [0.034]  C 2003yCat.2246....0C
 * %M.K 4.613 [0.057]  C 2003yCat.2246....0C
 * %I.0 HD 224891
 * #B 8
 * <p/>
 * #====Done (2015-Dec-07,22:54:16z)====
 */
public class Simbad4Client {

    public static final String SIMBAD_URL = "http://cdsweb.u-strasbg.fr/cgi-bin/nph-sesame/-oF/S?%s";

    private static final String J2000 = "%J";
    private static final String PM = "%P";
    private static final String PLX_V = "%X";
    private static final String RV = "%V";
    private static final String SPEC_TYPE = "%S";
    private static final String MORPH_TYPE = "%T";
    private static final String TYPE = "%C.0";
    private static final String B_MAG = "%M.B";
    private static final String V_MAG = "%M.V";

    public SimbadObject searchByName(String objectName)
            throws SimbadException, IOException {

        String url = String.format(SIMBAD_URL, URLEncoder.encode(objectName, "UTF-8"));
        try {
            String str = URLDownload.getStringFromURL(new URL(url), null);
            Map<String, String> results = parseResults(str);


            if (results.size() == 0) {
                throw new SimbadException("Simbad did not find the object: " + objectName);
            }


            SimbadObject sobj = new SimbadObject();
            sobj.setName(objectName);
            sobj.setType(getStringVal(results.get(TYPE), " ", 0));
            sobj.setRa(getDoubleVal(results.get(J2000), " ", 0));
            sobj.setDec(getDoubleVal(results.get(J2000), " ", 1));
            sobj.setRaPM((float) getDoubleVal(results.get(PM), " ", 0));
            sobj.setDecPM((float) getDoubleVal(results.get(PM), " ", 1));
            sobj.setParallax(getDoubleVal(results.get(PLX_V), " ", 0));
            sobj.setMorphology(getStringVal(results.get(MORPH_TYPE), " ", 0));
            sobj.setBMagnitude(getDoubleVal(results.get(B_MAG), " ", 0));

            sobj.setSpectralType(getStringVal(results.get(SPEC_TYPE), " ", 0));

            String rvType = getStringVal(results.get(RV), " ", 0);
            if (!StringUtils.isEmpty(rvType)) {
                if (rvType.equals("v")) {
                    sobj.setRadialVelocity(getDoubleVal(results.get(RV), " ", 1));
                } else {
                    sobj.setRedshift(getDoubleVal(results.get(RV), " ", 1));
                }
            }

            // based on previous implementation
            double vmag = getDoubleVal(results.get(V_MAG), " ", 0);
            if (Double.isNaN(vmag)) {
                sobj.setMagBand("B");
                sobj.setMagnitude(sobj.getBMagnitude());
            } else {
                sobj.setMagBand("V");
                sobj.setMagnitude(vmag);
            }

            ClientLog.message("results: " + sobj);
            return sobj;
        } catch (java.rmi.RemoteException e) {
            throw new SimbadException("Simbad did not find the object: " + objectName);
        } catch (FailedRequestException e) {
            throw new SimbadException("Simbad did not find the object: " + objectName);
        }
    }

    private String getStringVal(String s, String sep, int pos) {
        if (StringUtils.isEmpty(s)) {
            return "";
        }

        String[] parts = s.split(sep);
        return (pos > parts.length) ? "" : parts[pos].trim();
    }

    private Map<String, String> parseResults(String s) {

        HashMap<String, String> retval = new HashMap<String, String>();
        for (String l : s.split("\\n")) {
            if (!StringUtils.isEmpty(l) && l.startsWith("%")) {
                String[] kv = l.split(" ", 2);
                retval.put(kv[0].trim(), (kv.length > 1 ? kv[1].trim() : null));
            }
        }
        return retval;
    }

    private double getDoubleVal(String s, String sep, int pos) {
        String v = getStringVal(s, sep, pos);
        if (StringUtils.isEmpty(v)) {
            return Double.NaN;
        } else {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException nfx) {
                return Double.NaN;
            }
        }
    }

    public static void main(String[] args) {
        Simbad4Client client = new Simbad4Client();
        try {
            SimbadObject result = client.searchByName("NGC 4321");
            System.out.println(result.toString());
        } catch (SimbadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

