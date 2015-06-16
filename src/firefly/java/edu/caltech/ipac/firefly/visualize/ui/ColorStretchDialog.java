/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.RangeValues;


/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */


/**
 * @author Trey Roby
 */
public class ColorStretchDialog extends BaseDialog {

    interface PFile extends PropFile { @Source("ColorStretchDialog.prop") TextResource get(); }
    private static final WebClassProperties _prop= new WebClassProperties(ColorStretchDialog.class, (PFile)GWT.create(PFile.class));
    private static final String RANGES_STR= _prop.getName("ranges");
    private static final String THREE_COLOR_HEIGHT= "350px";
    private static final String STANDARD__HEIGHT= "280px";

    public static final int MINMAX_IDX= 0;
    public static final int ZSCALE_IDX= 1;


    private final BandPanel _bandPanel[]= new BandPanel[3];
    private final TabPane<BandPanel> _tabs= new TabPane<BandPanel>();
    private final FlowPanel _topPanel= new FlowPanel();
    private WebPlot _lastPlot= null;
    private Band _lastBandAry[]= null;
    private CheckBox _applyAll= GwtUtil.makeCheckBox(_prop.makeBase("applyAll"));


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ColorStretchDialog(Widget w) {
        super(w, ButtonType.APPLY_HELP, _prop.getTitle(), "visualization.fitsViewer");
        Button applyB= getButton(BaseDialog.ButtonID.APPLY);
        applyB.setText(_prop.getName("replot"));

        AllPlots.getInstance().addListener(new PlotChangeListener(this) );
        updateNewPlot();

        this.addButtonAreaWidgetBefore(_applyAll);
        _applyAll.setVisible(false);

        _topPanel.setSize("360px", "350px");
        this.setWidget(_topPanel);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    @Override
    public void setVisible(boolean v) {
        super.setVisible(v, PopupPane.Align.TOP_LEFT, 0, 45);
    }

    @Override
    protected void onVisible() { updateNewPlot(); }

    //======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void updateNewPlot() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            WebPlot plot= pv.getPrimaryPlot();
            Band bands[]= plot.getPlotState().getBands();

            if (_lastPlot!= plot) {
                createContents(plot);
            }
            else if (_lastBandAry==null) {
                createContents(plot);
            }
            else if (_lastBandAry.length!= bands.length) {
                createContents(plot);
            }
            else {
                for (int i= 0; (i<bands.length); i++) {
                    if (bands[i]!=_lastBandAry[i]) {
                        createContents(plot);
                        break;
                    }
                }
            }
            _lastBandAry= bands;
            _lastPlot= plot;

            for(Band band : bands) {
                int idx= band.getIdx();
                if (_bandPanel[idx]!=null)  _bandPanel[idx].newPlot(plot);
            }

            _applyAll.setVisible(AllPlots.getInstance().getActiveGroup().size()>1 || pv.size()>1);
            _topPanel.setHeight( plot.isThreeColor() ? THREE_COLOR_HEIGHT : STANDARD__HEIGHT);
        }
    }

    private void createContents(WebPlot plot) {

        Band bands[]= plot.getPlotState().getBands();
        for(int i= 0; (i<_bandPanel.length); i++) _bandPanel[i]= null;

        _topPanel.clear();
        if (bands.length==1 && bands[0]==Band.NO_BAND) {
            _bandPanel[0]= new BandPanel(plot,Band.NO_BAND);
            _topPanel.add(_bandPanel[0]);
        }
        else {
            _topPanel.add(_tabs);
            _tabs.setSize("100%", "100%");
            int cnt= _tabs.getWidgetCount();
            for(int i= 0; (i<cnt); i++) {
                _tabs.removeTab(_tabs.getSelectedTab());
            }
            
            String tabName;
            String tabTip;
            for(Band band : bands) {

                String property;
                switch (band) {
                    case RED : property= "red"; break;
                    case GREEN :property= "green"; break;
                    case BLUE : property= "blue"; break;
                    default : property= null; break;
                }

                tabName= _prop.getName(property);
                tabTip= _prop.getTip(property);

                _bandPanel[band.getIdx()]= new BandPanel(plot,band);
                _tabs.addTab(_bandPanel[band.getIdx()], tabName, tabTip, false, true);
            }
            _tabs.selectTab(0);
        }
    }


    @Override
    protected void inputComplete() {

        WebPlotView pv= AllPlots.getInstance().getPlotView();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            WebPlot plot= pv.getPrimaryPlot();
            Band bands[]=  plot.getPlotState().getBands();
            StretchData sd []= new StretchData[bands.length];

            RangeValues rv;
            int i=0;

            int tabIdx= plot.isThreeColor() ? _tabs.getSelectedIndex() : 0;


            for(Band band : bands) {
                int idx= band.getIdx();
                if (_bandPanel[idx]!=null)  {
                    rv= _bandPanel[idx].getRangeValues();
                    //LZ add this line to see if it works
                    plot.getPlotState().setRangeValues(rv,band );
                    sd[i++]= new StretchData(band,rv,_bandPanel[idx].isBandVisible());
                }
            }
            if (_applyAll.isVisible() && _applyAll.getValue()) {
                for (MiniPlotWidget mpw : AllPlots.getInstance().getActiveGroup()) {
                    WebPlotView pvItr= mpw.getPlotView();
                    StretchData stretchDataThree[]=sd;
                    StretchData stretchDataNoBand[]=
                            new StretchData[] {new StretchData(Band.NO_BAND, sd[tabIdx].getRangeValues(), true)};
                    if (sd.length==1) {
                        stretchDataThree= new StretchData[] {
                                new StretchData(Band.RED, sd[0].getRangeValues(), sd[0].isBandVisible()),
                                new StretchData(Band.GREEN, sd[0].getRangeValues(), sd[0].isBandVisible()),
                                new StretchData(Band.BLUE, sd[0].getRangeValues(), sd[0].isBandVisible()),
                                };
                    }
                    for (WebPlot p : pvItr) {
                        if (p.isThreeColor())  WebHistogramOps.recomputeStretch(p, stretchDataThree);
                        else                   WebHistogramOps.recomputeStretch(p, stretchDataNoBand);
                    }
                }
            }
            else {
                WebHistogramOps.recomputeStretch(plot, sd);
            }
        }
    }


    @Override
    protected boolean validateInput() throws ValidationException {

        boolean valid= true;
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            WebPlot plot= pv.getPrimaryPlot();
            for(Band band : plot.getPlotState().getBands()) {
                int idx= band.getIdx();
                if (_bandPanel[idx]!=null) valid= _bandPanel[idx].validateInput();
                if (!valid) break;
            }
        }
        return valid;
    }


// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================



    /**
     * making this class static and passing a parameter makes code splitting happen better
     */
    private static class PlotChangeListener implements WebEventListener {

        private ColorStretchDialog _dialog;

        PlotChangeListener(ColorStretchDialog dialog) { _dialog= dialog; }

        public void eventNotify(WebEvent ev) {
            Name name= ev.getName();
            if (name==Name.REPLOT) {
                ReplotDetails details= (ReplotDetails)ev.getData();
                if (details.getReplotReason()== ReplotDetails.Reason.IMAGE_RELOADED) {
                    if (_dialog.isVisible()) _dialog.updateNewPlot();
                }
            }
            else if (name==Name.FITS_VIEWER_CHANGE) {
                if (_dialog.isVisible()) _dialog.updateNewPlot();
            }

        }
    }



    public static class AsyncCreator {
        private ColorStretchDialog _dialog= null;
        private Widget _w;

        public AsyncCreator(Widget w) { _w= w; }

        public void show() {
            GWT.runAsync( new GwtUtil.DefAsync() {
                public void onSuccess() {
                    Vis.init(AllPlots.getInstance().getMiniPlotWidget(), new Vis.InitComplete() {
                        public void done() {
                            if (_dialog==null) _dialog= new ColorStretchDialog(_w);
                            _dialog.setVisible(true);
                        }
                    });
                }
            });
        }
    }
}
