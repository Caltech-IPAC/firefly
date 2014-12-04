package edu.caltech.ipac.hydra.ui.finderchart;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.ActiveTabPane;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

// convenience sharing of constants
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.*;
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
            if (field.getName().equals(FD_CAT_BY_BOUNDARY)) {
                if (Boolean.parseBoolean(field.getValue())) {
                    setValue(new Param(FD_CAT_BY_RADIUS, "false"));
                    return;
                }
            }

            if (field.getName().equals(FD_CAT_BY_RADIUS)) {
                if (Boolean.parseBoolean(field.getValue())) {
                    setValue(new Param(FD_CAT_BY_BOUNDARY, "false"));
                    return;
                }
            }
        }

        boolean doCatSearch = Boolean.parseBoolean(getValue(FD_OVERLAY_CAT));
        setCollapsiblePanelVisibility("catalog_options", doCatSearch);

        String sources = getValue(FD_SOURCES);
        sources = sources == null ? "" : sources;

        setVisible(Band.dss_bands.name(), sources.contains(Source.DSS.name()));
        setVisible(Band.iras_bands.name(), sources.contains(Source.IRIS.name()));
        setVisible(Band.twomass_bands.name(), sources.contains(Source.twomass.name()));
        setVisible(Band.wise_bands.name(), sources.contains(Source.WISE.name()));
        setVisible(Band.SDSS_bands.name(), sources.contains(Source.SDSS.name()));

        if ((sources.equals("") || sources.equals(Source.DSS.name())) && doCatSearch) {
            setValue(new Param(FD_OVERLAY_CAT, "false"));
            return;  // event will cause redo of doCheck.
        } else {
            setVisible(Radius.iras_radius.name(), sources.contains(Source.IRIS.name()));
            setVisible(Radius.twomass_radius.name(), sources.contains(Source.twomass.name()));
            setVisible(Radius.wise_radius.name(), sources.contains(Source.WISE.name()));
            setVisible(Radius.sdss_radius.name(), sources.contains(Source.SDSS.name()));
        }

        if (Boolean.parseBoolean(getValue(FD_CAT_BY_BOUNDARY))) {
            setVisible(Radius.iras_radius.name(), false);
            setVisible(Radius.twomass_radius.name(), false);
            setVisible(Radius.wise_radius.name(), false);
            setVisible(Radius.sdss_radius.name(), false);
            setVisible(FD_ONE_TO_ONE, false);
        } else {
            setVisible(FD_ONE_TO_ONE, true);
        }

        ActiveTabPane posUpldTab = getActiveTabPane("POS UPL TabPane");
        int posUpldTabSelIdx = posUpldTab == null ? 0 : posUpldTab.getTabPane().getSelectedIndex();

        if (posUpldTabSelIdx == 1) {
            setVisible(FD_CAT_BY_BOUNDARY, false);
            if (Boolean.parseBoolean(getValue(FD_CAT_BY_BOUNDARY))) {
                setValue(new Param(FD_CAT_BY_RADIUS, "true"));
            }
        } else {
            setVisible(FD_CAT_BY_BOUNDARY, true);
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
