/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.OffsetScreenPt;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.LineSelection;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.util.Arrays;
import java.util.List;


public class DistanceToolCmd extends BaseGroupVisCmd
                           implements WebEventListener/*, PrintableOverlay*/ {

    public enum Mode {SELECT, EDIT, OFF}
    public static final String HTML_DEG= "&deg;";

    public static final int EDIT_DISTANCE= BrowserUtil.isTouchInput() ? 15 : 8;
    public static final String _selHelpText = "Click and drag to find a distance";
    public static final String _editHelpText = "Click and drag at either end to adjust distance";

    public static final String CommandName= "DistanceTool";
    private DrawingManager _drawMan;
    private ImageWorkSpacePt _firstPt;
    private ImageWorkSpacePt _currentPt;
    private Mode _mode;
    private boolean _mouseDown= false;
    private final WebPlotView.MouseInfo _mouseInfo=
            new WebPlotView.MouseInfo(new Mouse(),"Use mouse mark a distance");
    private final static String _onIcon= "DistanceTool.on.Icon";
    private final static String _offIcon= "DistanceTool.off.Icon";
    private final static NumberFormat _nf= NumberFormat.getFormat("#.###");

    private final ImageWorkSpacePt[] _ptAry= new ImageWorkSpacePt[2];
    private final String DIST_READOUT = "DistanceReadout";
    private final String ARC_MIN = "arcmin";
    private final String ARC_SEC = "arcsec";
    private final String DEG = "deg";
    private DataConnect _dataConnect= new DataConnect("Distance Tool");
    private boolean _posAngle= false;



    public DistanceToolCmd() {
        super(CommandName);
        AllPlots.getInstance().addListener(this);
        changeMode(Mode.OFF);
    }


//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        if (ev.getName().equals(Name.REPLOT)) {
            ReplotDetails details = (ReplotDetails)ev.getData();
            ReplotDetails.Reason reason= details.getReplotReason();
            //todo: not to remove and turn off distance tool if expanding plot.
            /*disableSelection();
            changeMode(Mode.OFF);*/
        }

//        if (ev.getName().equals(Name.FITS_VIEWER_CHANGE)) {
//            LineSelection sel= (LineSelection)getPlotView().getAttribute(WebPlot.ACTIVE_DISTANCE);
//            changeMode(sel==null ? Mode.OFF : Mode.EDIT);
//        }
    }



