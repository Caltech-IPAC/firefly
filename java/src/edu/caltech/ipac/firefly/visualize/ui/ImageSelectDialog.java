package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */


/**
 * @author Trey Roby
 */
public class ImageSelectDialog extends BaseDialog {

    private enum PlotType {Normal, ThreeColorOps, ThreeColorPanel}
    private static final int TARGET_PANEL= 0;
    private static final int TARGET_DESC= 1;
    private static final int TARGET_HIDDEN= 2;


    interface PFile extends PropFile { @Source("ImageSelectDialog.prop") TextResource get(); }


    private static final WebClassProperties _prop= new WebClassProperties(ImageSelectDialog.class, (PFile)GWT.create(PFile.class));
    private static String RANGES_STR= _prop.getName("ranges");
    public static final String NEW3= _prop.getName("use3Color.new3");
    public static final String BAND= _prop.getName("use3Color.band");

    private static final String STANDARD_RADIUS= "StandardRadius";
    private static final String BLANK= "Blank";

//    private final MiniPlotWidget _plotWidget;
    private final TabPane<Panel> _tabs= new TabPane<Panel>();
    private final SimpleInputField _degreeField= SimpleInputField.createByProp(_prop.makeBase("radius"));
    private final Label _rangesLabel= new Label(RANGES_STR);

    private final List<PlotTypeUI> _plotType= new ArrayList<PlotTypeUI>(10);
    private final DeckPanel _boundryCards= new DeckPanel();
    private final DeckPanel _targetCards= new DeckPanel();
    private final Map<Object,Integer> _cardIdxMap= new HashMap<Object,Integer>();
    private final Label _blankLabel= new Label();
    private final Widget _radiusPanel= createRadiusPanel();
    private final SimpleTargetPanel _targetPanel = new SimpleTargetPanel();
    private final HTML _targetDesc = new HTML();
    private final SimplePanel _targetPanelHolder= new SimplePanel();
    private final boolean _addToHistory;
    private final AsyncCallback<WebPlot> _plotCallback;
    private final CheckBox _use3Color= GwtUtil.makeCheckBox(_prop.makeBase("use3Color"));
    private final SimpleInputField _threeColorBand= SimpleInputField.createByProp(_prop.makeBase("threeColorBand"));
    private final VerticalPanel _tcPanel = new VerticalPanel();
    private final HorizontalPanel _bandRemoveList= new HorizontalPanel();
    private final PlotWidgetFactory plotFactory;
    private final Map<MiniPlotWidget, BandRemoveListener> bandRemoveMap=
                              new HashMap<MiniPlotWidget, BandRemoveListener>();
    private CheckBox createNew;
    private PlotWidgetOps _ops= null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public ImageSelectDialog(PlotWidgetOps ops,
                             String title,
                             boolean addToHistory,
                             AsyncCallback<WebPlot> plotCallback,
                             PlotWidgetFactory plotFactory) {
        super(null, ButtonType.OK_CANCEL_HELP,
              title==null ? _prop.getTitle() : title, "visualization.fitsViewer");

        _addToHistory= addToHistory;
        _plotCallback= plotCallback;
        this.plotFactory = plotFactory;
        createContents(ops);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    protected void onFirstVisible() {
        _blankLabel.setSize(_radiusPanel.getOffsetWidth() +"px",
                            _radiusPanel.getOffsetHeight() +"px");
    }

    @Override
    public void setVisible(boolean v) {
        if (v) {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    MiniPlotWidget mpw = AllPlots.getInstance().getMiniPlotWidget();
                    _ops = (mpw!=null) ? mpw.getOps() : null;
                    PlotWidgetOps ops= (createNew!=null && createNew.getValue()) ? null : _ops;

                    updateToActive(ops);
                    setTargetCard(computeTargetCard());
                    populateBandRemove(ops);
                    updatePlotType(ops);
                }
            });

        }
        super.setVisible(v);
    }



    public int computeTargetCard() {
        int card;
        if (getActivePlotType().usesTarget()) {
            if (isValidPos()) {
                card= TARGET_DESC;
            }
            else {
                card= TARGET_PANEL;
            }
        }
        else {
            card= TARGET_HIDDEN;
        }
        return card;
    }

    int getPlotWidgetWidth() {
        return (_ops!=null) ? _ops.getPlotView().getMiniPlotWidget().getOffsetWidth() : 0;
    }
