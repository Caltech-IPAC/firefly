package edu.caltech.ipac.firefly.core.layout;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.Name;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;

/**
 * Date: Jan 17, 2012
 *
 * @author loi
 * @version $Id: LayoutSelector.java,v 1.6 2012/08/03 03:46:51 tatianag Exp $
 */
public class LayoutSelector extends Composite {

    private SimplePanel optionsWrapper = new SimplePanel();
    private EventHub hub;
    private Name selView = null;

    public LayoutSelector() {
        HorizontalPanel fp = new HorizontalPanel();
        fp.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        Label lbl = new Label("View Options:");
        lbl.addStyleName("result-title");
        fp.add(lbl);
        fp.add(optionsWrapper);
        initWidget(fp);
        setVisible(false);
    }

    public void setHub(EventHub hub) {
        this.hub = hub;
//        selView = null;
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                layout();
            }
        });
    }

    public void layout() {
        TablePanel table = hub.getActiveTable();
        optionsWrapper.clear();
        if (table == null) {
            return;
        }

        selView = table.getActiveView();
        selView = selView == null ? getFirstVisibleView(table).getName() : selView;

        HorizontalPanel options = new HorizontalPanel();
        List<TablePanel.View> views = table.getVisibleViews();
        for (TablePanel.View v : views) {
            options.add(GwtUtil.getFiller(5,0));
            options.add(makeImage(v));
        }
        options.add(GwtUtil.getFiller(10,0));
        optionsWrapper.setWidget(options);
        LayoutSelector loSel = Application.getInstance().getLayoutManager().getLayoutSelector();
        if (loSel != null) {
            if (views.size() > 1) {
                setVisible(true);
            } else {
                setVisible(false);
            }
        }
    }

    private Widget makeImage(final TablePanel.View v) {
        Image img = new Image(v.getIcon());
        img.setSize("24px", "24px");
        if (v.getName().equals(selView)) {
            img.addStyleName("selected-view");
            return img;
        } else {
            Widget w = GwtUtil.makeImageButton(img, v.getShortDesc(), new ClickHandler(){
                public void onClick(ClickEvent event) {
                    selView = v.getName();
                    TablePanel table = hub.getActiveTable();
                    table.switchView(selView);
                    layout();
                }
            });
            w.addStyleName("selectable-view");
            return w;
        }
    }

    private TablePanel.View getFirstVisibleView(TablePanel table) {
        List<TablePanel.View> views = table.getViews();
        for (TablePanel.View v : views) {
            if (!v.isHidden()) {
                return v;
            }
        }
        return null;
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