//======================================================================
//------------------ Methods from PrintableOverlay ------------------
//======================================================================


    private String makeNonHtml(String s) {
        String retval= s;
        if (s.endsWith(HTML_DEG)) {
            retval= s.substring(0,s.indexOf(HTML_DEG)) + " deg";
        }
        return retval;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



    protected void doExecute() {
        if (_drawMan==null) {
//            _drawMan= new DrawingManager(CommandName, _dataConnect,this);
            _drawMan= new DrawingManager(CommandName, _dataConnect);
            WebLayerItem.addUICreator(CommandName, new DistUICreator());
        }
        disableSelection();
        switch (_mode) {
            case SELECT :
            case EDIT :
                changeMode(Mode.OFF);
//                AlertLayerPopup.setAlert(false);
                break;
            case OFF :
//                AlertLayerPopup.setAlert(true);
                changeMode(Mode.SELECT);
                break;
            default :
                WebAssert.argTst(false, "only support for SelectType of SELECT or EDIT");
                break;
        }
        if (_mode == Mode.SELECT) {
           setupSelect();
        }
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


    public boolean isPosAngleEnabled() { return _posAngle; }

    private void setupSelect() {
        grabMouse();
        getMiniPlotWidget().hideSelectionBar();
    }

    private void setupEdit() {
        LineSelection sel= (LineSelection)getPlotView().getAttribute(WebPlot.ACTIVE_DISTANCE);
        grabMouse();
        if (sel==null) {
            WebAssert.tst(false, "no ActiveDistance found in plot");
        }
    }

    private void changeMode(Mode newMode) {
        _mode= newMode;
        switch (_mode) {
            case SELECT :
                setIconProperty(_onIcon);
                _drawMan.setHelp(_selHelpText);
                addDrawMan();
                _drawMan.showMouseHelp(getPlotView());
                break;
            case EDIT :
                setIconProperty(_onIcon);
                _drawMan.setHelp(_editHelpText);
                _drawMan.showMouseHelp(getPlotView());
                addDrawMan();
                break;
            case OFF :
                if (_drawMan!=null) removeDrawMan();
                setIconProperty(_offIcon);
                break;
            default :
                WebAssert.argTst(false, "only support for SelectType of SELECT or EDIT");
                break;
        }
    }

    private void disableSelection() {
        switch (_mode) {
            case SELECT :
                releaseMouse();
                break;
            case EDIT :
                releaseMouse();
                break;
            case OFF :
                // do nothing
                break;
            default :
                WebAssert.argTst(false, "only support for SelectType of SELECT or EDIT");
                break;
        }
        removeAttribute();
        _dataConnect.setData(null);
        if (_drawMan!=null) {
            clearPlotViews();
        }
    }


    private void begin(WebPlotView pv, ScreenPt spt) {
            WebPlot plot= pv.getPrimaryPlot();
            _mouseInfo.setEnableAllPersistent(true);
            _mouseInfo.setEnableAllExclusive(false);


            switch (_mode) {
                case SELECT :
                    _firstPt= plot.getImageWorkSpaceCoords(spt);
                    _currentPt= _firstPt;
                    break;
                case EDIT :
                    LineSelection sel= (LineSelection)pv.getAttribute(WebPlot.ACTIVE_DISTANCE);
                    if (sel==null) {
                        WebAssert.tst(false, "no RecSelection found in plot");
                    }

                    ImageWorkSpacePt ptAry[]= new ImageWorkSpacePt[] { sel.getPt1(), sel.getPt2()};

                    int idx= findClosestPtIdx(plot, ptAry, spt);
                    if (idx<0) return;
                    ScreenPt testPt= plot.getScreenCoords(ptAry[idx]);
                    double dist= distance(testPt,spt);
                    if (dist<EDIT_DISTANCE && dist>-1) {
                        int oppoIdx= idx==0 ? 1 : 0;
                        _firstPt= ptAry[oppoIdx];
                        _currentPt= ptAry[idx];
                    }
                    else {
                        _firstPt= plot.getImageWorkSpaceCoords(spt);
                        _currentPt= _firstPt;
                    }

                    break;
                case OFF :
                    releaseMouse();
                    break;
                default :
                    WebAssert.argTst(false, "only support for SelectType of SELECT EDIT, OFF");
                    break;
            }


    }

    private void drag(WebPlotView pv, ScreenPt spt) {
        WebPlot plot= pv.getPrimaryPlot();
        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);
        _currentPt= plot.getImageWorkSpaceCoords(spt);
        _dataConnect.setData(makeSelectedObj(plot));
        _drawMan.redraw();
    }

    private void end(WebPlotView pv) {
        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);
        setAttribute(makeSelection());
        if (_mode == Mode.SELECT) {
            releaseMouse();
            changeMode(Mode.EDIT);
            setupEdit();
        }
    }


    private List<DrawObj> makeSelectedObj(WebPlot plot) {
        List<DrawObj> retval;
        _ptAry[0]= _firstPt;
        _ptAry[1]= _currentPt;

        Projection proj= plot.getProjection();
        Pt anyPt1;
        Pt anyPt2;
        double dist;
        boolean world;
        if (proj.isSpecified()) {
            anyPt1 = plot.getWorldCoords(_ptAry[0]);
            anyPt2 = plot.getWorldCoords(_ptAry[1]);
            dist= VisUtil.computeDistance((WorldPt)anyPt1,(WorldPt)anyPt2);
            world= true;
        }
        else {
            anyPt1 = _ptAry[0];
            anyPt2 = _ptAry[1];
            dist= VisUtil.computeDistance(anyPt1,anyPt2);
            world= false;
        }

        if (anyPt1==null || anyPt2==null) return null;

        ShapeDataObj obj= ShapeDataObj.makeLine(anyPt1,anyPt2);
        obj.setStyle(ShapeDataObj.Style.HANDLED);
        setDistOnShape(obj,dist, ShapeDataObj.TextLocation.LINE_MID_POINT,world);
        obj.setTextOffset(new OffsetScreenPt(-15, 0));
        OffsetScreenPt angleOffPt;

        if (_posAngle) {
            Pt eastPt;
            Pt westPt;

            if (anyPt1.getX()>anyPt2.getX()) {
                eastPt= anyPt1;
                westPt= anyPt2;
            }
            else {
                eastPt= anyPt2;
                westPt= anyPt1;
            }

            ShapeDataObj adj;
            ShapeDataObj op;
            double adjDist;
            double opDist;
            Pt lonDelta1TextPt;
            if (world) {
                WorldPt lonDelta1= new WorldPt(eastPt.getX(), eastPt.getY());
                WorldPt lonDelta2= new WorldPt(westPt.getX(), eastPt.getY());
                adjDist= VisUtil.computeDistance(lonDelta1,lonDelta2);

                WorldPt latDelta1= new WorldPt(westPt.getX(), eastPt.getY());
                WorldPt latDelta2= new WorldPt(westPt.getX(), westPt.getY());
                opDist= VisUtil.computeDistance(latDelta1,latDelta2);
                adj= ShapeDataObj.makeLine(lonDelta1,lonDelta2);
                op= ShapeDataObj.makeLine(latDelta1, latDelta2);
                lonDelta1TextPt= lonDelta1;
            }
            else {
                ImageWorkSpacePt lonDelta1= new ImageWorkSpacePt(eastPt.getX(), eastPt.getY());
                ImageWorkSpacePt lonDelta2= new ImageWorkSpacePt(westPt.getX(), eastPt.getY());
                adjDist= VisUtil.computeDistance(lonDelta1,lonDelta2);

                ImageWorkSpacePt latDelta1= new ImageWorkSpacePt(westPt.getX(), eastPt.getY());
                ImageWorkSpacePt latDelta2= new ImageWorkSpacePt(westPt.getX(), westPt.getY());
                opDist= VisUtil.computeDistance(latDelta1,latDelta2);
                adj= ShapeDataObj.makeLine(lonDelta1, lonDelta2);
                op= ShapeDataObj.makeLine(latDelta1,latDelta2);
                lonDelta1TextPt= lonDelta1;
            }


            setDistOnShape(adj,adjDist, ShapeDataObj.TextLocation.LINE_MID_POINT_OR_BOTTOM,world);
            setDistOnShape(op,opDist, ShapeDataObj.TextLocation.LINE_MID_POINT_OR_TOP, world);
            op.setTextOffset(new OffsetScreenPt(0,15));

            double sinX= opDist/dist;
            double angle= Math.toDegrees(Math.asin(sinX));

            String aStr= _nf.format(angle) +HTML_DEG;
            ShapeDataObj angleShape= ShapeDataObj.makeText(new OffsetScreenPt(8,-8), lonDelta1TextPt, aStr);

            retval= Arrays.asList((DrawObj)obj, adj, op,angleShape);
        }
        else {
            retval= Arrays.asList((DrawObj)obj);

        }


        return retval;
    }

    private void setDistOnShape(ShapeDataObj obj,
                                double dist,
                                ShapeDataObj.TextLocation texLoc,
                                boolean isWorld) {
        String pref= Preferences.get(DIST_READOUT);

        if (isWorld)  {
            if(pref!=null && pref.equals(ARC_MIN)){
                double arcmin = dist*60.0;
                obj.setText(" "+_nf.format(arcmin)+"\'");
            }
            else if(pref!=null && pref.equals(ARC_SEC)){
                double arcsec = dist*3600.0;
                obj.setText(" "+_nf.format(arcsec)+"\"");
            } else {
                obj.setText(" "+_nf.format(dist) +"&deg;");
            }
        }
        else {
            obj.setText(" "+((int)dist) +" Pixels");
        }
        obj.setTextLocation(texLoc);
    }

    private LineSelection makeSelection() {
         return new LineSelection(_ptAry[0], _ptAry[1]);
    }

    private int findClosestPtIdx(WebPlot plot, Pt inPtAry[], ScreenPt spt) {


        int idx= -1;
        ScreenPt ptAry[]= new ScreenPt[inPtAry.length];
        for(int i= 0; (i<ptAry.length); i++) {
            ptAry[i]= plot.getScreenCoords(inPtAry[i]);
        }

        double dist= Double.MAX_VALUE;
        double testDist;
        for(int i=0; (i<ptAry.length); i++) {
            if (ptAry[i]!=null) {
                testDist= distance(ptAry[i],spt);
                if (testDist<dist && testDist>-1) {
                    dist= testDist;
                    idx= i;
                }
            }
        }
        return idx;
    }



    private static double distance(ScreenPt pt, ScreenPt pt2) {
        double dx= pt.getIX() - pt2.getIX();
        double dy= pt.getIY() - pt2.getIY();
        return Math.sqrt(dx*dx + dy*dy);
    }


    private void grabMouse() {

        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList)  {
            mpw.getPlotView().setTouchScrollingEnabled(false);
            mpw.getPlotView().grabMouse(_mouseInfo);
        }
    }

    private void releaseMouse() {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList)  {
            mpw.getPlotView().setTouchScrollingEnabled(true);
            mpw.getPlotView().releaseMouse(_mouseInfo);
        }
    }


    private void removeDrawMan() {
        _drawMan.clear();
        _dataConnect.setData(null);
    }

    private void addDrawMan() {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList) _drawMan.addPlotView(mpw.getPlotView());
    }



    private void removeAttribute() {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList)  {
            WebPlotView pv= mpw.getPlotView();
            pv.removeAttribute(WebPlot.ACTIVE_DISTANCE);
            WebEvent ev= new WebEvent<WebPlotView>(this, Name.LINE_SELECTION, pv);
            pv.fireEvent(ev);
        }
    }

    private void setAttribute(LineSelection o) {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        WebPlotView pv;
        for(MiniPlotWidget mpw : mpwList)  {
            pv= mpw.getPlotView();
            pv.setAttribute(WebPlot.ACTIVE_DISTANCE,o);
            WebEvent<Boolean> ev= new WebEvent<Boolean>(this, Name.LINE_SELECTION, true);
            pv.fireEvent(ev);
        }

    }

    private void clearPlotViews() {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList)  {
            _drawMan.removePlotView(mpw.getPlotView());
        }

    }

