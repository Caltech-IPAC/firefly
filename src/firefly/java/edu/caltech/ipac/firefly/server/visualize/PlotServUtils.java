/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Header;

import java.awt.Color;
import java.io.File;
import java.util.List;

import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.findHeaderValue;


/**
 * @author Trey Roby
 */
public class PlotServUtils {

    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    private static final Logger.LoggerImpl _log= Logger.getLogger();

    public static final String STARTING_READ_MSG = "Retrieving Data";
    public static final String READ_PERCENT_MSG = "Retrieving ";
    public static final String ENDING_READ_MSG = "Loading Data";
    public static final String CREATING_MSG =  "Creating Images";
    public static final String PROCESSING_MSG =  "Processing Images";
    public static final String PROCESSING_COMPLETED_MSG =  "Processing Images Completed";

    public static void updateProgress(ProgressStat pStat) {
        Cache cache= UserCache.getInstance();
        CacheKey key= new StringKey(pStat.getId());
        ProgressStat lastPstat= (ProgressStat) cache.get(key);
        boolean fireAction= true;
        if (lastPstat!=null) {
            if (lastPstat.getMessage()!=null && lastPstat.getMessage().equals((pStat.getMessage()))) {
                fireAction= false;
            }

        }
        if (pStat.getId()!=null) cache.put(key, pStat);


        if (fireAction)  {
            ProgressMessage progMsg= getPlotProgressMessage(pStat);
            FluxAction a= new FluxAction("ImagePlotCntlr.PlotProgressUpdate");
            a.setValue(progMsg.message,"message");
            a.setValue(pStat.getId(),"requestKey");
            a.setValue(pStat.getType()==ProgressStat.PType.GROUP,"group");
            a.setValue(progMsg.done,"done");
            a.setValue( pStat.getPlotId(),"plotId");
            ServerEventManager.fireAction(a);
        }
    }

    public static void updateProgress(String key, String plotId, ProgressStat.PType type, String progressMsg) {
        if (key==null) return;
        updateProgress(new ProgressStat(key,plotId, type,progressMsg));
    }

    public static void updateProgress(WebPlotRequest r, ProgressStat.PType type, String progressMsg) {
        if (r==null) return;
        String key= r.getProgressKey();
        if (key!=null) updateProgress(new ProgressStat(key,r.getPlotId(), type,progressMsg));
    }


    static String makePlotDesc(PlotState state, ActiveFitsReadGroup frGroup, String dataDesc, boolean isMultiImage) {

        WebPlotRequest req= state.getWebPlotRequest();
        Header header= frGroup.getFitsRead(state.firstBand()).getHeader();

        return switch (req.getTitleOptions()) {
            case PLOT_DESC -> (req.getTitle() == null ? "" : req.getTitle()) + dataDesc;
            case FILE_NAME -> (isMultiImage)? findTitleByHeader(header,state,req) : "";
            case HEADER_KEY -> findTitleByHeader(header,state,req);
            case PLOT_DESC_PLUS -> {
                String s= req.getPlotDescAppend();
                yield req.getTitle()+ (s!=null ? " "+s : "");
            }
            case SERVICE_OBS_DATE -> {
                if (req.getRequestType()!= RequestType.SERVICE) yield "";
                yield req.getTitle() + ": " + getDateValueFromServiceFits(req.getServiceType(), header);
            }
            default ->  "";
        };
    }

    private static String findTitleByHeader(Header header, PlotState state, WebPlotRequest req) {
        return findHeaderValue(header,
                        req.getHeaderKeyForTitle(),
                        "EXTNAME",
                        "EXTTYPE",
                        state.getCubeCnt()>0 ? "PLANE"+state.getImageIdx(state.firstBand()) : null
        );
    }



