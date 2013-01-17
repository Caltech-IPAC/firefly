package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.Component;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.util.AsyncCallbackGroup;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;


/**
 * Date: Feb 20, 2009
 *
 * @author loi
 * @version $Id: PreviewTabPane.java,v 1.5 2011/10/21 00:14:03 loi Exp $
 */
public class PreviewTabPane extends Component implements StatefulWidget, RequiresResize {

    TablePreviewEventHub eventHub;
    private TabPane<Widget> display;
    private String stateId = "PP";

    public PreviewTabPane() {
        this(null);
    }

    public PreviewTabPane(TablePreviewEventHub hub) {

        eventHub = hub == null ? new TablePreviewEventHub() : hub;

        display = new TabPane<Widget>();
        display.setSize("100%", "100%");
        initWidget(display);

        display.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>(){
            public void onBeforeSelection(BeforeSelectionEvent<Integer> ev) {
                TablePreview cview = getPreviewAtTabIdx(ev.getItem());
                if (cview != null) {
                    cview.onHide();
                }
            }
        });

        display.addSelectionHandler(new SelectionHandler<Integer>(){
            public void onSelection(SelectionEvent<Integer> ev) {
                TablePreview cview = getPreviewAtTabIdx(ev.getSelectedItem());
                if (cview != null) {
                    cview.onShow();
                }
            }
        });

        eventHub.getEventManager().addListener(TablePreviewEventHub.ENABLE_PREVIEW,
                    new WebEventListener(){
                        public void eventNotify(WebEvent ev) {
                            setPreviewEnabled((TablePreview) ev.getSource(), true);
                        }
                    });

        eventHub.getEventManager().addListener(TablePreviewEventHub.DISABLE_PREVIEW,
                    new WebEventListener(){
                        public void eventNotify(WebEvent ev) {
                            setPreviewEnabled((TablePreview) ev.getSource(), false);
                        }
                    });
    }


    public TabPane<Widget> getTabPane() {
        return display;
    }

    public TablePreviewEventHub getEventHub() {
        return eventHub;
    }

    public void bind(TablePanel table) {
        eventHub.bind(table);
    }

    public void unBind(TablePanel table) {
        eventHub.unbind(table);
    }

    public void initPreviewTab(int idx) {
        display.selectTab(idx);
    }

    public void addView(TablePreview preview) {
        boolean v= true;
        if (preview instanceof  AbstractTablePreview) {
            v= ((AbstractTablePreview)preview).isInitiallyVisible();
        }
        addView(preview,v);
    }

    public void addView(TablePreview preview, boolean visible) {
        eventHub.bind(preview);
        preview.bind(eventHub);
        TabPane.Tab t = display.addTab(preview.getDisplay(), preview.getName(), preview.getShortDesc(), false, visible);
    }

    public void removeView(TablePreview preview) {
        eventHub.unbind(preview);
        display.removeTab(preview.getName());
    }

    private void setPreviewEnabled(TablePreview tp, boolean enable) {
        if (enable) {
            display.showTab(display.getTab(tp.getName()));
        } else {
            display.hideTab(display.getTab(tp.getName()));
        }
    }

    public boolean isVisible(TablePreview preview) {
        return GwtUtil.isOnDisplay(preview.getDisplay());
//        TabPane.Tab<Widget> t = display.getSelectedTab();
//        return t != null && t.getName().equals(preview.getName());
    }

    private TablePreview getPreviewAtTabIdx(int idx) {
        if  (idx==-1) return null;
        TabPane.Tab t = display.getVisibleTab(idx);
        if (t != null) {
            for (TablePreview tp : eventHub.getPreviews()) {
                if (tp.getName().equals(t.getName())) {
                    return tp;
                }
            }
        }
        return null;
    }

//====================================================================
//  Implementing StatefulWidget
//====================================================================

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String id) {
        stateId = id;
    }

    public void recordCurrentState(Request request) {
        TabPane.Tab<Widget> tab = display.getSelectedTab();
        if (tab != null) {
            request.setParam(getStateId() + "_selTab", tab.getName());
        }
        for(TablePreview tp : eventHub.getPreviews()) {
            if (isVisible(tp)) {
                if (tp instanceof StatefulWidget) {
                    ((StatefulWidget)tp).recordCurrentState(request);
                }
            }
        }
    }

    public void moveToRequestState(Request request, AsyncCallback callback) {
        AsyncCallbackGroup acgroup = new AsyncCallbackGroup(callback);
        String stab = request.getParam(getStateId() + "_selTab");
        if (!StringUtils.isEmpty(stab)) {
            display.selectTab(display.getTab(stab));
        }
        for(TablePreview tp : eventHub.getPreviews()) {
            if (isVisible(tp)) {
                if (tp instanceof StatefulWidget) {
                    ((StatefulWidget)tp).moveToRequestState(request, acgroup.newCallback());
                }
            }
        }
        if (acgroup.getStackSize() == 0) {
            callback.onSuccess(null);
        }
    }

    public boolean isActive() {
        return GwtUtil.isOnDisplay(this);
    }

    public void onResize() {
        display.onResize();
    }

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