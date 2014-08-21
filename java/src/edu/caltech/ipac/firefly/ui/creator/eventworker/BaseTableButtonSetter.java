package edu.caltech.ipac.firefly.ui.creator.eventworker;

import com.google.gwt.user.client.ui.FocusWidget;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: BaseEventWorker.java,v 1.14 2012/09/21 23:35:38 roby Exp $
 */
public abstract class BaseTableButtonSetter extends BaseEventWorker {
    boolean buttonAdded = false;
    private Map<String, Boolean> addedMap;

    protected BaseTableButtonSetter(String type) {
        super(type);
        setEventsByName(Arrays.asList(EventHub.ON_TABLE_ADDED, EventHub.ON_TABLE_SHOW));
    }

    protected void handleEvent(WebEvent ev) {
        List<String> sources = getQuerySources();

        if (sources == null || sources.size() == 0) return;

        if (addedMap == null) {
            addedMap = new HashMap<String, Boolean>(sources.size());
            for(String s : sources) {
                addedMap.put(s, false);
            }
        }

        TablePanel table;
        if (ev.getData() instanceof TablePanel) {
            table = (TablePanel) ev.getData();
        } else {
            table = getEventHub().getActiveTable();
        }
        String tblName = table.getName();
        if (sources.contains(tblName)) {
            boolean isAdded = addedMap.get(tblName);
            if (!isAdded) {
                FocusWidget b = makeButton(table);
                table.addToolButton(b, false);
                addedMap.put(tblName, true);
            }
        }
    }

    abstract protected FocusWidget makeButton(TablePanel table);

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
