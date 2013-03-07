package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 5/9/12
 * Time: 4:26 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PrintableUtil {

    public static void createPrintableImage(WebPlot plot, final AsyncCallback<String> async) {
        ArrayList<StaticDrawInfo> drawInfoList= getDrawInfoList(plot);
        PlotService.App.getInstance().getImagePng(plot.getPlotState(), drawInfoList, new AsyncCallback<WebPlotResult>() {
            public void onFailure(Throwable caught) { async.onFailure(caught); }

            public void onSuccess(WebPlotResult result) {
                if (result.containsKey(WebPlotResult.IMAGE_FILE_NAME)) {
                    DataEntry.Str fname= (DataEntry.Str)result.getResult(WebPlotResult.IMAGE_FILE_NAME);
                    async.onSuccess(fname.getString());
                }
                else {
                    async.onFailure(new Exception("png file could not be created"));
                }
            }
        });
    }

    public static void createRegion(WebPlot plot, final AsyncCallback<String> async) {
        String regData;
        List<String> strList= new ArrayList<String>(100);
        String s;
        for(StaticDrawInfo drawInfo : getDrawInfoList(plot)) {
            if (drawInfo.getDrawType()== StaticDrawInfo.DrawType.REGION) {
                for(Region r : drawInfo.getRegionList()) {
                    s= r.serialize();
                    if (s!=null) strList.add(s);
                }
            }
        }
        regData= StringUtils.combineStringList(strList);

        PlotService.App.getInstance().saveDS9RegionFile(regData, new AsyncCallback<WebPlotResult>() {
            public void onFailure(Throwable caught) { async.onFailure(caught); }

            public void onSuccess(WebPlotResult result) {
                if (result.containsKey(WebPlotResult.REGION_FILE_NAME)) {
                    DataEntry.Str fname= (DataEntry.Str)result.getResult(WebPlotResult.REGION_FILE_NAME);
                    async.onSuccess(fname.getString());
                }
                else {
                    async.onFailure(new Exception("region file could not be created"));
                }
            }
        });
    }

    public static ArrayList<StaticDrawInfo> getDrawInfoList(WebPlot plot) {
        ArrayList<StaticDrawInfo> drawInfoList= null;
        if (plot.getPlotView()!=null) {
            Collection<WebLayerItem> itemList= plot.getPlotView().getUserDrawerLayerSet();
            drawInfoList= new ArrayList<StaticDrawInfo>(itemList.size());
            Drawer drawer;
            for(WebLayerItem item : itemList) {
                drawer= item.getDrawer();
                if (drawer.isVisible()) {
                    if (item.getPrintableOverlay()!=null) { // the item knows how to make a hardcopy overlay
                        item.getPrintableOverlay().addPrintableLayer(drawInfoList,plot,drawer,item);
                    }
                    else if (item.isCanDoRegion()) {
                        StaticDrawInfo drawInfo= makeDrawInfo(plot,drawer,item);
                        drawInfo.setDrawType(StaticDrawInfo.DrawType.REGION);
                        List<Region> regList= item.asRegionList();
                        drawInfo.addAllRegions(regList);
                        if (regList.size()>0) drawInfoList.add(drawInfo);
                    }
                    else { // fallback method: will work for catalog, artifact, & similar type overlays
                        StaticDrawInfo drawInfo= makeDrawInfo(plot,drawer,item);
                        for(DrawObj obj : drawer.getData()) { // any drawing layer that uses symbols, like catalogs or artifacts
                            drawInfo.setDrawType(StaticDrawInfo.DrawType.SYMBOL);
                            if (obj instanceof PointDataObj && obj.getCenterPt() instanceof WorldPt) {
                                drawInfo.setSymbol(((PointDataObj) obj).getSymbol());
                                drawInfo.add((WorldPt) obj.getCenterPt());
                            }
                        }
                        if (drawInfo.getList().size()>0)  drawInfoList.add(drawInfo);
                    }
                }
            }
        }
        return  drawInfoList;
    }

    public static StaticDrawInfo makeDrawInfo(WebPlot plot, Drawer drawer, WebLayerItem item) {
        StaticDrawInfo drawInfo= new StaticDrawInfo();
        AutoColor autoColor= new AutoColor(plot,drawer);
        drawInfo.setColor(autoColor.getColor(drawer.getDefaultColor()));
        drawInfo.setLabel(item.getID());
        return drawInfo;
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
