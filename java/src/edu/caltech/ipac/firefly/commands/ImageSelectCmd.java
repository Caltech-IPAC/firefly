package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageSelectCmd extends BaseGroupVisCmd implements WebEventListener  {
    public static final String CommandName= "ImageSelect";
    private final Map<MiniPlotWidget, ImageSelectDialog.AsyncCreator> _creatorMap=
            new HashMap<MiniPlotWidget, ImageSelectDialog.AsyncCreator>(3);
    private PlotWidgetFactory widgetFactory= null;
    private ImageSelectDropDownCmd dropDownCmd= null;
    private final static String standardIcon = "ImageSelect.Icon";
    private final static String threeIcon = "ImageSelect.color.Icon";


    public ImageSelectCmd() {
        super(CommandName);
        AllPlots.getInstance().addListener(this);
    }

    protected void doExecute() {

        if (dropDownCmd!=null) {
            dropDownCmd.doExecuteDynamic(false);
            return;
        }
        else {
            MiniPlotWidget mpw= null;

            if (getGroupActiveList().size()>1) {
                for(MiniPlotWidget mpwOp : getGroupActiveList()) {
                    if (mpwOp.isImageSelection()) {
                        mpw= mpwOp;
                        break;
                    }
                }
            }

            if (mpw==null) mpw= getMiniPlotWidget();

            final MiniPlotWidget mpwForCallback= mpw;
            ImageSelectDialog.AsyncCreator creator= _creatorMap.get(mpw);

            if (creator==null) {
                AsyncCallback<WebPlot> dialogCallback= null;
                if (mpw!=null && mpw.isLockImage()) {
                    dialogCallback= new AsyncCallback<WebPlot>() {
                        public void onFailure(Throwable caught) { }
                        public void onSuccess(WebPlot result) { mpwForCallback.getPlotView().setLockedHint(true); }
                    };
                }
                creator= new ImageSelectDialog.AsyncCreator(mpw,null,true,dialogCallback,widgetFactory);
                if (mpw!=null) _creatorMap.put(mpw,creator);
                purge();
            }
            creator.show();
        }




    }


    public void eventNotify(WebEvent ev) {
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        if (mpw!=null && mpw.getCurrentPlot()!=null && mpw.isImageSelection()) {
            setIconProperty(mpw.getCurrentPlot().isThreeColor() ? threeIcon : standardIcon);
        }
    }

    public void setPlotWidgetFactory(PlotWidgetFactory widgetFactory) {
       this.widgetFactory= widgetFactory;
    }

    public void setUseDropdownCmd(ImageSelectDropDownCmd cmd) { dropDownCmd= cmd;  }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals(standardIcon))  {
                return new Image(ic.getFITSNewModifyImage());
            }
            else if (iStr.equals(threeIcon))  {
                return new Image(ic.getFITSModify3Image());
            }
        }
        return null;
    }



    protected boolean computeEnabled() {
        return true;
    }

//    @Override
//    public Image createCmdImage() {
//        VisIconCreator ic= VisIconCreator.Creator.getInstance();
//        String iStr= getIconProperty();
//        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
//            return new Image(ic.getZoomUp());
//        }
//        return null;
//    }

    private void purge() {
        if (_creatorMap.size()>1) {
            List<MiniPlotWidget> delList= new ArrayList<MiniPlotWidget>(5);
            for(MiniPlotWidget mpw : _creatorMap.keySet()) {
                if (!super.getGroup().contains(mpw)) {
                    delList.add(mpw);
                }
            }
            if (delList.size()>0) {
                for(MiniPlotWidget mpw : delList) _creatorMap.remove(mpw);
            }
        }
    }

}