//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void updatePlotType(PlotWidgetOps ops) {
        WebPlot plot= ops!=null ? ops.getPlotView().getPrimaryPlot() : null;

        PlotTypeUI ptype= getActivePlotType();
        if (ptype.isThreeColor()) {
            setPlotType(PlotType.ThreeColorPanel);
        }
        else {
            if (plot==null) {
                setPlotType(PlotType.Normal);
            }
            else {
                setPlotType(plot.isThreeColor() ? PlotType.ThreeColorOps : PlotType.Normal);
            }
        }
        setHideAlgorythm(ptype.handlesSubmit() ? HideType.DONT_HIDE : HideType.BEFORE_COMPLETE );

    }

    private void updateToActive(PlotWidgetOps ops) {
        ActiveTarget at= ActiveTarget.getInstance();
        ActiveTarget.PosEntry entry= at.getActive();
        if (entry==null || (at.isComputed() || entry.getPt()==null)) {
            WebPlot plot= (ops!=null) ? ops.getPlotView().getPrimaryPlot() : null;
            if (plot!=null) {
                if (plot.containsAttributeKey(WebPlot.FIXED_TARGET)) {
                    Object o= plot.getAttribute(WebPlot.FIXED_TARGET);
                    if (o instanceof ActiveTarget.PosEntry) {
                        entry= (ActiveTarget.PosEntry )o;
                    }
                }
            }
        }
        _targetPanel.setTarget(entry);
    }

    private void createContents(final PlotWidgetOps ops) {

        createTargetPanel();

        ImageSelectDialogTypes sdt= new ImageSelectDialogTypes(this,_prop);

        PlotRelatedPanel ppAry[]= (ops!=null) ? ops.getGroup().getExtraPanels() : null;
        if (ppAry!=null) {
            for (PlotRelatedPanel pp : ppAry) {
                if (pp instanceof PlotTypeUI ) {
                    _plotType.add(((PlotTypeUI)pp));
                }
            }
        }

        _plotType.addAll(sdt.getPlotTypes());

        VerticalPanel vp= new VerticalPanel();
        createTabs(ops);

        int cardIdx=0;
        _boundryCards.add(_radiusPanel);
        _cardIdxMap.put(STANDARD_RADIUS,cardIdx++);
        _boundryCards.add(_blankLabel);
        _cardIdxMap.put(BLANK,cardIdx++);


        for(PlotTypeUI ptype : _plotType) {
            if (!ptype.usesRadius()) {
                Widget w= ptype.getAlternateRadiusWidget();
                if (w!=null) {
                    _boundryCards.add(w);
                    _cardIdxMap.put(ptype,cardIdx++);
                }
            }
        }

        _bandRemoveList.setSpacing(10);

//        _tcPanel.setSpacing(5);
        _tcPanel.add(_use3Color);
        _tcPanel.add(_threeColorBand);
        _tcPanel.add(_bandRemoveList);
        GwtUtil.setStyle(_threeColorBand,"paddingTop", "5px");
        GwtUtil.setStyle(_tcPanel,"padding", "8px 0 0 70px");
        _tcPanel.setWidth("250px");
//        _tcPanel.setHeight("70px");

        updatePlotType(ops);
        _use3Color.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                _threeColorBand.setVisible(_use3Color.getValue());
            }
        });


        HorizontalPanel bottom= new HorizontalPanel();
        bottom.add(_boundryCards);
        bottom.add(_tcPanel);
        bottom.addStyleName("image-select-range-panel");

        if (plotFactory !=null) {
            createNew= GwtUtil.makeCheckBox(plotFactory.getCreateDesc(), plotFactory.getCreateDesc(),true);
            if (ops==null) createNew.setVisible(false);
            createNew.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                public void onValueChange(ValueChangeEvent<Boolean> ev) {
                    PlotWidgetOps ops= ev.getValue() ? null : _ops;
                    updatePlotType(ops);
                    populateBandRemove(ops);
                }
            });
            SimplePanel boxPanel= new SimplePanel();
            boxPanel.setWidget(createNew);
            vp.add(boxPanel);
            GwtUtil.setStyle(boxPanel, "padding", "6px 0 9px 200px");
        }


        vp.add(_targetCards);
        vp.add(_tabs);
