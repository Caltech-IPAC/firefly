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


    public enum CoverageType {X, BOX}
    public enum FitType {WIDTH, WIDTH_HEIGHT}

    public String getTitle();
    public String getTip();
    public boolean getHasCoverageData(TableCtx table);
    public String getCoverageBaseTitle(TableCtx panel);

    public boolean canDoCorners(TableCtx table);
    public List<WorldPt[]> modifyBox(WorldPt[] pts, TableCtx table, TableData.Row<String> row);

    public TableMeta.LonLatColumns[] getCornersColumns(TableCtx table);
    public TableMeta.LonLatColumns getCenterColumns(TableCtx table);
    public List<String> getExtraColumns();
    public List<String> getEventWorkerList();
    public ZoomType getSmartZoomHint();
    public String getGroup();
    public DrawSymbol getShape(String id);
    public int getSymbolSize(String id);
    public String getColor(String id);
    public String getHighlightedColor(String id);
    public boolean isMultiCoverage();
    public WebPlotRequest.GridOnStatus getGridOn();
    public boolean isMinimalReadout();
    public int getMinWidth();
    public int getMinHeight();
    public boolean getUseBlankPlot();
    public boolean isTreatCatalogsAsOverlays();
    public WorldPt getQueryCenter();
    public FitType getFitType();
}
