/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.astro.net.NedParams;
import edu.caltech.ipac.astro.net.SimbadParams;
import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtils;

/**
 * Created by IntelliJ IDEA. User: tlau Date: Nov 17, 2011 Time: 5:19:29 PM To change this template use File | Settings
 * | File Templates.
 */
public class IpacTableTargetsParser {
    public static final String LINE_FEED = "\r\n";
    private static String RA = "RA";
    private static String DEC = "DEC";
    private static String OBJ_NAME = "OBJNAME";
    private static final String ATTRIB_SEP = "=";
    private static String FLOAT_MASK = "[+-]?[0-9]*[.]?[0-9]+";

    /**
     * @param dg            DataGroup containing RA and Dec columns
     * @param targets       a TargetList product
     * @param nedThenSimbad true if use NED then SIMBAD
     * @throws Exception while parsing
     */
    public static void parseTargets(DataGroup dg, TargetList targets, boolean nedThenSimbad) throws Exception {
        /*
            derived from FixedSingleTargetParser.parseTargets(BufferedReader in, TargetList targets)
         */

        float epoch = 2000.0F;
        ProperMotion pm = null;
        int errorsFound = 0;
        StringBuffer errorBuffer = new StringBuffer();
        String coord_system = CoordinateSys.EQUATORIAL_NAME;
        String equinox = "J2000";
        CoordinateSys coordSys = TargetUtil.makeCoordSys(coord_system, equinox);

        errorBuffer.append("IpacTableTargetsParser cannot process the following DataObject(s):" + LINE_FEED);

        String ra = null, raStr = null, dec = null, decStr = null, name = null, nameStr = null;

        // Find RA and Dec keys
        for (String key : dg.getKeySet()) {
            if (key.trim().toUpperCase().equals(RA)) {
                ra = key;
            } else if (key.trim().toUpperCase().equals(DEC)) {
                dec = key;
            } else if (key.trim().toUpperCase().equals(OBJ_NAME)) {
                name = key;
            }
        }

        if (name == null) {
            if (ra == null || dec == null)
                throw new Exception("IpacTableTargetsParser cannot find RA and/or Dec column(s).");
        }


        for (DataObject dataObject : dg.values()) {
            if (ra != null) raStr = getValidString(dataObject, ra, true);
            if (dec != null) decStr = getValidString(dataObject, dec, true);
            if (name != null) nameStr = getValidString(dataObject, name);

            if (raStr != null && decStr != null) {
                targets.addTarget(new TargetFixedSingle(nameStr, new PositionJ2000(
                        new UserPosition(raStr, decStr, pm, coordSys, epoch))));
            } else if (name != null && nameStr != null) {
                PositionJ2000 pos = null;
                if (nedThenSimbad) {
                    NedAttribute na = TargetNetwork.getNedPosition(new NedParams(nameStr));
                    pos = na.getPosition();
                }
                if (pos == null) {
                    SimbadAttribute na = TargetNetwork.getSimbadPosition(new SimbadParams(nameStr));
                    pos = na.getPosition();
                }
                targets.addTarget(new TargetFixedSingle(nameStr, pos));
            } else {
                errorBuffer.append("Unable to parse target: [");
                for (Object o : dataObject.getData()) {
                    errorBuffer.append(o);
                    errorBuffer.append(" ");
                }
                errorBuffer.append("]");
                errorBuffer.append(LINE_FEED);
                errorsFound++;
            }
        }


        if (errorsFound > 0) {
            throw new Exception(errorBuffer.toString());
        }
    }

    private static String getValidString(DataObject o, String key) throws Exception {
        return getValidString(o, key, false);
    }

    private static String getValidString(DataObject o, String key, boolean checkDecimal) throws Exception {
        String value = null;

        if (hasVaildData(key, o)) {
            if (o.getDataElement(key) instanceof String) {
                value = StringUtils.polishString((String) o.getDataElement(key));
                if (checkDecimal && value != null && value.matches(FLOAT_MASK)) value = value + "d";
            } else {
                Object v = o.getDataElement(key);
                if (v instanceof Double || v instanceof Float) {
                    value = String.format("%.7f", v) + "d";
                } else {
                    value = StringUtils.polishString(o.getDataElement(key).toString() + "d");
                }
            }
            if (value != null && value.trim().length() == 0) value = null;
        }

        return value;
    }

    private static boolean hasVaildData(String key, DataObject o) {
        return o.getDataElement(key) != null && isValidData(o.getDataElement(key).toString());
    }

    private static boolean isValidData(String s) {
        return s != null && !isAnAttribute(s);
    }

    private static boolean isAnAttribute(String s) {
        return s.indexOf(ATTRIB_SEP) > 0;
    }
}