    private static String getServiceDateHeaderKey(WebPlotRequest.ServiceType sType) {
        return switch (sType) {
            case TWOMASS -> "ORDATE";
            case ATLAS, DSS -> "DATE-OBS";
            case WISE -> "MIDOBS";
            case SDSS -> "DATE-OBS";
            case IRIS -> "DATEIRIS";
            default -> "none";
        };
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, File f) {
        Header header=  FitsReadUtil.getTopFitsHeader(f);
        return (header!=null) ? getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header) : "";
    }


    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, Header header) {
        return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
    }

    private static String getDateValueFromServiceFits(String headerKey, Header header) {
        long currentYear = Math.round(Math.floor((double) System.currentTimeMillis() /1000/3600/24/365.25) +1970);
        long year;
        String dateValue= header.getStringValue(headerKey);
        if(dateValue !=null){
            switch (headerKey) {
                case "ORDATE" -> {
                    if (dateValue.length() > 5) {
                        dateValue = dateValue.subSequence(0, 2) + "-" + dateValue.subSequence(2, 4) + "-" +
                                dateValue.subSequence(4, 6);
                        year = 2000 + Integer.parseInt(dateValue.subSequence(0, 2).toString());
                        if (year > currentYear) {
                            dateValue = "19" + dateValue;
                        } else {
                            dateValue = "20" + dateValue;
                        }
                    }
                }
                case "DATE-OBS" -> {
                    dateValue = dateValue.split("T")[0];
                    if (dateValue.contains("/")) {
                        String newDate = "";
                        for (String v : dateValue.split("/")) {
                            if (newDate.length() == 0) {
                                newDate = v;
                            } else {
                                newDate = v + "-" + newDate;
                            }
                        }
                        year = 2000 + Integer.parseInt(newDate.subSequence(0, 2).toString());
                        if (year > currentYear) {
                            dateValue = "19" + newDate;
                        } else {
                            dateValue = "20" + newDate;
                        }
                    }
                }
                case "MIDOBS" -> dateValue = dateValue.split("T")[0];
                case "DATEIRIS" -> dateValue = "1983";
            }
            return dateValue;
        }else{
            return "";
        }
    }


    static void statsLog(String function, Object... sAry) {
        _statsLog.stats(function, sAry);
    }

    public static Circle getRequestArea(WebPlotRequest request) {
        Circle retval = null;
        WorldPt wp= request.getWorldPt();

        if (wp==null && request.containsParam(WebPlotRequest.OBJECT_NAME)) {
            String objName= request.getObjectName();
            if (!StringUtils.isEmpty(objName)) {
                try {
                    wp= TargetNetwork.resolveToWorldPt(objName, request.getResolver());
                } catch (Exception ignore) { }
            }
        }

        float side = request.getSizeInDeg();
        if (wp != null) retval = new Circle(wp, side);
        return retval;
    }


    private static Color parseRGB(String color) {
        String rgb = "rgb";
        String rgba = "rgba";
        Color c = null;

        if ((color.startsWith(rgb) || color.startsWith(rgba)) && color.endsWith(")")) {
            int s = color.indexOf('(');
            int e = color.lastIndexOf(')');

            String   rgbStr = color.substring(s+1, e);
            String[] rgbVal = rgbStr.split(",");

            if ((color.startsWith(rgba)&&rgbVal.length == 4) || (rgbVal.length == 3)) {
                int i;
                int[] v = new int[3];

                for (i = 0; i < 3; i++) {
                    try {
                        v[i] = Integer.parseInt(rgbVal[i]);
                    } catch (NumberFormatException ex) {
                        break;
                    }
                }
                if (i == 3) {
                    c = new Color(v[0], v[1], v[2]);  // rgb only
                }
            }
        }
        if (c == null) {
            c = Color.lightGray;
            _log.debug("parseRGB(String color) does not understand " + color + ".  Color.lightGray is assigned.");
        }
        return c;
    }

    public static Color convertColorHtmlToJava(String color) {
        if (isHexColor(color)) {
            int[] rgb=  toRGB(color);
            return new Color(rgb[0],rgb[1],rgb[2]);
        }
        else if (color.startsWith("rgb")) {
            return parseRGB(color);
        } else {
            return switch (color) {
                case "black" -> Color.black;
                case "aqua" -> new Color(0, 255, 255);
                case "blue" -> Color.blue;
                case "cyan" -> Color.cyan;
                case "fuchsia" -> new Color(255, 0, 255);
                case "gray" -> new Color(128, 128, 128);
                case "green" -> new Color(0, 128, 0);
                case "lime" -> Color.green;  // this is correct, lime is 0,255,0
                case "magenta" -> Color.magenta;
                case "maroon" -> new Color(128, 0, 0);
                case "navy" -> new Color(0, 0, 128);
                case "olive" -> new Color(128, 128, 0);
                case "orange" -> Color.orange;
                case "pink" -> Color.pink;
                case "purple" -> new Color(128, 0, 128);
                case "red" -> Color.red;
                case "silver" -> new Color(192, 192, 192);
                case "teal" -> new Color(0, 128, 128);
                case "white" -> Color.white;
                case "yellow" -> Color.yellow;
                default -> Color.lightGray;// lightGray or white is better presentation for "unknown" color string.
            };
        }
    }

    private static final ProgressMessage EMPTY_MESSAGE= new ProgressMessage("",false);

    static ProgressMessage getPlotProgressMessage(ProgressStat stat) {
        ProgressMessage progMessage = EMPTY_MESSAGE;
        if (stat != null) {
            if (stat.isGroup()) {
                List<String> keyList = stat.getMemberIDList();
                progMessage = (keyList.size() == 1) ?
                        getSingleStatusMessage(keyList.get(0)) :
                        getMultiStatMessage(stat);
            } else {
                progMessage = new ProgressMessage(stat.getMessage(), stat.isDone());
            }
        }
        return progMessage;
    }


    private static ProgressMessage getSingleStatusMessage(String key) {
        ProgressStat stat = (ProgressStat) UserCache.getInstance().get(new StringKey(key));
        if (stat != null)  return new ProgressMessage(stat.getMessage(), stat.isDone());
        return EMPTY_MESSAGE;
    }

    private static ProgressMessage getMultiStatMessage(ProgressStat stat) {
        ProgressMessage retval;
        Cache cache = UserCache.getInstance();
        List<String> keyList = stat.getMemberIDList();
        ProgressStat statEntry;

        int numSuccess = 0;
        int numDone = 0;
        int total = keyList.size();

        String downloadMsg = null;
        String readingMsg = null;
        String creatingMsg = null;
        ProgressStat.PType ptype;

        for (String key : keyList) {
            statEntry = (ProgressStat) cache.get(new StringKey(key));
            if (statEntry != null) {
                ptype = statEntry.getType();
                if (ptype == ProgressStat.PType.SUCCESS) numSuccess++;
                if (statEntry.isDone()) numDone++;

                switch (ptype) {
                    case DOWNLOADING -> downloadMsg = statEntry.getMessage();
                    case READING -> readingMsg = statEntry.getMessage();
                    case CREATING -> creatingMsg = statEntry.getMessage();
                }
            }
        }
        if (downloadMsg != null) {
            retval = new ProgressMessage(downloadMsg,false);
        } else {
            retval = new ProgressMessage("Loaded " + numSuccess + " of " + total, numDone==keyList.size());
        }
        return retval;
    }

    private record ProgressMessage(String message, boolean done) { }

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

    private static int [] toRGB(String colorStr) {
        int [] retval= null;
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
            } catch (NumberFormatException ignore) {
            }
        }
        return retval;
    }

}
