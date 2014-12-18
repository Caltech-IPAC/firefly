package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FieldDefVisibilityEventWorker extends BaseFormEventWorker {

    List<String> masterList;
    List<String> showList;
    List<String> hideList;

    boolean retainSpace = true;
    private boolean andLogic = true;
    private boolean containsLogic = false;

    FieldDefVisibilityEventWorker(String masters, String shows, String hides, boolean keepSpace) {
        masterList = new ArrayList<String>();
        showList = new ArrayList<String>();
        hideList = new ArrayList<String>();

        retainSpace = keepSpace;

        // parse masters
        String _masters;

        if (masters.toLowerCase().startsWith("contains")) {
            containsLogic = true;
            _masters = masters.substring(8);
        } else if (masters.toLowerCase().startsWith("or")) {
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

        // parse shows
        if (!StringUtils.isEmpty(shows)) {
            String[] arr2 = shows.split(",");
            for (String arrStr : arr2) {
                if (!StringUtils.isEmpty(arrStr)) {
                    showList.add(arrStr);
                }
            }
        }

        // parse hides
        if (!StringUtils.isEmpty(hides)) {
            String[] arr3 = hides.split(",");
            for (String arrStr : arr3) {
                if (!StringUtils.isEmpty(arrStr)) {
                    hideList.add(arrStr);
                }
            }
        }
    }

    private boolean shouldHide() {
        boolean retVal;
        if (andLogic) {
            retVal = true;
        } else {
            retVal = false;
        }

        if (containsLogic) {
            retVal = false;
        }
//
//        List<String> names = getFieldIds();
//
//        for (String item : showList) {
//            if (!names.contains(item)) {
//                return false;
//            }
//        }
//
//        for (String item : hideList) {
//            if (!names.contains(item)) {
//                return false;
//            }
//        }

        for (String e : masterList) {
            String[] eArr = e.split("=");
//            if (eArr.length != 2){
//                continue;
//            }

            String masterId = eArr[0];
            String masterValue = eArr.length < 2 ? "" : eArr[1];

//            if (!names.contains(masterId)) {
//                return false;
//            }

            String mval = getValue((masterId));
            //tg mval = mval == null ? "" : mval;
            if (mval == null) {
                if (containsField(masterId)) {
                    mval = "";
                } else {
                    return false;
                }
            }
            //tg boolean c = mval.equals(masterValue) || (mval.length() > 0 && masterValue.equals("*")) ;
            boolean c =  masterValue.equals("*") || mval.matches(masterValue);
            if (containsLogic) {
                c = masterValue.equals("*") || contains(mval,masterValue);
                retVal = retVal || c;
            } else if (andLogic) {
                retVal = retVal && c;
            } else {
                retVal = retVal || c;
            }
        }

        return retVal;
    }

    private boolean contains(String mval, String masterValue) {
        boolean retval = false;
        for (String item: mval.split(",")) {
            if (item.equals(masterValue)) {
                retval = true;
                break;
            }
        }
        return retval;
    }
    public void bind(final FormHub hub) {
        if (hub != null) {
            addHub(hub);

            WebEventListener wel = new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    if (shouldHide()) {
                        for (String item : showList) {
                            hideItem(item, false);
                        }
                        for (String item : hideList) {
                            hideItem(item, true);
                        }
                    }
                }
            };
            hub.getEventManager().addListener(FormHub.ON_SHOW, wel);
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, wel);
        }
    }

    private void hideItem(String item, boolean hidden) {
        if (retainSpace) {
            setHidden(item, hidden);
        } else {
            setVisible(item, !hidden);
        }
    }

    public void bind(EventHub hub) {
        // n/a
    }
}

