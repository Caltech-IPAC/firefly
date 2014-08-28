package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 8/25/14
 * Time: 10:01 AM
 */


import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DynamicPlotData {

    private Map<String, WebPlotRequest> reqMap= new HashMap<String, WebPlotRequest>(37);
    private Map<String, List<WebPlotRequest>> req3ColorMap= new HashMap<String, List<WebPlotRequest>>(13);
    private List<DynUpdateListener> listenerList= new ArrayList<DynUpdateListener>(3);


    public void deleteID(String id) {
        if (reqMap.containsKey(id)) reqMap.remove(id);
        if (req3ColorMap.containsKey(id)) req3ColorMap.remove(id);
    }


    public void setID(String id, WebPlotRequest req) {
        reqMap.put(id,req);
        fireUpdate();
    }

    public boolean hasPlotsDefined() {
       return reqMap.size()>0 || req3ColorMap.size()>0;
    }


    public void set3ColorID(String id, List<WebPlotRequest> reqList) {
        req3ColorMap.put(id,reqList);
        fireUpdate();
    }


    public void set3ColorIDBand(String id, Band band, WebPlotRequest request) {
        List<WebPlotRequest> reqList= req3ColorMap.get(id);
        if (reqList.size()==3) {
            reqList.set(band.getIdx(), request);
        }
        for(DynUpdateListener l : listenerList) {
            l.bandAdded(this,id);
        }
    }


    public void remove3ColorIDBand(String id, Band band) {
        List<WebPlotRequest> reqList= req3ColorMap.get(id);
        if (reqList.size()==3) {
            reqList.set(band.getIdx(), null);
        }
        for(DynUpdateListener l : listenerList) {
            l.bandRemoved(this,id);
        }
    }

    public boolean has3ColorImages() {
        return (req3ColorMap.size()>0);
    }

    public Map<String,WebPlotRequest> getImageRequest() {
       return reqMap;
    }

    public Map<String,List<WebPlotRequest>> getThreeColorPlotRequest() {
        return req3ColorMap;
    }

    public void addListener(DynUpdateListener l) {
        if (!listenerList.contains(l)) listenerList.add(l);
    }

    public void removeListener(DynUpdateListener l) {
        if (listenerList.contains(l)) listenerList.remove(l);
    }

    private void fireUpdate() {
        for(DynUpdateListener l : listenerList) {
            l.newImage(this);
        }
    }


    public static interface DynUpdateListener {
        public void bandAdded(DynamicPlotData dpd, String id);
        public void bandRemoved(DynamicPlotData dpd, String id);
        public void newImage(DynamicPlotData dpd);

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
