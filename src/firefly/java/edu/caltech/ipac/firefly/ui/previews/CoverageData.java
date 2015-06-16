/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.List;


/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:17:40 AM
 */
public interface CoverageData {


    enum CoverageType {X, BOX, BOTH, GUESS}
    enum FitType {WIDTH, WIDTH_HEIGHT}

    String getTitle();
    String getTip();
    boolean getHasCoverageData(TableCtx table);
    String getCoverageBaseTitle(TableCtx panel);
    CoverageType getCoverageType();

    boolean canDoCorners(TableCtx table);
    List<WorldPt[]> modifyBox(WorldPt[] pts, TableCtx table, TableData.Row<String> row);

    TableMeta.LonLatColumns[] getCornersColumns(TableCtx table);
    TableMeta.LonLatColumns getCenterColumns(TableCtx table);
    List<String> getExtraColumns();
    List<String> getEventWorkerList();
    ZoomType getSmartZoomHint();
    String getGroup();
    DrawSymbol getShape(String id);
    int getSymbolSize(String id);
    String getColor(String id);
    String getHighlightedColor(String id);
    boolean isMultiCoverage();
    WebPlotRequest.GridOnStatus getGridOn();
    boolean isMinimalReadout();
    int getMinWidth();
    int getMinHeight();
    boolean getUseBlankPlot();
    boolean isTreatCatalogsAsOverlays();
    WorldPt getQueryCenter();
    FitType getFitType();
    boolean isUseTitleForPlot();
}
