/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.DrawingDef;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.PointSelection;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.util.ArrayList;
import java.util.List;


public class ActivePointToolCmd extends BaseGroupVisCmd implements WebEventListener {

    public static final String CommandName= "ActivePointTool";
    private DrawingManager drawMan;
    private boolean commandControl= false;

    private final WebPlotView.MouseInfo _mi = new WebPlotView.MouseInfo(new Mouse(),
                                                                        "Click on point");


    private boolean modeOn= false;
    private final static String _onIcon= "ActivePointTool.on.Icon";
    private final static String _offIcon= "ActivePointTool.off.Icon";
    private MarkedPointDisplay dataConnect = new MarkedPointDisplay();



    public ActivePointToolCmd() {
        super(CommandName);
        AllPlots.getInstance().addListener(this);
        changeMode(false);
    }


//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



    protected void doExecute() {
        changeMode(!modeOn);
    }

    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals(_onIcon))  {
                return new Image(ic.getDistanceOn());
            }
            else if (iStr.equals(_offIcon))  {
                return new Image(ic.getDistanceOff());
            }
            else if (iStr.equals(CommandName+".Icon"))  {
                return new Image(ic.getDistanceOff());
            }
        }
        return null;
    }

    public boolean isCommandControl() { return commandControl; }
    public void setCommandControl(boolean commandControl) {
        this.commandControl = commandControl;
    }

    public void changeMode(boolean on) {

        modeOn= on;
        if (on) {
            if (drawMan ==null) {
                drawMan = new DrawingManager("Clicked Point", dataConnect);
            }
            dataConnect.setPoint(null, null);
            drawMan.redraw();
            List<MiniPlotWidget> mpwList= AllPlots.getInstance().getAll();
            for (MiniPlotWidget mpw : mpwList) {
                drawMan.addPlotView(mpw.getPlotView());
                mpw.getPlotView().addPersistentMouseInfo(_mi);
            }
        }
        else {
            if (drawMan!=null) {
                List<MiniPlotWidget> mpwList= AllPlots.getInstance().getAll();
                for (MiniPlotWidget mpw : mpwList) {
                    drawMan.addPlotView(mpw.getPlotView());
                    mpw.getPlotView().removePersistentMouseInfo(_mi);
                }
                drawMan.clear();
                dataConnect.setPoint(null, null);
                removeAttribute();
            }
        }
    }


    private void removeAttribute() {
        List<MiniPlotWidget> mpwList= AllPlots.getInstance().getAll();
        for(MiniPlotWidget mpw : mpwList)  {
            WebPlotView pv= mpw.getPlotView();
            pv.removeAttribute(WebPlot.ACTIVE_POINT);
            WebEvent ev= new WebEvent<WebPlotView>(this, Name.POINT_SELECTION, pv);
            pv.fireEvent(ev);
        }
    }

    private void setAttribute(PointSelection o) {
        List<MiniPlotWidget> mpwList= AllPlots.getInstance().getAll();
        WebPlotView pv;
        for(MiniPlotWidget mpw : mpwList)  {
            pv= mpw.getPlotView();
            pv.setAttribute(WebPlot.ACTIVE_POINT, o);
            WebEvent<Boolean> ev= new WebEvent<Boolean>(this, Name.POINT_SELECTION, true);
            pv.fireEvent(ev);
        }

    }

    private void updatePoint(WebPlotView pv, ScreenPt spt) {
        if (modeOn) {
            WebPlot plot= pv.getPrimaryPlot();
            Projection proj= plot.getProjection();
            Pt pt;
            if (proj.isSpecified()) {
                pt= plot.getWorldCoords(spt);
            }
            else {
                pt= plot.getImageCoords(spt);
            }

            dataConnect.setPoint(pt, pv.getPrimaryPlot());
            drawMan.redraw();
            setAttribute(new PointSelection(plot.getImageWorkSpaceCoords(pt)));
        }
    }

//======================================================================
//------------------ Inner classes-----------------------
//======================================================================



    private class Mouse extends WebPlotView.DefMouseAll {

        @Override
        public void onClick(WebPlotView pv, ScreenPt spt) {
            updatePoint(pv,spt);
        }

    }


    private static class MarkedPointDisplay extends SimpleDataConnection {

        private final List<DrawObj> list = new ArrayList<DrawObj>(1);
        private WebPlot markedPlot = null;

        MarkedPointDisplay() {
            super("Clicked Point", "Point lock to your click", DrawingDef.COLOR_PT_3);
        }

        public void setPoint(Pt pt, WebPlot plot) {
            list.clear();
            if (pt != null && plot != null) {
                PointDataObj obj = new PointDataObj(pt, DrawSymbol.CIRCLE);
                list.add(obj);
                markedPlot = plot;
            }
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            List<DrawObj> retList = list;
            if (list.size() > 0 && plot != null && markedPlot != null && plot == markedPlot) {
                PointDataObj obj = new PointDataObj(list.get(0).getCenterPt(), DrawSymbol.SQUARE);
                retList = new ArrayList<DrawObj>(2);
                retList.addAll(list);
                retList.add(obj);
            }
            return retList;
        }


        @Override
        public boolean getSupportsMouse() { return false; }
    }
}