package edu.caltech.ipac.firefly.data.dyn;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.dyn.xstream.AccessTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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


    static public Request makeHydraRequest(Request req, String projectId) {
        req.setParam(HYDRA_PROJECT_ID, projectId);
        return req;
    }

    static public boolean checkRoleAccess(AccessTag at) {
        boolean accessFlag = true;

        if (at != null) {
            RoleList rList = Application.getInstance().getLoginManager().getLoginInfo().getRoles();

            String accessIncludes = at.getIncludes();
            if (accessIncludes.length() > 0) {
                accessFlag = accessFlag && rList.hasAccess(accessIncludes.split(","));
            }

            String accessExcludes = at.getExcludes();
            if (accessExcludes.length() > 0) {
                accessFlag = accessFlag && !rList.hasAccess(accessExcludes.split(","));
            }
        }

        return accessFlag;
    }

    static public List<ParamTag> convertParams(Map<String, String> pMap) {
        ArrayList<ParamTag> pList = new ArrayList<ParamTag>();

        for (Map.Entry<String, String> e : pMap.entrySet()) {
            pList.add(new ParamTag(e.getKey(), e.getValue()));
        }

        return pList;
    }

    static public Map<String, String> convertParams(List<ParamTag> pList) {
        HashMap map = new HashMap<String, String>(0);

        for (ParamTag p : pList) {
            String key = p.getKey();
            String value = p.getValue();

            if (map.containsKey(key)) {
                String oldValue = (String) map.get(key);
                // if old value does not contain "=", assume value replacement
                if (!oldValue.contains("=")) {
                    map.put(key, value);

                }
                
                // replace only the inner keyword/value - keep all others as-is
                else {
                    HashMap tmpMap = new HashMap<String, String>(0);

                    // add all old values to tmp map
                    String[] oldArr = oldValue.split(",");
                    for (String oldItem : oldArr) {
                        String[] oldItem2 = oldItem.split("=");
                        if (oldItem2.length == 1) {
                            tmpMap.put(oldItem2[0], null);
                        } else {
                            tmpMap.put(oldItem2[0], oldItem2[1]);
                        }
                    }

                    // add all new values to tmp map
                    String[] newArr = value.split(",");
                    for (String newItem : newArr) {
                        String[] newItem2 = newItem.split("=");
                        if (newItem2.length == 1) {
                            tmpMap.put(newItem2[0], null);
                        } else {
                            tmpMap.put(newItem2[0], newItem2[1]);
                        }
                    }

                    // create new unique string
                    String finalStr = "";
                    Set<String> finalKeys = tmpMap.keySet();
                    int itemCnt = 0;
                    for (String finalKey : finalKeys) {
                        itemCnt++;
                        if (itemCnt > 1) {
                            finalStr += ",";
                        }

                        String finalValue = (String) tmpMap.get(finalKey);
                        if (finalValue == null) {
                            finalStr += finalKey;
                        } else {
                            finalStr += finalKey + "=" + finalValue;
                        }
                    }

                    map.put(key, finalStr);
                }

            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    static public List<Param> convertToParamList(List<ParamTag> pList) {
        ArrayList<Param> paramList = new ArrayList<Param>();

        for (ParamTag p : pList) {
            paramList.add(new Param(p.getKey(), p.getValue()));
        }

        return paramList;
    }

    static public void PopupMessage(String title, String message) {
        final DialogBox p = new DialogBox(false, false);
        //p.setStyleName(INFO_MSG_STYLE);

        if (title != null) {
            p.setTitle(title);
        }

        VerticalPanel vp = new VerticalPanel();
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        vp.setSpacing(10);
        vp.add(new HTML(message));
        vp.add(new HTML(""));

        Button b = new Button("Close");
        vp.add(b);

        p.setWidget(vp);
        p.center();

        b.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                p.hide();
                p.clear();
            }

        });

        p.show();
    }

    // obtain project id from URL instead of from DynRequestHandler - safer
    public static String getProjectIdFromUrl() {
        String urlHash = Window.Location.getHash();
        if (!StringUtils.isEmpty(urlHash)) {
            List<String> hashList = StringUtils.asList(urlHash, "[#&]");
            for (String hashItem : hashList) {
                if (hashItem.contains("=")) {
                    String[] param = hashItem.split("=");
                    if (param[0].equals(DynUtils.HYDRA_PROJECT_ID)) {
                        return param[1];
                    }
                }
            }
        }

        return "";
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
