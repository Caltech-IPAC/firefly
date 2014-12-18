package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
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
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.RadioGroupInputField;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
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
public class ImageSelectPanel2 implements ImageSelectAccess {

    private enum PlotType {Normal, ThreeColorOps, ThreeColorPanel}
    private static final int TARGET_PANEL= 0;
    private static final int TARGET_DESC= 1;
    private static final int TARGET_HIDDEN= 2;


    interface PFile extends PropFile { @Source("ImageSelectPanel.prop") TextResource get(); }


    private static final WebClassProperties _prop= new WebClassProperties(ImageSelectPanel.class, (PFile)GWT.create(PFile.class));
    private static String RANGES_STR= _prop.getName("ranges");
    public static final String NEW3= _prop.getName("use3Color.new3");
    public static final String BAND= _prop.getName("use3Color.band");

    private static final String STANDARD_RADIUS= "StandardRadius";
    private static final String BLANK= "Blank";


    private static final String IN_PLACE_STANDARD= " "+ _prop.getName("plotWhere.inPlace");
    private static final String IN_PLACE_3COLOR= " "+ _prop.getName("plotWhere.inPlace.threeColor");


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
    private final CheckBox _use3Color= GwtUtil.makeCheckBox(_prop.makeBase("use3Color"));
    private final SimpleInputField _threeColorBand= SimpleInputField.createByProp(_prop.makeBase("threeColorBand"));
    private final VerticalPanel _tcPanel = new VerticalPanel();
    private final HorizontalPanel _bandRemoveList= new HorizontalPanel();
    private final Map<MiniPlotWidget, BandRemoveListener> bandRemoveMap= new HashMap<MiniPlotWidget, BandRemoveListener>();
    private final Label hideTargetLabel= new Label();
    private SimpleInputField createNew;
    private boolean firstShow= true;
    private Widget  mainPanel= null;
    private final PanelComplete panelComplete;
    private final ImageSelectPanelPlotter plotter;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public ImageSelectPanel2(PanelComplete panelComplete, ImageSelectPanelPlotter plotter) {
        this.panelComplete= panelComplete;
        this.plotter= plotter;
        createContents();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    protected void onFirstVisible() {
        if (firstShow) {
            _blankLabel.setSize(_radiusPanel.getOffsetWidth() +"px",
                                _radiusPanel.getOffsetHeight() +"px");
            firstShow= false;
        }
    }

    private boolean isCreateNew() {
        boolean canCreate= createNew!=null && (createNew.getValue().equals("inNew")) ;
        if (canCreate) {
            MiniPlotWidget mpw = AllPlots.getInstance().getMiniPlotWidget();
            canCreate=  mpw==null ? true : mpw.hasNewPlotContainer();
        }
        return canCreate;
    }

    private void updateCreateOp() {
        if (createNew!=null) {
            RadioGroupInputField  radio= (RadioGroupInputField)createNew.getField();
            if (getCurrentPlot()!=null && getCurrentPlot().isThreeColor()) {
                radio.getRadioButton("inPlace").setHTML(IN_PLACE_3COLOR);
            }
            else {
                radio.getRadioButton("inPlace").setHTML(IN_PLACE_STANDARD);
            }
        }
    }

    public void showPanel() {
        onFirstVisible();
        Vis.init(new Vis.InitComplete() {
            public void done() {
                AllPlots ap= AllPlots.getInstance();
                MiniPlotWidget mpw = ap.getMiniPlotWidget();
                if (createNew!=null) {
                    createNew.setVisible(mpw!=null && mpw.hasNewPlotContainer() && plotter.isCreateNewVisible());
                }

                updateToActive();
                setTargetCard(computeTargetCard());
                populateBandRemove();
                updatePlotType();
                updateCreateOp();
                plotter.showing(_plotType);
            }
        });
    }


    public Widget getMainPanel() {
        return mainPanel;
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

    public int getPlotWidgetWidth() {
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        return mpw!=null ? mpw.getOffsetWidth() : 0;
    }
//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void updatePlotType() {
        WebPlot plot= getCurrentPlot();

        PlotTypeUI ptype= getActivePlotType();
        boolean plotIs3Color=  (plot!=null && plot.isThreeColor());
        setPlotType(plotIs3Color|| _use3Color.getValue()  ? PlotType.ThreeColorOps : PlotType.Normal);
        panelComplete.setHideAlgorythm(ptype.handlesSubmit() ? BaseDialog.HideType.DONT_HIDE :
                                                               BaseDialog.HideType.AFTER_COMPLETE);

        GwtUtil.setHidden(_tcPanel,ptype.isThreeColor());

    }

    private void updateToActive() {
        ActiveTarget at= ActiveTarget.getInstance();
        ActiveTarget.PosEntry entry= at.getActive();
        if (entry==null || (at.isComputed() || entry.getPt()==null)) {
            WebPlot plot= getCurrentPlot();
            if (plot!=null) {
                if (plot.containsAttributeKey(WebPlot.FIXED_TARGET)) {
                    Object o= plot.getAttribute(WebPlot.FIXED_TARGET);
                    if (o instanceof ActiveTarget.PosEntry) {
                        entry= (ActiveTarget.PosEntry )o;
                    }
                }
            }
        }

        boolean updateTargetPanel= true;
        ActiveTarget.PosEntry curr= _targetPanel.getTarget();
        if (curr!=null && entry!=null) {
            WorldPt currWp= curr.getPt();
            WorldPt entryWp= entry.getPt();
            if (currWp!=null && entryWp!=null) {
                if (currWp instanceof ResolvedWorldPt)   currWp= new WorldPt(currWp);
                updateTargetPanel= !currWp.equals(entryWp);
            }

        }
        if (updateTargetPanel) _targetPanel.setTarget(entry);
    }

    private void createContents() {

        createTargetPanel();

        ImageSelectDialogTypes sdt= new ImageSelectDialogTypes(this,_prop);



        PlotTypeUI[] extraPanels= plotter.getExtraPanels();
        if (extraPanels!=null) {
            for (PlotTypeUI pt : plotter.getExtraPanels())  _plotType.add((pt));
        }

        _plotType.addAll(sdt.getPlotTypes());

        VerticalPanel vp= new VerticalPanel();
        createTabs();

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

        updatePlotType();
        _use3Color.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                _threeColorBand.setVisible(_use3Color.getValue());
            }
        });


        HorizontalPanel bottom= new HorizontalPanel();
        bottom.add(_boundryCards);
        bottom.add(_tcPanel);
        bottom.addStyleName("image-select-range-panel");



        createNew= SimpleInputField.createByProp(_prop.makeBase("plotWhere"));

        RadioGroupInputField  radio= (RadioGroupInputField)createNew.getField();
        radio.setPaddingBetween(10);
        radio.getRadioButton("inNew").setHTML(" Create New Plot");


