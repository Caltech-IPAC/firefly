package edu.caltech.ipac.firefly.data.form;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.targetgui.net.Resolver;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 29, 2010
 * Time: 2:57:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class PositionFieldDef extends StringFieldDef {
    //ra -> lon
    //dec -> lat


    public enum Input {Name,Position}

    public final static String DEFAULT_COORD_SYS = "equ j2000";
    public final static String RA = "ra";
    public final static String DEC = "dec";
    public final static String COORDINATE_SYS = "coordSys";
    private final static String INVALID = "INVALID";
    private String _ra = null;
    private String _dec = null;
    private String _coordSys = null;
    private String _objName = null;
    private Input _inputType= Input.Position;
    private static final NumberFormat _nf = NumberFormat.getFormat("#.######");
    private static final NumberFormat _parseNF = NumberFormat.getFormat("##0.###");





    @Override
    public boolean validateSoft(Object aValue) throws ValidationException {
        String s= (aValue==null) ? null : aValue.toString();
        return validateInternal(s,false);
    }


    @Override
    public boolean validate(Object aValue) throws ValidationException {
        String s= (aValue==null) ? null : aValue.toString();
        return validateInternal(s,true);
    }

    public boolean validateInternal(String s, boolean hard) throws ValidationException {
        if (StringUtils.isEmpty(s) ) {
            if (isNullAllow()) {
                return true;
            } else {
                throw new ValidationException("You must enter a valid position or name");
            }
        }
        s= StringUtils.crunch(s);
        s= StringUtils.polishString(s); //remove non-standard-ASCII characters. 
        boolean valid;
        try {
            _coordSys= null;
            _ra      = null;
            _dec     = null;
            _objName = null;
            _inputType= determineType(s);
            if (_inputType==Input.Name) {
                valid = true;
                if (s.length()>1) {
                    _objName= s;
                }
                else if (hard) {
                    throw new ValidationException("Object names must be more than one character");
                }
            }
            else {
                HashMap<String,String> map= getPositionMap(s);
                _coordSys= map.get(COORDINATE_SYS);
                _ra= map.get(RA);
                _dec= map.get(DEC);


                try {
                    if (hard)  getLon();
                    else if (_ra!=null)  {
                        if (!(_ra.length()==1 && _ra.charAt(0)=='.')) getLon();
                    }
                } catch (CoordException e) {
                    throw new ValidationException(getErrMsg()+"- unable to parse Ra.");
                }

                try {
                    if (hard)  getLat();
                    else if (_dec!=null)  {
                        if (!(_dec.length()==1 && (_dec.charAt(0)=='+' || _dec.charAt(0)=='-' || _dec.charAt(0)=='.'))) {
                            getLat();
                        }
                    }
                } catch (CoordException e) {
                    throw new ValidationException(getErrMsg() +"- unable to parse DEC.");
                }

                if (_coordSys.startsWith(INVALID)) {
                    throw new ValidationException(getErrMsg()+ "- invalid coordinate system.");
                }
                valid = true;
            }

        }catch (NullPointerException e) {
            throw new ValidationException(getErrMsg() + "- error parsing");
        }
        if (!valid) throw new ValidationException(getErrMsg());

        return valid;
    }

    // -------------------- public methods --------------------
    public double getLon() throws CoordException {
        if (_ra==null) throw new CoordException("no lon");
        try {
            Float.parseFloat(_ra); // validate we have a float
            String tst= _ra;
            if (!tst.endsWith("d")) tst+="d";
//            GwtUtil.showScrollingDebugMsg("parsing:"+_coordSys+"$$$") ;
            return (float) CoordUtil.convertStringToLon(tst,getCoordSys());
        } catch (NumberFormatException e) {
            return (float) CoordUtil.convertStringToLon(_ra,getCoordSys());
        }
    }



    public double getLat() throws CoordException  {
        if (_dec==null) throw new CoordException("no lat");
        try {
            Float.parseFloat(_dec); // validate we have a float
            String tst= _dec;
            if (!tst.endsWith("d")) tst+="d";
            return CoordUtil.convertStringToLat(tst,getCoordSys());
        } catch (NumberFormatException e) {
            return  CoordUtil.convertStringToLat(_dec,getCoordSys());
        }
    }

    public CoordinateSys getCoordSys() throws CoordException  {
        CoordinateSys c= CoordinateSys.parse(_coordSys);
        if (c==null) throw new CoordException("not valid Coordinate System");
        return c;
    }


    public WorldPt getPosition() {
        try {
            return new WorldPt(getLon(), getLat(), getCoordSys());
        } catch (CoordException e) {
            return null;
        }
    }



    public Input getInputType() {
        return _inputType;
    }

    public String getObjectName() {
        if (_inputType==Input.Name) {
            return _objName;
        }
        else {
            return null;
        }
    }

    public void setObjectName(String name) {
        _inputType=Input.Name;
        _objName= name;
    }


    // -------------------- static methods --------------------
    public static HashMap<String,String> getPositionMap(String text) {
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

    public static String parseAndConvertToMeta(String text) {
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
                        if (isNumeric(item)) {
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
                    if (isNumeric(token)) {
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

                                if (isNumeric(ra))
                                    ra = convertRa(ra);
                                if (isNumeric(dec))
                                    dec = convertDEC(dec);
                            } else if (item.contains("-") && !item.startsWith("-")) {
                                // 0042443-411608 case
                                String[] array = item.split("-");
                                ra = array[0];
                                dec = "-"+array[1];

                                if (isNumeric(ra))
                                    ra = convertRa(ra);
                                if (isNumeric(dec))
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

//    private static boolean isNumeric(String inputData) {
//        return matches(inputData,"[-+]?\\d+(\\.\\d*)?") ||
//               matches(inputData,"[-+]?\\.?(\\d+)");
//    }

    private static boolean isNumeric(String inputData) {
        try {
            _parseNF.parse(inputData);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


//    private static boolean isFloating(String inputData) {
//        return isNumeric(inputData) && inputData.contains(".");
//    }

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


    private static String getvalidCoordSys(String s) {
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

    private static boolean matches(String s, String regExp) {
        return GwtUtil.matchesIgCase(s,regExp);
    }

    public static String formatPosForTextField(WorldPt wp) {
        String retval;
        try {
            String lon;
            String lat;
            if (wp.getCoordSys().isEquatorial()) {
                lon= CoordUtil.convertLonToString(wp.getLon(),wp.getCoordSys());
                lat= CoordUtil.convertLatToString(wp.getLat(),wp.getCoordSys());
            }
            else {
                lon= _nf.format(wp.getLon());
                lat= _nf.format(wp.getLat());
            }
            retval= lon + " " + lat + " " + coordToString(wp.getCoordSys());

        } catch (Exception e) {
            retval= "";
        }
        return retval;

    }

    public static String formatTargetForHelp(ResolvedWorldPt wp) {
        if (wp==null) return "";
        String name= wp.getObjName();
        Resolver resolver= wp.getResolver();
        String s;

        if (name!=null && resolver!=null) {
            s= "<b>"+ name  +"</b>" +
                    " <i>resolved by</i> " + resolver.getUserDesc() +
                    "<div  style=\"padding-top:6px;\">" +
                    PositionFieldDef.formatPosForHelp(wp) +
                    "</div>";
        }
        else {
            s=  PositionFieldDef.formatPosForHelp(wp);
        }
        return s;

    }

    public static String formatTargetForHelp(String name, Resolver resolver, WorldPt wp) {
        String retval= null;
        if (wp==null) retval= "";
        if (retval==null && wp instanceof ResolvedWorldPt) {
            ResolvedWorldPt rWp= (ResolvedWorldPt)wp;
            if (rWp.getResolver()!=null && rWp.getObjName()!=null) {
                retval= formatTargetForHelp(rWp);
            }
        }
        if (retval==null) retval= formatTargetForHelp(new ResolvedWorldPt(wp,name,resolver));
        return retval;
    }

    public static String formatPosForHelp(WorldPt wp) {
        if (wp==null) return "";
        String s;
        try {
            String lonStr= _nf.format(wp.getLon());
            String latStr= _nf.format(wp.getLat());
            String csys= coordToString(wp.getCoordSys());
            if (wp.getCoordSys().isEquatorial()) {

                String hmsRa= CoordUtil.convertLonToString(wp.getLon(), true);
                String hmsDec= CoordUtil.convertLatToString(wp.getLat(), true);

                s= "<div class=\"on-dialog-help faded-text\" style=\"font-size:10px;\">"+
                        lonStr +",&nbsp;"+latStr+ "&nbsp;&nbsp;"+csys +
                        " &nbsp;&nbsp;&nbsp;<i>or</i> &nbsp;&nbsp;&nbsp;"+
                        hmsRa +",&nbsp;"+hmsDec+ "&nbsp;&nbsp;"+csys +
                        "</div>";
            }
            else {
                s= "<div class=on-dialog-help>"+
                        lonStr +",&nbsp;"+latStr+ "&nbsp;&nbsp;"+csys + "</div>";
            }
        } catch (CoordException e) {
            s= "";
        }
        return s;

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
    private static Input determineType(String s) {
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
        else if  (isNumeric(firstStr) || CoordUtil.validLon(firstStr,CoordinateSys.EQ_J2000)) {
            retval= Input.Position;
        }
        else {
            retval= Input.Name;
        }
        return retval;
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