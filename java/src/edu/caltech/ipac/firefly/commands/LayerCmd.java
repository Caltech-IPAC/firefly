package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.ui.WebLayerControlPopup;


public class LayerCmd extends BaseGroupVisCmd {

    public static final String CommandName= "layer";
    private static int _totalClickCnt= 0;
    private static final String _brightIcon= CommandName+".bright.Icon";
    private static final String _dimIcon= CommandName+".Icon";
    private final WebLayerControlPopup.AsyncCreator _creator;


//    private Timer _timer= new Timer() {
//        public void run() {
//            if (_totalClickCnt<4) {
//                if (getPlotView()!=null) {
//                    getPlotView().showMouseHelp(getLayerHelp("More options: click the <i>layer</i> button"));
//                }
//            }
//        }
//    };

    public Widget getLayerHelp(String s) {
        Image im= createCmdImage();
        HorizontalPanel fp= new HorizontalPanel();
        fp.add(im);
        fp.add(new HTML(s));
        return fp;
    }

    @Override
    protected boolean computeEnabled() {
        if (getPlotView()!=null) {
            setBadgeCount(getPlotView().getUserDrawerLayerListSize());
        }
        return super.computeEnabled();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public LayerCmd() {
        super(CommandName);
        LayerListener _listener= new LayerListener();
        AllPlots.getInstance().addListener(Name.LAYER_ITEM_ADDED,_listener);
        AllPlots.getInstance().addListener(Name.LAYER_ITEM_REMOVED,_listener);
        AllPlots.getInstance().addListener(Name.FITS_VIEWER_CHANGE,_listener);
//        changeAlertLevel();
        _creator= new WebLayerControlPopup.AsyncCreator();

    }


    protected void doExecute() {
        _totalClickCnt++;
        _creator.showOrHide();
    }

//    private void changeAlertLevel()  {
//        if (getPlotView()!=null && getPlotView().getUserDrawerLayerListSize()>0) {
//            this.setIconProperty(_brightIcon);
//        }
//        else {
//            this.setIconProperty(_dimIcon);
//        }
//
//    }



    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals(CommandName+ ".bright.Icon"))  {
                return new Image(ic.getLayerBright());
            }
            else if (iStr.equals(CommandName+".Icon"))  {
                return new Image(ic.getLayer());
            }
        }
        return null;
    }



    private class LayerListener implements WebEventListener {


        public void eventNotify(WebEvent ev) {
//            changeAlertLevel();
            setBadgeCount(getPlotView()==null? 0 : getPlotView().getUserDrawerLayerListSize());
        }
    }
}