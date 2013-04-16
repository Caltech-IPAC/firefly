package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;

import java.util.List;


public class RotateNorthCmd extends BaseGroupVisCmd {

    public static final String CommandName= "RotateNorth";
    private final String _onIcon= CommandName+".on.Icon";
    private final String _offIcon= CommandName+".Icon";
    public RotateNorthCmd() {
        super(CommandName);
        updateIcon(getMiniPlotWidget());


        AllPlots.getInstance().getEventManager().addListener(Name.FITS_VIEWER_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateIcon(getMiniPlotWidget());
            }
        });
        AllPlots.getInstance().getEventManager().addListener(Name.PRIMARY_PLOT_CHANGE,  new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateIcon(getMiniPlotWidget());
            }
        });



    }


    protected void doExecute() {

        List<MiniPlotWidget> mpwList= getGroupActiveList();
        if (mpwList.size()>0) {
            for(MiniPlotWidget mpwItem : getGroupActiveList()) {
                if (canRotate(mpwItem)) {
                    mpwItem.setRotateNorth(!isNorth(mpwItem));
                    updateIcon(mpwItem);
                }
            }
        }
    }

    private void updateIcon(MiniPlotWidget mpw)  {
        setIconProperty(isNorth(mpw) ? _onIcon : _offIcon);
    }

    private boolean isNorth(MiniPlotWidget mpw) {
        boolean retval= false;
        if (mpw!=null && mpw.getCurrentPlot()!=null) {
            WebPlot p= mpw.getCurrentPlot();
            if (p.getRotationType()== PlotState.RotateType.NORTH ||
                (p.isRotated() && VisUtil.isPlotNorth(p)) ) {
                retval= true;
            }
        }
        return retval;
    }

    private static boolean canRotate(MiniPlotWidget mpw) {
        boolean retval= true;
        WebPlot plot= mpw.getCurrentPlot();
        if (plot!=null && !plot.isRotatable()) {
            PopupUtil.showInfo(plot.getNonRotatableReason());
            retval= false;
        }
        return retval;
    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals(_onIcon))  {
                return new Image(ic.getRotateNorthOn());
            }
            else if (iStr.equals(_offIcon))  {
                return new Image(ic.getRotateNorth());
            }
        }
        return null;
    }
}