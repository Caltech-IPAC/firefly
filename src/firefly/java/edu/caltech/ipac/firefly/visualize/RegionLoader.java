/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 5/26/15
 * Time: 1:02 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.LayerDrawer;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.RegionData;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.RegParseException;
import edu.caltech.ipac.util.dd.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Trey Roby
 */
public class RegionLoader {


    private static int cnt= 1;
    private static Map<String,RegionDrawing> regMap= new HashMap<String, RegionDrawing>(13);

    public static void loadRegFile(String fileOnServer,
                                   final String regionId,
                                   final AsyncCallback<String> cb,
                                   final String plotIds[]) {
        new VisTask().getDS9Region(fileOnServer,new AsyncCallback<RegionData>() {
            public void onFailure(Throwable caught) {
                PopupUtil.showInfo("failed");
            }

            public void onSuccess(RegionData result) {
                loadRegion(result.getTitle(),
                        result.getRegionTextData(),
                        result.getRegionParseErrors(),
                        regionId, plotIds);
                if (cb!=null) cb.onSuccess("ok");
            }
        });
    }

    public static void loadRegion(String title, String regText, String regErr, String regionId, String plotIds[]) {
        DrawingManager drawMan;
        if (regMap.containsKey(regionId)) {
            addToRegion(regText,regionId,plotIds);
        }
        else {
            List<String> retStrList= StringUtils.parseStringList(regText, StringUtils.STRING_SPLIT_TOKEN, 0);
            List<String> errStrList= StringUtils.parseStringList(regErr);
            List<Region> regList= new ArrayList<Region>(retStrList.size());
            for(String s : retStrList) {
                Region r= Region.parse(s);
                if (r!=null)  regList.add(r);
            }
            if (regList.size()>0) {
                RegionConnection rc= new RegionConnection(title, regList);
                String id= (regionId!=null) ? regionId : "RegionOverlay" + (cnt++);
                RegionPrintable printable= new RegionPrintable(regList);
                drawMan= new DrawingManager(id, rc, printable);
                drawMan.setCanDoRegion(false); // we actually can do region but we want to do it manually
                List<String> pIdList= (plotIds!=null && plotIds.length>0) ? Arrays.asList(plotIds) : null;
                regMap.put(id,new RegionDrawing(id,drawMan, rc,pIdList));
                for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
                    if (pIdList==null) {
                        drawMan.addPlotView(mpw.getPlotView());
                    }
                    else if (mpw.getPlotId()!=null && pIdList.contains(mpw.getPlotId())) {
                        drawMan.addPlotView(mpw.getPlotView());
                    }
                }
            }
            checkAndHandleError(regList,errStrList);
        }
    }

    private static void addToRegion(String regText, String regId, String plotIds[]) {
        List<String> retStrList= StringUtils.parseStringList(regText, StringUtils.STRING_SPLIT_TOKEN, 0);
        List<String> errStrList= new ArrayList<String>();
        List<Region> regList= new ArrayList<Region>(retStrList.size());
        if (!regMap.containsKey(regId)) return;
        int line=1;
        for(String s : retStrList) {
            try {
                regList.add(Region.parseWithErrorChecking(s));
            } catch (RegParseException e) {
                errStrList.add("Error parsing line " + line + ": " + e.getMessage());
            }
            line++;
        }
        if (regList.size()>0) {
            RegionDrawing regionDrawing= regMap.get(regId);
            RegionConnection rc= regionDrawing.regionConnection;
            rc.addRegions(regList);
            regionDrawing.drawMan.redraw();


            if (plotIds!=null && plotIds.length>0) {
                List<String> pIdList= Arrays.asList(plotIds);
                Set currPVSet= new HashSet<WebPlotView>(regionDrawing.drawMan.getPlotViewSet());
                for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
                    if (mpw.getPlotId()!=null && pIdList.contains(mpw.getPlotId())) {
                        if (!currPVSet.contains(mpw)) {
                            regionDrawing.drawMan.addPlotView(mpw.getPlotView());
                        }
                    }
                }
            }
        }
        checkAndHandleError(regList,errStrList);
    }

    public static void removeFromRegion(String regText, String regId) {
        List<String> retStrList= StringUtils.parseStringList(regText, StringUtils.STRING_SPLIT_TOKEN, 0);
        List<Region> regList= new ArrayList<Region>(retStrList.size());
        if (!regMap.containsKey(regId)) return;
        for(String s : retStrList) {
            try {
                regList.add(Region.parseWithErrorChecking(s));
            } catch (RegParseException e) {
                // ignore
            }
        }
        if (regList.size()>0) {
            RegionDrawing regionDrawing= regMap.get(regId);
            RegionConnection rc= regionDrawing.regionConnection;
            rc.removeRegions(regList);
            regionDrawing.drawMan.redraw();
        }
    }

    private static void checkAndHandleError(List<Region> regList, List<String> errStrList) {
        if (regList.size()==0 || errStrList.size()>0) {
            StringBuilder sb= new StringBuilder(20);
            if (regList.size()==0) {
                sb.append("<b>No regions loaded.</b>");
                if (errStrList.size()>0) sb.append("<br><br>");
            }
            if (errStrList.size()>0) {
                sb.append("<span style=\"text-decoration: underline;\">The following "+
                        (errStrList.size()>1 ? "errors" : "error") +
                        " occurred parsing the file</span><br><br>");
                sb.append(makeErrorList(errStrList));
            }
            PopupUtil.showError("Region", sb.toString());
        }
    }

    private static String makeErrorList(List<String> errStrList) {
        StringBuilder sb= new StringBuilder(200);
        for(String s : errStrList) {
            sb.append(s).append("<br>");
        }
        if (errStrList.size()>0) {
           sb.delete(sb.length()-4, sb.length());
        }
        return sb.toString();
    }

    public static void removeRegion(String id, String plotIdAry[]) {
        RegionDrawing rd= regMap.get(id);
        if (rd!=null) {
            List plotIdList= Collections.emptyList();
            boolean removeAll=  plotIdAry==null || plotIdAry.length==0;
            if (!removeAll) plotIdList= Arrays.asList(plotIdAry);
            Set<WebPlotView> pvSet= new HashSet<WebPlotView>(rd.drawMan.getPlotViewSet());
            if (!removeAll && plotIdAry.length==pvSet.size()) {
                removeAll= true;
                for(WebPlotView pv : pvSet) {
                    if (!plotIdList.contains(pv.getMiniPlotWidget().getPlotId())){
                        removeAll= true;
                    }
                }
            }
            if (removeAll){
                rd.freeResources();
                regMap.remove(id);
            }
            else {
                for(WebPlotView pv : pvSet) {
                    String plotId= pv.getMiniPlotWidget().getPlotId();
                    if (!StringUtils.isEmpty(plotId) && plotIdList.contains(plotId)) {
                        rd.drawMan.removePlotView(pv);
                    }
                }
                if (rd.drawMan.getPlotViewSet().size()==0) {
                    rd.freeResources();
                    regMap.remove(id);
                }
            }
        }
    }

    private static class RegionDrawing {
        public final String id;
        public final List<String> plotIDList;
        public DrawingManager drawMan;
        public RegionConnection regionConnection;

        private RegionDrawing(String id, DrawingManager drawMan, RegionConnection regionConnection, List<String> plotIDList) {
            this.id = id;
            this.drawMan = drawMan;
            this.regionConnection= regionConnection;
            this.plotIDList= plotIDList;
            if (!WebLayerItem.hasUICreator(id)) {
                WebLayerItem.addUICreator(id, new RegionUICreator());
            }
        }

        public void freeResources() {
            drawMan.dispose();
            drawMan = null;
            regionConnection= null;
        }

    }

    private static class RegionUICreator extends WebLayerItem.UICreator {

        private RegionUICreator() { super(false,true); }

        public void delete(WebLayerItem item) { removeRegion(item.getID(),null); }
    }

    private static class RegionPrintable implements PrintableOverlay {

        List<Region> regList;

        private RegionPrintable(List<Region> regList) {
            this.regList = regList;
        }

        public void addPrintableLayer(List<StaticDrawInfo> drawInfoList, WebPlot plot, LayerDrawer drawer, WebLayerItem item) {
            StaticDrawInfo info= new StaticDrawInfo();
            info.setDrawType(StaticDrawInfo.DrawType.REGION);
            for(Region r : regList) {
                if (r.getOptions().isInclude()) {
                    info.addRegion(r);
                }
            }
            drawInfoList.add(info);
        }
    }
}
