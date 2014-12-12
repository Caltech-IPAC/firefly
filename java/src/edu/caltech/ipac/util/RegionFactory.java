package edu.caltech.ipac.util;
/**
 * User: roby
 * Date: 2/12/13
 * Time: 2:11 PM
 */


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.util.dd.ContainsOptions;
import edu.caltech.ipac.util.dd.Global;
import edu.caltech.ipac.util.dd.RegionFileElement;
import edu.caltech.ipac.util.dd.RegParseException;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionAnnulus;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionBoxAnnulus;
import edu.caltech.ipac.util.dd.RegionCsys;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionEllipse;
import edu.caltech.ipac.util.dd.RegionEllipseAnnulus;
import edu.caltech.ipac.util.dd.RegionFont;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.RegionOptions;
import edu.caltech.ipac.util.dd.RegionPoint;
import edu.caltech.ipac.util.dd.RegionText;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Trey Roby
 */
public class RegionFactory {

    enum ValueType {LON, LAT, VALUE}
    private static final String DELIM= "(), ";

    private static CoordConverter converter= null;

    public static void setCoordConverter(CoordConverter c) { converter= c;  }


    public static ParseRet processInput(LineGetter lineGetter) {
        int line= 1;
        List<Region> regList= new ArrayList<Region>(300);
        List<String> msgList= new ArrayList<String>(30);
        RegionCsys coordSys= RegionCsys.PHYSICAL;
        Global g= new Global(new RegionOptions());
        boolean allowHeader= true;
        for (String s= lineGetter.getLine();(s!=null);s= lineGetter.getLine()) {
            if (!StringUtils.isEmpty(s)) {
                try {
                    List<RegionFileElement> resultList= RegionFactory.parsePart(s, coordSys, g, allowHeader);
                    if (allowHeader) {
                        g= RegionFactory.getGlobal(resultList, g);
                        coordSys= RegionFactory.getCsys(resultList, coordSys);
                    }
                    if (RegionFactory.containsRegion(resultList)) {
                        allowHeader= false;
                        RegionFactory.addAllRegions(resultList, regList);
                    }
                } catch (RegParseException e) {
                    msgList.add("Error parsing line " + line + ": " + e.getMessage());
                }
            }
            line++;
        }
        return new ParseRet(regList,msgList);
    }





    public static List<RegionFileElement> parsePart(String inString) throws RegParseException {
        return parsePart(inString, RegionCsys.PHYSICAL, new Global(new RegionOptions()),false);
    }

