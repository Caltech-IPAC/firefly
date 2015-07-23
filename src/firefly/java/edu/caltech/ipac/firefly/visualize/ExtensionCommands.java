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
import edu.caltech.ipac.firefly.visualize.draw.LineSelection;
import edu.caltech.ipac.firefly.visualize.draw.PointSelection;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class ExtensionCommands {


    public static abstract class ExtensionBaseCmd extends GeneralCommand {

        protected final Ext.Extension ext;
        protected final MiniPlotWidget mpw;


        public ExtensionBaseCmd(MiniPlotWidget mpw, Ext.Extension ext) {
            super(ext.title());
            this.mpw= mpw;
            this.ext= ext;
            this.setShortDesc(ext.title());
            if (ext.imageUrl()!=null) this.setIcon(ext.imageUrl());
        }

        protected void doExecute() {
            Ext.ExtensionResult result= Ext.makeExtensionResult();
            result.setExtValue("id", ext.id());
            result.setExtValue("PlotId", mpw.getPlotId());
            result.setExtValue("type", ext.extType());
            addResultValue(result);
            Ext.fireExtAction(ext,result);
        }

        protected abstract String getSendValue();
        protected abstract void addResultValue(Ext.ExtensionResult result);

        protected String makeIDAndType() {
            return "id : \"" + ext.id()        + "\"," +
                   "type : \"" + ext.extType() + "\",";

        }

    }


    public static class ExtensionPointSelectCmd extends ExtensionBaseCmd {

        public ExtensionPointSelectCmd(MiniPlotWidget mpw, Ext.Extension ext) {
            super(mpw,ext);
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

        @Override
        protected void addResultValue(Ext.ExtensionResult result) {
            PointSelection sel= (PointSelection)mpw.getPlotView().getAttribute(WebPlot.ACTIVE_POINT);
            WebPlot p= mpw.getPlotView().getPrimaryPlot();

            ImagePt ipt= p.getImageCoords(sel.getPt());
            WorldPt wpt= p.getWorldCoords(sel.getPt());

            if (ipt!=null) {
                result.setExtValue("ipt", ipt.serialize());
            }
            if (wpt!=null) {
                result.setExtValue("wpt", wpt.serialize());
            }
            for (Map.Entry<String,String> entry : p.getPlotState().originKeyValues().entrySet()) {
                result.setExtValue(entry.getKey(), entry.getValue());
            }
        }
    }

    public static class ExtensionLineSelectCmd extends ExtensionBaseCmd {

        public ExtensionLineSelectCmd(MiniPlotWidget mpw, Ext.Extension  ext) {
            super(mpw,ext);
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

        @Override
        protected void addResultValue(Ext.ExtensionResult result) {
            LineSelection sel= (LineSelection)mpw.getPlotView().getAttribute(WebPlot.ACTIVE_DISTANCE);
            WebPlot p= mpw.getPlotView().getPrimaryPlot();

            ImagePt ipt0= p.getImageCoords(sel.getPt1());
            ImagePt ipt1= p.getImageCoords(sel.getPt2());

            WorldPt wpt0= p.getWorldCoords(sel.getPt1());
            WorldPt wpt1= p.getWorldCoords(sel.getPt2());


            if (ipt0!=null && ipt1!=null) {
                result.setExtValue("ipt0", ipt0.serialize());
                result.setExtValue("ipt1", ipt1.serialize());
            }
            if (wpt0!=null && wpt1!=null) {
                result.setExtValue("wpt0", wpt0.serialize());
                result.setExtValue("wpt1", wpt1.serialize());

            }
            for (Map.Entry<String,String> entry : p.getPlotState().originKeyValues().entrySet()) {
                result.setExtValue(entry.getKey(), entry.getValue());
            }
        }
    }

    public static class ExtensionAreaSelectCmd extends ExtensionBaseCmd {

        public ExtensionAreaSelectCmd(MiniPlotWidget mpw, Ext.Extension ext) {
            super(mpw,ext);
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

        @Override
        protected void addResultValue(Ext.ExtensionResult result) {
            RecSelection sel= (RecSelection)mpw.getPlotView().getAttribute(WebPlot.SELECTION);
            WebPlot p= mpw.getPlotView().getPrimaryPlot();

            ImagePt ipt0= p.getImageCoords(sel.getPt0());
            ImagePt ipt1= p.getImageCoords(sel.getPt1());

            WorldPt wpt0= p.getWorldCoords(sel.getPt0());
            WorldPt wpt1= p.getWorldCoords(sel.getPt1());


            if (ipt0!=null && ipt1!=null) {
                result.setExtValue("ipt0", ipt0.serialize());
                result.setExtValue("ipt1", ipt1.serialize());
            }
            if (wpt0!=null && wpt1!=null) {
                result.setExtValue("wpt0", wpt0.serialize());
                result.setExtValue("wpt1", wpt1.serialize());

            }

            for (Map.Entry<String,String> entry : p.getPlotState().originKeyValues().entrySet()) {
                result.setExtValue(entry.getKey(), entry.getValue());
            }
        }
    }
}
