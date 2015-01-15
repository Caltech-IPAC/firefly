/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static edu.caltech.ipac.firefly.data.SpacialType.Box;
import static edu.caltech.ipac.firefly.data.SpacialType.Cone;

/**
 * @author tatianag
 */
public class LSSTCatalogSearchUI  implements SearchUI {

    DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);
    private DockLayoutPanel topArea= new DockLayoutPanel(Style.Unit.PX);

    private SpacialSelectUI spacialArea;

    public String getKey() {
        return "LSSTCatalog";
    }

    public String getPanelTitle() {
        return "Search LSST";
    }

    public String getDesc() {
        return "Search LSST Catalogs";
    }

    public Widget makeUI() {
        spacialArea= new SpacialSelectUI(new SpacialSelectUI.TabChange() {
                    public void onTabChange() {
                        adjustSpacialHeight();
                    }
                });
        spacialArea.setSpacialOptions(new HashSet<SpacialType>(Arrays.asList(Cone, Box)), DataSetInfo.DataTypes.CATALOGS);
        mainPanel.setSize("100%", "100%");

        topArea= new DockLayoutPanel(Style.Unit.PX);
        Widget spacialAreaWrap= new SimplePanel(spacialArea);
        topArea.add(spacialAreaWrap);
        mainPanel.addNorth(topArea, spacialArea.getHeightRequired());
        DOM.getParent(topArea.getElement()).addClassName("change-height-transition");
        return mainPanel;
    }

    public boolean validate() {
        return spacialArea.validate();
    }

    public String getSearchTitle() {
        return spacialArea.getSpacialDesc();
    }

    public void makeServerRequest(final AsyncCallback<ServerRequest> cb) {
        final ServerRequest r= new ServerRequest("LSSTCatalogQuery");

        r.setParam(ServerParams.REQUESTED_DATA_SET, "DeepSource");

        spacialArea.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            public void onSuccess(List<Param> l) {
                r.setParams(l);
                cb.onSuccess(r);
            }
        });
    }

    public boolean setServerRequest(ServerRequest request) {
        List<Param> fieldValues= request.getParams();
        spacialArea.setFieldValues(fieldValues);
        return true;
    }

    private void adjustSpacialHeight() {
        int h= spacialArea.getHeightRequired();
        GwtUtil.DockLayout.setWidgetChildSize(topArea, h>160?h:160);
        mainPanel.forceLayout();
    }

}