//        vp.add(_boundryCards);
        vp.add(GwtUtil.centerAlign(bottom));
        setWidget(vp);

        _tabs.setWidth(575 + "px");
        _tabs.setHeight(75 + "px");
        _rangesLabel.addStyleName("on-dialog-help");
        _tabs.selectTab(0);
        PlotTypeUI ptype= getActivePlotType();
        _tabs.setHeight((ptype.getHeight()+5) + "px");

        _tabs.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent selectionEvent) {
                PlotTypeUI ptype= getActivePlotType();
                _tabs.setHeight((ptype.getHeight()+5) + "px");
            }
        });


        computeRanges();


        //todo change
        if (ops!=null) {
            ops.getPlotView().getEventManager().addListener(Name.REPLOT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (ImageSelectDialog.this.isVisible()) {
                        populateBandRemove(ops);
                    }
                }
            });
        }
    }


    private void populateBandRemove(final PlotWidgetOps ops) {
        WebPlot plot= (ops!=null) ? ops.getCurrentPlot() : null;
        _bandRemoveList.clear();
        if (plot!=null && plot.isThreeColor()) {
            for(Band band : plot.getBands()) {
                final Band removeBand= band;
                Widget b= GwtUtil.makeLinkButton("Remove "+ band.toString(), "", new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        ops.removeColorBand(removeBand);
                    }
                });
                _bandRemoveList.add(b);
            }
        }

    }

    private void setPlotType(PlotType pt) {
        switch (pt) {
            case Normal:
                _use3Color.setValue(false);
                _threeColorBand.setVisible(false);
                _use3Color.setText(NEW3);
                _use3Color.setVisible(true);
                break;
            case ThreeColorOps:
                _use3Color.setVisible(true);
                _use3Color.setValue(true);
                _threeColorBand.setVisible(true);
                _use3Color.setText(BAND);
                break;
            case ThreeColorPanel:
                _use3Color.setVisible(false);
                _threeColorBand.setVisible(false);
                break;

        }
    }

    private boolean isValidPos() { return _targetPanel.getPos()!=null; }

    private void updateTargetDesc() {
//        String name= _targetPanel.getTargetName();
//        String lon= _targetPanel.getLonStr();
//        String lat= _targetPanel.getLatStr();
//        String csys= _targetPanel.getCoordSysStr();
//
//        String s;
//        if (!StringUtils.isEmpty(name)) {
//            s= "Target Name: " + "<b>"+name+"</b>"+ "<br><div class=on-dialog-help>"+
//                    lon +",&nbsp;"+lat+ "&nbsp;&nbsp;"+csys + "</div>";
//        }
//        else {
//            s= lon +",&nbsp;"+lat+ "&nbsp;&nbsp;"+csys;
//        }
        ActiveTarget.PosEntry t= _targetPanel.getTarget();
        String wrapBegin= "<div style=\"text-align: center;\">";
        String wrapEnd= "</div>";
        String s= PositionFieldDef.formatTargetForHelp( t.getName(),t.getResolver(),t.getPt());
        String html= wrapBegin +  s + wrapEnd;

       _targetDesc.setHTML(html);
    }

    private void setTargetCard(int card) {
        if (card==TARGET_PANEL && _targetPanelHolder.getWidget()==null) {
            _targetPanelHolder.setWidget(_targetPanel);
        }
        if (card==TARGET_DESC) updateTargetDesc();

        if (card==TARGET_HIDDEN && _targetCards.getVisibleWidget()==TARGET_DESC)  {
            Widget desc= _targetCards.getWidget(TARGET_DESC);
            Widget hidden= _targetCards.getWidget(TARGET_HIDDEN);
            hidden.setSize(desc.getOffsetWidth() +"px",
                    desc.getOffsetHeight() +"px");
        }


        _targetCards.showWidget(card);
    }

    private void createTargetPanel() {
        HorizontalPanel hp= new HorizontalPanel();
        Widget modTarget= GwtUtil.makeLinkButton(_prop.getTitle("modTarget"),
                                                 _prop.getTip("modTarget"),
                                                 new ClickHandler() {
                                                     public void onClick(ClickEvent ev) {
                                                         setTargetCard(TARGET_PANEL);
                                                     }
                                                 });
        hp.add(_targetDesc);
        hp.setSpacing(5);
        hp.add(modTarget);

        updateTargetDesc();
        _targetCards.add(_targetPanelHolder);
        _targetCards.add(hp);
        _targetCards.add(new Label());
        setTargetCard(TARGET_DESC);
        _targetCards.addStyleName("image-select-target-area");
    }

    private Widget createRadiusPanel() {
        //TODO - write radius panel
        VerticalPanel vp= new VerticalPanel();
//        vp.add(_degreeField);
        vp.add(_degreeField);
        vp.add(_rangesLabel);

        _degreeField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                computeRanges();
            }
        });

        return GwtUtil.centerAlign(vp);
    }

    

    private void computeRanges() {
        int cardIdx;
        PlotTypeUI ptype= getActivePlotType();
        if (ptype.usesRadius()) {
            cardIdx= _cardIdxMap.get(STANDARD_RADIUS);
        }
        else {
            cardIdx= _cardIdxMap.get(_cardIdxMap.containsKey(ptype) ? ptype : BLANK);
        }
        ptype.updateSizeArea();
        _boundryCards.showWidget(cardIdx);
    }


    private PlotTypeUI getActivePlotType() {
        int idx= _tabs.getSelectedIndex();
        return _plotType.get(idx);
    }


    public void updateSizeIfChange(double minDeg,double maxDeg,double defDeg) {

        DegreeFieldDef df = (DegreeFieldDef)_degreeField.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
        double oldMin= df.getMinValue().doubleValue();
        double oldMax= df.getMaxValue().doubleValue();
//        double oldDef= Double.parseDouble(df.getDefaultValue());


        double oldMinDeg= DegreeFieldDef.convert(currentUnits, DegreeFieldDef.Units.DEGREE,
                                           oldMin);
        double oldMaxDeg= DegreeFieldDef.convert(currentUnits, DegreeFieldDef.Units.DEGREE,
                                           oldMax);
//        double oldDefDeg= DegreeFieldDef.convert(currentUnits, DegreeFieldDef.Units.DEGREE,
//                                                 oldDef);
        

        if (Math.abs(oldMinDeg-minDeg)>.001 || Math.abs(oldMaxDeg-maxDeg)>.001)  {
            updateDegField(_ops, minDeg,maxDeg,defDeg);
        }
    }



