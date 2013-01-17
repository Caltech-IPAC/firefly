package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:17:40 AM
 */
public interface PreviewData {

    enum Type { SPECTRUM, FITS }

    public Info createRequestForRow(TableData.Row<String> row,
                                    Map<String, String> metaAttributes,
                                    List<String> columns);
    public boolean getHasPreviewData(String id,
                                     List<String> colNames,
                                     Map<String, String> metaAttributes);
    public String getTabTitle();
    public String getTip();
    public void prePlot(MiniPlotWidget mpw, Map<String, String> metaAttributes);
    public void postPlot(MiniPlotWidget mpw, WebPlot plot);
    public boolean isThreeColor();
    public String getGroup();
    public int getMinWidth();
    public int getMinHeight();
    public PlotRelatedPanel[] getExtraPanels();
    public List<String> getEventWorkerList();
    public boolean getPlotFailShowPrevious();
    public boolean getSaveImageCorners();

    public static class Info {
        private final Type _type;
        private final Map<Band,WebPlotRequest> _reqMap;
        private final boolean _threeColor;

        public Info(Type type, WebPlotRequest request) {
            _type= type;
            _reqMap= new HashMap<Band,WebPlotRequest>(2);
            _reqMap.put(Band.NO_BAND,request);
            _threeColor= false;
        }

        public Info(Type type, Map<Band,WebPlotRequest> reqMap, boolean threeColor) {
            _type= type;
            _reqMap= reqMap;
            _threeColor= threeColor;
        }

        public Type getType() { return _type; }

        public Map<Band,WebPlotRequest> getRequestMap() { return _reqMap; }

        public boolean isThreeColor() {return _threeColor;}
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
