/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PositionParser;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 29, 2010
 * Time: 2:57:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class PositionFieldDef extends StringFieldDef {
    private static final NumberFormat _nf = NumberFormat.getFormat("#.######");
    PositionParser _parser = new PositionParser(new ClientPositionResolverHelper());


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
        boolean isValid = _parser.parse(s);

        if (!isValid) {
            String errMsg = "";

            // validate ObjName
            if (_parser.getInputType().equals(PositionParser.Input.Name)) {
                if (hard) {
                    throw new ValidationException("Object names must be more than one character");
                }
            } else {

                // check coordinate system
                if (_parser.getCoordSys() == null) {
                    throw new ValidationException(getErrMsg()+ "- invalid coordinate system.");
                }

                // validate RA
                double ra = _parser.getRa();
                if (Double.isNaN(ra)) {
                    String raStr = _parser.getRaString();
                    if (hard || (raStr != null && !(raStr.length() == 1 && raStr.charAt(0) == '.')) ) {
                        throw new ValidationException(getErrMsg()+"- unable to parse RA.");
                    }
                }
                // validate DEC
                double dec = _parser.getDec();
                if (Double.isNaN(dec)) {
                    String decStr = _parser.getDecString();
                    if (hard || (decStr != null && !(decStr.length() == 1 && (decStr.charAt(0) == '+' || decStr.charAt(0) == '-' || decStr.charAt(0) == '.'))) ) {
                        throw new ValidationException(getErrMsg()+"- unable to parse DEC.");
                    }
                }
            }
        }

        return true;
    }

    // -------------------- public methods --------------------
    public WorldPt getPosition() {
        return _parser.getPosition();
    }



    public PositionParser.Input getInputType() {
        return _parser.getInputType();
    }

    public String getObjectName() {
        return _parser.getObjName();
    }

    public void setObjectName(String name) {
        _parser.setObjName(name);
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

    public class ClientPositionResolverHelper implements PositionParser.Helper {

        public double convertStringToLon(String s, CoordinateSys coordsys) {
            try {
                return CoordUtil.convertStringToLon(s, coordsys);
            } catch (Exception e) {
                return Double.NaN;
            }
        }

        public double convertStringToLat(String s, CoordinateSys coordsys) {
            try {
                return CoordUtil.convertStringToLat(s, coordsys);
            } catch (Exception e) {
                return Double.NaN;
            }
        }

        public WorldPt resolveName(String objName) {
            return null;
        }

        public boolean matchesIgnoreCase(String s, String regExp) {
            return GwtUtil.matchesIgCase(s, regExp);
        }
    }

}

