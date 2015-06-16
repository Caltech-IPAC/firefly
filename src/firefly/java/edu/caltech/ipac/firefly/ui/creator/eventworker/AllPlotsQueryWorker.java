/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.PrintableUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 7, 2012
 * Time: 6:18:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class AllPlotsQueryWorker implements EventWorker, WebEventListener {
    public static final String PLOT_WIDGET_GROUP="PlotWidgetGroup";
    public static final String QUERY_ITEMS="QueryItems";
    private EventHub eventHub;
    private List<String> querySources;
    private List<Name> events = new ArrayList<Name>();
    private String type;
    private String desc;
    private String id= DEFAULT_ID;
    private Map<String, String> params;
    private boolean enabled= true;
    private WebEvent _lastEvent= null;
    private AllPlots ap= AllPlots.getInstance();
    private LinkedHashMap<String,String> layerMap = null;
    public AllPlotsQueryWorker() {
    }

    public void insertCommonArgs(Map<String, String> params) {
        setParams(params);
    }


    public void eventNotify(WebEvent ev) {
        Object source= ev.getSource();
        String stateStr = "";
        String drawInfoListStr= "";
        ArrayList<String> queryItems = new ArrayList<String>();
        if (source instanceof DownloadRequest && ev.getName().equals(Name.ON_PACKAGE_SUBMIT)) {
            DownloadRequest request= (DownloadRequest) source;
            PlotWidgetGroup group;
            WebPlot currentPlot;
            if (params.containsKey(PLOT_WIDGET_GROUP))
                group = ap.getGroupByName(params.get(PLOT_WIDGET_GROUP));
            else
                group= ap.getActiveGroup(); // not safe approach.

            if (params.containsKey(QUERY_ITEMS)) {
                if (params.get(QUERY_ITEMS).contains(",")) {
                    queryItems.addAll(Arrays.asList(params.get(QUERY_ITEMS).split(",")));
                } else {
                    queryItems.add(params.get(QUERY_ITEMS));
                }
            }

            for (MiniPlotWidget mpw: group.getAllActive()) {
                if (mpw.getTitle().length()==0) break;
                currentPlot = mpw.getCurrentPlot();
                if (queryItems.contains("PlotStates")) {
                    stateStr += getPlotStates(currentPlot);
                    stateStr +="&&";
                }
                if (queryItems.contains("DrawInfoList")) {
                    drawInfoListStr +=getDrawInfoListString(currentPlot);
                    drawInfoListStr +="&&";
                }
                collectLayerInfo(currentPlot);
            }
            if (queryItems.contains("PlotStates")) request.setParam("PlotStates",stateStr);
            if (queryItems.contains("DrawInfoList")) request.setParam("DrawInfoList",drawInfoListStr);
            if (queryItems.contains("LayerInfo") && layerMap !=null) {
                String layerInfo = serializeLayerInfo();
                request.setParam("LayerInfo", layerInfo);
            }
        }
    }

    protected void setParams(Map<String, String> params) {
        this.params = params;
    }

    private String getPlotStates(WebPlot plot) {
        String retval="";

        PlotState state;

        if (plot!=null) {
            state= plot.getPlotState();
            if (state != null) {
                retval = state.serialize();
            }
        } else {
            retval=Constants.SPLIT_TOKEN;
        }


        return retval;
    }

    private String getDrawInfoListString(WebPlot plot) {
        StringBuilder sb= new StringBuilder(350);

        if (plot!=null) {
            for (StaticDrawInfo info: PrintableUtil.getDrawInfoList(plot)) {
                if (info.getDrawType().equals(StaticDrawInfo.DrawType.SYMBOL) && !info.getLabel().contains("CatalogID")) {
                    info.setList(new ArrayList<WorldPt>(0));
                }
                sb.append(info.serialize()).append(Constants.SPLIT_TOKEN);
            }
        } else {
            sb.append(Constants.SPLIT_TOKEN);
        }
        return sb.toString();       
    }

    private void collectLayerInfo(WebPlot plot) {
        String title, defaultColor;
        if (plot!=null) {
            for (WebLayerItem layer: plot.getPlotView().getUserDrawerLayerSet()) {
                if (layer.getDrawer().isVisible()) {
                    defaultColor = layer.getColor();
                    title = layer.getTitle();
                    if (layerMap ==null) layerMap = new LinkedHashMap<String,String>();
                    if (!layerMap.keySet().contains(title)) {
                        layerMap.put(title, defaultColor);
                    }
                }
            }
        }
    }

    private String serializeLayerInfo() {
        StringBuilder sb= new StringBuilder(350);
        if (layerMap !=null && layerMap.size()>0) {
            for (String key: layerMap.keySet()) {
                sb.append(key);
                sb.append("==");
                sb.append(layerMap.get(key));
                sb.append(Constants.SPLIT_TOKEN);
            }
        }
        return sb.toString();
    }

    private String getUserDrawerLayerSet(WebPlot plot) {
        StringBuilder sb= new StringBuilder(350);

        if (plot!=null) {
            for (WebLayerItem layer: plot.getPlotView().getUserDrawerLayerSet()) {
                if (layer.getDrawer().isVisible()) {
                    sb.append(layer.getID().trim().replaceAll(" ","_"));
                    sb.append(Constants.SPLIT_TOKEN);
                }
            }
        } else {
            sb.append(Constants.SPLIT_TOKEN);
        }
        return sb.toString();
    }

    // --- EventWorker implementations ---

    public void bind(EventHub hub) {
        eventHub = hub;
        WebEventManager.getAppEvManager().addListener(Name.ON_PACKAGE_SUBMIT, this);
        hub.bind(this);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getID() { return id; }

    public int getDelayTime() { return 0; }

    public void setDelayTime(int delayTime) {}

    public void setID(String id) { this.id= id; }

    public List<String> getQuerySources() { return querySources; }

    
}

