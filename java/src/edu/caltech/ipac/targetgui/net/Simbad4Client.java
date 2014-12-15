package edu.caltech.ipac.targetgui.net;

import cds.simbad.uif.WSQueryInterface;
import cds.simbad.uif.WSQueryInterfaceServiceLocator;
import edu.caltech.ipac.astro.simbad.SimbadObject;
import edu.caltech.ipac.astro.simbad.SimbadException;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.util.ClientLog;

import javax.xml.rpc.ServiceException;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * This class provide the ability to query Simbad4 Database using web services.
 * Use <tt>searchByName</tt> to query by identifier.<p>
 *
 */
public class Simbad4Client {

    private static final String ENDPOINT_ADDR = "http://%s/axis/services/WSQuery";
    private static final String QUERY_STRING;

    private static final String SEPARATOR = ":";

    /**
     * Define the query string for each attribute.
     * When possible, define it in a way so that only the value of
     * that attribute is returned.  If Simbad returns values separated by
     * '~', only the first portion of that value will be stored.
     * The return values will be parsed into a <tt>Map</tt>, keyed by
     * Atrib.name.
     */
    private enum Attrib {
        RA  ("%.8COO(d;A;ICRS;J2000)"),
        DEC ("%.8COO(d;D;ICRS;J2000)"),
        PM_RA  ("%PM(A)"),
        PM_DEC ("%PM(D)"),
        PARALLAX ("%PLX(V)"),
        RV_TYPE ("%RV(T)"),
        RV_VALUE ("%RV(R)"),
        SPEC_TYPE ("%SP"),
        MORPH_TYPE ("%MT"),
        TYPE ("%OTYPE(V)"),
        B_MAG  ("%FLUXLIST(B)[%*(F)]"),
        V_MAG  ("%FLUXLIST(V)[%*(F)]")
        ;
        private String _queryString;
        Attrib(String queryString) {
            _queryString = queryString;
        }

        public String toString() {
            return name() + SEPARATOR + _queryString;
        }

    }


    static {
        StringBuffer sb = new StringBuffer("");
        for(Attrib a : Attrib.values()) {
            sb.append(a).append("\\n");
        }
        QUERY_STRING = sb.toString();

    }


    public SimbadObject searchByName(String objectName)
                        throws SimbadException, IOException {

        HostPort server = NetworkManager.getInstance().getServer(NetworkManager.SIMBAD4_NAME_RESOLVER);

        ClientLog.message(server.toString());
        ClientLog.message("Searching for " + objectName);

        WSQueryInterfaceServiceLocator locator = new WSQueryInterfaceServiceLocator();
        String endPointAddr = String.format(ENDPOINT_ADDR, server.getHost());
        locator.setWSQueryEndpointAddress(endPointAddr);
        try {
            WSQueryInterface lookup = locator.getWSQuery();
            String s = lookup.queryObjectById(objectName,  QUERY_STRING , "txt");

            if (s == null || s.length() == 0) {
                throw new SimbadException("Simbad did not find the object: " + objectName);
            }

            Map<String,String> attribs = parseResults(s);

            SimbadObject sobj = new SimbadObject();
            sobj.setName(objectName);
            sobj.setType(attribs.get(Attrib.TYPE.name()));
            sobj.setRa(parseDouble(attribs.get(Attrib.RA.name())));
            sobj.setRaPM((float) parseDouble(attribs.get(Attrib.PM_RA.name())));
            sobj.setDec(parseDouble(attribs.get(Attrib.DEC.name())));
            sobj.setDecPM((float) parseDouble(attribs.get(Attrib.PM_DEC.name())));
            sobj.setParallax(parseDouble(attribs.get(Attrib.PARALLAX.name())));
            sobj.setMorphology(attribs.get(Attrib.MORPH_TYPE.name()));
            sobj.setBMagnitude(parseDouble(attribs.get(Attrib.B_MAG.name())));

            // spec_type returns more than just the spectral type.  return just the first portion.
            sobj.setSpectralType(getFirstVal(attribs.get(Attrib.SPEC_TYPE.name()), "\\s"));

            String rvType = attribs.get(Attrib.RV_TYPE.name());
            if (rvType.length() > 0) {
                if (rvType.equals("v")) {
                    sobj.setRadialVelocity(parseDouble(attribs.get(Attrib.RV_VALUE.name())));
                } else {
                    sobj.setRedshift(parseDouble(attribs.get(Attrib.RV_VALUE.name())));
                }
            }

            // based on previous implementation
            double vmag = parseDouble(attribs.get(Attrib.V_MAG.name()));
            if (Double.isNaN(vmag)) {
                sobj.setMagBand("B");
                sobj.setMagnitude(sobj.getBMagnitude());
            } else {
                sobj.setMagBand("V");
                sobj.setMagnitude(vmag);
            }

            ClientLog.message("results: " + sobj);
            return sobj;
        } catch (ServiceException e) {
            throw new IOException("ServiceException: " + e.getMessage());
        } catch (java.rmi.RemoteException e) {
            throw new SimbadException("Simbad did not find the object: " + objectName);
            //throw new IOException("RemoteException: " + e.getMessage());
        }
    }

    private String getFirstVal(String s, String sep) {
        if ( s == null || s.length() == 0) {
            return "";
        }

        String[] vals = s.split(sep);
        return (vals.length > 0) ? vals[0].trim() : "";
    }

    private Map<String,String> parseResults(String s) {

        HashMap<String, String> retval = new HashMap<String, String>();
        for(String v : s.split("\\n") ) {
            String[] kv = v.split(SEPARATOR, 2);
            String key = kv[0];
            String val = kv.length>1 ? kv[1].trim() : "";

            // if the string contains '~', return only the first portion of it.
            val = getFirstVal(val, "~");
            retval.put(key, val);

        }
        return retval;
    }

    private double parseDouble(String s) {
        if(s==null) return Double.NaN;

        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfx) {
            return Double.NaN;
        }
    }


    public static void main(String[] args) {
        try {
            edu.caltech.ipac.target.SimbadAttribute resolver =  SimbadNameResolver.getPosition(args[0], new javax.swing.JFrame());
        } catch (FailedRequestException e) {
            e.printStackTrace();
        }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
