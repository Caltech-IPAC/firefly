/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 3/30/15
 * Time: 4:10 PM
 */


import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.visualize.draw.ActionReporter;
import edu.caltech.ipac.firefly.visualize.draw.LineSelection;
import edu.caltech.ipac.firefly.visualize.draw.PointSelection;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;

/**
 * @author Trey Roby
 */
public class ExtensionCommands {


    public static abstract class ExtensionBaseCmd extends GeneralCommand {

        protected final ActionReporter actionReporter;
        protected final PlotCmdExtension ext;
        protected final MiniPlotWidget mpw;


        public ExtensionBaseCmd(MiniPlotWidget mpw, PlotCmdExtension ext, ActionReporter actionReporter) {
            super(ext.getTitle());
            this.mpw= mpw;
            this.ext= ext;
            this.actionReporter= actionReporter;
            this.setShortDesc(ext.getTitle());
            if (ext.getImageUrl()!=null) this.setIcon(ext.getImageUrl());
        }

        protected void doExecute() {
            if (actionReporter.isReporting()) {
                String sendVal= getSendValue();
                if (sendVal!=null) {
                    actionReporter.report(ext.getId(),sendVal);
                }
            }
        }

        protected abstract String getSendValue();

        protected String makeIDAndType() {
            return "id : \"" + ext.getId()        + "\"," +
                   "type : \"" + ext.getExtType() + "\",";

        }

    }


    public static class ExtensionPointSelectCmd extends ExtensionBaseCmd {

        public ExtensionPointSelectCmd(MiniPlotWidget mpw, PlotCmdExtension ext, ActionReporter actionReporter) {
            super(mpw,ext,actionReporter);
        }

        protected String getSendValue() {
            PointSelection sel= (PointSelection)mpw.getPlotView().getAttribute(WebPlot.ACTIVE_POINT);
            String sendVal= null;
            if (sel!=null) {
                sendVal= "{" +
                        makeIDAndType()+
                        "pt : \""  + sel.getPt().serialize() + "\"," +
                        "}";
            }
            return sendVal;
        }
    }

    public static class ExtensionLineSelectCmd extends ExtensionBaseCmd {

        public ExtensionLineSelectCmd(MiniPlotWidget mpw, PlotCmdExtension ext, ActionReporter actionReporter) {
            super(mpw,ext,actionReporter);
        }

        protected String getSendValue() {
            LineSelection sel= (LineSelection)mpw.getPlotView().getAttribute(WebPlot.ACTIVE_DISTANCE);
            String sendVal= null;
            if (sel!=null) {
                sendVal= "{" +
                        makeIDAndType()+
                        "pt0 : \""+  sel.getPt1().serialize() + "\"," +
                        "pt1 : \"" + sel.getPt2().serialize() + "\"" +
                        "}";
            }
            return sendVal;
        }
    }

    public static class ExtensionAreaSelectCmd extends ExtensionBaseCmd {

        public ExtensionAreaSelectCmd(MiniPlotWidget mpw, PlotCmdExtension ext, ActionReporter actionReporter) {
            super(mpw,ext,actionReporter);
        }

        protected String getSendValue() {
            RecSelection sel= (RecSelection)mpw.getPlotView().getAttribute(WebPlot.SELECTION);
            String sendVal= null;
            if (sel!=null) {
                sendVal= "{" +
                        makeIDAndType()+
                        "pt0 : \""+  sel.getPt0().serialize() + "\"," +
                        "pt1 : \"" + sel.getPt1().serialize() +"\""  +
                        "}";
            }
            return sendVal;
        }
    }
}
