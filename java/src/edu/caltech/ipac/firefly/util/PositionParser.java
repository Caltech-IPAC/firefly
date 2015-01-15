/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;

public class PositionParser {


    public enum Input {Name,Position}

    public final static String DEFAULT_COORD_SYS = "equ j2000";
    public final static String RA = "ra";
    public final static String DEC = "dec";
    public final static String COORDINATE_SYS = "coordSys";
    private final static String INVALID = "INVALID";
    private String  _ra = null;
    private String   _dec = null;
    private CoordinateSys _coordSys = null;
    private String _objName = null;
    private Input _inputType= Input.Position;
    private Helper helper = null;
    private boolean isValid;


    public interface Helper {
        /**
         * return Double.NaN if the given string cannot be converted
         * @param s
         * @param coordsys
         * @return
         */
        double convertStringToLon(String s, CoordinateSys coordsys);
        /**
         * return Double.NaN if the given string cannot be converted
         * @param s
         * @param coordsys
         * @return
         */
        double convertStringToLat(String s, CoordinateSys coordsys);
        /**
         * return null if the given object name cannot be resolved
         * @param objName
         * @return
         */
        WorldPt resolveName(String objName);

        boolean matchesIgnoreCase(String s, String regExp);
    }


    public PositionParser(Helper helper) {
        this.helper = helper;
        if (helper == null) {
            this.helper = new Helper() {
                public double convertStringToLon(String s, CoordinateSys coordsys) {
                    return Double.NaN;
                }
                public double convertStringToLat(String s, CoordinateSys coordsys) {
                    return Double.NaN;
                }
                public WorldPt resolveName(String objName) {
                    return null;
                }

                public boolean matchesIgnoreCase(String s, String regExp) {
                    return false;
                }
            };
        }
    }

    /**
     * parse the given string into the resolver.
     * Returns true if the string is a valid position.
     * @param s
     * @return
     */
    public boolean parse(String s) {

        isValid = false;
        _ra = null;
        _dec = null;
        _objName = null;
        _coordSys = null;

        if (!StringUtils.isEmpty(s) ) {
            s= StringUtils.crunch(s);
            s= StringUtils.polishString(s); //remove non-standard-ASCII characters.
            _inputType= determineType(s);
            if (_inputType == Input.Name) {
                if (s.trim().length()>1) {
                    _objName= s;
                    isValid = true;
                }
            } else {
                HashMap<String,String> map= getPositionMap(s);
                _coordSys= getCoordSys(map.get(COORDINATE_SYS));
                _ra = map.get(RA);
                _dec = map.get(DEC);
                isValid = getCoordSys() != CoordinateSys.UNDEFINED && !Double.isNaN(getRa()) && !Double.isNaN(getDec());
            }
        }

        return isValid;
    }

    public Input getInputType() {
        return  _inputType;
    }

    public boolean isValid() {
        return isValid;
    }

    public double getRa() {
        String v = isNumeric(_ra) ? _ra + "d" : _ra;
        return helper.convertStringToLon(v, _coordSys);
    }

    public double getDec() {
        String v = isNumeric(_dec) ? _dec + "d" : _dec;
        return helper.convertStringToLat(v, _coordSys);
    }

    public String getRaString() {
        return _ra;
    }

    public String getDecString() {
        return _dec;
    }

    /**
     * returns CoordinateSys.UNDEFINED if it's a bad coordinate system.
     * @return
     */
    public CoordinateSys getCoordSys() {
        return _coordSys == null ? CoordinateSys.UNDEFINED : _coordSys;
    }

    public String getObjName() {
        return _objName;
    }

    public void setObjName(String objName) {
        isValid = true;
        _inputType = Input.Name;
        _objName = objName;
    }

    public WorldPt getPosition() {
        if (!isValid()) return null;
        WorldPt wp = null;
        if (getInputType().equals(Input.Name)) {
            wp = helper.resolveName(getObjName());
        } else {
            wp = new WorldPt(getRa(), getDec(), getCoordSys());
        }
        return wp;
    }

