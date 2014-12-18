package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDropDown;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectPanelConverterPlotter;

import java.util.HashMap;
import java.util.Map;


public class ImageSelectDropDownCmd extends GeneralCommand {
    public static final String COMMAND_NAME = "ImageSelectDropDownCmd";
    private ImageSelectDropDown fallback;
    private ImageSelectDropDown activeDD= null;

    private final Map<DatasetInfoConverter, ImageSelectDropDown> ddMap= new HashMap<DatasetInfoConverter, ImageSelectDropDown>(3);
    private boolean useNewPanel;
    private ImageSelectDropDown ddCreation = null;

    public ImageSelectDropDownCmd(boolean useNewPanel) {
        this();
        this.useNewPanel= useNewPanel;
    }

    public ImageSelectDropDownCmd() {
        super(COMMAND_NAME);
    }

    protected void doExecute() {
        doExecuteDynamic(true);
    }


    public void doExecuteDynamic(boolean useCreatorDD) {
        activeDD= null;
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        if ((useCreatorDD || mpw==null || mpw.getPlotView()==null)  && ddCreation !=null) {
            activeDD= ddCreation;
        }
        else {
            DatasetInfoConverter info= getKey(mpw);
            if (info!=null) {
                activeDD = ddMap.get(info);
                if (activeDD==null) {
                    ImageSelectPanelConverterPlotter plotter= new ImageSelectPanelConverterPlotter(info);
                    activeDD= new ImageSelectDropDown(null,true,plotter);
                    ddMap.put(info,activeDD);
                }
            }
        }

        if (activeDD==null) {
            if (fallback==null) fallback= new ImageSelectDropDown(null,false,null);
            activeDD = fallback;
        }

        activeDD.show();
    }



    public boolean isInProcessOfPlotting() {
        return activeDD!=null ? activeDD.isInProcess() : false;
    }

    public void addImageSelectDropDown(DatasetInfoConverter info, ImageSelectDropDown dd) {
        ddMap.put(info,dd);
    }
    
    public void setDatasetInfoConverterForCreation(ImageSelectDropDown dd) {
        ddCreation = dd;
    }

    private DatasetInfoConverter getKey(MiniPlotWidget mpw) {
        DatasetInfoConverter retval= null;
        if (mpw!=null && mpw.getPlotView()!=null) {
            retval= (DatasetInfoConverter)mpw.getPlotView().getAttribute(WebPlotView.DATASET_INFO_CONVERTER);
        }
        return retval;
    }

    
}
