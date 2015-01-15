/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 3:45:42 PM
 */


/**
 * @author Trey Roby
 */
public class RotateDialog extends BaseDialog {

    private static final WebClassProperties _prop= new WebClassProperties(RotateDialog.class);

    private final VerticalPanel _topPanel= new VerticalPanel();
    private CheckBox _applyAll= GwtUtil.makeCheckBox(_prop.makeBase("applyAll"));
    private SimpleInputField _angle= SimpleInputField.createByProp(_prop.makeBase("angle"));


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public RotateDialog() {
        super(RootPanel.get(), ButtonType.APPLY_HELP, _prop.getTitle(), "visualization.Rotate");
        Button applyB= getButton(ButtonID.APPLY);
        applyB.setText(_prop.getName("replot"));

        createContents();
        AllPlots.getInstance().addListener(new PlotChangeListener(this) );
        updateNewPlot();


        this.addButtonAreaWidgetBefore(_applyAll);
        _applyAll.setVisible(false);

//        _topPanel.setSize("360px", "300px");
        setWidget(_topPanel);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    @Override
    public void setVisible(boolean v) {
        super.setVisible(v, PopupPane.Align.TOP_LEFT, 200, 45);
    }

    @Override
    protected void onVisible() { updateNewPlot(); }

    //======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void updateNewPlot() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            _applyAll.setVisible(AllPlots.getInstance().getActiveGroup().size()>1 || pv.size()>1);
        }
    }

    private void createContents() {
        _topPanel.add(_angle);


    }


    @Override
    protected void inputComplete() {

        WebPlotView pv= AllPlots.getInstance().getPlotView();
        AllPlots.getInstance().disableWCSMatch();
        if (pv!=null && pv.getPrimaryPlot()!=null) {
            WebPlot plot= pv.getPrimaryPlot();

            if (_applyAll.isVisible() && _applyAll.getValue()) {
                for (MiniPlotWidget mpw : AllPlots.getInstance().getActiveGroup()) {
                    WebPlotView pvItr= mpw.getPlotView();
                    for (WebPlot p : pvItr) {
                        rotatePlot(mpw, p);
                    }
                }
            }
            else {
                rotatePlot(pv.getMiniPlotWidget(), plot);
            }
        }

    }


    private void rotatePlot(MiniPlotWidget mpw, WebPlot plot) {
        if (_angle.getField().getNumberValue().intValue()!=0) {
            double angleValue= _angle.getField().getNumberValue().doubleValue();
            VisTask.getInstance().rotate(plot, true, angleValue, -1, mpw);
        }
        else {
            VisTask.getInstance().rotate(plot, false, Double.NaN, -1, mpw);
        }

    }


    @Override
    protected boolean validateInput() throws ValidationException {

        boolean valid= _angle.validate();
        return valid;
    }


// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================



    /**
     * making this class static and passing a parameter makes code splitting happen better
     */
    private static class PlotChangeListener implements WebEventListener {

        private RotateDialog _dialog;

        PlotChangeListener(RotateDialog dialog) { _dialog= dialog; }

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


}
