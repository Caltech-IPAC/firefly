/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Sep 11, 2009
 * Time: 11:16:02 AM
 */


/**
 * @author Trey Roby
 */
public class WebPlotInitializer implements Serializable, DataEntry {

    private final static String SPLIT_TOKEN= "--WebPlotInitializer--";

    private CoordinateSys _imageCoordSys;
    private String    _projectionSerialized;
    private int           _dataWidth;
    private int           _dataHeight;
    private int           _imageScaleFactor;
    private PlotImages    _initImages;
    private PlotState     _plotState;
    private WebFitsData   _fitsData[];
    private String        _desc;
    private String        _dataDesc;



    private WebPlotInitializer() {}


    public WebPlotInitializer(PlotState plotState,
                             PlotImages images,
                             CoordinateSys imageCoordSys,
                             Projection projection,
                             int dataWidth,
                             int dataHeight,
                             int imageScaleFactor,
                             WebFitsData  fitsData[],
                             String desc,
                             String dataDesc) {

        _plotState= plotState;
        _initImages= images;
        _imageCoordSys= imageCoordSys;
        _projectionSerialized= ProjectionSerializer.serializeProjection(projection);
        _dataWidth= dataWidth;
        _dataHeight= dataHeight;
        _imageScaleFactor= imageScaleFactor;
        _fitsData= fitsData;
        _desc= desc;
        _dataDesc= dataDesc;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    
    public PlotState getPlotState() { return _plotState; }
    public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }
    public PlotImages getInitImages() { return _initImages; }

    public Projection getProjection() {
//        return _projection;
        return ProjectionSerializer.deserializeProjection(_projectionSerialized);
    }

    public int getDataWidth() { return _dataWidth; }
    public int getDataHeight() { return _dataHeight; }
    public int getImageScaleFactor() { return _imageScaleFactor; }
    public WebFitsData[] getFitsData()  { return _fitsData; }

    public String getPlotDesc() { return _desc; }
    public void setPlotDesc(String d) { _desc= d; }
    public String getDataDesc() { return _dataDesc; }

    public String toString() {
        StringBuilder sb= new StringBuilder(300);
        sb.append(_imageCoordSys).append(SPLIT_TOKEN);
        sb.append(_projectionSerialized).append(SPLIT_TOKEN);
        sb.append(_dataWidth).append(SPLIT_TOKEN);
        sb.append(_dataHeight).append(SPLIT_TOKEN);
        sb.append(_imageScaleFactor).append(SPLIT_TOKEN);
        sb.append(_initImages).append(SPLIT_TOKEN);
        sb.append(_plotState).append(SPLIT_TOKEN);
        sb.append(_desc).append(SPLIT_TOKEN);
        sb.append(_dataDesc).append(SPLIT_TOKEN);
        for(int i=0; (i<_fitsData.length);i++) {
            sb.append(_fitsData[i]);
            if (i<_fitsData.length-1) {
                sb.append(SPLIT_TOKEN);
            }

        }
        return sb.toString();
    }

    public static WebPlotInitializer parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,13);
        WebPlotInitializer retval= null;
        if (sAry.length>=10 && sAry.length<=12) {
            try {
                int i= 0;
                CoordinateSys imageCoordSys= CoordinateSys.parse(sAry[i++]);
                Projection    projection= ProjectionSerializer.deserializeProjection(sAry[i++]);
                int           dataWidth= Integer.parseInt(sAry[i++]);
                int           dataHeight= Integer.parseInt(sAry[i++]);
                int           imageScaleFactor= Integer.parseInt(sAry[i++]);
                PlotImages    initImages= PlotImages.parse(sAry[i++]);
                PlotState     plotState= PlotState.parse(sAry[i++]);
                String        desc= getString(sAry[i++]);
                String        dataDesc= getString(sAry[i++]);
                List<WebFitsData> fdList= new ArrayList<WebFitsData>(3);
                while (i<sAry.length) {
                   fdList.add(WebFitsData.parse(sAry[i++]));
                }
                WebFitsData fitsData[]= fdList.toArray(new WebFitsData[fdList.size()]);
                retval= new WebPlotInitializer(plotState,initImages,imageCoordSys,projection,dataWidth,dataHeight,
                                               imageScaleFactor,fitsData,desc,dataDesc);

            } catch (NumberFormatException e) {
                retval= null;
            }
        }
        return retval;

    }

    private static String getString(String s) { return s.equals("null") ? null : s; }
}

