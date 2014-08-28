package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDropDown;


public class ImageSelectDropDownCmd extends GeneralCommand {
    public static final String COMMAND_NAME = "ImageSelectDropDownCmd";
    private ImageSelectDropDown dropDown;
//    private PlotWidgetFactory widgetFactory= null;
    private boolean useNewPanel;

    public ImageSelectDropDownCmd(boolean useNewPanel) {
        this();
        this.useNewPanel= useNewPanel;
    }

    public ImageSelectDropDownCmd() {
        super(COMMAND_NAME);
//        setPlotWidgetFactory(widgetFactory);
    }
////    public void setPlotWidgetFactory(PlotWidgetFactory widgetFactory) {
//        this.widgetFactory= widgetFactory;
//    }

    protected void doExecute() {
        if (dropDown==null) dropDown= new ImageSelectDropDown(null,useNewPanel);

        dropDown.show();
    }

    public boolean isInProcessOfPlotting() {
        return dropDown!=null ? dropDown.isInProcess() : false;
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
