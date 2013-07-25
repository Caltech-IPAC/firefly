package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 12/19/12
 * Time: 11:46 AM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class SrvParam {

    private final Map<String, String[]> paramMap;

    public SrvParam(Map<String, String[]> paramMap) {
        this.paramMap=paramMap;
    }

    public boolean contains(String key) { return paramMap.containsKey(key); }

    /**
     * Look for the ServerParams.STATE key and parse the value into a PlotState Object
     * Throw an exception if it is not found
     * @return a PlotState object
     */
    public PlotState getState() {
        PlotState state= PlotState.parse(getRequired(ServerParams.STATE));
        if (state == null) {
            throw new IllegalArgumentException("parameter in wrong format: state, (see PlotState.serialize()");
        }
        return state;
    }

    /**
     * Look for the ServerParams.ID key and return the string value
     * Throw an exception if it is not found
     * @return in ID value
     */
    public String getID() { return getRequired(ServerParams.ID); }

    /**
     * Look for the ServerParams.ID keys and a list of values
     * Throw an exception if at least one is not found
     * @return in ID values
     */
    public List<String> getIDList() { return getRequiredList(ServerParams.ID); }


    public String getRequired(String key) {
        String ary[]= paramMap.get(key);
        if (ary != null && ary.length>0) {
            return ary[0];
        }
        else {
            throw new IllegalArgumentException("missing parameter: "+ key);
        }
    }

    public List<String> getRequiredList(String key) {
        String ary[]= paramMap.get(key);
        if (ary != null && ary.length>0) {
            return Arrays.asList(ary);
        }
        else {
            throw new IllegalArgumentException("missing parameter: "+ key);
        }
    }

    public List<String> getOptionalList(String key) {
        String ary[]= paramMap.get(key);
        if (ary != null && ary.length>0) {
            return Arrays.asList(ary);
        }
        else {
            return Collections.emptyList();
        }
    }

    public String getOptional(String key) {
        String ary[]= paramMap.get(key);
        if (ary != null && ary.length>0) {
            return ary[0];
        }
        else {
            return null;
        }
    }

    public float getOptionalFloat(String key, float defValue) {
        String ary[]= paramMap.get(key);
        if (ary != null && ary.length>0) {
            try {
                return Float.parseFloat(ary[0]);
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        else {
            return defValue;
        }
    }

    public int getRequiredInt(String key) {
        String v= getRequired(key);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as int: parameter: "+ key + ", value: "+v, e);
        }
    }

    public float getRequiredFloat(String key) {
        String v= getRequired(key);
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as float: parameter: "+ key + ", value: "+v, e);
        }
    }

    public double getRequiredDouble(String key) {
        String v= getRequired(key);
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as double: parameter: "+ key + ", value: "+v, e);
        }
    }

    public ImagePt getRequiredImagePt(String key) {
        String v= getRequired(key);
        try {
            return ImagePt.parse(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "parameter could not be parsed as ImagePt: parameter: "+ key + ", value: "+v, e);
        }
    }

    public boolean getOptionalBoolean(String key, boolean defval) {
        String v= getOptional(key);
        return v==null ? defval : Boolean.valueOf(v);
    }

    public boolean getRequiredBoolean(String key) {
        return Boolean.valueOf(getRequired(key));
    }

    public WebPlotRequest getOptionalWebPlotRequest(String key) {
        return WebPlotRequest.parse(getOptional(key));
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
