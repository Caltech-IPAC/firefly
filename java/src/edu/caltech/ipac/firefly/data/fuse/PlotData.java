package edu.caltech.ipac.firefly.data.fuse;
/**
 * User: roby
 * Date: 8/25/14
 * Time: 10:01 AM
 */


import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class PlotData {
    public static enum GroupMode {TABLE_ROW_ONLY, WHOLE_GROUP}

    private Map<String, WebPlotRequest> dynReqMap= new HashMap<String, WebPlotRequest>(37);
    private Map<String, List<WebPlotRequest>> dynReq3ColorMap= new HashMap<String, List<WebPlotRequest>>(13);
    private Map<String, List<String>> req3IDMap= new LinkedHashMap<String, List<String>>(13);
    private Map<String, String> titleToIDMap= new HashMap<String, String>(11);
    private Map<String, Boolean> canDeleteMap= new HashMap<String, Boolean>(22);
    private List<DynUpdateListener> listenerList= new ArrayList<DynUpdateListener>(3);
    private final Resolver resolver;
    private SelectedRowData selRowData = null;
    private final boolean threeColorDataOP;
    private final boolean canCreateAnyNewPlot;
    private final boolean canDoGroupingChanges;

    public PlotData(Resolver resolver, boolean threeColorDataOP, boolean canCreateAnyNewPlot, boolean canDoGroupingChanges) {
        this.resolver= resolver;
        this.threeColorDataOP = threeColorDataOP;
        this.canCreateAnyNewPlot = canCreateAnyNewPlot;
        this.canDoGroupingChanges= canDoGroupingChanges;
    }

    public boolean is3ColorOptional() { return threeColorDataOP; }
    public boolean getCanDoGroupingChanges() { return canDoGroupingChanges; }

    public void setSelectedRowData(SelectedRowData currentData) {
        this.selRowData = currentData;
    }

    public SelectedRowData getSelectedRowData() {
        return selRowData;
    }

    public boolean getCanCreateAnyNewPlot() {
        return canCreateAnyNewPlot;
    }

    public void deleteID(String id) {
        if (dynReqMap.containsKey(id)) dynReqMap.remove(id);
        if (dynReq3ColorMap.containsKey(id)) dynReq3ColorMap.remove(id);
    }

    public Resolver getResolver() {
       return resolver;
    }

    public void setCanDelete(String id, boolean canDelete) {
        canDeleteMap.put(id,canDelete);
    }

    public boolean getCanDelete(String id) {
        boolean retval= false;
        if (canDeleteMap.containsKey(id)) {
           retval= canDeleteMap.get(id) ;
        }
        return retval;
    }

    public void setID(String id, WebPlotRequest req) {
        dynReqMap.put(id,req);
        setCanDelete(id,true);
        req.setHasNewPlotContainer(true);
        fireUpdate();
    }

    public boolean hasPlotsDefined() {
       return getImageRequest(GroupMode.TABLE_ROW_ONLY).size()>0 ||
              get3ColorImageRequest().size()>0;
    }

    public boolean has3ColorImages() {
        return hasOptional3ColorImages() || hasDynamic3ColorImages();
    }

    public boolean hasOptional3ColorImages() {
        return req3IDMap.size()>0;
    }

    public boolean hasDynamic3ColorImages() {
        return dynReq3ColorMap.size()>0;
    }

    public void set3ColorIDRequest(String id, List<WebPlotRequest> reqList) {
        dynReq3ColorMap.put(id,reqList);
        for(WebPlotRequest r : reqList) {
            if (r!=null) {
                r.setHasNewPlotContainer(true);
                r.setAllowImageSelection(true);
                if (titleToIDMap.containsKey(id)) {
                    r.setTitle(titleToIDMap.get(id));
                }
            }
        }
        fireUpdate();
    }

    public void set3ColorIDOfIDs(String id, List<String> idList) {
        req3IDMap.put(id,idList);
    }

    public void setTitle(String id, String title) {
        titleToIDMap.put(id, title);
        titleToIDMap.put(title, id);
    }

    public String getTitleFromID(String id) { return titleToIDMap.get(id); }
    public String getIDFromTitle(String title) { return titleToIDMap.get(title); }

    public List<String> get3ColorIDOfIDs(String id) {
        return req3IDMap.get(id);
    }


    public void set3ColorIDBand(String id, Band band, WebPlotRequest request) {
        List<WebPlotRequest> reqList= dynReq3ColorMap.get(id);
        request.setHasNewPlotContainer(true);
        if (reqList!=null) {
            if (reqList.size()==3) {
                reqList.set(band.getIdx(), request);
            }
            for(DynUpdateListener l : listenerList) {
                l.bandAdded(this,id);
            }
        }
        else {
            List<WebPlotRequest> rList= Arrays.asList(null,null,null);
            rList.set(band.getIdx(), request);
            set3ColorIDRequest(id,rList);
        }
    }


    public void remove3ColorIDBand(String id, Band band) {
        List<WebPlotRequest> reqList= dynReq3ColorMap.get(id);
        if (reqList.size()==3) {
            reqList.set(band.getIdx(), null);
        }
        for(DynUpdateListener l : listenerList) {
            l.bandRemoved(this,id);
        }
    }


    public Map<String,WebPlotRequest> getImageRequest(GroupMode mode) {
        HashMap<String,WebPlotRequest> retMap= new LinkedHashMap<String, WebPlotRequest>(dynReqMap);
        if (resolver!=null)  {
            List<String> idList= resolver.getIDsForMode(mode,selRowData);
            for(String id : idList) {
                retMap.put(id,resolver.getRequestForID(id,selRowData, false));
            }
        }
       return retMap;
    }

    public Map<String,List<WebPlotRequest>> get3ColorImageRequest() {
        Map<String,List<WebPlotRequest>> retMap= new LinkedHashMap<String, List<WebPlotRequest>>(dynReq3ColorMap);
        if (resolver!=null) {
            List<String> id3ColorList= resolver.get3ColorIDsForMode(selRowData);
            for(String id3 : id3ColorList) {
                if (req3IDMap.containsKey(id3)) {
                    List<String> bandIDList= req3IDMap.get(id3);
                    List<WebPlotRequest> reqList= Arrays.asList(null, null, null);
                    int len= Math.min(reqList.size(), bandIDList.size());
                    for(int i=0; i<len; i++) {
                        String reqID= bandIDList.get(i);
                        WebPlotRequest r= reqID!=null ?resolver.getRequestForID(reqID, selRowData, true) : null;
                        reqList.set(i, r);
                        if (r!=null && titleToIDMap.containsKey(id3)) {
                           r.setTitle(titleToIDMap.get(id3));
                           r.setAllowImageSelection(true);
                        }
                    }
                    retMap.put(id3,reqList);
                    if (dynReq3ColorMap.containsKey(id3)) {
                        List<WebPlotRequest> dynReplaceList= dynReq3ColorMap.get(id3);
                        for(int i=0; (i<3); i++) {
                            WebPlotRequest r= dynReplaceList.get(i);
                            if (r!=null) {
                                reqList.set(i,r);
                            }
                        }
                    }
                }
            }
        }
        return retMap;
    }

    public void addListener(DynUpdateListener l) {
        if (!listenerList.contains(l)) listenerList.add(l);
    }

    public void removeListener(DynUpdateListener l) {
        if (listenerList.contains(l)) listenerList.remove(l);
    }

    public void fireUpdate() {
        for(DynUpdateListener l : listenerList) {
            l.newImage(this);
        }
    }



    public static interface DynUpdateListener {
        public void bandAdded(PlotData dpd, String id);
        public void bandRemoved(PlotData dpd, String id);
        public void newImage(PlotData dpd);

    }

    public static interface Resolver {
        public abstract WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor);
        public abstract List<String> getIDsForMode(GroupMode mode, SelectedRowData selData);
        public abstract List<String> get3ColorIDsForMode(SelectedRowData selData);
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
