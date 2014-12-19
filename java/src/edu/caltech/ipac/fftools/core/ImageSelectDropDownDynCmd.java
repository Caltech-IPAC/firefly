package edu.caltech.ipac.fftools.core;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDropDown;


@Deprecated
public class ImageSelectDropDownDynCmd extends GeneralCommand {
    public static final String COMMAND_NAME = "ImageSelectDropDownDynCmd";
    private ImageSelectDropDown dropDown;
    private final StandaloneUI aloneUI;


    public ImageSelectDropDownDynCmd(StandaloneUI aloneUI) {
        super(COMMAND_NAME);
        this.aloneUI= aloneUI;
    }


    protected void doExecute() {
        ImageSelectPanelDynPlotter plotter= new ImageSelectPanelDynPlotter(ConverterStore.get(ConverterStore.DYNAMIC).getPlotData());
        if (dropDown==null) dropDown= new ImageSelectDropDown(null,true,plotter);

        aloneUI.ensureDynImageTabShowing();
        dropDown.show();
    }

    public boolean isInProcessOfPlotting() {
        return dropDown!=null ? dropDown.isInProcess() : false;
    }

}
