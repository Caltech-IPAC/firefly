package edu.caltech.ipac.hydra.ui;

import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


public class CollapsiblePanelVisibilityEventWorker extends BaseFormEventWorker {

    List<String> masterList;
    List<String> slaveList;

    private boolean andLogic = true;

    CollapsiblePanelVisibilityEventWorker(String masters, String slaves) {
        masterList = new ArrayList<String>();
        slaveList = new ArrayList<String>();

        // parse masters
        String _masters;
        if (masters.toLowerCase().startsWith("or")) {
            andLogic = false;
            _masters = masters.substring(2);
        } else if (masters.toLowerCase().startsWith("and")) {
            andLogic = true;
            _masters = masters.substring(3);
        } else {
            _masters = masters;
        }

        if (_masters.startsWith("(")) {
            _masters = _masters.substring(1);
        }
        if (_masters.endsWith(")")) {
            _masters = _masters.substring(0, _masters.length() - 1);
        }

        String[] arr = _masters.split(",");
        for (String arrStr : arr) {
            masterList.add(arrStr);
        }

        // parse slaves
        String[] arr2 = slaves.split(",");
        for (String arrStr : arr2) {
            slaveList.add(arrStr);
        }
    }

    private boolean shouldShow() {
        boolean retVal;
        if (andLogic) {
            retVal = true;
        } else {
            retVal = false;
        }

        List<String> names = getFieldIds();

        for (String e : masterList) {
            String[] eArr = e.split("=");
//            if (eArr.length != 2) {
//                continue;
//            }

            String masterId = eArr[0];
            String masterValue = eArr.length < 2 ? "" : eArr[1];


//            if (!names.contains(masterId)) {
//                return false;
//            }
//
            String value = getValue(masterId);
            List<String> valueList = StringUtils.isEmpty(value) ? Arrays.asList("") : StringUtils.asList(value, ",");
            if (andLogic) {
                retVal = retVal && (valueList.contains(masterValue));
            } else {
                retVal = retVal || (valueList.contains(masterValue));
            }
        }

        return retVal;
    }

    public void bind(final FormHub hub) {
        if (hub != null) {
            addHub(hub);

            hub.getEventManager().addListener(FormHub.ON_SHOW, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (shouldShow()) {
                        for (String slave : slaveList) {
                            setCollapsiblePanelVisibility(slave, true);
                        }
                    } else {
                        for (String slave : slaveList) {
                            setCollapsiblePanelVisibility(slave, false);
                        }
                    }
                }
            });
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (shouldShow()) {
                        for (String slave : slaveList) {
                            setCollapsiblePanelVisibility(slave, true);
                        }
                    } else {
                        for (String slave : slaveList) {
                            setCollapsiblePanelVisibility(slave, false);
                        }
                    }
                }
            });
        }
    }

    public void bind(EventHub hub) {
        // n/a
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
