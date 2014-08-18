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
