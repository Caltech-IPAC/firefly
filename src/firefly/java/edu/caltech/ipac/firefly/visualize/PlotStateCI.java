/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 10/13/15
 * Time: 3:46 PM
 */


import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Map;


/**
 * @author Trey Roby
 */
@JsType
public interface PlotStateCI {

    BandCI[] getBandsCI();

    boolean isBandUsed(Band band);

    String getContextString();

    int getColorTableId();

    boolean isThreeColor();

    int getThumbnailSize();

    float getZoomLevel();

//    RotateType getRotateType();

    boolean isFlippedY();

    double getRotationAngle();

    CoordinateSys getRotateNorthType();


    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param band the band to get the request for
     * @return the WebPlotRequest
     */
    WebPlotRequest getWebPlotRequest(Band band);



    boolean isBandVisible(Band band);


    boolean isMultiImageFile(Band band);
    boolean isTileCompress(Band band);

    int getCubeCnt(Band band);

    int getCubePlaneNumber(Band band);


    RangeValues getRangeValues(Band band);

    FileAndHeaderInfo getFileAndHeaderInfo(Band band);

    ClientFitsHeader getHeader(Band band);


    String getWorkingFitsFileStr(Band band);

    String getOriginalFitsFileStr(Band band);

    String getUploadFileName(Band band);

    int getImageIdx(Band band);

    int getOriginalImageIdx(Band band);

    boolean hasOperation(PlotState.Operation op);


    Map<String,String> originKeyValues();

}