//
    public void updateDegField(PlotWidgetOps ops, double minDeg,double maxDeg,double defDeg) {
        DegreeFieldDef df = (DegreeFieldDef)_degreeField.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
        String unitDesc= DegreeFieldDef.getUnitDesc(currentUnits);
//        double currentVDeg= df.getDegreeValue(df.getDoubleValue(_degreeField.getValue()), currentUnits);
        double currentVDeg= df.getDoubleValue(_degreeField.getValue());

        WebPlot currPlot= ops!=null ? ops.getCurrentPlot() : null;

        if (currPlot!=null && currPlot.containsAttributeKey(WebPlot.REQUESTED_SIZE)) {
            currentVDeg= (Double)currPlot.getAttribute(WebPlot.REQUESTED_SIZE);
        }

        double newVDeg = currentVDeg;


        WebAssert.tst(minDeg!=maxDeg, "problem in computing ranges: minDeg="+
                                      minDeg+" maxDeg= " + maxDeg);

        if (currentVDeg<minDeg || currentVDeg>maxDeg) newVDeg= defDeg;


        double min= DegreeFieldDef.convert(DegreeFieldDef.Units.DEGREE,
                                           currentUnits, minDeg);
        double max= DegreeFieldDef.convert(DegreeFieldDef.Units.DEGREE,
                                           currentUnits, maxDeg);
//        double newV= DegreeFieldDef.convert(DegreeFieldDef.Units.DEGREE,
//                                           currentUnits, newVDeg);

        _rangesLabel.setText(RANGES_STR + df.format(min) +unitDesc +
                             " and " + df.format(max) + unitDesc);

        df.setMinValue(min);
        df.setMaxValue(max);
        _degreeField.setValue(newVDeg+"");
    }

