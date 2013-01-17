package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.commands.AreaStatCmd;
import edu.caltech.ipac.firefly.commands.CenterPlotOnQueryCmd;
import edu.caltech.ipac.firefly.commands.ChangeColorCmd;
import edu.caltech.ipac.firefly.commands.CropCmd;
import edu.caltech.ipac.firefly.commands.DistanceToolCmd;
import edu.caltech.ipac.firefly.commands.ExpandCmd;
import edu.caltech.ipac.firefly.commands.FitsDownloadCmd;
import edu.caltech.ipac.firefly.commands.FitsHeaderCmd;
import edu.caltech.ipac.firefly.commands.FlipImageCmd;
import edu.caltech.ipac.firefly.commands.FlipLeftCmd;
import edu.caltech.ipac.firefly.commands.FlipRightCmd;
import edu.caltech.ipac.firefly.commands.GridCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogCmd;
import edu.caltech.ipac.firefly.commands.LayerCmd;
import edu.caltech.ipac.firefly.commands.LockImageCmd;
import edu.caltech.ipac.firefly.commands.MarkerToolCmd;
import edu.caltech.ipac.firefly.commands.NorthArrowCmd;
import edu.caltech.ipac.firefly.commands.QuickStretchCmd;
import edu.caltech.ipac.firefly.commands.RestoreCmd;
import edu.caltech.ipac.firefly.commands.RotateCmd;
import edu.caltech.ipac.firefly.commands.RotateNorthCmd;
import edu.caltech.ipac.firefly.commands.SelectAreaCmd;
import edu.caltech.ipac.firefly.commands.ShowColorOpsCmd;
import edu.caltech.ipac.firefly.commands.ZoomDownCmd;
import edu.caltech.ipac.firefly.commands.ZoomOriginalCmd;
import edu.caltech.ipac.firefly.commands.ZoomUpCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 10:45:15 AM
 */


/**
 * @author Trey Roby
 */
public class WebVisInit {

    interface ColorTableFile extends WebAppProperties.PropFile { @ClientBundle.Source("colorTable.prop") TextResource get(); }
    interface VisMenuBarFile extends WebAppProperties.PropFile { @ClientBundle.Source("VisMenuBar.prop") TextResource get(); }
    interface ReadoutSideFile extends WebAppProperties.PropFile { @ClientBundle.Source("ReadoutSideCmd.prop") TextResource get(); }
    public static boolean init= false;

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static void loadSharedVisCommands(Map<String, GeneralCommand> commandMap) {

        if (!init) {  // Load Properties
            WebAppProperties appProp= Application.getInstance().getProperties();
            appProp.load((ColorTableFile)GWT.create(ColorTableFile.class) );
            appProp.load((VisMenuBarFile)GWT.create(VisMenuBarFile.class) );
            appProp.load((ReadoutSideFile)GWT.create(ReadoutSideFile.class) );
            init= true;
        }


        commandMap.put(GridCmd.CommandName,         new GridCmd());
        commandMap.put(ZoomDownCmd.CommandName,     new ZoomDownCmd());
        commandMap.put(ZoomUpCmd.CommandName,       new ZoomUpCmd());
        commandMap.put(ZoomOriginalCmd.CommandName, new ZoomOriginalCmd());
        commandMap.put(RestoreCmd.CommandName,      new RestoreCmd());
        commandMap.put(ExpandCmd.CommandName,       new ExpandCmd());
        commandMap.put(SelectAreaCmd.CommandName,   new SelectAreaCmd());
        commandMap.put(FitsHeaderCmd.CommandName,   new FitsHeaderCmd());
        commandMap.put(FitsDownloadCmd.CommandName, new FitsDownloadCmd());
        commandMap.put(ColorTable.CommandName,      new ColorTable());
        commandMap.put(Stretch.CommandName,         new Stretch());
        commandMap.put(LayerCmd.CommandName,        new LayerCmd());
        commandMap.put(RotateNorthCmd.CommandName,  new RotateNorthCmd());
        commandMap.put(RotateCmd.COMMAND_NAME,      new RotateCmd());
        commandMap.put(FlipImageCmd.COMMAND_NAME,   new FlipImageCmd());
        commandMap.put(DistanceToolCmd.CommandName, new DistanceToolCmd());
        commandMap.put(CenterPlotOnQueryCmd.CommandName, new CenterPlotOnQueryCmd());
        commandMap.put(MarkerToolCmd.CommandName,   new MarkerToolCmd());
        commandMap.put(NorthArrowCmd.CommandName,   new NorthArrowCmd());
        commandMap.put(IrsaCatalogCmd.CommandName,  new IrsaCatalogCmd());

        commandMap.put(LockImageCmd.CommandName, new LockImageCmd());
        commandMap.put(ImageSelectCmd.CommandName, new ImageSelectCmd());

        commandMap.put(ShowColorOpsCmd.COMMAND_NAME,  new ShowColorOpsCmd());

        commandMap.put("zscaleLinear",new QuickStretchCmd("zscaleLinear", RangeValues.STRETCH_LINEAR));
        commandMap.put("zscaleLog",   new QuickStretchCmd("zscaleLog",    RangeValues.STRETCH_LOG));
        commandMap.put("zscaleLogLog",new QuickStretchCmd("zscaleLogLog", RangeValues.STRETCH_LOGLOG));

        
        commandMap.put("stretch99",new QuickStretchCmd("stretch99",99F));
        commandMap.put("stretch98",new QuickStretchCmd("stretch98",98F));
        commandMap.put("stretch97",new QuickStretchCmd("stretch97",97F));
        commandMap.put("stretch95",new QuickStretchCmd("stretch95",95F));
        commandMap.put("stretch90",new QuickStretchCmd("stretch90",90F));
        commandMap.put("stretch85",new QuickStretchCmd("stretch85",85F));
        commandMap.put("stretch85",new QuickStretchCmd("stretchSigma",-2F,10F,RangeValues.SIGMA));


        for(int i=0; (i<22); i++) {
            commandMap.put("colorTable"+i, new ChangeColorCmd("colorTable"+i,i));
        }

    }


    public static void loadPrivateVisCommands(Map<String, GeneralCommand> commandMap,
                                             MiniPlotWidget mpw) {

        commandMap.put(CropCmd.CommandName,new CropCmd(mpw));
        commandMap.put(AreaStatCmd.CommandName, new AreaStatCmd(mpw));
        commandMap.put(FlipRightCmd.CommandName,new FlipRightCmd(mpw));
        commandMap.put(FlipLeftCmd.CommandName,new FlipLeftCmd(mpw));
    }




    public static class ColorTable extends MenuGenerator.MenuBarCmd {
        public static final String CommandName= "colorTable";
        public ColorTable() { super(CommandName); }

        @Override
        protected Image createCmdImage() {
            VisIconCreator ic= VisIconCreator.Creator.getInstance();
            String iStr= this.getIconProperty();
            if (iStr!=null && iStr.equals("colorTable.Icon"))  {
                return new Image(ic.getColorTable());
            }
            return null;
        }
    }


    public static class Stretch extends MenuGenerator.MenuBarCmd {
        public static final String CommandName= "stretchQuick";
        public Stretch() { super(CommandName); }

        @Override
        protected Image createCmdImage() {
            VisIconCreator ic= VisIconCreator.Creator.getInstance();
            String iStr= this.getIconProperty();
            if (iStr!=null && iStr.equals("stretchQuick.Icon"))  {
                return new Image(ic.getStretchQuick());
            }
            return null;
        }
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