//======================================================================
//------------------ Inner classes-----------------------
//======================================================================

    private class Mouse extends WebPlotView.DefMouseAll  {

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
            _mouseDown= true;
            begin(pv, spt);
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) {
            if (_mouseDown) drag(pv, spt);
        }

        @Override
        public void onMouseUp(WebPlotView pv, ScreenPt spt) {
            if (_mouseDown) {
                _mouseDown= false;
                end(pv);
            }
        }

        @Override
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
            _mouseDown= true;
            begin(pv, spt);
        }

        @Override
        public void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev) {
            if (_mouseDown) drag(pv, spt);
        }

        @Override
        public void onTouchEnd(WebPlotView pv) {
            if (_mouseDown) {
                _mouseDown= false;
                end(pv);
            }
        }
    }

    private class DataConnect extends SimpleDataConnection {
        List<DrawObj> _data;

        DataConnect(String title) { super(title,_editHelpText );}

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot p) { return _data; }

        public void setData(List<DrawObj> data) {
            _data= data;
        }
    }




    private class DistUICreator extends WebLayerItem.UICreator {

        private DistUICreator() { super(true, true); }

        public Widget makeExtraUI(final WebLayerItem item) {


            final CheckBox cb= GwtUtil.makeCheckBox("Offset Calculation",
                                           "Calculate the distance, delta(RA), delta(Dec), and PA between two points",
                                           _posAngle);
            cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                public void onValueChange(ValueChangeEvent<Boolean> ev) {
                    _posAngle= ev.getValue();
                    redraw();
                }
            });


            SimpleInputField units= SimpleInputField.createByProp("PrefGroup.Generic.field.DistanceReadout");
            String pref= Preferences.get(DIST_READOUT);
            units.setValue(pref==null ? DEG : pref);
            units.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> ev) {
                    Preferences.set(DIST_READOUT,ev.getValue());
                    redraw();
                }
            });




            VerticalPanel vp = new VerticalPanel ();
            vp.add(cb);
            vp.add(units);


            return vp;
        }

        private void redraw() {
            if (_dataConnect!=null && _drawMan!=null && getPlotView()!=null && getPlotView().getPrimaryPlot()!=null) {
                _dataConnect.setData(makeSelectedObj(getPlotView().getPrimaryPlot()));
                _drawMan.redraw();
            }

        }
        public void delete(WebLayerItem item) { doExecute(); }
    }



}