//    private String format(double v) {
//        return v+"";
//    }


    private void createTabs(final PlotWidgetOps ops) {
        for(PlotTypeUI ptype : _plotType) ptype.addTab(_tabs);
        _tabs.selectTab(0);

        _tabs.addSelectionHandler(new SelectionHandler<Integer>() {

            public void onSelection(SelectionEvent<Integer> selectionEvent) {
                computeRanges();
                setTargetCard(computeTargetCard());
                updatePlotType(ops);
            }
        });
    }


    float getStandardPanelDegreeValue() {
        DegreeFieldDef df = (DegreeFieldDef)_degreeField.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
//        double currentVDeg= df.getDegreeValue(df.getDoubleValue(_degreeField.getValue()), currentUnits);
        double currentVDeg= _degreeField.getField().getNumberValue().doubleValue();
        return (float)currentVDeg;
    }

    WorldPt getJ2000Pos() {
        return _targetPanel.getJ2000Pos();
    }


    @Override
    protected void inputComplete() {


        if (createNew.getValue()) {
            final MiniPlotWidget mpw= plotFactory.create();
            plotFactory.prepare(mpw, new Vis.InitComplete() {
                public void done() {
                    determinePlot(mpw.getOps());
                }
            });
        }
        else {
            determinePlot(_ops);
        }
    }

    private void determinePlot(PlotWidgetOps ops) {

        if (ops!=null) {
            WebEventManager evM= ops.getPlotView().getEventManager();
            if (!bandRemoveMap.containsKey(ops.getMPW())) {
                BandRemoveListener l= new BandRemoveListener(ops);
                bandRemoveMap.put(ops.getMPW(),l);
                evM.addListener(Name.REPLOT, l);
            }
        }

        PlotTypeUI ptype= getActivePlotType();
        ops.getPlotView().getEventManager().fireEvent(new WebEvent(this,Name.SELECT_DIALOG_BEGIN_PLOT ));
        if (!ptype.handlesSubmit()) {
            plot(ops, ptype);
        }
        else {
            ptype.submit(ops);
        }
    }

    @Override
    protected void inputCanceled() {
        AllPlots.getInstance().getEventManager().fireEvent(new WebEvent(this,Name.SELECT_DIALOG_CANCEL));
    }

    @Override
    protected boolean validateInput() throws ValidationException {
        PlotTypeUI ptype= getActivePlotType();
        boolean retval= true;

        if (ptype.usesTarget() &&
            _targetCards.getVisibleWidget()==TARGET_PANEL) {
            setHideAlgorythm(HideType.AFTER_COMPLETE);
            if (_targetPanel.isAsyncCallRequired()) {
                retval= false;
                _targetPanel.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
                    public void onFailure(Throwable caught) {
                        if (caught!=null) PopupUtil.showSevereError(caught);
                    }

                    public void onSuccess(List<Param> params) {

                        boolean valid= false;
                        for(Param p : params) {
                            if (p.getName().equals(ReqConst.USER_TARGET_WORLD_PT) &&
                                    p.getValue()!=null) {
                                valid= true;
                            }
                        }
                        if (valid) {
                            if (getHideAlgorythm()==HideType.BEFORE_COMPLETE) setVisible(false);
                            performInputComplete(null);
                        }
                        else {
                            PopupUtil.showError("Error", "You must enter a target to search");
                        }
                    }
                });
            }
            else if (!isValidPos()) {
                throw new ValidationException("You must enter a target to search");
            }

        }

        return retval;
    }

    void plot(PlotWidgetOps ops, PlotTypeUI ptype) {

        boolean expanded= plotFactory!=null && plotFactory.isPlottingExpanded();
        if (ptype.isThreeColor()) {
            WebPlotRequest request[]= ptype.createThreeColorRequest();
            for(WebPlotRequest r : request) {
                if (r!=null) {
                    r.setTitle(ptype.getDesc() + " 3 Color");
                    int width= getPlotWidgetWidth();
                    if (r.getZoomType()== ZoomType.TO_WIDTH) {
                         if (width>50)  r.setZoomToWidth(width);
                         else  r.setZoomType(ZoomType.SMART);
                    }
                }
            }
            if (plotFactory !=null && createNew.getValue()) {
                for (int i=0; (i<request.length);i++)  {
                    if (request[i]!=null) request[i]= plotFactory.customizeRequest(ops.getMPW(),request[i]);
                }
            }
            ops.plot3Color(request[0], request[1], request[2],_addToHistory,_plotCallback);
        }
        else {
            WebPlotRequest request= ptype.createRequest();
            if (plotFactory !=null && createNew.getValue()) {
                request= plotFactory.customizeRequest(ops.getMPW(),request);
            }
            request.setTitle(ptype.getDesc());
            if (_use3Color.getValue()) {
                String bandStr= _threeColorBand.getValue();
                Band band= Band.RED;
                if (bandStr.equalsIgnoreCase("red"))        band= Band.RED;
                else if (bandStr.equalsIgnoreCase("green")) band= Band.GREEN;
                else if (bandStr.equalsIgnoreCase("blue"))  band= Band.BLUE;

                if (useAddBand(ops, band)) {
                    ops.addColorBand(request,band,_plotCallback);
                }
                else {
                    ops.plot3Color(request, band, _addToHistory,expanded, _plotCallback);
                }
            }
            else {
                ops.plot(request,_addToHistory,expanded, _plotCallback);
            }
        }
    }

    private boolean useAddBand(PlotWidgetOps ops, Band band) {
        WebPlot plot= (ops!=null) ? ops.getCurrentPlot() : null;
        boolean retval= false;
        if (plot!=null) {
            int numBands= plot.getBands().length;
            retval=  (plot.isThreeColor() && numBands>0 &&
                    !(numBands==1 && band==plot.getFirstBand()));
        }
        return retval;
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================


    public static class AsyncCreator {
        private ImageSelectDialog _dialog= null;
        private final MiniPlotWidget _mpw;
        private final String _title;
        private final boolean _addToHistory;
        private final AsyncCallback<WebPlot> _plotCallback;
        private final PlotWidgetFactory _widgetFactory;

        public AsyncCreator(MiniPlotWidget mpw,
                            String title,
                            boolean addToHistory,
                            AsyncCallback<WebPlot> plotCallback,
                            PlotWidgetFactory widgetFactory) {
            _mpw= mpw;
            _title= title;
            _addToHistory= addToHistory;
            _plotCallback= plotCallback;
            _widgetFactory= widgetFactory;
        }


        public void show() {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    PlotWidgetOps ops= (_mpw!=null) ? _mpw.getOps() : null;
                    if (_dialog==null) {
                        _dialog= new ImageSelectDialog(ops,_title, _addToHistory,
                                                       _plotCallback,_widgetFactory);
                    }
                    _dialog.setVisible(true);
                }
            });
        }

    }


    class BandRemoveListener implements WebEventListener {
        PlotWidgetOps ops;

        BandRemoveListener(PlotWidgetOps ops) {
            this.ops = ops;
        }

        public void eventNotify(WebEvent ev) {
            if (ImageSelectDialog.this.isVisible()) {
                populateBandRemove(ops);
            }
        }
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
