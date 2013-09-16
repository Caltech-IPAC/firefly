package edu.caltech.ipac.firefly.commands;

import com.google.gwt.event.dom.client.HumanInputEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.SelectBox;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Arrays;
import java.util.List;


public class SelectAreaCmd extends BaseGroupVisCmd
                           implements WebEventListener {

    public enum Mode {SELECT, EDIT, OFF}

    public static final int EDIT_DISTANCE= BrowserUtil.isTouchInput() ? 18 : 10;

    public static final String CommandName= "SelectArea";
    private DrawingManager _drawMan;
    private ImageWorkSpacePt _firstPt;
    private ImageWorkSpacePt _currentPt;
    private Mode _mode;
    private boolean _mouseDown= false;
    private DataConnect _dataConnect= new DataConnect("Selection Tool");
    public static final String _selHelpText= "Click and drag to select an area, then choose from Options<br>" +
            "To modify your selection: click on the corners<br>" +
            "To select again: hold down the shift key, click, and drag";
    public static final String _editHelpText= "Click and drag a corner to resize selection area, then choose from Options<br>" +
                                              "To select again: hold down the shift key, click, and drag";
    private final WebPlotView.MouseInfo _mouseInfo=
            new WebPlotView.MouseInfo(new Mouse(),"Use mouse to select an area");
    private final String _onIcon= "SelectArea.on.Icon";
    private final String _offIcon= "SelectArea.off.Icon";

    private final WorldPt[] _ptAry= new WorldPt[2];
    private PlotWidgetGroup activeGroup= null;



    public SelectAreaCmd() {
        super(CommandName);
        AllPlots.getInstance().addListener(this);
        changeMode(Mode.OFF,false);
    }


//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        if (ev.getName().equals(Name.REPLOT)) {
            ReplotDetails details = (ReplotDetails) ev.getData();
            if (details.getReplotReason() != ReplotDetails.Reason.IMAGE_RELOADED &&
                details.getReplotReason() != ReplotDetails.Reason.ZOOM_COMPLETED) {
                changeMode(Mode.OFF,false);
            }
        }
        else if (ev.getName().equals(Name.FITS_VIEWER_CHANGE)) {
            MiniPlotWidget mpw= getMiniPlotWidget();
            PlotWidgetGroup g= (mpw!=null) ? mpw.getGroup() : null;
            if (_mode!=Mode.OFF && (g!=activeGroup || !g.getLockRelated())) {
                changeMode(Mode.OFF, false);
            }

        }
        else if (ev.getName().equals(Name.CROP)) {
            changeMode(Mode.OFF, false);
        }
    }
    
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



    protected void doExecute() {
        if (_drawMan==null) {
            _drawMan= new DrawingManager(CommandName, _dataConnect);
            WebLayerItem.addUICreator(CommandName, new WebLayerItem.UICreator(false,false));
        }
        switch (_mode) {
            case SELECT :
            case EDIT :
                changeMode(Mode.OFF,true);
                break;
            case OFF :
                disableSelection();
                changeMode(Mode.SELECT,true);
                break;
            default :
                WebAssert.argTst(false, "only support for SelectType of SELECT or EDIT");
                break;
        }
        if (_mode == Mode.SELECT) {
           setupSelect();
        }
    }

    public void clearSelect() { changeMode(Mode.OFF,false); }

    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("SelectArea.on.Icon"))  {
                return new Image(ic.getSelectAreaOn());
            }
            else if (iStr.equals("SelectArea.off.Icon"))  {
                return new Image(ic.getSelectAreaOff());
            }
            else if (iStr.equals("SelectArea.Icon"))  {
                return new Image(ic.getSelectAreaOff());
            }
        }
        return null;
    }



    private void setupSelect() {
        grabMouse();
        getMiniPlotWidget().setSelectionBarVisible(false);
    }

    private void setupEdit() {
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        grabMouse();
        if (sel==null) {
            WebAssert.tst(false, "no RecSelection found in plot");
        }
    }

    private void changeMode(Mode newMode, boolean initiatedByUser) {
        _mode= newMode;
        switch (_mode) {
            case SELECT :
                activeGroup= getMiniPlotWidget().getGroup();
                setIconProperty(_onIcon);
                _drawMan.setHelp(_selHelpText);

                addDrawMan();
                _drawMan.showMouseHelp(getPlotView());
                break;
            case EDIT :
                activeGroup= getMiniPlotWidget().getGroup();
                setIconProperty(_onIcon);
                _drawMan.setHelp(_editHelpText);
                addDrawMan();
                _drawMan.showMouseHelp(getPlotView());
                break;
            case OFF :
                releaseMouse();
                if (getMiniPlotWidget()!=null) {
                    for (MiniPlotWidget mpw :AllPlots.getInstance().getAll()) {
                        mpw.setSelectionBarVisible(false);
                    }
                }
                if (_drawMan!=null) removeDrawMan();
                setIconProperty(_offIcon);
                removeSelectionAttribute(initiatedByUser);
                if (_drawMan!=null) {
                    clearPlotViews();
                }
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
        removeSelectionAttribute(false);
        if (_drawMan!=null) {
            clearPlotViews();
        }
    }


    private void begin(WebPlotView pv, ScreenPt spt, HumanInputEvent ev) {
        try {
            WebPlot plot= pv.getPrimaryPlot();
            pv.fixScrollPosition();
            _mouseInfo.setEnableAllPersistent(true);
            _mouseInfo.setEnableAllExclusive(false);


            switch (_mode) {
                case SELECT :
                    _firstPt= plot.getImageWorkSpaceCoords(spt);
                    _currentPt= _firstPt;
                    break;
                case EDIT :
                    RecSelection sel= (RecSelection)pv.getAttribute(WebPlot.SELECTION);
                    if (sel==null) {
                        WebAssert.tst(false, "no RecSelection found in plot");
                    }

                    WorldPt wptAry[]= new WorldPt[] { sel.getPt0(), sel.getPt1()};


                    ScreenPt ptAry[]= new ScreenPt[4];

                    ptAry[0]= plot.getScreenCoords(wptAry[0]);
                    ptAry[2]= plot.getScreenCoords(wptAry[1]);
                    ptAry[1]= new ScreenPt(ptAry[2].getIX(), ptAry[0].getIY());
                    ptAry[3]= new ScreenPt(ptAry[0].getIX(), ptAry[2].getIY());


                    int idx= findClosestPtIdx(ptAry,spt);
                    ScreenPt testPt= plot.getScreenCoords(ptAry[idx]);
                    double dist= distance(testPt,spt);
                    if (dist<EDIT_DISTANCE) {
                        int oppoIdx= (idx+2) % 4;
                        _firstPt= plot.getImageWorkSpaceCoords(ptAry[oppoIdx]);
                        _currentPt= plot.getImageWorkSpaceCoords(ptAry[idx]);
                    }
                    else {
                        if (ev.isShiftKeyDown()) {
                            changeMode(Mode.SELECT,true);
                            begin(pv, spt, ev);
                        }
                        else {
                            _mouseDown= false;
                            _mouseInfo.setEnableAllExclusive(true);
                        }
                    }

                    break;
            //                    _debugLabel.setText("begin-edit: dist="+dist+", mouse down="+_mouseDown);
    default :
                    WebAssert.argTst(false, "only support for SelectType of SELECT or EDIT");
                    break;
            }

        } catch (ProjectionException e) {
            WebAssert.fail("Can't select area");
        }

    }

    private void drag(WebPlotView pv, ScreenPt spt) {
        WebPlot plot= pv.getPrimaryPlot();
        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);
        _currentPt= plot.getImageWorkSpaceCoords(spt);
        try {
            _dataConnect.setData(makeSelectedObj(plot));
            _drawMan.redraw();
        } catch (ProjectionException e) {
           // TODO - what should I do here?
        }
    }

    private void end(WebPlotView pv) {
        WebPlot plot= pv.getPrimaryPlot();
        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);
        setSelectionAttribute(makeSelection());

        if (_mode == Mode.SELECT) {
            releaseMouse();
            changeMode(Mode.EDIT,true);
            setupEdit();

        }
    }


    private List<DrawObj> makeSelectedObj(WebPlot plot) throws ProjectionException {
        _ptAry[0]=  plot.getWorldCoords(_firstPt);
        _ptAry[1]= plot.getWorldCoords(_currentPt);

        SelectBox fo= new SelectBox(_ptAry[0],_ptAry[1]);
        fo.setColor("black");
        fo.setInnerBoxColor("white");

        fo.setStyle(SelectBox.Style.HANDLED);

        return Arrays.asList((DrawObj)fo);
    }


    private RecSelection makeSelection() {
         return new RecSelection(_ptAry[0], _ptAry[1]);
    }

    private int findClosestPtIdx(ScreenPt ptAry[], ScreenPt pt)
                                  throws ProjectionException {


        double dist= Double.MAX_VALUE;
        double testDist;
        int idx= 0;
        for(int i=0; (i<ptAry.length); i++) {
            testDist= distance(ptAry[i],pt);
            if (testDist<dist) {
                dist= testDist;
                idx= i;
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
        for(MiniPlotWidget mpw : mpwList)  mpw.getPlotView().grabMouse(_mouseInfo);
    }

    private void releaseMouse() {
        List<MiniPlotWidget> mpwList= AllPlots.getInstance().getAll();
        for(MiniPlotWidget mpw : mpwList)  mpw.getPlotView().releaseMouse(_mouseInfo);
    }


    private void removeDrawMan() {
        _drawMan.clear();
        _dataConnect.setData(null);
    }

    private void addDrawMan() {
        List<MiniPlotWidget> mpwList= getGroupActiveList();
        for(MiniPlotWidget mpw : mpwList) _drawMan.addPlotView(mpw.getPlotView());
    }



    private void removeSelectionAttribute(boolean initiatedByUser) {
        for(MiniPlotWidget mpw : getGroupActiveList())  {
            WebPlotView pv= mpw.getPlotView();
            if (pv.containsAttributeKey(WebPlot.SELECTION)) {
                pv.removeAttribute(WebPlot.SELECTION);
                WebEvent<Boolean> ev= new WebEvent<Boolean>(this, Name.AREA_SELECTION, initiatedByUser);
                pv.fireEvent(ev);
            }
        }
    }


    private void setSelectionAttribute(RecSelection selection) {
        for(MiniPlotWidget mpw : getGroupActiveList())  {
            mpw.getPlotView().setAttribute(WebPlot.SELECTION,selection);
            WebEvent<Boolean> ev= new WebEvent<Boolean>(this, Name.AREA_SELECTION, true);
            mpw.getPlotView().fireEvent(ev);
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

    private class Mouse extends WebPlotView.DefMouseAll {

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
            _mouseDown= true;
            begin(pv, spt, ev);
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt) {
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
            begin(pv, spt, ev);
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

        DataConnect(String title) { super(title,_selHelpText);}

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot p) { return _data; }

        public void setData(List<DrawObj> data) {
            _data= data;
        }
    }


}