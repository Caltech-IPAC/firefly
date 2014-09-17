package edu.caltech.ipac.hydra.ui.finderchart;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.ActiveTabPane;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import org.apache.xpath.operations.Bool;

import java.util.Arrays;
import java.util.List;

/**
* Date: 9/12/14
*
* @author loi
* @version $Id: $
*/
public class FinderChartFormController extends BaseFormEventWorker {

    public void bind(FormHub hub) {
        if (hub != null) {
            addHub(hub);

            WebEventListener wel = new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    Param p = ev.getData() instanceof Param ? (Param) ev.getData() : null;
                    doCheck(p);
                }
            };
            hub.getEventManager().addListener(FormHub.ON_SHOW, wel);
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, wel);
        }
    }

    private void doCheck(Param field) {

        // keep the two radio buttons in sync.
        if (field != null) {
            if (field.getName().equals("catalog_by_img_boundary")) {
                if (Boolean.parseBoolean(field.getValue())) {
                    setValue(new Param("catalog_by_radius", "false"));
                    return;
                }
            }

            if (field.getName().equals("catalog_by_radius")) {
                if (Boolean.parseBoolean(field.getValue())) {
                    setValue(new Param("catalog_by_img_boundary", "false"));
                    return;
                }
            }
        }

        boolean doCatSearch = Boolean.parseBoolean(getValue("overlay_catalog"));
        setCollapsiblePanelVisibility("catalog_options", doCatSearch);

        String sources = getValue("sources");
        sources = sources == null ? "" : sources;

        setVisible("dss_bands", sources.contains("DSS"));
        setVisible("iras_bands", sources.contains("IRIS"));
        setVisible("twomass_bands", sources.contains("twomass"));
        setVisible("wise_bands", sources.contains("WISE"));
        setVisible("SDSS_bands", sources.contains("SDSS"));

        if ((sources.equals("") || sources.equals("DSS")) && doCatSearch) {
            setValue(new Param("overlay_catalog", "false"));
            return;  // event will cause redo of doCheck.
        } else {
            setVisible("iras_radius", sources.contains("IRIS"));
            setVisible("2mass_radius", sources.contains("twomass"));
            setVisible("wise_radius", sources.contains("WISE"));
            setVisible("sdss_radius", sources.contains("SDSS"));
        }

        if (Boolean.parseBoolean(getValue("catalog_by_img_boundary"))) {
            setVisible("iras_radius", false);
            setVisible("2mass_radius", false);
            setVisible("wise_radius", false);
            setVisible("sdss_radius", false);
            setVisible("one_to_one", false);
        } else {
            setVisible("one_to_one", true);
        }

        ActiveTabPane posUpldTab = getActiveTabPane("POS UPL TabPane");
        int posUpldTabSelIdx = posUpldTab == null ? 0 : posUpldTab.getTabPane().getSelectedIndex();

        if (posUpldTabSelIdx == 1) {
            setVisible("catalog_by_img_boundary", false);
            if (Boolean.parseBoolean(getValue("catalog_by_img_boundary"))) {
                setValue(new Param("catalog_by_radius", "true"));
            }
        } else {
            setVisible("catalog_by_img_boundary", true);
        }
    }

    public void bind(EventHub hub) {

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