    public static List<RegionFileElement> parsePart(String inString, RegionCsys coordSys, Global global, boolean allowHeader)
                     throws RegParseException{

        if (coordSys==null) coordSys= RegionCsys.PHYSICAL;
        RegionOptions globalOps= global!=null ? global.getOptions() : null;
        String sAry[]= inString.split(";");
        List<RegionFileElement> retList= new ArrayList<RegionFileElement>(4);

        for (String virtualLine: sAry) {
            String lineBegin;
            if (virtualLine == null || virtualLine.trim().startsWith("#")) break;
            String virtualLineNoInclude= virtualLine.trim();

            String virualLinePart[]= virtualLineNoInclude.split("#");
            String regionParams= virualLinePart[0];
            String options= virualLinePart.length>1 ? virualLinePart[1] : null;


            StringTokenizer st = new StringTokenizer(virualLinePart[0], DELIM);
            RegionOptions ops= (globalOps!=null) ? globalOps.copy() : new RegionOptions();

            Region region = null;
            String region_type= null;
            boolean include= true;

            try
            {
                if (st.hasMoreToken()) {
                    lineBegin = st.nextToken();
                    if (lineBegin.startsWith("global")) {
                        globalOps= parseRegionOption(st.getRestOfString(),globalOps,true);
                        retList.add(new Global(globalOps));
                        continue;
                    }
                    if (isCoordSys(lineBegin)) {
                        coordSys = getCoordSys(lineBegin);
                        if (allowHeader) {
                            retList.add(coordSys);
                        }
                        continue;
                    }
                    else {
                        region_type = lineBegin;
                        if (lineBegin.startsWith("-") || lineBegin.startsWith("+")) {
                            region_type= region_type.substring(1);
                            include= lineBegin.charAt(0)=='+';
                            virtualLineNoInclude= virtualLine.trim().substring(1);
                            ops.setInclude(include);
                        }
                    }
                }
                if (st.hasMoreToken()) {
                    if (region_type.equals("vector")      ||
                        region_type.equals("ruler")       ||
                        region_type.equals("compass")     ||
                        region_type.equals("projection")  ||
                        region_type.equals("panda")       ||
                        region_type.equals("epanda")      ||
                        region_type.equals("bpanda")  ) {
                        throw new RegParseException(region_type +" is not yet implemented");
                    }
                    else if (region_type.equals("text")) {
                        String textInput= st.getRestOfString();

                        ops= (options!=null) ? parseRegionOption(options,globalOps,include) : new RegionOptions();


                        String textStart= getStartCharOfString(textInput);
                        String coordStr= textInput;
                        if (textStart!=null) {
                            st= new StringTokenizer(textInput,textStart);
                            coordStr= st.nextToken();
                            String textPart= st.nextToken();
                            String end=  (textStart.equals("{")) ? "}" : textStart;
                            int endIdx= textPart.indexOf(end);
                            if (endIdx>-1) {
                                String textString= textPart.substring(0,endIdx);
                                ops.setText(textString);
                            }
                        }
                        st= new StringTokenizer(coordStr,DELIM);
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        region= new RegionText(wp);
                        region.setOptions(ops);

                    }
                    else if (isPointTypeTwo(virtualLineNoInclude)) {
                        if (st.hasMoreToken()) {
                            if (st.nextToken().equals("point")) {
                                WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                                RegionPoint.PointType pointType = convertPointType(region_type);
                                if (options!=null)  ops= parseRegionOption(options,globalOps,include);
                                region = new RegionPoint(wp,pointType,5);
                                region.setOptions(ops);
                            }
                        }

                    }
                    else if (region_type.equals("box")) {
                        boolean isBoxAnaulus= false;
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        String width_string = st.nextToken();
                        RegionValue width= convertToRegionValue(width_string,ValueType.VALUE, false);
                        String height_string = st.nextToken();
                        RegionValue height= convertToRegionValue(height_string, ValueType.VALUE, false);
                        String angle_string = st.nextToken();
                        RegionValue angle= convertToRegionValue(angle_string,ValueType.VALUE, false);

		    /* now check for box annulus */
                        RegionValue width1 = null;
                        RegionValue height1 = null;

                        if (st.hasMoreToken())
                        {
                            String token = st.nextToken();
			    /* its a box_annulus */
                            width1 = angle;            //already fetched
                            height_string = token;
                            height1= convertToRegionValue(height_string, ValueType.VALUE, false);
                            angle_string = st.nextToken();
                            angle= convertToRegionValue(angle_string, ValueType.VALUE, false);
                            isBoxAnaulus= true;
                        }
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);

                        if (!isBoxAnaulus) {
                            region = new RegionBox(wp,new RegionDimension(width,height), angle);
                        }
                        else {
                            region = new RegionBoxAnnulus(wp, angle, new RegionDimension(width,height),
                                                          new RegionDimension(width1,height1));
                        }
                        region.setOptions(ops);
                    }
                    else if (region_type.equals("annulus")) {
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        String radius_string = st.nextToken();
                        RegionValue radius= convertToRegionValue(radius_string, ValueType.VALUE, false);
                        radius_string = st.nextToken();
                        RegionValue radius2= convertToRegionValue(radius_string, ValueType.VALUE, false);
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);
                        region = new RegionAnnulus(wp,radius,radius2);
                        region.setOptions(ops);
                    }
                    else if (region_type.equals("circle")) {
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        String radius_string = st.nextToken();
                        RegionValue radius= convertToRegionValue(radius_string, ValueType.VALUE, false);
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);
                        region = new RegionAnnulus(wp, radius);
                        region.setOptions(ops);
                    }
                    else if (region_type.equals("ellipse"))
                    {
                        boolean isEllipseAnnulus= false;
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        String radius_string = st.nextToken();
                        RegionValue radius1= convertToRegionValue(radius_string, ValueType.VALUE, false);
                        radius_string = st.nextToken();
                        RegionValue radius2= convertToRegionValue(radius_string, ValueType.VALUE, false);
                        String angle_string = st.nextToken();
                        RegionValue angle= convertToRegionValue(angle_string, ValueType.VALUE, false);


		    /* now check for ellipse annulus */
                        RegionValue radius3= null;
                        RegionValue radius4= null;

                        if (st.hasMoreToken())
                        {
                            String token = st.nextToken();
			    /* its a box_annulus */
                            radius3 = angle;            //already fetched
                            radius_string = token;
                            radius4 = convertToRegionValue(radius_string, ValueType.VALUE, false);
                            angle_string = st.nextToken();
                            angle= convertToRegionValue(angle_string, ValueType.VALUE, false);
                            isEllipseAnnulus= true;
                        }
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);

                        if (isEllipseAnnulus) {
                            region = new RegionEllipseAnnulus(wp,angle,radius1,radius2,radius3,radius4);
                        }
                        else {
                            region = new RegionEllipse(wp, radius1, radius2, angle);
                        }
                        region.setOptions(ops);

                    }
                    else if (region_type.equals("point")) {
                        WorldPt wp= parseWorldPt(coordSys, st.nextToken(), st.nextToken());
                        RegOpsParseRet ret=null;

                        RegionPoint.PointType pointType = RegionPoint.PointType.X;
                        int pointSize= -1;
                        if (options!=null) {
                            ret=parseRegionOptionPlus(options,globalOps,include);
                            pointType= ret.pointType;
                            pointSize= ret.ptSize;
                            ops= ret.ops;
                        }
                        if (ret!=null) {
                            region = new RegionPoint(wp,pointType,pointSize);
                            region.setOptions(ops);
                        }
                    }
                    else if (region_type.equals("line")) {
                        WorldPt wp1= parseWorldPt(coordSys,st.nextToken(), st.nextToken());
                        WorldPt wp2= parseWorldPt(coordSys,st.nextToken(), st.nextToken());
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);
                        region = new RegionLines(wp1,wp2);
                        region.setOptions(ops);
                    }

                    else if (region_type.equals("polygon"))
                    {
                        ArrayList<String> x_string_buffer = new ArrayList<String>();
                        ArrayList<String> y_string_buffer = new ArrayList<String>();
                        while (st.hasMoreToken())
                        {
                            String token = st.nextToken();
                            x_string_buffer.add(token);
                            y_string_buffer.add(st.nextToken());
                        }
                        if (options!=null) ops= parseRegionOption(options,globalOps,include);
                        String x_string[] = x_string_buffer.toArray(
                                new String[x_string_buffer.size()]);
                        String y_string[] = y_string_buffer.toArray(
                                new String[y_string_buffer.size()]);
                        WorldPt wpAry[]= new WorldPt[x_string.length];

                        for(int i=0; (i<wpAry.length); i++) {
                            wpAry[i]= parseWorldPt(coordSys, x_string[i], y_string[i]);
                        }
                        region = new RegionLines(wpAry);
                        region.setOptions(ops);
                    }
                    else
                    {
                        throw new RegParseException("unrecognized region type: " + region_type);
                    }
                    if (region!=null) {
                        retList.add(region);
                        allowHeader= true;
                    }
                }
	      /* Now do the rendering of the region */
            }
            catch (NoSuchElementException e)
            {
                throw new RegParseException("missing argument(s) on following line: "+virtualLine);
            }
        }
        return retList;
    }

    public static Global createGlobal(List<Region> regList) {
        RegionOptions ops= new RegionOptions();
        Map<String, Integer> colorCnt= new HashMap<String, Integer>(regList.size());
        Map<RegionFont, Integer> fontCnt= new HashMap<RegionFont, Integer>(regList.size());
        String color;
        RegionFont font;
        RegionOptions regOps;
        for(Region r : regList) {
            regOps= r.getOptions();
            color= regOps.getColor();
            font= regOps.getFont();
            if (colorCnt.containsKey(color)) {
                int v= colorCnt.get(color);
                v++;
                colorCnt.put(color,v);
            }
            else {
                colorCnt.put(color, 1);
            }
            if (fontCnt.containsKey(font)) {
                int v= fontCnt.get(font);
                v++;
                fontCnt.put(font, v);
            }
            else {
                fontCnt.put(font, 1);
            }
        }
        int max=1;
        String maxColor= null;
        for(Map.Entry<String,Integer> entry : colorCnt.entrySet()) {
            if (entry.getValue()>max) {
                max= entry.getValue();
                maxColor= entry.getKey();
            }
        }
        if (maxColor!=null) ops.setColor(maxColor);


        RegionFont maxFont= null;
        max= 1;
        for(Map.Entry<RegionFont,Integer> entry : fontCnt.entrySet()) {
            if (entry.getValue()>max) {
                max= entry.getValue();
                maxFont= entry.getKey();
            }
        }
        if (maxFont!=null) ops.setFont(maxFont);
        return new Global(ops);
    }

    private static WorldPt parseWorldPt(RegionCsys coordSys, String xStr, String yStr) throws RegParseException{
        boolean contextIsWorld=  (coordSys!=RegionCsys.PHYSICAL &&
                                  coordSys!=RegionCsys.UNDEFINED &&
                                  coordSys!=RegionCsys.IMAGE &&
                                  coordSys!=RegionCsys.ICRS &&
                                  coordSys!=RegionCsys.AMPLIFIER &&
                                  coordSys!=RegionCsys.LINEAR &&
                                  coordSys!=RegionCsys.DETECTOR);

        RegionValue x= convertToRegionValue(xStr, ValueType.LON, contextIsWorld);
        RegionValue y= convertToRegionValue(yStr, ValueType.LAT,contextIsWorld);

        if (!contextIsWorld && (x.isWorldCoords() || y.isWorldCoords())) {
            coordSys= RegionCsys.J2000;
        }

        return makeWorldPt(coordSys, x, y);
    }

    private static WorldPt makeWorldPt(RegionCsys coord_sys, RegionValue x, RegionValue y) {
        CoordinateSys csys= parse_coordinates(coord_sys);
        WorldPt wp;

        if (x.isWorldCoords() && y.isWorldCoords()) {
            wp= new WorldPt(x.toDegree(), y.toDegree(), csys);
        }
        else {
            if (x.getType()==RegionValue.Unit.SCREEN_PIXEL) csys= CoordinateSys.SCREEN_PIXEL;
            else if (x.getType()==RegionValue.Unit.IMAGE_PIXEL) csys= CoordinateSys.PIXEL;
            else csys= CoordinateSys.SCREEN_PIXEL;
            wp= new WorldPt(x.getValue(), y.getValue(), csys);
        }
        return wp;
    }

    /** Convert coordinates to J2000 */
    private static CoordinateSys parse_coordinates(RegionCsys coord_sys)  {
        CoordinateSys coordinate_sys;
        switch (coord_sys)
        {
            case FK4:
            case B1950:
                coordinate_sys = CoordinateSys.EQ_B1950;
                break;
            case FK5:
            case J2000:
            case ICRS:
                coordinate_sys = CoordinateSys.EQ_J2000;
                break;
            case ECLIPTIC:
                coordinate_sys = CoordinateSys.ECL_J2000;
                break;
            case GALACTIC:
                coordinate_sys = CoordinateSys.GALACTIC;
                break;
            case IMAGE:
                coordinate_sys = CoordinateSys.PIXEL;
                break;
            case PHYSICAL:
                coordinate_sys = CoordinateSys.SCREEN_PIXEL;
                break;
            case LINEAR:
            case AMPLIFIER:
            case DETECTOR:
            case UNDEFINED:
                coordinate_sys = CoordinateSys.UNDEFINED;
                break;
            default:
                coordinate_sys = CoordinateSys.EQ_J2000;
        }
        return coordinate_sys;
    }

    private static ParseValueRet parseCoordinates(String vString, ValueType vType)  throws RegParseException {
        double v;
        boolean isHMS= false;

        try
        {
            v = Double.parseDouble(vString);
        }
        catch (NumberFormatException nex) {
	    /* not a simple number;  look for sexigesimal etc.  */
                try {
                    if (vType==ValueType.LON) {
                        v = convertStringToLon(vString);
                        isHMS= true;
                    }
                    else if (vType==ValueType.LAT) {
                        v = convertStringToLat(vString);
                        isHMS= true;
                    }
                    else {
                        throw new RegParseException("Error parsing value: " + nex.getMessage() );
                    }
                } catch (CoordException ce) {
                    throw new RegParseException("Error parsing coordinates: " + nex.getMessage() );
                }
        }
        return new ParseValueRet(v,isHMS);
    }


    public static double convertStringToLon(String hms) throws CoordException {
        if (converter!=null) return converter.convertStringToLon(hms);
        else throw new CoordException("No CoordConverter defined");
    }

    public static double convertStringToLat(String dms) throws CoordException {
        if (converter!=null) return converter.convertStringToLat(dms);
        else throw new CoordException("No CoordConverter defined");
    }


    private static RegionValue convertToRegionValue(String radius_string, ValueType vType, boolean contextIsWorld)
                                                     throws RegParseException {
        char unit_char =
                radius_string.charAt(radius_string.length() - 1);
        RegionValue.Unit unit = RegionValue.Unit.CONTEXT;
        String originalStr= radius_string;
        if (!Character.isDigit(unit_char))
        {
	    /* remove trailing character */
            radius_string = radius_string.substring(0, radius_string.length() - 1);
            switch(unit_char)
            {
                case '"':      // arcsec
                    unit= RegionValue.Unit.ARCSEC;
                    break;
                case '\'':     // arcmin
                    unit= RegionValue.Unit.ARCMIN;
                    break;
                case 'd':      // degrees
                    unit= RegionValue.Unit.DEGREE;
                    break;
                case 'r':      // radians
                    unit= RegionValue.Unit.RADIANS;
                    break;
                case 'p':      // physical
                    unit= RegionValue.Unit.SCREEN_PIXEL;
                    break;
                case 'i':      // image
                    unit= RegionValue.Unit.IMAGE_PIXEL;
                    break;
                default:
                    unit = RegionValue.Unit.CONTEXT;
                    radius_string= originalStr;
                    break;
            }
        }


        ParseValueRet pV= parseCoordinates(radius_string,vType);

        if (contextIsWorld && unit== RegionValue.Unit.CONTEXT) unit= RegionValue.Unit.DEGREE;
        else if (pV.isHMS) unit= RegionValue.Unit.DEGREE;

        return new RegionValue(pV.getValue(),unit);
    }

    private static RegionOptions parseRegionOption(String s, RegionOptions fallback, boolean include) {
        RegOpsParseRet ret= parseRegionOptionPlus(s,fallback,include);
        return ret.ops;
    }

    private static RegOpsParseRet parseRegionOptionPlus(String s, RegionOptions fallback, boolean include) {
        s= s.replaceAll(" *=", "=");
        s= s.replaceAll("= *", "=");

        RegOpsParseRet retval= new RegOpsParseRet();
        retval.ops= (fallback!=null) ? fallback.copy() : new RegionOptions();
        boolean lookForPointSize= false;
        StringTokenizer st= new StringTokenizer(s, " ");
        String workStr= s;
        retval.ops.setInclude(include);
        while (st.hasMoreToken())
        {
            String token = st.nextToken();
            if (token.toLowerCase().startsWith("color=")) {
                retval.ops.setColor(parseColor(token));
            }
            else if (token.toLowerCase().startsWith("width=")) {
                retval.ops.setLineWidth(parseInt(token,1));
            }
            else if (token.toLowerCase().startsWith("highlite=") ||
                     token.toLowerCase().startsWith("highlight=")) {
                retval.ops.setHighlightable(parseBoolean(token, true));
            }
            else if (token.toLowerCase().startsWith("include=") ) {
                retval.ops.setInclude(parseBoolean(token, true));
            }
            else if (token.toLowerCase().startsWith("edit=")) {
                retval.ops.setEditable(parseBoolean(token, true));
            }
            else if (token.toLowerCase().startsWith("offsetx=")) {
                retval.ops.setOffsetX(parseInt(token, 0));
            }
            else if (token.toLowerCase().startsWith("offsety=")) {
                retval.ops.setOffsetY(parseInt(token, 0));
            }
            else if (token.toLowerCase().startsWith("text=")) {
                int endIdx= 5;
                String textPlus= workStr.substring(workStr.indexOf("text=")+5);
                String start= getStartCharOfString(textPlus);
                if (start!=null && textPlus.startsWith(start)) {
                    String end=  (start.equals("{")) ? "}" : start;
                    textPlus= textPlus.substring(1);
                    endIdx= textPlus.indexOf(end);
                    if (endIdx>-1) {
                        String textString= textPlus.substring(0,endIdx);
                        retval.ops.setText(textString);
                    }
                    else {
                        endIdx= 5;
                    }
                }
                workStr= workStr.substring(endIdx+1).trim();
                st= new StringTokenizer(workStr, " ");
            }
            else if (token.toLowerCase().startsWith("font=")) {
                int endIdx= 5;
                String textPlus= workStr.substring(workStr.indexOf("font=")+5);
                if (textPlus.startsWith("\"")) {
                    textPlus= textPlus.substring(1);
                    endIdx= textPlus.indexOf("\"");
                    if (endIdx>-1) {
                        String fontString= textPlus.substring(0,endIdx);
                        retval.ops.setFont(new RegionFont(fontString));
                    }
                    else {
                        endIdx= 5;
                    }
                }
                workStr= workStr.substring(endIdx+1).trim();
                st= new StringTokenizer(workStr, " ");
            }
            else if (token.toLowerCase().startsWith("point=")) {
                retval.pointType= parsePointType(token);
                lookForPointSize= true;
            }
            else if (lookForPointSize) {
                try {
                    retval.ptSize= Integer.parseInt(token);
                    lookForPointSize= false;
                } catch (NumberFormatException e) {
                    // ignore and more one.
                }

            }
        }
        return retval;

    }

    private static RegionPoint.PointType parsePointType(String token) {
        RegionPoint.PointType retval= RegionPoint.PointType.X;
        token= token.toLowerCase();
        String ptAry[]= token.split("=");
        if (ptAry.length==2 && ptAry[0].equalsIgnoreCase("point")) {
            retval= convertPointType(ptAry[1]);
            if (retval==null) retval= RegionPoint.PointType.X;
        }
        return retval;
    }

    private static boolean isPointTypeTwo(String s) {
        StringTokenizer st= new StringTokenizer(s,DELIM);
        String ptTypeStr= st.hasMoreToken() ? st.nextToken() : null;
        String ptConfirm= st.hasMoreToken() ? st.nextToken() : null;
        boolean retval= (ptConfirm.equalsIgnoreCase("point") && convertPointType(ptTypeStr)!=null);
        return retval;
    }

    private static RegionPoint.PointType convertPointType(String s) {
        RegionPoint.PointType retval= null;

        if      (s==null)                         retval = null;
        else if (s.equalsIgnoreCase("circle"))    retval = RegionPoint.PointType.Circle;
        else if (s.equalsIgnoreCase("box"))       retval = RegionPoint.PointType.Box;
        else if (s.equalsIgnoreCase("diamond"))   retval = RegionPoint.PointType.Diamond;
        else if (s.equalsIgnoreCase("cross"))     retval = RegionPoint.PointType.Cross;
        else if (s.equalsIgnoreCase("x"))         retval = RegionPoint.PointType.X;
        else if (s.equalsIgnoreCase("arrow"))     retval = RegionPoint.PointType.Arrow;
        else if (s.equals("boxcircle")) retval = RegionPoint.PointType.BoxCircle;
        return retval;
    }


    private static String parseColor(String token)
    {
        String return_color = "green";
        StringTokenizer st1 = new StringTokenizer(token, "=");
        if (st1.hasMoreToken()) st1.nextToken();
        if (st1.hasMoreToken())
        {
            return_color = st1.nextToken().trim();
        }
        return(return_color);
    }

    private static int parseInt(String token, int defWidth)
    {
        int width = defWidth;
        StringTokenizer st1 = new StringTokenizer(token, "=");
        if (st1.hasMoreToken()) st1.nextToken();
        if (st1.hasMoreToken())
        {
            try {
                width= Integer.parseInt(st1.nextToken().trim());
            } catch (NumberFormatException e) {
                width= defWidth;
            }
        }
        return width ;
    }

    private static boolean isCoordSys(String s) {
        return getCoordSys(s)!=null;
    }

    private static RegionCsys getCoordSys(String s) {
        RegionCsys coordSys;
        if (s.equalsIgnoreCase("fk4") || s.equalsIgnoreCase("B1950")) {
            coordSys = RegionCsys.FK4;
        }
        else if (s.equalsIgnoreCase("fk5") || s.equalsIgnoreCase("J2000")) {
            coordSys = RegionCsys.FK5;
        }
        else if (s.equalsIgnoreCase("ecliptic")) {
            coordSys = RegionCsys.ECLIPTIC;
        }
        else if (s.equalsIgnoreCase("galactic"))
        {
            coordSys = RegionCsys.GALACTIC;
        }
        else if (s.equalsIgnoreCase("image"))
        {
            coordSys = RegionCsys.IMAGE;
        }
        else if (s.equalsIgnoreCase("physical"))
        {
            coordSys = RegionCsys.PHYSICAL;
        }
        else {
            coordSys= null;
        }
        return coordSys;
    }

    private static String getRegionCSysStr(CoordinateSys csys) {
        String retval= "PHYSICAL";
        if      (csys.equals(CoordinateSys.EQ_B1950))     retval= "B1950";
        else if (csys.equals(CoordinateSys.EQ_J2000))     retval= "J2000";
        else if (csys.equals(CoordinateSys.ECL_J2000))    retval= "ECLIPTIC";
        else if (csys.equals(CoordinateSys.ECL_J2000))    retval= "ECLIPTIC";
        else if (csys.equals(CoordinateSys.PIXEL))        retval= "IMAGE";
        else if (csys.equals(CoordinateSys.SCREEN_PIXEL)) retval= "PHYSICAL";
        else  retval= "PHYSICAL";

        return retval;
    }


    private static boolean parseBoolean(String token, boolean def)
    {
        boolean retval= def;
        StringTokenizer st1 = new StringTokenizer(token, "=");
        if (st1.hasMoreToken()) st1.nextToken();
        if (st1.hasMoreToken())
        {
            String v= st1.nextToken().trim();
            if (v.length()==1) {
                if (v.equals("1")) retval= true;
                else if (v.equals("0")) retval= false;
            }
        }
        return(retval);
    }

    public static boolean containsRegion(List<RegionFileElement> resultList) {
        boolean retval= false;
        for(RegionFileElement p : resultList) {
            if (p instanceof Region) {
                retval= true;
                break;
            }
        }
        return retval;
    }

    public static void addAllRegions(List<RegionFileElement> resultList, List<Region> regList) {
        for(RegionFileElement p : resultList) {
            if (p instanceof Region) regList.add((Region)p);
        }
    }

    public static Global getGlobal(List<RegionFileElement> resultList, Global fallback) {
        Global retval= fallback;
        for(RegionFileElement p : resultList) {
            if (p instanceof Global)  retval= (Global)p;
        }
        return retval;
    }

    public static RegionCsys getCsys(List<RegionFileElement> resultList, RegionCsys coordSys) {
        RegionCsys retval= coordSys;
        for(RegionFileElement p : resultList) {
            if (p instanceof RegionCsys)  retval= (RegionCsys)p;
        }
        return retval;
    }

    public static String serialize(Global global, boolean supportExt) {
        return "global " + makeOptionOut(global,null,supportExt);
    }


    public static String serialize(Region r) { return serialize(r,null,true); }

    public static String serialize(Region r, Global global, boolean supportExt) {
        String retval= null;
        if (r instanceof RegionAnnulus) {
            RegionAnnulus ra= (RegionAnnulus)r;
            retval= ra.isCircle() ? makeCircleString(ra,global, supportExt) :
                                    makeAnnulusString(ra,global,supportExt);
        }
        else if (r instanceof RegionBox) {
            retval= makeBoxString((RegionBox)r,global,supportExt);
        }
        else if (r instanceof RegionBoxAnnulus) {
            retval= makeBoxAnnulusString((RegionBoxAnnulus)r,global,supportExt);
        }
        else if (r instanceof RegionEllipse) {
            RegionEllipse re= (RegionEllipse)r;
            throw new IllegalArgumentException("not implemented");

        }
        else if (r instanceof RegionEllipseAnnulus) {
            RegionEllipseAnnulus rea= (RegionEllipseAnnulus)r;
            throw new IllegalArgumentException("not implemented");
        }
        else if (r instanceof RegionLines) {
            RegionLines rl= (RegionLines)r;
            retval= rl.isPolygon() ? makePolygonString(rl,global,supportExt) :
                                     makeLineString(rl,global,supportExt);
        }
        else if (r instanceof RegionPoint) {
            retval= makePointString((RegionPoint)r,global,supportExt);

        }
        else if (r instanceof RegionText) {
            retval= makeTextString((RegionText)r,global,supportExt);
        }

        return retval;
    }


    private static String makeCoordStart(Region r) {
        return getRegionCSysStr(r.getPt().getCoordSys()) + ";";
    }

    private static String makeXY(WorldPt wp) { return wp.getLon() + " "  + wp.getLat(); }

    private static String makeValue(String k, int v) { return makeValue(k,v+""); }

    private static String makeValue(String k, String v) { return k+"="+v+" "; }

    private static String makeValue(String k, boolean v) {
        String vStr= v ? "1" : "0";
        return k+"="+vStr+" ";
    }

    private static String makeColorValue(String k, String color, boolean supportExt) {
        String retval;
        if (supportExt) {
           retval= makeValue(k,color);
        }
        else {
            if (isHexColor(color)) {
                retval= makeValue(k,convertColor(color));
            }
            else {
                retval= makeValue(k,color);
            }

        }
        return retval;
    }

    private static String makeOptionOut(ContainsOptions optionElement,
                                        Global global,
                                        boolean supportExt ) {
        RegionOptions globalOps= (global!=null) ? global.getOptions() : new RegionOptions();
        boolean alwaysWrite= optionElement instanceof Global;

        RegionOptions op= optionElement.getOptions();

        String retval= "";
        if (op!=null) {
            StringBuilder sb= new StringBuilder(150);
            if (!op.getColor().equals(globalOps.getColor()) || alwaysWrite) {
                sb.append(makeColorValue("color",op.getColor(),supportExt));
            }
            if (op.isEditable()!=globalOps.isEditable()) {
                sb.append(makeValue("edit",op.isEditable()));
            }
            if (op.isMovable()!=globalOps.isMovable()) {
                sb.append(makeValue("move",op.isMovable()));
            }
            if (op.isRotatable()!=globalOps.isRotatable()) {
                sb.append(makeValue("rotate",op.isRotatable()));
            }
            if (op.isHighlightable()!=globalOps.isHighlightable()) {
                sb.append(makeValue("highlight",op.isHighlightable()));
            }
            if (op.isInclude()!=globalOps.isInclude()) {
                sb.append(makeValue("include",op.isInclude()));
            }
            if (op.isDeletable()!=globalOps.isDeletable()) {
                sb.append(makeValue("delete",op.isDeletable()));
            }
            if (op.isFixedSize()!=globalOps.isFixedSize()) {
                sb.append(makeValue("fixed",op.isFixedSize()));
            }
            if (op.getLineWidth()!=globalOps.getLineWidth()) {
                sb.append(makeValue("width",op.getLineWidth()));
            }
            if (op.getOffsetX()!=globalOps.getOffsetX() && supportExt) {
                sb.append(makeValue("offsetx",op.getOffsetX()));
            }
            if (op.getOffsetY()!=globalOps.getOffsetY() && supportExt) {
                sb.append(makeValue("offsety",op.getOffsetY()));
            }
            if (!StringUtils.isEmpty(op.getText())) {
                sb.append(makeValue("text","{"+op.getText()+"}"));
            }
            if (!op.getFont().equals(globalOps.getFont())) {
                sb.append(makeValue("font","\""+op.getFont()+"\""));
            }
            if (optionElement instanceof RegionPoint) {
                RegionPoint rp= (RegionPoint)optionElement;
                sb.append(makeValue("point", rp.getPointType().toString().toLowerCase()));
                if (rp.getPointSize()>0) sb.append(rp.getPointSize());
            }
            retval= sb.toString();
        }
        return retval;
    }

    private static String makePropOut(Region r, Global g, boolean supportExt) {
        String opOut= makeOptionOut(r,g, supportExt);
        if (opOut.length()>0)  return " # "+ opOut;
        else                   return "";

    }


    private static String makeCircleString(RegionAnnulus ra, Global g, boolean supportExt) {
        return makeCoordStart(ra) + "circle " +
                makeXY(ra.getPt()) + " "+
                ra.getRadii()[0] +
                makePropOut(ra,g,supportExt);
    }

    private static String makeAnnulusString(RegionAnnulus ra, Global g, boolean supportExt) {
        StringBuilder sb= new StringBuilder(100);
        sb.append(makeCoordStart(ra));
        sb.append("annulus ").append(makeXY(ra.getPt())).append(" ");
        for(RegionValue v : ra.getRadii()) {
            sb.append(v).append(" ");
        }
        sb.append(makePropOut(ra,g, supportExt));
        return sb.toString();
    }

    private static String makeBoxString(RegionBox rb, Global g, boolean supportExt) {
        RegionDimension dim= rb.getDim();
        return makeCoordStart(rb) +
                "box " + makeXY(rb.getPt()) + " "+
                dim.getWidth() + " " + dim.getHeight() +" " +
                rb.getAngle() + makePropOut(rb,g,supportExt);
    }

    private static String makeBoxAnnulusString(RegionBoxAnnulus ra, Global g, boolean supportExt) {
        StringBuilder sb= new StringBuilder(100);
        sb.append(makeCoordStart(ra));
        sb.append("box ").append(makeXY(ra.getPt())).append(" ");
        for(RegionDimension dim : ra.getDim()) {
            sb.append(dim.getWidth()).append(" ").append(dim.getHeight()).append(" ");
        }
        sb.append(ra.getRotation());
        sb.append(makePropOut(ra,g,supportExt));
        return sb.toString();

    }

    private static String makePointString(RegionPoint rl, Global g, boolean supportExt) {
        return makeCoordStart(rl) + "point " + makeXY(rl.getPt()) + " "+ makePropOut(rl,g,supportExt);
    }


    private static String makeTextString(RegionText rl, Global g, boolean supportExt) {
        return makeCoordStart(rl) + "text " + makeXY(rl.getPt()) + " "+ makePropOut(rl,g,supportExt);
    }


    private static String makeLineString(RegionLines rl, Global g, boolean supportExt) {
        return makeCoordStart(rl) + "line " +
                makeXY(rl.getPtAry()[0]) + " "+
                makeXY(rl.getPtAry()[1]) + " "+
                makePropOut(rl,g,supportExt);
    }



    private static String makePolygonString(RegionLines rl, Global g, boolean supportExt) {
        StringBuilder sb= new StringBuilder(100);
        sb.append(makeCoordStart(rl));
        sb.append("polygon ");
        for(WorldPt wp : rl.getPtAry()) {
            sb.append(makeXY(wp)).append(" ");
        }
        sb.append(makePropOut(rl,g,supportExt));
        return sb.toString();
    }


    private static class RegOpsParseRet {
        RegionPoint.PointType pointType= RegionPoint.PointType.X;
        RegionOptions ops= null;
        int ptSize = -1;
    }

    public interface CoordConverter {
        public double convertStringToLon(String hms) throws CoordException;
        public double convertStringToLat(String dms) throws CoordException;
    }

    public static interface LineGetter {
        public  String getLine();
    }

    public static class ParseRet {
        final List<Region>  regionList;
        final List<String> msgList;

        public ParseRet(List<Region> regionList, List<String> msgList) {
            this.regionList = regionList;
            this.msgList = msgList;
        }

        public List<Region> getRegionList() { return regionList; }
        public List<String> getMsgList() { return msgList; }
    }


    public static String getStartCharOfString(String s)  {
        int idx= getStartOfString(s);
        String retval= (idx>-1) ? s.charAt(idx)+"" : null;
        return retval;

    }

    public static int getStartOfString(String s)  {
        int bracket= s.indexOf("{");
        int quote= s.indexOf("\"");
        int single= s.indexOf("'");
        if (bracket==-1) bracket= Integer.MAX_VALUE;
        if (quote==-1)   quote= Integer.MAX_VALUE;
        if (single==-1)  single= Integer.MAX_VALUE;
        int retval= Math.min(bracket, Math.min(quote,single));
        if (retval==Integer.MAX_VALUE) retval= -1;
        return retval;
    }


    private static boolean isHexColor(String text) {
        boolean retval= false;
        if (text.length() == 6 || (text.length()==7 && text.startsWith("#"))) {
            if (text.startsWith("#")) text=text.substring(1);
            retval= true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.digit(c, 16) == -1) {
                    retval= false;
                    break;
                }
            }
        }
        return retval;
    }

