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
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.RegionData;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.RegParseException;
import edu.caltech.ipac.util.dd.Region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class RegionLoader {


    private static int cnt= 1;
    private static Map<String,RegionDrawing> regMap= new HashMap<String, RegionDrawing>(13);

    public static void loadRegFile(String fileOnServer, final String regionId, final AsyncCallback<String> cb) {
        new VisTask().getDS9Region(fileOnServer,new AsyncCallback<RegionData>() {
            public void onFailure(Throwable caught) {
                PopupUtil.showInfo("failed");
            }

            public void onSuccess(RegionData result) {
                loadRegion(result.getTitle(),
                        result.getRegionTextData(),
                        result.getRegionParseErrors(),
                        regionId);
                if (cb!=null) cb.onSuccess("ok");
            }
        });
    }

    public static void loadRegion(String title, String regText, String regErr, String regionId) {
        DrawingManager drawMan;
        if (regMap.containsKey(regionId)) {
            addToRegion(regText,regionId);
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
                regMap.put(id,new RegionDrawing(id,drawMan, rc));
                for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
                    drawMan.addPlotView(mpw.getPlotView());
                }
            }
            checkAndHandleError(regList,errStrList);
        }
    }

    private static void addToRegion(String regText, String regId) {
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

    public static void removeRegion(String id) {
        RegionDrawing rd= regMap.get(id);
        if (rd!=null) rd.freeResources();
        regMap.remove(id);
    }

    private static class RegionDrawing {
        private final String id;
        private DrawingManager drawMan;
        private RegionConnection regionConnection;

        private RegionDrawing(String id, DrawingManager drawMan, RegionConnection regionConnection) {
            this.id = id;
            this.drawMan = drawMan;
            this.regionConnection= regionConnection;
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

        public void delete(WebLayerItem item) { removeRegion(item.getID()); }
    }

    private static class RegionPrintable implements PrintableOverlay {

        List<Region> regList;

        private RegionPrintable(List<Region> regList) {
            this.regList = regList;
        }

        public void addPrintableLayer(List<StaticDrawInfo> drawInfoList, WebPlot plot, Drawer drawer, WebLayerItem item) {
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
