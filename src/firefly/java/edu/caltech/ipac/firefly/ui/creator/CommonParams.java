/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

    public static final String COLOR = "COLOR";
    public static final String SYMBOL = "SYMBOL";
    public static final String TYPE = "Type";
    public static final String CACHE_KEY= "cacheKey";
    public static final String RESOLVE_PROCESSOR = "resolveProcessor";
    public static final String ALL= "ALL";

    public enum DataSource { URL, FILE, REQUEST }


    //"NED",  "Simbad", "NEDthenSimbad", "SimbadThenNED", "PTF", "smart"
    public static final String TITLE = "Title";                        // a string
    public static final String NAME = "Name";                           // a string
    public static final String SEARCH_PROCESSOR_ID = "searchProcessorId";
    public static final String ZOOM= "Zoom";                          // how to use this parameter is explained in DataViewCreator
    public static final String BLANK= "Blank";
    public static final String URL = "URL";

    // external task launcher params
    public static final String LAUNCHER = "launcher";
    public static final String TASK = "task";
    public static final String TASK_PARAMS = "taskParams";


    public static final String PREVIEW_SOURCE_HEADER = "PreviewSource";
    public static final String PREVIEW_COLUMN_HEADER = "PreviewColumn";

    public static final String TARGET_NAME_KEY = "TargetPanel.field.targetName";
    public static final String SEARCH_DESC_RESOLVER_SUFFIX = "SearchDescResolver";
    public static final String COD_ID = "COD_ID";
    public final static String SPLIT_TOKEN= "--split--";

}

