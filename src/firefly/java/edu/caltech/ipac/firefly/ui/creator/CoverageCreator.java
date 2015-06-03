/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.previews.AbstractCoverageData;
import edu.caltech.ipac.firefly.ui.previews.CoverageData;
import edu.caltech.ipac.firefly.ui.previews.CoveragePreview;
import edu.caltech.ipac.firefly.ui.previews.SimpleCoverageData;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 15, 2010
 * Time: 11:41:20 AM
 */



/**
 * @author Trey Roby
 */
public class CoverageCreator implements ObsResultCreator {

    public static final String QUERY_ID= "QUERY_ID";

    public TablePreview create(Map<String, String> params) {

        List<String> sources= DataViewCreator.getListParam(params,QUERY_ID);



        ZoomType hint= ZoomType.SMART;
        if (params.containsKey(WebPlotRequest.ZOOM_TYPE)) {
            try {
                hint= Enum.valueOf(ZoomType.class, params.get(WebPlotRequest.ZOOM_TYPE));
            } catch (Exception e) {
                hint= ZoomType.SMART;
            }
        }
        SimpleCoverageData covData= new SimpleCoverageData(sources,hint);
        covData.setEventWorkerList(DataViewCreator.getListParam(params,"EVENT_WORKER_ID"));

        boolean enableDefColumns=DataViewCreator.getBooleanParam(params,CommonParams.ENABLE_DEFAULT_COLUMNS);
        if (enableDefColumns) {
           covData.enableDefaultColumns();
        }
        else {
            List<String> cenC= DataViewCreator.getListParam(params,CommonParams.CENTER_COLUMNS);
            if (cenC!=null && cenC.size()==2) {
                List<String> corC= DataViewCreator.getListParam(params,CommonParams.CORNER_COLUMNS);
                if (corC!=null && corC.size()==8) {
                    covData.initFallbackCol(cenC.get(0), cenC.get(1),
                                            corC.get(0), corC.get(1),
                                            corC.get(2), corC.get(3),
                                            corC.get(4), corC.get(5),
                                            corC.get(6), corC.get(7) );
                }
                else {
                    covData.initFallbackCol(cenC.get(0),cenC.get(1));
                }
            }

        }

        String group= params.get(CommonParams.PLOT_GROUP);
        if (group!=null)  covData.setGroup(group);




        if (params.containsKey(CommonParams.MIN_SIZE)) {
            String s[]= params.get(CommonParams.MIN_SIZE).split("x",2);
            if (s.length==2) {
                try {
                    int minWidth= Integer.parseInt(s[0]);
                    int minHeight= Integer.parseInt(s[1]);
                    covData.setMinSize(minWidth,minHeight);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            params.remove(CommonParams.MIN_SIZE);
        }


        boolean multi= DataViewCreator.getBooleanParam(params,CommonParams.MULTI_COVERAGE);
        covData.setMultiCoverage(multi);

        boolean catalogsAsOverlays= DataViewCreator.getBooleanParam(params,CommonParams.CATALOGS_AS_OVERLAYS, true);
        covData.setTreatCatalogsAsOverlays(catalogsAsOverlays);

        if (params.containsKey(CommonParams.TITLE)) {
            covData.setTitle(params.get(CommonParams.TITLE));
            covData.setUseTitleForPlot(true);
        }


        if (params.containsKey(CommonParams.SHAPE)) {
            List<String> sList= DataViewCreator.getListParam(params,CommonParams.SHAPE);
            if (sList.size()==1 && !sList.get(0).contains("=")) {
                covData.setShape(AbstractCoverageData.DEFAULT_VALUE,DrawSymbol.getSymbol(sList.get(0)));
            }
            else {
                for(String s : sList) {
                    String sAry[]= s.split("=");
                    if (sAry.length==2) {
                        covData.setShape(sAry[0], DrawSymbol.getSymbol(sAry[1]));
                    }
                }
            }
        }

        if (params.containsKey(CommonParams.COLOR)) {
            List<String> sList= DataViewCreator.getListParam(params,CommonParams.COLOR);
            if (sList.size()==1 && !sList.get(0).contains("=")) {
                covData.setColor(AbstractCoverageData.DEFAULT_VALUE, sList.get(0));
            }
            else {
                for(String s : sList) {
                    String sAry[]= s.split("=");
                    if (sAry.length==2) {
                        covData.setColor(sAry[0], sAry[1]);
                    }
                }
            }
        }

        if (params.containsKey(CommonParams.HIGHLIGHTED_COLOR)) {
            List<String> sList= DataViewCreator.getListParam(params,CommonParams.HIGHLIGHTED_COLOR);
            if (sList.size()==1 && !sList.get(0).contains("=")) {
                covData.setHighlightedColor(AbstractCoverageData.DEFAULT_VALUE, sList.get(0));
            }
            else {
                for(String s : sList) {
                    String sAry[]= s.split("=");
                    if (sAry.length==2) {
                        covData.setHighlightedColor(sAry[0], sAry[1]);
                    }
                }
            }
        }


        if (params.containsKey(CommonParams.SYMBOL_SIZE)) {
            List<String> sList= DataViewCreator.getListParam(params,CommonParams.SYMBOL_SIZE);
            if (sList.size()==1 && !sList.get(0).contains("=")) {
                try {
                    covData.setSymbolSize(AbstractCoverageData.DEFAULT_VALUE, Integer.parseInt(sList.get(0)));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            else {
                for(String s : sList) {
                    String sAry[]= s.split("=");
                    if (sAry.length==2) {
                        try {
                            covData.setSymbolSize(sAry[0], Integer.parseInt(sAry[1]));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        }


        if (params.containsKey(CommonParams.FIT_TYPE)) {
            CoverageData.FitType ft;
            try {
                String v= params.get(CommonParams.FIT_TYPE).toUpperCase();
                ft = Enum.valueOf(CoverageData.FitType.class, v);
            } catch (IllegalArgumentException e) {
                ft= CoverageData.FitType.WIDTH;
            }
            covData.setFitType(ft);
        }


        boolean blank= DataViewCreator.getBooleanParam(params,CommonParams.BLANK);
        covData.setUseBlankPlot(blank);

        if (params.containsKey(WebPlotRequest.GRID_ON)) {
            WebPlotRequest.GridOnStatus retval;
            try {
                String v= params.get(WebPlotRequest.GRID_ON).toUpperCase();
                retval = Enum.valueOf(WebPlotRequest.GridOnStatus.class, v);
            } catch (IllegalArgumentException e) {
                retval = WebPlotRequest.GridOnStatus.FALSE;
            }
            covData.setGridOn(retval);
        }

        if (params.containsKey(WebPlotRequest.OVERLAY_POSITION)) {
            try {
                WorldPt wp = ResolvedWorldPt.parse(params.get(WebPlotRequest.OVERLAY_POSITION));
                if (wp!=null) covData.setQueryCenter(wp);
            } catch (NumberFormatException e) {
                // do nothing
            }

        }

        covData.setMinimalReadout(DataViewCreator.getBooleanParam(params,WebPlotRequest.MINIMAL_READOUT));

        return new CoveragePreview(covData);



    }

}

