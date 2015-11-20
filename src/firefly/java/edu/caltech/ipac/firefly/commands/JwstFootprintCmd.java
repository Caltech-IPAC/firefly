/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.TextBoxInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.FootprintDs9;
import edu.caltech.ipac.firefly.visualize.FootprintFactory.FOOTPRINT;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.OverlayMarker;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;


public class JwstFootprintCmd extends    BaseGroupVisCmd
                           implements WebEventListener/*, PrintableOverlay*/ {

    public enum Mode {ADD_FOOTPRINT, MOVE, ROTATE, OFF}

    public static final String BASE_DRAWER_ID = "FootPrintID";
    public static final int EDIT_DISTANCE = BrowserUtil.isTouchInput() ? 20 : 12;
    public static final String _selHelpText = "Tap to create footprint";
    public static final String _editHelpText = "Click center and drag to move, click corner and drag to rotate";
    public static final String BASE_TITLE = "FootPrint ";

    public static int mCnt = 0;

    public static final String CommandName = "Footprint";
    private HashMap<OverlayMarker, MarkerDrawing> _markerMap = new HashMap<OverlayMarker, MarkerDrawing>(10);
    private OverlayMarker _activeMarker = null;

    private Mode _mode;
    private boolean _mouseDown = false;
    private boolean _doMove = false;
    private final WebPlotView.MouseInfo _mouseInfo =
            new WebPlotView.MouseInfo(new Mouse(), "Create a footprint overlay");
    private String _onLabel = "Hide @ footprint";
    private String _offLabel = "Show @ footprint";
    private String _addLabel = "Add @ footprint";

    private Label disToCenter = new Label("");
    private Label centerPos = new Label("");
	public SimpleInputField angle;
	protected FOOTPRINT footprintDef;

    public JwstFootprintCmd() {
        super(CommandName+FOOTPRINT.JWST.name());
        this.footprintDef = FOOTPRINT.JWST;//Defautl
        _onLabel = _onLabel.replace("@", footprintDef.name());
        _offLabel = _offLabel.replace("@", footprintDef.name());
        _addLabel = _addLabel.replace("@", footprintDef.name());
        AllPlots.getInstance().addListener(this);
        changeMode(Mode.OFF);
    }

    public JwstFootprintCmd(FOOTPRINT fp) {
        super(CommandName+fp.name());
        AllPlots.getInstance().addListener(this);
        changeMode(Mode.OFF);
        this.footprintDef = fp;
        _onLabel = _onLabel.replace("@", footprintDef.name());
        _offLabel = _offLabel.replace("@", footprintDef.name());
        _addLabel = _addLabel.replace("@", footprintDef.name());
    }
//======================================================================
//------------------ Methods from WebEventListener ------------------
//======================================================================

    public void eventNotify(WebEvent ev) {
        Name name = ev.getName();
        if (name.equals(Name.FITS_VIEWER_ADDED)) {
            if (_markerMap.size() > 0) {
                addViewer((MiniPlotWidget) ev.getData());
            }
        } else if (name.equals(Name.ALL_FITS_VIEWERS_TEARDOWN)) {
            clearAllViewers();
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void addViewer(MiniPlotWidget mpw) {
        for (Map.Entry<OverlayMarker, MarkerDrawing> entry : _markerMap.entrySet()) {
            entry.getValue().getDrawMan().addPlotView(mpw.getPlotView());
        }
    }

    private void clearAllViewers() {
        for (Map.Entry<OverlayMarker, MarkerDrawing> entry : _markerMap.entrySet()) {
            entry.getValue().getDrawMan().clear();
        }
        _markerMap.clear();

    }

    protected void doExecute() {
		switch (_mode) {
		case ADD_FOOTPRINT:
		case ROTATE:
		case MOVE:
			disableSelection();
			changeMode(Mode.OFF);
			// AlertLayerPopup.setAlert(false);
			break;
		case OFF:
			if (_markerMap.size() == 0)
				changeMode(Mode.ADD_FOOTPRINT);
			else
				changeMode(Mode.MOVE);
			setupMouse();
			// AlertLayerPopup.setAlert(true);
			break;
		default:
			WebAssert.argTst(false, "doExecute() only support for SelectType of ADD_FOOTPRINT or MOVE");
			break;
		}
    }

    @Override
    public boolean hasIcon() {
        return false;
    }

    private void setupMouse() {
        grabMouse();
        getMiniPlotWidget().hideSelectionBar();
    }

    private void changeMode(Mode newMode) {
        _mode = newMode;
        switch (_mode) {
            case ADD_FOOTPRINT:
                createDrawMan();
                setLabel(_onLabel);
                break;
            case MOVE:
                setLabel(_onLabel);
                changeToEditHelp();
                addDrawMan();
                break;
            case ROTATE:
                setLabel(_onLabel);
                changeToEditHelp();
                addDrawMan();
                break;
            case OFF:
                removeDrawMan();
                setLabel(_markerMap.size() > 0 ? _offLabel : _addLabel);
                break;
            default:
                WebAssert.argTst(false, "only support for SelectType of ADD_MARKER, RESIZE or MOVE");
                break;
        }
    }

    private void disableSelection() {
        switch (_mode) {
            case ADD_FOOTPRINT:
            case ROTATE:
            case MOVE:
                releaseMouse();
                break;
            case OFF: /* do nothing*/
                break;
            default:
                WebAssert.argTst(false, "only support for SelectType of ADD_MARKER, RESIZE or MOVE");
                break;
        }
    }

    private void setActiveMarker(OverlayMarker m) {
        _activeMarker= m;
    }

    private void begin(WebPlotView pv, ScreenPt spt) {
		WebPlot plot = pv.getPrimaryPlot();
		_mouseInfo.setEnableAllPersistent(true);
		_mouseInfo.setEnableAllExclusive(false);

		if (_mode == Mode.ADD_FOOTPRINT) {
			_doMove = true;
			changeMode(Mode.MOVE);
			drag(pv, spt);
		} else {
			OverlayMarker m = findFootprint(plot, spt);
			if (m != null) {
				setActiveMarker(m); // ONLY set _activeMarker if m!=null
				_markerMap.get(m).cancelDeferred();
				_doMove = true;
                if (m.contains(spt, plot)) {
                	changeMode(Mode.MOVE);
                 // need to call add_draw anyway even if no resize! always can ad rotation afterward on corners enabled
				}else{
					changeMode(Mode.ROTATE);
				}
				drag(pv, spt);
			} else {
				_mouseInfo.setEnableAllExclusive(true);
			}
		}


    }


    private void drag(WebPlotView pv, ScreenPt spt) {
        if (!_doMove) return;

        WebPlot plot = pv.getPrimaryPlot();
        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);

            switch (_mode) {
                case ADD_FOOTPRINT:
//                    break;
                case MOVE:
                    WorldPt center = plot.getWorldCoords(spt);
                    if (center==null) return;
                    _activeMarker.move(center, plot);
                    handleCenterChanged(center);
                    break;
                case ROTATE:
                    _activeMarker.setEndPt(plot.getWorldCoords(spt), plot);
                    handleRotationChanged(((FootprintDs9)_activeMarker).getRotAngle());
                    break;
                default:
                    WebAssert.argTst(false, "only support for SelectType of ADD_MARKER or MOVE");
                    break;
            }


            updateData(_activeMarker, plot, true);
            _markerMap.get(_activeMarker).getDrawMan().redraw();
    }

    private void end(WebPlotView pv) {
        if (!_doMove) return;

        _mouseInfo.setEnableAllPersistent(true);
        _mouseInfo.setEnableAllExclusive(false);
            switch (_mode) {
                case ADD_FOOTPRINT:
                    releaseMouse();
                    break;
                case MOVE:
                case ROTATE:
                    _activeMarker.adjustStartEnd(pv.getPrimaryPlot());
                    _markerMap.get(_activeMarker).deferDraw(pv.getPrimaryPlot(),_activeMarker);

//                    updateData(_activeMarker, null);
//                    _markerMap.get(_activeMarker).getDrawMan().redraw();
                    break;
                default:
                    WebAssert.argTst(false, "only support for SelectType of ADD_MARKER or MOVE");
                    break;
            }
        _doMove = false;
    }



    private OverlayMarker findFootprint(WebPlot plot, ScreenPt pt) {
        if (plot == null || pt == null) return null;

        OverlayMarker retval = null;
        List<OverlayMarker> centerList = new ArrayList<OverlayMarker>(10);
        for (OverlayMarker m : _markerMap.keySet()) {
            if (m.contains(pt, plot)) centerList.add(m);
        }
        int minDist = Integer.MAX_VALUE;
        int dist;
        if (centerList.size() > 0) {
            for (OverlayMarker m : centerList) {
                dist = m.getCenterDistance(pt, plot);
                if (dist < minDist && dist > -1) {
                    retval = m;
                    minDist = dist;
                }
            }
        } else {
            OverlayMarker candidate = null;
            OverlayMarker.MinCorner editCorner = null;
            for (OverlayMarker m : _markerMap.keySet()) {
                OverlayMarker.MinCorner minC = m.getMinCornerDistance(pt, plot);
                if (minC != null && minC.getDistance() < minDist) {
                    candidate = m;
                    minDist = minC.getDistance();
                    editCorner = minC;
                }
                if (minDist < EDIT_DISTANCE) {
                    retval = candidate;
                    retval.setEditCorner(editCorner.getCorner(), plot);
                }
            }

        }
        return retval;

    }

    private void updateAllData() {
        for (OverlayMarker m : _markerMap.keySet()) {
            WebPlot p= getPlotView()!=null ? getPlotView().getPrimaryPlot() : null;
            updateData(m, getPlotView().getPrimaryPlot(), false);
        }
    }


    private void updateData(OverlayMarker m, WebPlot plot, boolean drawHandles) {
        if (m.isReady()) {
			List<DrawObj> data = new ArrayList<DrawObj>();
			List<DrawObj> editData = new ArrayList<DrawObj>();

			List<DrawObj> fp = m.getShape();
			ShapeDataObj centerShape = (ShapeDataObj) fp.get(0);//circle 
			GwtUtil.logToServer(Level.INFO,
					"updateData(WebPlot) - circle center =" + centerShape.getCenterPt() );
			GwtUtil.logToServer(Level.INFO,
					"updateData(WebPlot) - marker center =" + m.getCenter(plot) );
			
			data.addAll(fp);



            if (drawHandles) {
                int size = 5;
                editData.add(ShapeDataObj.makeRectangle(m.getCorner(OverlayMarker.Corner.NW, plot), size, size));
                editData.add(ShapeDataObj.makeRectangle(m.getCorner(OverlayMarker.Corner.NE, plot), -size, size));
                editData.add(ShapeDataObj.makeRectangle(m.getCorner(OverlayMarker.Corner.SW, plot), size, -size));
                editData.add(ShapeDataObj.makeRectangle(m.getCorner(OverlayMarker.Corner.SE, plot), -size, -size));
				for (DrawObj drawObj : fp) {
					if(drawObj instanceof FootprintObj){
						for(WorldPt[] pt:((FootprintObj)drawObj).getPos()){
							for (int i = 0; i < pt.length; i++) {
								ScreenPt spt= plot.getScreenCoords(pt[i]);
								editData.add(ShapeDataObj.makeRectangle(spt, -size, -size));//FIXME make those point as point object and movable from the marker 
							}
						}						
					}
				}
            }
            if (!StringUtils.isEmpty(m.getTitle()) && plot!=null) {
            	centerShape.setFontName(m.getFont());
            	centerShape.setText(SafeHtmlUtils.fromString(m.getTitle()).asString());
            	centerShape.setTextLocation(convertTextLoc(m.getTextCorner()));
            }


            _markerMap.get(m).getConnect().setData(data, editData, plot);
        }
    }


    private static ShapeDataObj.TextLocation convertTextLoc(OverlayMarker.Corner corner) {
        switch (corner) {
            case NE: return ShapeDataObj.TextLocation.CIRCLE_NE;
            case NW: return ShapeDataObj.TextLocation.CIRCLE_NW;
            case SE: return ShapeDataObj.TextLocation.CIRCLE_SE;
            case SW: return ShapeDataObj.TextLocation.CIRCLE_SW;
        }
        return ShapeDataObj.TextLocation.CIRCLE_NE;
    }


    private void grabMouse() {
        List<MiniPlotWidget> mpwList = getAllActivePlots();
        for (MiniPlotWidget mpw : mpwList) {
            mpw.getPlotView().setTouchScrollingEnabled(false);
            mpw.getPlotView().grabMouse(_mouseInfo);
        }
    }

    private void releaseMouse() {
        List<MiniPlotWidget> mpwList = getAllActivePlots();
        for (MiniPlotWidget mpw : mpwList) {
            mpw.getPlotView().setTouchScrollingEnabled(true);
            mpw.getPlotView().releaseMouse(_mouseInfo);
        }
    }


    private void removeDrawMan() {
        DrawingManager drawMan;
        for (MarkerDrawing md : _markerMap.values()) {
            md.saveColor();
            drawMan = md.getDrawMan();
            drawMan.clear();
            for (MiniPlotWidget mpw : getAllActivePlots()) {
                drawMan.removePlotView(mpw.getPlotView());
            }
            md.getConnect().clearData();
        }
    }


    private void createDrawMan() {
    	WebPlotView pv= getPlotView();
    	 WorldPt center = null;
    	 if (pv!=null && pv.getPrimaryPlot()!=null) {
        	WebPlot plot= pv.getPrimaryPlot();
            center = pv.findCurrentCenterWorldPoint();
            _activeMarker = new FootprintDs9(center,plot,this.footprintDef);
//            _activeMarker = new FootprintDs9(center,plot,FOOTPRINT.HST);
        	GwtUtil.logToServer(Level.INFO, "createDrawMan() - FP created with constructor center, plot "+center.toString());
            
        }
        else{
        	_activeMarker = new FootprintDs9(this.footprintDef);

        	GwtUtil.logToServer(Level.INFO, "createDrawMan() - FP created with default empty constructor");
        }
        
//        Footprint fp = new Footprint();
//		_activeMarker = fp;
//		FootprintAsMarkers fp = new FootprintAsMarkers();
        
//
        _markerMap.put(_activeMarker, new MarkerDrawing());
//        WebPlotView pv= getPlotView();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            WebPlot plot= pv.getPrimaryPlot();
            center = pv.findCurrentCenterWorldPoint();
            GwtUtil.logToServer(Level.INFO, " center plot "+center.toString());
           
			_activeMarker.move(center, plot);
            updateData(_activeMarker, plot, true);
            _markerMap.get(_activeMarker).getDrawMan().redraw();
        }

    }

    private void changeToEditHelp() {
        boolean first = true;
        for (MarkerDrawing md : _markerMap.values()) {
            md.getDrawMan().setHelp(_editHelpText);
            if (first) {
                md.getDrawMan().showMouseHelp(getPlotView());
                first = false;
            }
        }

    }


    private void addDrawMan() {
        DrawingManager drawMan;
        updateAllData();
        for (MarkerDrawing md : _markerMap.values()) {
            drawMan = md.getDrawMan();
            for (MiniPlotWidget mpw : AllPlots.getInstance().getAll(true)) {
                drawMan.addPlotView(mpw.getPlotView());
            }
            md.restoreColor();
            drawMan.redraw();
        }
    }


    private void removeMarker(String id) {
        OverlayMarker marker = null;
        MarkerDrawing drawing = null;
        for (Map.Entry<OverlayMarker, MarkerDrawing> entry : _markerMap.entrySet()) {
            if (entry.getValue().getID().equals(id)) {
                marker = entry.getKey();
                drawing = entry.getValue();
                break;
            }
        }
        if (marker != null) {
            _markerMap.remove(marker);
            drawing.freeResources();
        }
    }

    private OverlayMarker findMarker(String id) {
        OverlayMarker retval= null;
        for (Map.Entry<OverlayMarker, MarkerDrawing> entry : _markerMap.entrySet()) {
            if (entry.getValue().getID().equals(id)) {
                retval = entry.getKey();
                break;
            }
        }
        return retval;
    }



//======================================================================
//------------------ Inner classes-----------------------
//======================================================================

    private class Mouse extends WebPlotView.DefMouseAll {

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
            _mouseDown = true;
            begin(pv, spt);
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) {
            if (_mouseDown) drag(pv, spt);
            handleMouseMove(pv, spt);
        }

        @Override
        public void onMouseUp(WebPlotView pv, ScreenPt spt) {
            if (_mouseDown) {
                _mouseDown = false;
                end(pv);
            }
        }

        @Override
        public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
            _mouseDown = true;
            begin(pv, spt);
        }

        @Override
        public void onTouchMove(WebPlotView pv, ScreenPt spt, TouchMoveEvent ev) {
            if (_mouseDown) drag(pv, spt);
        }

        @Override
        public void onTouchEnd(WebPlotView pv) {
            if (_mouseDown) {
                _mouseDown = false;
                end(pv);
            }
        }
    }

    private void handleMouseMove(WebPlotView pv, ScreenPt spt) {
        disToCenter.setText((spt.getIX() - spt.getIY()) + " arsecs");
    }

    private void handleCenterChanged(WorldPt center) {
        centerPos.setText(center.toString());
        
    }
    private void handleRotationChanged(double rot) {
        if(angle!=null){
        	angle.setValue(""+Math.toDegrees(rot));
        }
        
    }
    private class MarkerDrawing {
        private MarkerConnect connect;
        private DrawingManager drawMan;
        private DeferredDrawer _dd= new DeferredDrawer();
        private String _color;
        private String _id;

        private MarkerDrawing() {
            mCnt++;
            _id = BASE_DRAWER_ID +footprintDef.name()+"#"+ mCnt;
            connect = new MarkerConnect(BASE_TITLE + footprintDef.name()+" #"+mCnt);
            if (!WebLayerItem.hasUICreator(_id)) {
                WebLayerItem.addUICreator(_id, new MarkerUICreator());
            }
            drawMan = new DrawingManager(_id, connect);
            drawMan.setHelp(_selHelpText);
            drawMan.showMouseHelp(getPlotView());
            List<MiniPlotWidget> mpwList = getAllActivePlots();
            for (MiniPlotWidget mpw : mpwList) drawMan.addPlotView(mpw.getPlotView());
        }

        public void freeResources() {
            List<MiniPlotWidget> mpwList = AllPlots.getInstance().getAll();
            for (MiniPlotWidget mpw : mpwList) drawMan.removePlotView(mpw.getPlotView());
            connect = null;
            drawMan = null;
        }

        public MarkerConnect getConnect() {
            return connect;
        }

        public DrawingManager getDrawMan() {
            return drawMan;
        }

        public void saveColor() {
            _color = drawMan.getActiveColor();
        }

        public void restoreColor() {
            if (_color != null) drawMan.setDefaultColor(_color);
        }

        public String getID() {
            return _id;
        }
        public void deferDraw(WebPlot plot, OverlayMarker marker) {
            _dd.cancel();
            _dd.draw(plot,marker,drawMan);
        }
        public void cancelDeferred() { _dd.cancel(); }
    }


    private class MarkerConnect extends SimpleDataConnection {
        private List<DrawObj> _mainData;
        private List<DrawObj> _editData;
        private WebPlot _editingPlot = null;

        MarkerConnect(String title) {
            super(title, _editHelpText);
        }

        @Override
        public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
            List<DrawObj> retval;
            if (plot==null) return _mainData;

            if (plot != _editingPlot) {
                retval = _mainData;
            } else {
                if (_mainData!=null) {
                    retval = new ArrayList<DrawObj>(_mainData.size() + _editData.size());
                    retval.addAll(_mainData);
                    retval.addAll(_editData);
                }
                else {
                    retval= Collections.emptyList();
                }
            }
            return retval;
        }

        public void setData(List<DrawObj> mainData, List<DrawObj> editData, WebPlot editingPlot) {
            _mainData = mainData;
            _editData = editData;
            _editingPlot = editingPlot;
        }

        public void clearData() {
            _mainData = null;
            _editData = null;
            _editingPlot = null;
        }

        @Override
        public boolean getHasPerPlotData() {
            return true;
        }
    }


    private class MarkerUICreator extends WebLayerItem.UICreator {

        private MarkerUICreator() { super(true,true);
        }

        public Widget makeExtraUI(final WebLayerItem item) {
            Label add = GwtUtil.makeLinkButton("Add Footprint", "Add a Footprint", new ClickHandler() {
                public void onClick(ClickEvent event) {
                    changeMode(Mode.ADD_FOOTPRINT);
                }
            });
//            Label remove = GwtUtil.makeLinkButton("remove", "remove marker", new ClickHandler() {
//                public void onClick(ClickEvent event) {
//                    removeMarker(item.getID());
//                    if (_markerMap.size()==0) changeMode(Mode.OFF);
//                }
//            });
//            StringFieldDef fd= new StringFieldDef("JwstFootprintCmd.title");

            final SimpleInputField field= SimpleInputField.createByProp("JwstFootprint.title");
            field.setInternalCellSpacing(1);
            final InputField tIf= ((ValidationInputField)field.getField()).getIF();
            final String originalTitle= item.getTitle();
            TextBox tb= ((TextBoxInputField)tIf).getTextBox();

            OverlayMarker marker= findMarker(item.getID());
            if (marker!=null) {
                String t= marker.getTitle();
                field.setValue(t);
                if (!StringUtils.isEmpty(t)) item.setTitle(t);
            }

            final SimpleInputField corner= SimpleInputField.createByProp("JwstFootprint.corner");
            corner.setInternalCellSpacing(1);

            angle= SimpleInputField.createByProp("JwstFootprint.angle");
            angle.setInternalCellSpacing(1);


            GwtUtil.setStyle(add, "padding", "5px 0 0 0");

            HorizontalPanel xui = new HorizontalPanel();
            FlowPanel p1 = new FlowPanel();
            p1.add(field);
            p1.add(corner);

            FlowPanel p2 = new FlowPanel();
            p2.add(angle);
            p2.add(add);

            FlowPanel p3 = new FlowPanel();
            HorizontalPanel center = new HorizontalPanel();
            center.add(new Label("center:"));
            center.add(centerPos);
            HorizontalPanel dtc = new HorizontalPanel();
            dtc.add(new Label("DTC   :"));
            dtc.add(disToCenter);
            p3.add(center);
            p3.add(dtc);

            xui.add(p1);
            xui.add(p2);
            xui.add(p3);


            tb.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    DeferredCommand.addCommand(new Command() {
                        public void execute() { updateTitle(item,  tIf.getValue(),
                                                            corner.getValue(),
                                                            item.getID(),
                                                            originalTitle); }
                    });
                }
            });
            corner.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> ev) {
                    updateTitle(item, tIf.getValue(), corner.getValue(), item.getID(), originalTitle);
                }
            });



            return xui;
        }

        private void updateTitle(WebLayerItem item,
                                 String title,
                                 String cStr,
                                 String id,
                                 String originalTitle) {

            OverlayMarker.Corner corner;
            try {
                corner = Enum.valueOf(OverlayMarker.Corner.class, cStr);
            } catch (Exception e) {
                corner= OverlayMarker.Corner.SE;
            }

            item.setTitle(StringUtils.isEmpty(title) ? originalTitle : title);
            OverlayMarker marker= findMarker(id);
            setActiveMarker(marker);
            marker.setTitle(title);
            marker.setTitleCorner(corner);
            WebPlotView pv= getPlotView();
            updateData(_activeMarker,pv.getPrimaryPlot(),false);
            _markerMap.get(_activeMarker).getDrawMan().redraw();
        }
        public void delete(WebLayerItem item) {
            removeMarker(item.getID());
            if (_markerMap.size()==0) changeMode(Mode.OFF);
        }
    }


    private class DeferredDrawer extends Timer {
        private DrawingManager drawer;
        private WebPlot plot;

        private void draw(WebPlot plot, OverlayMarker marker, DrawingManager drawer) {
            this.drawer= drawer;
            this.plot= plot;
            updateData(_activeMarker, plot, true);
            drawer.redraw();
            schedule(2000);
        }

        @Override
        public void run() {
            updateData(_activeMarker, plot, false);
            drawer.redraw();
        }
    }
}