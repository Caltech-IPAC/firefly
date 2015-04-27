/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.StretchInputField;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebDefaultMouseReadoutHandler;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.RangeValues;
/**
 * User: roby
 * Date: Mar 11, 2010
 * Time: 3:26:14 PM
 */


/**
 * @author Trey Roby
*/
public class BandPanel extends Composite {

    private static final String LINEAR_KEY       = "linear";
    private static final String LOG_KEY          = "log";
    private static final String LOGLOG_KEY       = "loglog";
    private static final String EQUALIZATION_KEY = "equalization";
    private static final String SQUARED_KEY      = "squared";
    private static final String SQRT_KEY         = "sqrt";
    private static final String ASINH_KEY         = "asinh";
    private static final String POWERLAW_GAMMA_KEY         = "powerlaw_gamma";

    private static final String TEMP_GIF=  GWT.getModuleBaseURL()+"images/transparent-20x20.gif";

    interface PFile extends PropFile { @Source("BandPanel.prop") TextResource get(); }
    private static final WebClassProperties _prop= new WebClassProperties(BandPanel.class, (PFile)GWT.create(PFile.class));


    private final SimpleInputField _stretchType= SimpleInputField.createByProp(_prop.makeBase("stretchType.type"));
    private final CheckBox _useZScale= GwtUtil.makeCheckBox(_prop.makeBase("useZScale"));
    private       CheckBox _showBand= null;
    private final StretchInputField _minStretch;
    private final StretchInputField _maxStretch;
    private final StretchInputField _drStretch;
    private final StretchInputField _gammaStretch;

    private final DeckPanel _stPanel   = new DeckPanel();
    //    private final Image             _colorHist = new Image(TEMP_GIF);
    private final Image _dataHist  = new Image(TEMP_GIF);
    private final Image             _cbar      = new Image(TEMP_GIF);
    private final Form _zscaleForm= new Form(false);
    private final Form _rangeForm = new Form(false);
    private boolean     _imageLoaded;
    private final Label _colorHistReadout= new Label();
    private InputField _contrast;
    private InputField  _numSamp;
    private InputField  _sampPerLine;
    private WebHistogramOps _histOps= null;
    private final Band _band;
    private WebPlot _plot;
    private SimpleInputField ifDR;
    private  SimpleInputField ifGamma;

    public BandPanel(WebPlot plot, Band band) {
        _band= band;
        _plot= plot;
        WebFitsData fData= plot.getFitsData(band);
        _minStretch= new StretchInputField(StretchInputField.Type.MIN,fData);
        _maxStretch= new StretchInputField(StretchInputField.Type.MAX,fData);
        _drStretch= new StretchInputField(StretchInputField.Type.DR,fData);
        _gammaStretch= new StretchInputField(StretchInputField.Type.GAMMA,fData);

        createContents();
        _dataHist.setPixelSize(340,55);
    }



    private void createContents() {
        VerticalPanel vp= new VerticalPanel();
        initWidget(vp);

        SimpleInputField ifMin= new  SimpleInputField(_minStretch,new SimpleInputField.Config("100px"),true);
        SimpleInputField ifMax= new  SimpleInputField(_maxStretch,new SimpleInputField.Config("100px"),true);

        ifDR= new  SimpleInputField(_drStretch,new SimpleInputField.Config("100px"),true);
        ifGamma= new  SimpleInputField(_gammaStretch,new SimpleInputField.Config("100px"),true);


        VerticalPanel barPanel= new VerticalPanel();

        DOM.setStyleAttribute(barPanel.getElement(), "border", "1px solid black");
//        barPanel.add(_colorHist);
        barPanel.add(_dataHist);
//        barPanel.add(_cbar);
        Widget cbarCenter= GwtUtil.centerAlign(_cbar);
        DOM.setStyleAttribute(cbarCenter.getElement(), "padding", "5px 1px 5px 1px");


        VerticalPanel fp= new VerticalPanel();
        fp.setSpacing(6);
        fp.add(_useZScale);
        if (_band!=Band.NO_BAND) {
            _showBand= GwtUtil.makeCheckBox(_prop.makeBase("useBand"));
            fp.add(_showBand);
        }


        vp.add(GwtUtil.centerAlign(barPanel));
        vp.add(_colorHistReadout);
        vp.add(GwtUtil.centerAlign(_stretchType));
        vp.add(_stPanel);
        vp.add(GwtUtil.centerAlign(fp));
        vp.add(cbarCenter);


        DOM.setStyleAttribute(_colorHistReadout.getElement(), "padding", "5px 1px 5px 1px");

        VerticalPanel minmaxPanel= new VerticalPanel();
        minmaxPanel.add(ifMin);
        minmaxPanel.add(ifMax);
        minmaxPanel.add(ifDR);
        ifDR.setVisible(false);
        minmaxPanel.add(ifGamma);
        ifGamma.setVisible(false);
        _rangeForm.add(minmaxPanel);



        _contrast= FormBuilder.createField(_prop.makeBase("contrast"));
        _numSamp= FormBuilder.createField(_prop.makeBase("numSamp"));
        _sampPerLine= FormBuilder.createField(_prop.makeBase("sampPerLine"));

        Widget zscalePanel= FormBuilder.createPanel(150, _contrast, _numSamp, _sampPerLine);
        _zscaleForm.add(zscalePanel);

        _stPanel.add(_rangeForm);
        _stPanel.add(_zscaleForm);
        _stPanel.setSize("350px", "11em");


        setUseZScale(false);


        ReadoutMouse rm= new ReadoutMouse();
        _dataHist.addMouseMoveHandler(rm);
        _dataHist.addMouseOutHandler(rm);


        _colorHistReadout.setHeight("1em");

        _dataHist.addLoadHandler(new LoadHandler() {
            public void onLoad(LoadEvent event) { _imageLoaded= true; }
        });

        _useZScale.addValueChangeHandler(new ZScaleChange());


    }



