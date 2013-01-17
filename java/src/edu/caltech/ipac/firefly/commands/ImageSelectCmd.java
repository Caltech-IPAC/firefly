package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageSelectCmd extends BaseGroupVisCmd {
    public static final String CommandName= "ImageSelect";
    private final Map<MiniPlotWidget, ImageSelectDialog.AsyncCreator> _creatorMap=
            new HashMap<MiniPlotWidget, ImageSelectDialog.AsyncCreator>(3);


    public ImageSelectCmd() {
        super(CommandName);
    }

    protected void doExecute() {


        MiniPlotWidget mpw= null;

        if (getActiveList().size()>1) {
            for(MiniPlotWidget mpwOp : getActiveList()) {
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
            if (mpw.isLockImage()) {
                dialogCallback= new AsyncCallback<WebPlot>() {
                    public void onFailure(Throwable caught) { }
                    public void onSuccess(WebPlot result) { mpwForCallback.getPlotView().setLockedHint(true); }
                };
            }
            creator= new ImageSelectDialog.AsyncCreator(mpw,null,true,dialogCallback);
            _creatorMap.put(mpw,creator);
            purge();
        }
        creator.show();


    }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals("ImageSelect.Icon"))  {
                return new Image(ic.getStarrySky());
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