/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 8/15/14
 * Time: 11:13 AM
 */


import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DefaultRequestInfo {

    private final Map<Band,WebPlotRequest> requestMap;
    private boolean threeColor = false;


    public DefaultRequestInfo(Map<Band, WebPlotRequest> requestMap) {
        this.requestMap = requestMap;
        threeColor = true;
    }


    public DefaultRequestInfo(WebPlotRequest red, WebPlotRequest green, WebPlotRequest blue){
        this.requestMap= new HashMap<Band, WebPlotRequest>(5);
        requestMap.put(Band.RED,red);
        requestMap.put(Band.GREEN,green);
        requestMap.put(Band.BLUE,blue);
        threeColor = true;
    }



    public DefaultRequestInfo(Band band, WebPlotRequest request){
        this.requestMap= new HashMap<Band, WebPlotRequest>(1);
        requestMap.put(band,request);
        threeColor = (band!=Band.NO_BAND);
    }


    public DefaultRequestInfo(WebPlotRequest request){
        this.requestMap= new HashMap<Band, WebPlotRequest>(1);
        requestMap.put(Band.NO_BAND,request);
        threeColor = false;
    }

    public boolean isThreeColor() { return threeColor; }

    public WebPlotRequest getRequest(Band band) {
        return requestMap.get(band);
    }
}

