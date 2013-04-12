package edu.caltech.ipac.heritage.ui;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.previews.PreviewData;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRelatedPanel;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.heritage.commands.HeritageRequestCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.download.HeritageDOCFileRequest;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:40:59 AM
 */



/**
 * @author Trey Roby
 * @version: $Id: HeritageDOCPreviewData.java,v 1.29 2012/09/24 18:31:04 roby Exp $
 */
public class HeritageDOCPreviewData implements PreviewData {

    private static final String UM=  "&#x3bc;m";
//    private static final String UM=  "\u03bcm";
    private static final WebClassProperties _prop= new WebClassProperties(HeritageDOCPreviewData.class);
    private String _lastReqmode;
    private String _lastDispInstrument;


    public Info createRequestForRow(TableData.Row<String> row, Map<String, String> metaAttributes, List<String> columns) {
        WebPlotRequest request= null;

        if (row!=null) {
            String externalname=row.getValue("depthofcoverage");
            _lastDispInstrument=row.getValue("modedisplayname");
            _lastReqmode =row.getValue("reqmode");

            if (!StringUtils.isEmpty(externalname)) { 
                request= makeRequest(row);

                String reqkey = row.getValue("reqkey");
                request.setTitle((StringUtils.isEmpty(reqkey) ? "" : reqkey+": ")+
                        "Coverage Image");
                request.setTitleOptions(WebPlotRequest.TitleOptions.HEADER_KEY);
                request.setHeaderKeyForTitle("CHANNUM");
            }
        }
        return (request!=null) ? new Info(Type.FITS,request) : null;
    }

    private WebPlotRequest makeRequest(TableData.Row<String> row) {
        String reqkey= row.getValue("reqkey");
        ServerRequest fileRequest = new HeritageDOCFileRequest(reqkey);
        WebPlotRequest wpr= WebPlotRequest.makeProcessorRequest(fileRequest, fileRequest.getRequestId());
        wpr.setZoomType(ZoomType.SMART);
//        WebPlotRequest wpr= makeRequestDUMMY(); // for testing
        RangeValues rv= new RangeValues(RangeValues.MAXMIN,0,RangeValues.MAXMIN,0,RangeValues.STRETCH_LINEAR);
        wpr.setInitialRangeValues(rv);
        return wpr;
    }


    public String getTabTitle() { return _prop.getTitle(); }
    public String getTip() { return _prop.getTip(); }

    public boolean getHasPreviewData(String id, List<String> colNames, Map<String, String> metaAttributes) {
        DataType dType= DataType.parse(metaAttributes.get(HeritageSearch.DATA_TYPE));
        return (dType==DataType.AOR);
    }

    public boolean isThreeColor() { return false; }
    public String getGroup() { return null; }
    public PlotRelatedPanel[] getExtraPanels() { return null; }
    public boolean getSaveImageCorners() { return false; }
    public List<String> getEventWorkerList() { return Arrays.asList(HeritageRequestCmd.ACTIVE_TARGET_ID); }

    public int getMinWidth() { return 0; }
    public int getMinHeight() { return 0; }

    public void prePlot(MiniPlotWidget mpw, Map<String, String> metaAttributes) {
        mpw.setPreferenceColorKey("SHA-DOC-color");
    }

    public void postPlot(MiniPlotWidget mpw, WebPlot plot) {
        int chan;
//        mpw.getPlotView().setAttribute(WebPlot.FLIP_ZOOM_BY_LEVEL, "true");
        mpw.getPlotView().setAttribute(WebPlot.FLIP_ZOOM_TO_FILL, "true");
        mpw.getPlotView().setAttribute(WebPlot.EXPANDED_TO_FIT_TYPE, VisUtil.FullType.WIDTH_HEIGHT.toString());
        for(WebPlot p : plot.getPlotView()) {
            String orgTitle= p.getPlotDesc();
            try {
                chan= Integer.parseInt(orgTitle);
                p.setPlotDesc(getDesc(_lastReqmode,_lastDispInstrument,chan));
            } catch (NumberFormatException e) {
                // don't modify
            }
        }
    }


    public String getDesc(String reqmode, String dispInstr, int chan) {

	//GwtUtil.showScrollingDebugMsg("RBH in HeritageDOCPreviewData.getDesc  reqmode = " + reqmode);
        String retval= "don't know";
        if (reqmode.contains("Irac") || reqmode.contains("IRAC")) {
            switch (chan) {
                case 1 :
                    retval = dispInstr + " 3.6 " + UM;
                    break;
                case 2 :
                    retval = dispInstr + " 4.5 " + UM;
                    break;
                case 3 :
                    retval = dispInstr + " 5.8 " + UM;
                    break;
                case 4 :
                    retval = dispInstr + " 8.0 " + UM;
                    break;
            }
        }
        else if (reqmode.contains("MipsSed")) {
            retval = dispInstr + " 53-100 " + UM;
        }
        else if (reqmode.contains("Mips") || reqmode.contains("MIPS")) {
            switch (chan) {
                case 1 :
                    retval = dispInstr + " 24 " + UM;
                    break;
                case 2 :
                    retval = dispInstr + " 70 " + UM;
                    break;
                case 3 :
                    retval = dispInstr + " 160 " + UM;
                    break;
            }
        }
        else if (reqmode.contains("IrsPeakupImage")) {
            switch (chan) {
                case 0 :
                    retval = dispInstr + " blue 16 " + UM;
                    break;
                case 999 :
                    retval = dispInstr + " red 22 " + UM;
                    break;
            }
        }
        else if (reqmode.contains("Irs") || reqmode.contains("IRS")) {
            switch (chan) {
                case 998 :
                    retval = dispInstr + " blue 16 " + UM;
                    break;
                case 999 :
                    retval = dispInstr + " red 22 " + UM;
                    break;
                case 26 :
                case 28 :
                    retval = dispInstr + " SL 5.2-8.7 " + UM;
                    break;
                case 29 :
                    retval = dispInstr + " SL 5.2-14.5 " + UM;
                    break;
                case 32 :
                case 34 :
                    retval = dispInstr + " SL 7.4-14.5 " + UM;
                    break;
                case 1 :
                    retval = dispInstr + " SH 9.9-19.6 " + UM;
                    break;
                case 38 :
                case 40 :
                    retval = dispInstr + " LL 14.0-21.3 " + UM;
                    break;
                case 44 :
                case 46 :
                    retval = dispInstr + " LL 19.5-38.0 " + UM;
                    break;
                case 41 :
                    retval = dispInstr + " LL 14.0-38.0 " + UM;
                    break;
                case 3 :
                    retval = dispInstr + " LH 18.7-37.2 " + UM;
                    break;
            }
        }
        return retval;

    }

    public boolean getPlotFailShowPrevious() { return false;   }
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
