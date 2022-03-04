/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.VectorObject;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.output.PlotOutput;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Trey Roby
 */
public class PlotServUtils {

    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final int PLOT_FULL_WIDTH = -25;
    private static final int PLOT_FULL_HEIGHT = -25;
    private static final AtomicLong _nameCnt= new AtomicLong(0);

    private static final String JPG_NAME_EXT=FileUtil.jpg;
    private static final String PNG_NAME_EXT=FileUtil.png;
    private static final String _hostname;
    private static final String _pngNameExt="." + PNG_NAME_EXT;

    public static final String STARTING_READ_MSG = "Retrieving Data";
    public static final String READ_PERCENT_MSG = "Retrieving ";
    public static final String ENDING_READ_MSG = "Loading Data";
    public static final String CREATING_MSG =  "Creating Images";
    public static final String PROCESSING_MSG =  "Processing Images";
    public static final String PROCESSING_COMPLETED_MSG =  "Processing Images Completed";

    static {
        _hostname= FileUtil.getHostname();
    }

    static File createFullTile(ImagePlot plot,
                               ActiveFitsReadGroup frGroup,
                               File f,
                               List<FixedObjectGroup> fog,
                               List<VectorObject> vectorList,
                               List<ScalableObjectPosition> scaleList,
                               GridLayer gridLayer) throws IOException {
        return  createOneTile(plot,frGroup,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT,
                              fog,vectorList, scaleList, gridLayer);
    }



    private static File createOneTile(ImagePlot plot,
                                      ActiveFitsReadGroup frGroup,
                                      File f,
                                      int x,
                                      int y,
                                      int width,
                                      int height,
                                      List<FixedObjectGroup> fogList,
                                      List<VectorObject> vectorList,
                                      List<ScalableObjectPosition> scaleList,
                                      GridLayer gridLayer) throws IOException {

        PlotOutput po= new PlotOutput(plot,frGroup);
        if (fogList!=null) po.setFixedObjectGroupList(fogList);
        if (gridLayer!=null) po.setGridLayer(gridLayer);
        if (vectorList!=null) po.setVectorList(vectorList);
        if (scaleList!=null) po.setScaleList(scaleList);
        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (width== PLOT_FULL_WIDTH) width= plot.getScreenWidth();
        if (height== PLOT_FULL_HEIGHT) height= plot.getScreenHeight();

        po.writeTile(f, ext, plot.isUseForMask(),x, y, width, height, null);
        return f;

    }


    public static void updatePlotCreateProgress(ProgressStat pStat) {
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

    public static void updatePlotCreateProgress(String key, String plotId, ProgressStat.PType type, String progressMsg) {
        if (key!=null) {
            updatePlotCreateProgress(new ProgressStat(key,plotId, type,progressMsg));
        }

    }

    public static void updatePlotCreateProgress(WebPlotRequest r, ProgressStat.PType type, String progressMsg) {
        if (r!=null) {
            String key= r.getProgressKey();
            String plotId= r.getPlotId();
            if (key!=null) updatePlotCreateProgress(new ProgressStat(key,plotId, type,progressMsg));
        }
    }

    private static Header getTopFitsHeader(File f) {
        try {
            Fits fits= new Fits(f);
            Header header=  fits.getHDU(0).getHeader();
            FitsReadUtil.closeFits(fits);
            return header;
        } catch (FitsException|IOException  e) {
            return null;
        }
    }


    private static String getServiceDateHeaderKey(WebPlotRequest.ServiceType sType) {
        String header= "none";
        switch (sType) {
            case TWOMASS:
                header= "ORDATE";
                break;
            case ATLAS:
            case DSS:
                header= "DATE-OBS";
                break;
            case WISE:
                header= "MIDOBS";
                break;
            case SDSS:
                header= "DATE-OBS";
                break;
            case IRIS:
                header= "DATEIRIS";
                break;
        }
        return header;
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, File f) {
        Header header=  getTopFitsHeader(f);
        if (header!=null) {
            return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
        }
        else {
            return "";
        }
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, Header header) {
        return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
    }

    private static String getDateValueFromServiceFits(String headerKey, Header header) {
        long currentYear = Math.round(Math.floor(System.currentTimeMillis()/1000/3600/24/365.25) +1970);
        long year;
        String dateValue= header.getStringValue(headerKey);
        if(dateValue !=null){
            switch (headerKey) {
                case "ORDATE":
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
                    break;
                case "DATE-OBS":
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
                    break;
                case "MIDOBS":
                    dateValue = dateValue.split("T")[0];
                    break;
                case "DATEIRIS":
                    dateValue = "1983";
                    break;
            }
            return dateValue;
        }else{
            return "";
        }
    }


    static void statsLog(String function, Object... sAry) {
        _statsLog.stats(function, sAry);
    }

    public static File findWorkingFitsName(File f) {
        String ext= FileUtil.getExtension(f);
        File retval= f;
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            retval= new File(ServerContext.getVisSessionDir(), FileUtil.getBase(f.getName()));
        }
        return retval;
    }




    static File getUniquePngFileName(String nameBase, File dir) {
        File f= new File(dir,nameBase + "-" + _nameCnt.incrementAndGet() +"-"+ _hostname+ _pngNameExt);
        f= FileUtil.createUniqueFileFromFile(f);
        return f;
    }

