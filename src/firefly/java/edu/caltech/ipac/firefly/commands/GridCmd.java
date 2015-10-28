/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PrintableOverlay;
import edu.caltech.ipac.firefly.visualize.PrintableUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.LayerDrawer;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebGridLayer;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GridCmd extends BaseGroupVisCmd implements PrintableOverlay {

    public static final String CommandName= "grid";
    private boolean _lastState= true;
    private final String _onIcon= "grid.on.Icon";
    private final String _offIcon= "grid.off.Icon";
    private final Map<WebPlotView, WebGridLayer> _gridLayerMap= new HashMap<WebPlotView, WebGridLayer>(3);

    public GridCmd() {
        super(CommandName);
        changeMode(false);

        AllPlots.getInstance().addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                WebGridLayer gridLayer= _gridLayerMap.get(getPlotView());
                changeMode(gridLayer!=null ? gridLayer.isShowing() : false);
            }
        });
    }

    public boolean init() {
        return true;
    }

    protected void doExecute() {

        boolean forceShow= getForceShow();

        for (MiniPlotWidget mpw : getGroupActiveList()) {
            WebGridLayer gridLayer= getLayer(mpw);
            if (!gridLayer.isShowing() || forceShow ) {
                setGridEnable(mpw,true,true,true);
            }
            else {
                setGridEnable(mpw,false,true,false);
            }
        }
    }

    public void setGridEnable(MiniPlotWidget mpw, boolean enable, boolean includeLabels, boolean showAlert) {
        WebGridLayer gridLayer= getLayer(mpw);
        if (enable) {
            gridLayer.setUseLabels(includeLabels);
            gridLayer.setShowing(true, showAlert);
//            if (showAlert) AlertLayerPopup.setAlert(true);
        }
        else {
            gridLayer.setShowing(false);
//            AlertLayerPopup.setAlert(false);
        }

    }

    private boolean getForceShow() {
        if (getGroupActiveList().size()<2) return false;

        boolean allFalse= true;
        boolean allTrue= true;
        for (MiniPlotWidget mpw : getGroupActiveList()) {
            if (mpw.getCurrentPlot()!=null) {
                WebGridLayer gridLayer= getLayer(mpw);
                if (gridLayer.isShowing()) {
                    allFalse= false;
                }
                else {
                    allTrue= false;
                }
            }
        }
        return (!(allFalse || allTrue));
    }


    public WebGridLayer getLayer(MiniPlotWidget mpw) {
        WebPlotView pv= mpw.getPlotView();
        WebGridLayer gridLayer= _gridLayerMap.get(pv);
        if (gridLayer==null) {
            gridLayer= new WebGridLayer(pv,this);
            _gridLayerMap.put(pv,gridLayer);
        }
        return gridLayer;
    }

    private void changeMode(boolean showing) {
        if (showing!=_lastState) {
            setIconProperty(showing ? _onIcon : _offIcon);
            _lastState= showing;
        }
    }

    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("grid.on.Icon"))  {
                return new Image(ic.getGridOn());
            }
            else if (iStr.equals("grid.off.Icon"))  {
                return new Image(ic.getGridOff());
            }
            else if (iStr.equals("grid.Icon"))  {
                return new Image(ic.getGridOff());
            }
        }
        return null;
    }


//======================================================================
//------------------ Methods from PrintableOverlay ------------------
//======================================================================

    public void addPrintableLayer(List<StaticDrawInfo> drawInfoList,
                                  WebPlot plot,
                                  LayerDrawer drawer,
                                  WebLayerItem item) {
        StaticDrawInfo drawInfo= PrintableUtil.makeDrawInfo(plot, drawer, item);
        drawInfo.setDrawType(StaticDrawInfo.DrawType.GRID);
        WebGridLayer layer= getLayer(plot.getPlotView().getMiniPlotWidget());
        drawInfo.setGridType(layer.getCoordSystem());
        drawInfoList.add(drawInfo);
    }



}