    public void newPlot(WebPlot plot) {
        _plot= plot;
        WebFitsData fData= plot.getFitsData(_band);
        _minStretch.setWebFitsData(fData);
        _maxStretch.setWebFitsData(fData);
        if (_showBand!=null) _showBand.setValue(_plot.getPlotState().isBandVisible(_band));
        updatePlotStatus();
        updateColorHistogram();
        clearReadout();
    }

    private void updatePlotStatus() {
        RangeValues rv= _plot.getPlotState().getRangeValues(_band);
        int sType= rv.getStretchAlgorithm();
        setStretch(sType);
        if (rv.getLowerWhich()==RangeValues.ZSCALE) {
            setUseZScale(true);

        }
        else {
            setUseZScale(false);

            if (rv.getLowerWhich()==RangeValues.PERCENTAGE) {
                _minStretch.setUnits(StretchInputField.Units.PERCENT);
                _minStretch.setValue(rv.getLowerValue()+"");
            }
            else if (rv.getLowerWhich()==RangeValues.SIGMA) {
                _minStretch.setUnits(StretchInputField.Units.SIGMA);
                _minStretch.setValue(rv.getLowerValue()+"");
            }
            else if (rv.getLowerWhich()==RangeValues.ABSOLUTE) {
                _minStretch.setUnits(StretchInputField.Units.DATA);
                _minStretch.setValue(rv.getLowerValue()+"");
            }
            else {
                _minStretch.setUnits(StretchInputField.Units.MINMAX);
            }

            if (rv.getUpperWhich()==RangeValues.PERCENTAGE) {
                _maxStretch.setUnits(StretchInputField.Units.PERCENT);
                _maxStretch.setValue(rv.getUpperValue()+"");
            }
            else if (rv.getUpperWhich()==RangeValues.SIGMA) {
                _maxStretch.setUnits(StretchInputField.Units.SIGMA);
                _maxStretch.setValue(rv.getUpperValue()+"");
            }
            else if (rv.getUpperWhich()==RangeValues.ABSOLUTE) {
                _maxStretch.setUnits(StretchInputField.Units.DATA);
                _maxStretch.setValue(rv.getUpperValue()+"");
            }
            else {
                _maxStretch.setUnits(StretchInputField.Units.MINMAX);
            }

        }


    }

    boolean isBandVisible() {
        return _showBand==null ? true : _showBand.getValue();
    }

    RangeValues getRangeValues() {

        float min= _minStretch.getNumberValue().floatValue();
        float max= _maxStretch.getNumberValue().floatValue();

        _minStretch.getDataType();

        RangeValues range;


        if (_useZScale.getValue()) {
            range= new RangeValues( RangeValues.ZSCALE, min,
                                    RangeValues.ZSCALE, max,
                                    getStretch(),
                                    _contrast.getNumberValue().intValue(),
                                    _numSamp.getNumberValue().intValue(),
                                    _sampPerLine.getNumberValue().intValue());
        }
        else {
            range=  new RangeValues( _minStretch.getDataType(), min,
                                     _maxStretch.getDataType(), max,
                                     getStretch());
        }
        return range;
    }

    boolean validateInput() throws ValidationException {

        boolean valid;
        if (_useZScale.getValue()) {
            valid= _zscaleForm.validate();
        }
        else {
            valid= _rangeForm.validate();
        }
        if (!valid) {
            throw new ValidationException("More fields are not valid");
        }
        return valid;
    }