//            createNew= GwtUtil.makeCheckBox(plotFactory.getCreateDesc(), plotFactory.getCreateDesc(),true);
        createNew.setVisible(false);
        createNew.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> ev) {
                updatePlotType();
                populateBandRemove();
            }
        });
        SimplePanel boxPanel= new SimplePanel();
        boxPanel.setWidget(createNew);
        vp.add(boxPanel);
        GwtUtil.setStyle(boxPanel, "padding", "6px 0 9px 200px");


        vp.add(_targetCards);
        vp.add(_tabs);
//        vp.add(_boundryCards);
        vp.add(GwtUtil.centerAlign(bottom));

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
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        if (mpw!=null && mpw.getPlotView()!=null) {
            mpw.getPlotView().addListener(Name.REPLOT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (GwtUtil.isOnDisplay(mainPanel)) {
                        populateBandRemove();
                    }
                }
            });
        }

        mainPanel= vp;
    }


    private void populateBandRemove() {
        WebPlot plot= getCurrentPlot();
        _bandRemoveList.clear();
        if (plot!=null && plot.isThreeColor()) {
            for(Band band : plot.getBands()) {
                final Band removeBand= band;
                Widget b= GwtUtil.makeLinkButton("Remove "+ band.toString(), "", new ClickHandler() {
                    public void onClick(ClickEvent event) {
                       plotter.remove3ColorIDBand(removeBand);
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
        if (card==TARGET_DESC) {
            updateTargetDesc();
        }

        int hideHeight= Math.max(_targetPanelHolder.getOffsetHeight(), _targetDesc.getOffsetHeight());
        int oldHeight= hideTargetLabel.getOffsetHeight();
        hideTargetLabel.setPixelSize(10, hideHeight>oldHeight ? hideHeight : oldHeight);


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


        _targetPanelHolder.setWidget(_targetPanel);

        _targetCards.add(_targetPanelHolder);
        _targetCards.add(hp);
        _targetCards.add(hideTargetLabel);
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
            updateDegField(minDeg,maxDeg,defDeg);
        }
    }


    public String getCurrentPlotID() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return pv!=null ? (String)pv.getAttribute(WebPlotView.GRID_ID) : null;
    }


    public WebPlot getCurrentPlot() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return (pv==null) ? null : pv.getPrimaryPlot();
    }


//
    public void updateDegField(double minDeg,double maxDeg,double defDeg) {
        DegreeFieldDef df = (DegreeFieldDef)_degreeField.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
        String unitDesc= DegreeFieldDef.getUnitDesc(currentUnits);
//        double currentVDeg= df.getDegreeValue(df.getDoubleValue(_degreeField.getValue()), currentUnits);
        double currentVDeg= df.getDoubleValue(_degreeField.getValue());

        WebPlot currPlot= getCurrentPlot();

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


    private void createTabs() {
        for(PlotTypeUI ptype : _plotType) ptype.addTab(_tabs);
        _tabs.selectTab(0);

        _tabs.addSelectionHandler(new SelectionHandler<Integer>() {

            public void onSelection(SelectionEvent<Integer> selectionEvent) {
                computeRanges();
                setTargetCard(computeTargetCard());
                updatePlotType();
            }
        });
    }


    public float getStandardPanelDegreeValue() {
        DegreeFieldDef df = (DegreeFieldDef)_degreeField.getFieldDef();
        DegreeFieldDef.Units currentUnits= df.getUnits();
//        double currentVDeg= df.getDegreeValue(df.getDoubleValue(_degreeField.getValue()), currentUnits);
        double currentVDeg= _degreeField.getField().getNumberValue().doubleValue();
        return (float)currentVDeg;
    }

    public WorldPt getJ2000Pos() {
        return _targetPanel.getJ2000Pos();
    }


    public void inputComplete() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                determinePlot();
            }
        });
    }


    private void determinePlot() {

        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        if (mpw!=null && !bandRemoveMap.containsKey(mpw)) {
            BandRemoveListener l= new BandRemoveListener();
            bandRemoveMap.put(mpw,l);
            mpw.getPlotView().addListener(Name.REPLOT, l);
        }
//        mpw.getPlotView().fireEvent(new WebEvent(this,Name.SELECT_DIALOG_BEGIN_PLOT ));

        PlotTypeUI ptype= getActivePlotType();
        if (!ptype.handlesSubmit()) {
            plot(null, ptype);
        }
        else {
            ptype.submit(null);
        }
    }

    public void inputCanceled() {
        AllPlots.getInstance().fireEvent(new WebEvent(this,Name.SELECT_DIALOG_CANCEL));
    }

    public boolean validateInput() throws ValidationException {
        PlotTypeUI ptype= getActivePlotType();
        boolean retval= true;

        if (ptype.usesTarget() &&
            _targetCards.getVisibleWidget()==TARGET_PANEL) {
            panelComplete.setHideAlgorythm(BaseDialog.HideType.AFTER_COMPLETE);
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
                            if (panelComplete.getHideAlgorythm()== BaseDialog.HideType.BEFORE_COMPLETE) {
                                hide();
                            }
                            panelComplete.performInputComplete();
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


    public void plot(PlotWidgetOps ops, PlotTypeUI ptype) {
        boolean three= _use3Color.getValue();
        Band band= Band.NO_BAND;
        if (three) {
            String bandStr= _threeColorBand.getValue();
            band= Band.RED;
            if (bandStr.equalsIgnoreCase("red"))        band= Band.RED;
            else if (bandStr.equalsIgnoreCase("green")) band= Band.GREEN;
            else if (bandStr.equalsIgnoreCase("blue"))  band= Band.BLUE;
        }
        plotter.doPlot(ops,ptype,isCreateNew(),three, band);
    }





    private boolean useAddBand(Band band) {
        WebPlot plot= getCurrentPlot();
        boolean retval= false;
        if (plot!=null) {
            int numBands= plot.getBands().length;
            retval=  (plot.isThreeColor() && numBands>0 &&
                    !(numBands==1 && band==plot.getFirstBand()));
        }
        return retval;
    }

    public void hide() {
        panelComplete.hide();
    }


    class BandRemoveListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            if (GwtUtil.isOnDisplay(mainPanel)) {
                populateBandRemove();
            }
        }
    }

    public interface PanelComplete {
        void performInputComplete();
        BaseDialog.HideType getHideAlgorythm();
        void setHideAlgorythm(BaseDialog.HideType hideType);
        void hide();
    }

}

