/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn;

import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DynUtils {

    static public final String HYDRA_COMMAND_NAME_PREFIX = "Hydra_";

    static public final String HYDRA_APP_DATA = "hydraData";
    static public final String HYDRA_PROJECT_ID = "projectId";
    static public final String QUERY_ID = "queryId";
    static public final String SEARCH_NAME = "searchName";

    static public final String DEFAULT_DOWNLOAD_TITLE = "Download Options";
    static public final String DEFAULT_FILE_PREFIX = "";
    static public final String DEFAULT_TITLE_PREFIX = "";
    static public final String DEFAULT_EVENT_WORKER_TYPE = "artifacts";
    static public final String DEFAULT_FIELD_GROUP_TYPE = "default";
    static public final String DEFAULT_FIELD_GROUP_DIRECTION = "vertical";
    static public final int DEFAULT_FIELD_GROUP_LABEL_WIDTH = 100;
    static public final String DEFAULT_FIELD_GROUP_ALIGN = "right";
    static public final String DEFAULT_FIELD_GROUP_TITLE = "";
    static public final String DEFAULT_FIELD_GROUP_TOOLTIP = "";
    static public final double DEFAULT_LAYOUT_AREA_WIDTH = 100;
    static public final double DEFAULT_LAYOUT_AREA_HEIGHT = 100;
    static public final String DEFAULT_PREVIEW_TYPE = "imageViewer";
    static public final String DEFAULT_PREVIEW_ALIGN = "left";
    static public final String DEFAULT_PREVIEW_FRAME_TYPE = "frameOnly";
    static public final String DEFAULT_PROJECT_ITEM_ACTIVE_FLAG = "true";
    static public final String DEFAULT_PROJECT_ITEM_IS_COMMAND_FLAG = "false";
    static public final String DEFAULT_TABLE_TYPE = "selectableTable";
    static public final String DEFAULT_TABLE_ALIGN = "left";

    private static final String INFO_MSG_STYLE = "info-msg";

    static public List<ParamTag> convertParams(Map<String, String> pMap) {
        ArrayList<ParamTag> pList = new ArrayList<ParamTag>();

        for (Map.Entry<String, String> e : pMap.entrySet()) {
            pList.add(new ParamTag(e.getKey(), e.getValue()));
        }

        return pList;
    }





}

