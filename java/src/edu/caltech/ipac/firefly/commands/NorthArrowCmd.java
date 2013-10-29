package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.AutoColor;
import edu.caltech.ipac.firefly.visualize.draw.DirectionArrowDataObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;


public class NorthArrowCmd extends    BaseGroupVisCmd
                           implements WebEventListener/*, PrintableOverlay*/ {

    private static final int ARROW_LENTH = 60;
    private boolean _arrowShowing = true;
    public static final String CommandName= "NorthArrow";
    private final String _onIcon= "northArrow.on.Icon";
    private final String _offIcon= "northArrow.off.Icon";
    private DrawingManager drawingManager;
//    private final List<WebEventListener> _clearList= new ArrayList<WebEventListener>(34);

    public NorthArrowCmd() {
        super(CommandName);
        changeMode(false);
    }

    public boolean init() {
        drawingManager= new DrawingManager(CommandName,null);
        drawingManager.setDefaultColor(AutoColor.DRAW_1);
        drawingManager.setDataConnection(new NorthArrowData());
        AllPlots.getInstance().addListener(this);
        return true;
    }

    protected void doExecute() {

        changeMode(!_arrowShowing);
    }


    private void changeMode(boolean showing) {
        if (showing!= _arrowShowing) {
            setIconProperty(showing ? _onIcon : _offIcon);
            _arrowShowing = showing;
            if (drawingManager!=null) {
                if (showing) {
                    for(MiniPlotWidget mpw : AllPlots.getInstance().getActiveList()) {
                        addMPW(mpw);
                    }
                }
                else {
                    drawingManager.clear();
                }
            }
        }
    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic = VisIconCreator.Creator.getInstance();
        String iStr = this.getIconProperty();
        if (iStr != null) {
            if (iStr.equals(_onIcon)) {
                return new Image(ic.getCompassOn());
            } else if (iStr.equals(_offIcon)) {
                return new Image(ic.getCompass());
            } else if (iStr.equals(CommandName + ".Icon")) {
                return new Image(ic.getCompass());
            }
        }
        return null;
    }

//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        Name name = ev.getName();
        if (name.equals(Name.FITS_VIEWER_ADDED)) {
            MiniPlotWidget mpw= (MiniPlotWidget)ev.getData();
//            drawingManager.addPlotView(mpw.getPlotView());
            if (_arrowShowing) addMPW(mpw);
        } else if (name.equals(Name.ALL_FITS_VIEWERS_TEARDOWN)) {
            changeMode(false);
        } else if (name.equals(Name.ALL_FITS_VIEWERS_TEARDOWN)) {
        }
    }

    private void addMPW(final MiniPlotWidget mpw) {
        drawingManager.addPlotView(mpw.getPlotView());
        WebEventListener l= new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                drawingManager.redraw(mpw.getPlotView());
            }
        };
        mpw.getPlotView().addListener(Name.REPLOT, l);
//        _clearList.add(l);
    }

//======================================================================
//------------------ Methods from PrintableOverlay ------------------
//======================================================================

    /*
    public void addPrintableLayer(List<StaticDrawInfo> drawInfoList,
                                  WebPlot plot,
                                  Drawer drawer,
                                  WebLayerItem item) {
        StaticDrawInfo drawInfo= PrintableUtil.makeDrawInfo(plot, drawer, item);
        List<DrawObj> data= drawer.getData();
        if (data.size()==2&& data.get(0) instanceof DirectionArrowDataObj) {
            DirectionArrowDataObj dirObj= (DirectionArrowDataObj)data.get(0);
            drawInfo.setDrawType(StaticDrawInfo.DrawType.NORTH_ARROW);
            drawInfo.setDim1((float)dirObj.getCenterPt().getX());
            drawInfo.setDim2((float)dirObj.getCenterPt().getY());
            drawInfoList.add(drawInfo);
        }
    }
    */

//======================================================================
//------------------ Inner Class ---------------------------------------
//======================================================================

    private static class NorthArrowData extends SimpleDataConnection {

        public NorthArrowData() {
            super("North Arrow - EQ. J2000", "North Arrow - EQ. J2000");
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {

            if (plot==null) return null;

            List<DrawObj> retval;
            double iWidth= plot.getViewPortDimension().getWidth();
            double iHeight= plot.getViewPortDimension().getHeight();
            double ix= (iWidth<100) ? iWidth*.5 : iWidth*.25;
            double iy= (iHeight<100) ? iHeight*.5 : iWidth*.25;
            WorldPt wpStart= plot.getWorldCoords(new ViewPortPt((int)ix,(int)iy));
            double cdelt1 = plot.getImagePixelScaleInDeg();
            float zf= plot.getZoomFact();
            WorldPt wpt2= new WorldPt(wpStart.getLon(), wpStart.getLat() + (Math.abs(cdelt1)/zf)*(ARROW_LENTH/2));
            WorldPt wptE2= new WorldPt(wpStart.getLon()+(Math.abs(cdelt1)/zf)*(ARROW_LENTH/2), wpStart.getLat());

            ScreenPt sptStart= plot.getScreenCoords(wpStart);
            ScreenPt spt2= plot.getScreenCoords(wpt2);

            ScreenPt sptE2= plot.getScreenCoords(wptE2);
            if (sptStart==null || spt2==null || sptE2==null) return null;

            DirectionArrowDataObj dataN= new DirectionArrowDataObj(sptStart, spt2,"N");
            DirectionArrowDataObj dataE= new DirectionArrowDataObj(sptStart, sptE2,"E");

            return Arrays.asList(new DrawObj[]{dataN, dataE});

        }

        @Override
        public boolean getHasPerPlotData() { return true; }
    }

}


