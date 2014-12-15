package edu.caltech.ipac.target;

import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.targetgui.net.NedParams;
import edu.caltech.ipac.targetgui.net.SimbadParams;
import edu.caltech.ipac.targetgui.net.TargetNetwork;
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
     * parse DataGroup's RA and DEC columns and produce a TargetList
     *
     * @param dg      DataGroup containing RA and Dec columns
     * @param targets a TargetList product
     * @throws Exception
     */
    public static void parseTargets(DataGroup dg, TargetList targets) throws Exception {
        parseTargets(dg, targets, false);
    }

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
                    NedAttribute na = TargetNetwork.getNedPosition(new NedParams(nameStr), null);
                    pos = na.getPosition();
                }
                if (pos == null) {
                    SimbadAttribute na = TargetNetwork.getSimbadPosition(new SimbadParams(nameStr), null);
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
    /*
    private static String OBJ_NAME = "OBJ_NAME";
    private static String RA = "RA";
    private static String DEC = "DEC";
    private static String PM_RA = "PM-RA";
    private static String PM_DEC = "PM-DEC";
    private static String EPOCH = "EPOCH";
    private static String RESOLVER_KEY = "RESOLVER";

    public void parseTargets(DataGroup dg, TargetList targets)
        throws TargetParseException{
        TargetFixedSingle target;
        PositionJ2000 tgtPosition;
        UserPosition userPosition;
        CoordinateSys coordSys = CoordinateSys.EQ_J2000;
        boolean correctFloatFormat;
        Float fl;
        float lonpm = 0.0F, latpm = 0.0F;
        float epoch = 2000.0F;
        ProperMotion pm = null;
        int line=1;

        _errorsFound = 0;
        _errorBuffer = new StringBuffer();
        _coord_system = CoordinateSys.EQUATORIAL_NAME;
        _equinox = "J2000";
        _resolver = SIMBAD_RESOLVER;

        for (DataObject o: dg.values()) {
            try {
                target = new TargetFixedSingle();

                if (hasVaildData(OBJ_NAME, dg, o))
                    target.setName(getValidString(o, OBJ_NAME));

                coordSys = TargetUtil.makeCoordSys(_coord_system, _equinox);
                correctFloatFormat = true;
                fl = null;

                if (hasVaildData(EPOCH, dg, o)) {
                    try {
                        fl = new Float(getValidString(o, PM_RA));
                    } catch (Exception nfe) {
                        correctFloatFormat = false;
                    }
                }

                if (correctFloatFormat && hasVaildData(EPOCH, dg, o) &&  (fl.floatValue() > 1899)) {
                    epoch = fl.floatValue();
                } else {
                    if ( hasVaildData(PM_RA, dg, o) && hasVaildData(PM_DEC, dg, o) ) {
                        lonpm = Float.parseFloat(getValidString(o, PM_RA));
                        latpm = Float.parseFloat(getValidString(o, PM_DEC));
                        pm = new ProperMotion(lonpm, latpm);
                    }
                    if (hasVaildData(EPOCH, dg, o)) {
                        try {
                            epoch = new Float(getValidString(o, EPOCH));
                        } catch (Exception nfe) {
                            correctFloatFormat = false;
                            epoch = 2000.0F;
                        }
                    }
                }

                if (hasVaildData(OBJ_NAME, dg, o) && !hasVaildData(DEC, dg, o)) {
                    String resolveType = _resolver;
                    if (hasVaildData(RESOLVER_KEY, dg, o)) resolveType = (getValidString(o, RESOLVER_KEY));
                    PositionAttributePair pap = resolveName(getValidString(o, OBJ_NAME), resolveType);
                    tgtPosition = pap.position;
                    if (getAttributesHandler() != null) {
                        getAttributesHandler().setResolvedAttributes(target, pap.attribute);
                    }
                } else {
                    userPosition = new UserPosition(getValidString(o, RA), getValidString(o, DEC), pm, coordSys, epoch);
                    tgtPosition = new PositionJ2000(userPosition);
                }
                target.setPosition(tgtPosition);
                targets.addTarget(target);
            } catch (Exception e) {
                String stack="";
                for (StackTraceElement ste: e.getStackTrace()) {
                    stack += (ste.toString()+LINE_FEED);
                }
                _errorBuffer.append("-Line number " + line +
                LINE_FEED + "- stack: " +
                stack + LINE_FEED + LINE_FEED);
                _errorsFound++;
            }
            line++;
        }

        if (_errorsFound > 0) {
            throw new TargetParseException(_errorBuffer.toString());
        }
    }


    * */
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