    void setStretch(int sType) {
        switch (sType) {
            case RangeValues.STRETCH_LINEAR :  _stretchType.setValue(LINEAR_KEY); break;
            case RangeValues.STRETCH_LOG :     _stretchType.setValue(LOG_KEY); break;
            case RangeValues.STRETCH_LOGLOG :  _stretchType.setValue(LOGLOG_KEY); break;
            case RangeValues.STRETCH_EQUAL :   _stretchType.setValue(EQUALIZATION_KEY); break;
            case RangeValues.STRETCH_SQUARED : _stretchType.setValue(SQUARED_KEY); break;
            case RangeValues.STRETCH_SQRT :    _stretchType.setValue(SQRT_KEY); break;
            default:
                assert false;  // if we end up here then there is a new stretch type that has been added
                break;
        }
    }

     int getStretch() {

        String sTypeStr= _stretchType.getValue();
         ifDR.setVisible(false);
         ifGamma.setVisible(false);
        int sType;
        if      (sTypeStr.equals(LINEAR_KEY))       sType= RangeValues.STRETCH_LINEAR;
        else if (sTypeStr.equals(LOG_KEY))          sType= RangeValues.STRETCH_LOG;
        else if (sTypeStr.equals(LOGLOG_KEY))       sType= RangeValues.STRETCH_LOGLOG;
        else if (sTypeStr.equals(EQUALIZATION_KEY)) sType= RangeValues.STRETCH_EQUAL;
        else if (sTypeStr.equals(SQUARED_KEY))      sType= RangeValues.STRETCH_SQUARED;

        else if (sTypeStr.equals(SQRT_KEY))         sType= RangeValues.STRETCH_SQRT;
        else if (sTypeStr.equals(ASINH_KEY))        {
            sType= RangeValues.STRETCH_ASINH;
            ifDR.setVisible(true);
        }
        else if (sTypeStr.equals(POWERLAW_GAMMA_KEY))   {
            sType= RangeValues.STRETCH_POWERLAW_GAMMA;
            ifGamma.setVisible(true);
        }
        else {
            sType= -1;
            assert false;  // if we end up here then there is a new stretch type that has been added
        }

        return sType;
    }


    private void setUseZScale(boolean useZ) {
        _stPanel.showWidget(useZ ? ColorStretchDialog.ZSCALE_IDX : ColorStretchDialog.MINMAX_IDX);
        if (_useZScale.getValue()!=useZ) _useZScale.setValue(useZ);
    }

    private void updateColorHistogram() {
        _histOps= _plot.getHistogramOps(_band);
        _imageLoaded= false;
        _histOps.computeHistogramImage( 340,55, new AsyncCallback<WebPlotResult>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(WebPlotResult r) {
                updateColorBars(r.getStringResult(WebPlotResult.DATA_HIST_IMAGE_URL),
                                r.getStringResult(WebPlotResult.CBAR_IMAGE_URL));
            }
        } );

    }

    private void updateColorBars(String dHistBar,
                                 String cBar) {
        _dataHist.setUrl(createImageUrl(dHistBar));
        _cbar.setUrl(createImageUrl(cBar));
    }

    private String createImageUrl(String imageURL) {
        Param[] params= new Param[] {
                new Param("file", imageURL),
                new Param("type", "any"),
        };
        return WebUtil.encodeUrl(GWT.getModuleBaseURL()+ "sticky/FireFly_ImageDownload", params);
    }



    private void updateDataReadout(int x, int y) {
        if (_imageLoaded && _histOps!=null && _dataHist!=null) {
            int idx= _histOps.getDataHistogramIdxFromScreenIdx(x,_dataHist.getWidth());

            String redspan= "<span style=\"color:red;\">";
            String endRedspan= "</span>";

            _colorHistReadout.getElement().setInnerHTML(
                    "<span style=\"font-size: 9pt\"> Histogram: index: "+
                            redspan +
                            idx +
                            endRedspan +
                            ", Size: "+
                            redspan +
                            _histOps.getDataHistogramValue(idx)+
                            endRedspan +
                            ", Mean Value : " +
                            redspan +
                            WebDefaultMouseReadoutHandler.formatFlux(_histOps.getDataHistogramMean(idx), _plot, _band) +
                                                                        endRedspan +
                            "</span>" );

        }
    }



    private void clearReadout() {
        _colorHistReadout.getElement().setInnerHTML(
                "<span style=\"float:right; padding-right:30;" +
                        "opacity:.4; filter:alpha(opacity=40);\">" +
                        "Move mouse over graph to see values" +
                        "</span>");
    }


    private class ReadoutMouse implements MouseMoveHandler, MouseOutHandler {

        public void onMouseMove(MouseMoveEvent ev) {
            updateDataReadout(ev.getX(), ev.getY());
        }

        public void onMouseOut(MouseOutEvent ev) { clearReadout(); }
    }


    private class ZScaleChange implements ValueChangeHandler<Boolean> {

        public void onValueChange(ValueChangeEvent<Boolean> ev) {
            setUseZScale(ev.getValue());
        }
    }
}