    static ImagePlot makeImagePlot(ActiveFitsReadGroup frGroup,
                                   float     initialZoomLevel,
                                   boolean   threeColor,
                                   Band      band,
                                   int       initColorID,
                                   RangeValues stretch) throws FitsException {
        return new ImagePlot(null, frGroup,initialZoomLevel, threeColor, band, initColorID, stretch);
    }

    /**
     * Sort the imageMask array in the ascending order based on the mask's index (the bit offset)
     * When such mask array passed to create IndexColorModel, the number of the colors can be decided using the
     * masks colors and store the color according to the order of the imageMask in the array.
     *
     * @param imageMasks the mask
     * @return the ImageMask array
     */
    private static ImageMask[] sortImageMaskArrayInIndexOrder(ImageMask[] imageMasks){

        Map<Integer, ImageMask> unsortedMap= new HashMap<>();
        for (ImageMask imageMask : imageMasks) {
            unsortedMap.put(imageMask.getIndex(), imageMask);
        }

        Map<Integer, ImageMask> treeMap = new TreeMap<>(unsortedMap);
        return treeMap.values().toArray(new ImageMask[0]);
    }

    static ImagePlot makeMaskImagePlot(ActiveFitsReadGroup frGroup,
                                       float               initialZoomLevel,
                                       WebPlotRequest      request,
                                       RangeValues         stretch) throws FitsException {

         ImageMask[] maskDef= createMaskDefinition(request);
         return new ImagePlot(null, frGroup,initialZoomLevel, sortImageMaskArrayInIndexOrder(maskDef) , stretch);
    }

    public static Circle getRequestArea(WebPlotRequest request) {
        Circle retval = null;
        WorldPt wp= request.getWorldPt();

        if (wp==null && request.containsParam(WebPlotRequest.OBJECT_NAME)) {
            String objName= request.getObjectName();
            if (!StringUtils.isEmpty(objName)) {
                try {
                    wp= TargetNetwork.resolveToWorldPt(objName, request.getResolver());
                } catch (Exception e) {
                    wp= null;
                }
            }
        }

        float side = request.getSizeInDeg();
        if (wp != null) retval = new Circle(wp, side);

        return retval;
    }

    private static ImageMask[] createMaskDefinition(WebPlotRequest r) {
        List<String> maskColors= r.getMaskColors();
        Color[] cAry= new Color[maskColors.size()];
        List<ImageMask> masksList=  new ArrayList<ImageMask>();
        int bits= r.getMaskBits();
        int colorIdx= 0;
        for(String htmlColor : maskColors) {
            cAry[colorIdx++]= convertColorHtmlToJava(htmlColor);
        }
        colorIdx= 0;

        for(int j= 0; (j<31); j++) {
            if (((bits>>j) & 1) != 0) {
                Color c= (colorIdx<cAry.length) ? cAry[colorIdx] : Color.pink;
                colorIdx++;
                masksList.add(new ImageMask(j,c));
            }
        }
        return masksList.toArray(new ImageMask[masksList.size()]);
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
        Color c;
        if (isHexColor(color)) {
            int rgb[]=  toRGB(color);
            c= new Color(rgb[0],rgb[1],rgb[2]);
        }
        else if (color.startsWith("rgb")) {
            c = parseRGB(color);
        } else {
            if      (color.equals("black"))   c= Color.black;
            else if (color.equals("aqua"))    c= new Color(0,255,255);
            else if (color.equals("blue"))    c= Color.blue;
            else if (color.equals("cyan"))    c= Color.cyan;
            else if (color.equals("fuchsia")) c= new Color(255,0,255);
            else if (color.equals("gray"))    c= new Color(128,128,128);
            else if (color.equals("green"))   c= new Color(0,128,0);
            else if (color.equals("lime"))    c= Color.green;  // this is correct, lime is 0,255,0
            else if (color.equals("magenta")) c= Color.magenta;
            else if (color.equals("maroon"))  c= new Color(128,0,0);
            else if (color.equals("navy"))    c= new Color(0,0,128);
            else if (color.equals("olive"))   c= new Color(128,128,0);
            else if (color.equals("orange"))  c= Color.orange;
            else if (color.equals("pink"))    c= Color.pink;
            else if (color.equals("purple"))  c= new Color(128,0,128);
            else if (color.equals("red"))     c= Color.red;
            else if (color.equals("silver"))  c= new Color(192,192,192);
            else if (color.equals("teal"))    c= new Color(0,128,128);
            else if (color.equals("white"))   c= Color.white;
            else if (color.equals("yellow"))  c= Color.yellow;
            else {
                // lightGray or white is a better presentation for "unknown" color string. -TLau
                c= Color.lightGray;
                _log.debug("convertColorHtmlToJava(String color) does not understand " + color + ".  Color.lightGray is assigned.");
            }
        }
        return c;
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
                    case DOWNLOADING:
                        downloadMsg = statEntry.getMessage();
                        break;
                    case READING:
                        readingMsg = statEntry.getMessage();
                        break;
                    case CREATING:
                        creatingMsg = statEntry.getMessage();
                        break;
                    case GROUP:
                    case OTHER:
                    case SUCCESS:
                    default:
                        // ignore
                        break;
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

    public static class ProgressMessage {
        final String message;
        final boolean done;
        ProgressMessage(String message, boolean done) {
            this.message= message;
            this.done= done;
        }
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

