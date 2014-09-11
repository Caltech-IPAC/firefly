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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
