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
