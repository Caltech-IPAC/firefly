package edu.caltech.ipac.firefly.ui.creator;
/**
 * User: roby
 * Date: Oct 11, 2010
 * Time: 12:34:31 PM
 */


/**
 * @author Trey Roby
 */
public class CommonParams {

    public static final String UNIQUE_KEY_COLUMNS = "UniqueKeyColumns";
    public static final String COLOR = "COLOR";
    public static final String HIGHLIGHTED_COLOR = "HighlightedColor";
    public static final String MATCH_COLOR = "MatchColor";
    public static final String ENABLED = "Enabled";
    public static final String SYMBOL = "SYMBOL";
    public static final String ENABLING_PREFERENCE= "EnablingPreference";
    public static final String TYPE = "Type";
    public static final String MISSION_TYPE = "MissionType";
    public static final String CACHE_KEY= "cacheKey";
    public static final String RESOLVE_PROCESSOR = "resolveProcessor";
    public static final String DO_PLOT = "DoPlot";
    public static final String VIS_ONLY ="VisOnly";
    public static final String ACTIVE_TARGET= "ActiveTarget";
    public static final String TARGET_TYPE= "TargetType";
    public static final String QUERY_CENTER= "QueryCenter";
    public static final String TABLE_ROW= "TableRow";
    public static final String INPUT_FORMAT= "InputFormat";
    public static final String TARGET_COLUMNS= "TargetColumns";
    public static final String ALL= "ALL";

    public static final String USER_CATALOG_FROM_FILE="userCatalogFromFile";

    public enum DataSource { URL, FILE, REQUEST }


    //"NED",  "Simbad", "NEDthenSimbad", "SimbadThenNED", "PTF", "smart"
    public static final String RESOLVERS = "Resolvers";                // values: NED,  Simbad, NEDThenSimbad, SimbadThenNED, PTF, smart
    public static final String EXTRA_PARAMS = "ExtraParams";           // values: name1=value1,name2=value2
    public static final String RED_EXTRA_PARAMS = "RedExtraParams";     // values: name1=value1,name2=value2
    public static final String GREEN_EXTRA_PARAMS = "GreenExtraParams";  // values: name1=value1,name2=value2
    public static final String BLUE_EXTRA_PARAMS = "BlueExtraParams";  // values: name1=value1,name2=value2
    public static final String EVENTS_PARAM = "Events";                // values separated by ','
    public static final String TITLE = "Title";                        // a string
    public static final String NAME = "Name";                           // a string
    public static final String ARG_COLS = "ArgCols";                   // values separated by ','
    public static final String ARG_HEADERS = "ArgHeaders";                   // values separated by ','
    public static final String SEARCH_PROCESSOR_ID = "searchProcessorId";
    public static final String RED_SEARCH_PROCESSOR_ID = "RedSearchProcessorId";
    public static final String GREEN_SEARCH_PROCESSOR_ID = "GreenSearchProcessorId";
    public static final String BLUE_SEARCH_PROCESSOR_ID = "BlueSearchProcessorId";
    public static final String RED_CONTINUE_ON_FAIL = "RedContinueOnFail";
    public static final String GREEN_CONTINUE_ON_FAIL = "GreenContinueOnFail";
    public static final String BLUE_CONTINUE_ON_FAIL  = "BlueContinueOnFail";
    public static final String PLOT_GROUP = "PlotGroup";               //  the name of a PlotWidgetGroup
    public static final String MIN_SIZE = "MinSize";                 // value:
    public static final String CENTER_ON_QUERY_TARGET = "CenterOnQueryTarget"; // center thing image on the query target, true or false
    public static final String STRETCH = "Stretch";                   // value like: min%=1,max%=99,Type=Log
    public static final String COLOR_TABLE_ID = "ColorTableId";       // integer
    public static final String READOUT = "Readout";                   // 
    public static final String LAYOUT_TOOLBAR_OVER= "LayoutToolbarOver";    // the name of a plot
    public static final String LOCK_RELATED= "LockRelated";           // lock related plots - true or false
    public static final String NORTH_UP= "NorthUp";                  // if true, rotate the images to north up
    public static final String IMAGE_SELECTION= "ImageSelection";      // if true, then this plot will change on grouped image selection
    public static final String REMEMBER_PREFS = "RememberPrefs";         // if true, remember the preferences when next image is loaded
    public static final String USE_SCROLL_BARS= "UseScrollBars";      // enabled scroll bars with not expended  - true or false, default true
    public static final String COLOR_PREFERENCE_KEY= "ColorPreferenceKey";   // how to use this parameter is explained in DataViewCreator
    public static final String ZOOM_PREFERENCE_KEY= "ZoomPreferenceKey";     // how to use this parameter is explained in DataViewCreator
    public static final String ZOOM= "Zoom";                          // how to use this parameter is explained in DataViewCreator
    public static final String PLOT_EVENT_WORKERS= "PlotEventWorkers";  // event worker for the plot inside a BasicImageGrid
    public static final String PAGE_SIZE= "PageSize";                 // page size
    public static final String WORLD_PT= "worldPt";                 // serialized world point
    public static final String ENABLE_DEFAULT_COLUMNS= "EnableDefaultColumns";  //  use the default table columns name for coverage
    public static final String CENTER_COLUMNS= "CenterColumns";  // specify center columns for coverage, format: "raColName,decColName"
    public static final String CORNER_COLUMNS= "CornerColumns";  // specify corner columns for coverage, format "raCol1,decCol1,raCol2,decCol2,raCol3,decCol3,raCol4,decCol4"
    public static final String DATA_SOURCE= "DataSource";
    public static final String DATA_COLUMN= "DataColumn";
    public static final String ALL_TABLES= "AllTables";
    public static final String HAS_PREVIEW_DATA= "HasPreviewData";
    public static final String HAS_COVERAGE_DATA= "HasCoverageData";
    public static final String MULTI_COVERAGE= "MultiCoverage";
    public static final String CATALOGS_AS_OVERLAYS= "CatalogsAsOverlays";
    public static final String BLANK= "Blank";
    public static final String SHAPE= "Shape";
    public static final String ENABLE_DETAILS= "EnableDetails";


    public static final String PREVIEW_SOURCE_HEADER = "PreviewSource";
    public static final String PREVIEW_COLUMN_HEADER = "PreviewColumn";

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