    // -------------------- static methods --------------------

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private CoordinateSys getCoordSys(String s)  {
        return StringUtils.isEmpty(s) ? null : CoordinateSys.parse(s);
    }

    private HashMap<String,String> getPositionMap(String text) {
        HashMap<String,String> map =  new HashMap<String,String>(3);
        String[] array;
        try {
            for (String value: parseAndConvertToMeta(text).split("&")) {
                array = value.split("=");
                if (array.length>1) {
                    map.put(array[0], array[1]);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // do nothing - parse failed
        }
        return map;
    }

    private String parseAndConvertToMeta(String text) {
        ArrayList<String> numericList = new ArrayList<String>(1);
        ArrayList<String> alphabetList = new ArrayList<String>(1);

        String retval;

        //1. convert string to Ra&DEC&{CoordSys} format
        String ra="", dec="", coordSys="";
        if (text.contains(",")) {
            // Ra, DEC {Coord-Sys} case
            String[] values = text.split(",");
            ra = values[0];
            if (values.length>1) {
                for (String item: values[1].split("[ ]|[,]")) {
                    if (item.length()>0) {
                        if (isDouble(item)) {
                            numericList.add(item);
                        } else {
                            alphabetList.add(item);
                        }
                    }
                }
                if (numericList.size()>0) {
                    for (String item: numericList) {
                        dec += (item+" ");
                    }
                    for (String item: alphabetList) {
                        coordSys += (item+" ");
                    }
                } else {
                    if (alphabetList.size()==1) {
                        dec = alphabetList.get(0);
                        coordSys = DEFAULT_COORD_SYS;
                    } else if (alphabetList.size()>1){
                        for (int i=0;i<alphabetList.size();i++) {
                            if (i==0) {
                                dec = alphabetList.get(i);
                            } else {
                                coordSys +=(alphabetList.get(i)+" ");
                            }
                        }

                    }

                }
            }
        } else {
            // Ra DEC {coordSys} case (no comma)
            String tokenAry[]= text.split(" ");
            for (String token: tokenAry) {
                if (token.length()>0) {
                    if (isDouble(token)) {
                        numericList.add(token);
                    } else {
                        alphabetList.add(token);
                    }
                }
            }
            if (numericList.size()>0) {
            //so we have more than one numeric strings, divide numeric strings
            //list in half, first half = ra, second half = dec.
                if (numericList.size()==2) {
                    ra = numericList.get(0);
                    dec = numericList.get(1);

//                    if (ra.startsWith("+") || ra.startsWith("-")) {
//                        if (ra.length()>3 && !isFloating(ra))
//                            ra = convertRa(ra);
//                    } else {
//                        if (ra.length()>2 && !isFloating(ra))
//                            ra = convertRa(ra);
//                    }
//                    if (dec.startsWith("+") || dec.startsWith("-")) {
//                        if (dec.length()>3 && !isFloating(dec))
//                            dec = convertDEC(dec);
//                    } else {
//                        if (ra.length()>2 && !isFloating(ra))
//                            dec = convertDEC(dec);
//                    }


                } else if (numericList.size()>2) {
                    int idx=0;
                    for (String item: numericList) {

                        if ((idx++)*2<numericList.size()) {
                            ra += (item + " ");
                        } else {
                            dec += (item + " ");
                        }
                    }
                } else if (numericList.size()==1) {
                    ra= tokenAry[0];
                    if (tokenAry.length>1) dec= tokenAry[1];
                }

                if (tokenAry.length>=3) {
                    for (String item: alphabetList) {
                        coordSys += (item+" ");
                    }
                }
            } else {
                // Ra and DEC are non-numeric strings
                if (alphabetList.size()>0) {
                    int idx =0;
                    ra = "";
                    dec = "";
                    coordSys = "";
                    for (String item:alphabetList) {
                        if (idx==0) {
                            if (item.contains("+") && !item.startsWith("+")) {
                                // 0042443+411608 case
                                String[] array = item.split("\\+");
                                ra = array[0];
                                dec = array[1];

                                if (isDouble(ra))
                                    ra = convertRa(ra);
                                if (isDouble(dec))
                                    dec = convertDEC(dec);
                            } else if (item.contains("-") && !item.startsWith("-")) {
                                // 0042443-411608 case
                                String[] array = item.split("-");
                                ra = array[0];
                                dec = "-"+array[1];

                                if (isDouble(ra))
                                    ra = convertRa(ra);
                                if (isDouble(dec))
                                    dec = convertDEC(dec);
                            } else {
                                ra = item;
                            }
                        } else {
                            if (dec.length()==0)
                                dec = item;
                            else
                                coordSys += (item+" ");
                        }
                        idx++;
                    }
                }
            }
        }
        ra = ra.trim();
        dec = dec.trim();
        coordSys = getvalidCoordSys(coordSys);
        if (coordSys.length()==0) coordSys = DEFAULT_COORD_SYS;
        retval = RA+"="+ra.trim() + "&"+DEC+"=" + dec.trim() +"&"+COORDINATE_SYS+"="+coordSys.trim();
        return retval;
    }

    private static boolean isDouble(String inputData) {
        try {
            Double.parseDouble(inputData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String convertRa(String s) {
        String retval;
        String[] hms= {"","",""};

        int idx=0;
        for (char c:s.toCharArray()) {
            if (hms[idx].length()>=2) {
                if (idx < (hms.length-1)) idx++;
            }
            hms[idx] += c;
        }

        for (int i=0;i<hms.length-1;i++) {
            if (hms[i].length()==0)
                hms[i]="0";
        }
        if (hms[2].length()>2) {
            hms[2] = hms[2].substring(0,2)+"."+hms[2].substring(2);
        }
        retval = hms[0]+" "+hms[1]+" "+hms[2];
        return retval;
    }

    private static String convertDEC(String s) {
        String retval;
        String[] dms= {"","",""};

        int idx=0;
        for (char c:s.toCharArray()) {
            if (idx < (dms.length-1)) {
                if (idx==0 && ((dms[idx].startsWith("+")) || (dms[idx].startsWith("-")))) {
                    if (dms[idx].length()>2) idx++;
                } else if (dms[idx].length()>=2) idx++;
            }
            dms[idx] += c;
        }

        for (int i=0;i<dms.length-1;i++) {
            if (dms[i].length()==0)
                dms[i]="0";
        }
        if (dms[2].length()>2) {
            dms[2] = dms[2].substring(0,2)+"."+dms[2].substring(2);
        }
        retval = dms[0]+" "+dms[1]+" "+dms[2];
        return retval;
    }

    private final static String F1950 = "^B1950$|^B195$|^B19$|^B1$|^B$";
    private final static String FJ2000 = "^J2000$|^J200$|^J20$|^J2$|^J$";
    private final static String EJ2000 = "J2000$|J200$|J20$|J2$|J$";
    private final static String E1950 = "B1950$|B195$|B19$|B1$|B$";
    private final static String SECL = "^ECLIPTIC|^ECL|^EC";
    private final static String SEQ = "^EQUATORIAL|^EQU|^EQ";


    private final static String COMBINE_SYS = "(^ECLIPTIC|^ECL|^EC|^EQUATORIAL|^EQU|^EQ)("+EJ2000+ "|" +E1950+ ")";
    private final static String ECL_1950 = "("+SECL +")("+E1950+ ")";
    private final static String ECL_2000 = "("+SECL +")("+EJ2000+ ")";

    private final static String EQ_1950 = "("+SEQ+")(" +E1950+ ")";
    private final static String EQ_2000 = "("+SEQ+")(" +EJ2000+ ")";


    private String getvalidCoordSys(String s) {
        String retval="EQ_J2000";
        String[] array = s.trim().split(" ");

        if (!StringUtils.isEmpty(s) && array.length>0) {
            if (array.length==1 && matches(array[0],COMBINE_SYS)) {
                if (array[0].toUpperCase().startsWith("EC")) {
                    if (matches(array[0],ECL_1950)) {
                        retval="EC_B1950" ;
                    } else if (matches(array[0],ECL_2000)) {
                        retval="EC_J2000" ;
                    }
                } else if (array[0].toUpperCase().startsWith("EQ")) {
                    if (matches(array[0],EQ_1950)) {
                        retval="EQ_B1950" ;
                    } else if (matches(array[0],EQ_2000)) {
                        retval="EQ_J2000" ;
                    }
                }
            } else if (matches(array[0],"^EQUATORIAL$|^EQU$|^EQ$|^E$")) {
                if (array.length>1) {
                    if (matches(array[1], F1950)) {
                        retval="EQ_B1950" ;
                    } else if (matches(array[1], FJ2000)) {
                        retval="EQ_J2000" ;
                    }
                    else {
                        retval=INVALID+" COORDINATE SYSTEM: "+s ;
                    }
                } else {
                    retval="EQ_J2000" ;
                }
            } else if (matches(array[0],"^ECLIPTIC$|^ECL$|^EC$")) {
                if (array.length>1) {
                    if (matches(array[1], F1950)) {
                        retval="EC_B1950" ;
                    } else if (matches(array[1], FJ2000)) {
                        retval="EC_J2000" ;
                    }
                    else {
                        retval=INVALID+" COORDINATE SYSTEM: "+s ;
                    }
                } else {
                    retval="EC_J2000" ;
                }
            } else if (matches(array[0],"^GALACTIC$|^GAL$|^GA$|^G$")) {
                retval="GALACTIC";
            } else if (matches(array[0], FJ2000)) {
                retval="EQ_J2000" ;
            } else if (matches(array[0], F1950)) {
                retval="EQ_B1950" ;
            } else {
                retval=INVALID+" COORDINATE SYSTEM: "+s ;
            }
        }
        return retval;
    }



    private static String coordToString(CoordinateSys csys) {
        String retval;

        if (csys.equals(CoordinateSys.EQ_J2000))      retval= "Equ J2000";
        else if (csys.equals(CoordinateSys.EQ_B1950)) retval= "Equ B1950";
        else if (csys.equals(CoordinateSys.GALACTIC)) retval= "Gal";
        else if (csys.equals(CoordinateSys.ECL_J2000)) retval= "Ecl J2000";
        else if (csys.equals(CoordinateSys.ECL_B1950)) retval= "Ecl B1950";
        else retval= "";

        return retval;

    }

    private boolean matches(String s, String regExp) {
        return helper.matchesIgnoreCase(s, regExp);
    }


    /**
     * Determine the type of input.
     * The input is a position if any of the following test are true
     * <ul>
     * <li>name: the first character is not a digit, a '+', or a decimal point
     * <li>position: first string is numeric or is a parsable lon
     * </ul>
     * @param s the crunched input string
     * @return the Input type
     */
    private Input determineType(String s) {
        Input retval;
        char firstChar= s.charAt(0);
        String firstStr= "";

        if (s.length()>=2) {
            if (s.startsWith("-") || s.startsWith("+")) {
                String sAry[]= s.substring(1).split("[+ ,-]");
                firstStr= s.charAt(0) + ((sAry.length>0) ? sAry[0] : "");

            }
            else {
                String sAry[]= s.split("[+ ,-]");
                if (sAry.length>0)  firstStr= sAry[0];
            }
        }


        if (s.length()<2) {
            retval= Input.Name;
        }
        else if (!Character.isDigit(firstChar) && firstChar!='+' && firstChar!='-' && firstChar!='.') {
            retval= Input.Name;
        }
        else if  (isDouble(firstStr) || !Double.isNaN(helper.convertStringToLon(firstStr,CoordinateSys.EQ_J2000))) {
            retval= Input.Position;
        }
        else {
            retval= Input.Name;
        }
        return retval;
    }


}