//    public static String  pullOutString(String s) {
//        int idx= getStartOfString(s);
//        String retval= null;
//        if (idx>-1) {
//            char start= s.charAt(idx);
//            int end= s.indexOf(start, idx+1);
//            if (end>-1) retval= s.substring(idx,end);
//        }
//        return retval;
//    }


    private static class ParseValueRet {
        private final double value;
        private final boolean isHMS;

        private ParseValueRet(double value, boolean HMS) {
            this.value = value;
            isHMS = HMS;
        }

        public double getValue() {
            return value;
        }

        public boolean isHMS() {
            return isHMS;
        }
    }

    private static String convertColor(String inColor) {
        String retval= inColor;
        int rgb[]= toRGB(inColor);
        if (rgb!=null) {
            if (rgb[0]==rgb[1] && rgb[0]==rgb[2]) {
                retval= "gray";
            }
            else if (rgb[0]>rgb[1] && rgb[0]>rgb[2]) {
                retval= "red";
            }
            else if (rgb[1]>rgb[0] && rgb[1]>rgb[2]) {
                retval= "green";
            }
            else if (rgb[2]>rgb[0] && rgb[2]>rgb[1]) {
                retval= "blue";
            }
            else if (rgb[0]==rgb[1]) {
                retval= "yellow";
            }
            else if (rgb[1]==rgb[2]) {
                retval= "aqua";
            }
            else {
                retval= "red";
            }
        }
        return retval;

    }

    public static int [] toRGB(String colorStr) {
        int retval[]= null;
        if (isHexColor(colorStr)) {
            try {
                //Note: subString(1) returned a new instance, so isHexColor(colorStr) cannot remove "#" in colorStr.
                if (colorStr.startsWith("#")) colorStr=colorStr.substring(1);
                String rStr= colorStr.substring(0,2);
                String gStr= colorStr.substring(2,4);
                String bStr= colorStr.substring(4);
                int red= Integer.parseInt(rStr, 16);
                int green= Integer.parseInt(gStr, 16);
                int blue= Integer.parseInt(bStr, 16);
                retval= new int[] { red, green, blue};
            } catch (NumberFormatException e) {
                retval= null;
            }
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
