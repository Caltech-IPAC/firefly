/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.panels.user;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.Arrays;
import java.util.Set;

/**
 * 
 * @author tatianag
 * @version $Id: PreferencePanel.java,v 1.9 2012/04/05 23:10:40 tatianag Exp $
 */
public class PreferencePanel extends Composite {

    private static String PROP_BASE = "PrefGroup";
    private final TabPane<Widget> tabs = new TabPane<Widget>();
    private final PrefGroupPanel [] groupPanels;
    private int lastTab = -1;
    private boolean hasViewAll = false;

    VerticalPanel panel = new VerticalPanel();
    Grid grid = new Grid();

    WebEventListener eventListener = new WebEventListener() {
        public void eventNotify(WebEvent ev) {
                populatePreferences();
        }
    };

    public PreferencePanel() {
        //populatePreferences();
        String[] items = WebProp.getItems(PROP_BASE);
        groupPanels = new PrefGroupPanel[items.length];
        int idx = 0;
        for (String s : items) {
            // construct tab
            String title = WebProp.getTitle(PROP_BASE+"."+s);
            groupPanels[idx] = new PrefGroupPanel(PROP_BASE+"."+s+".field");
            tabs.addTab(groupPanels[idx], title);
            idx++;
        }
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        panel.add(grid);
        panel.addStyleName("content-panel");
        WebAppProperties wap = Application.getInstance().getProperties();
        hasViewAll = wap.getBooleanProperty(PROP_BASE+".hasViewAll");
        if (hasViewAll) {
            tabs.addTab(panel, "View All");
        }

        // make sure preference view is in sync with the latest updates
        tabs.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            public void onBeforeSelection(BeforeSelectionEvent<Integer> ev) {
                int tabIndex= ev.getItem();
                if (hasViewAll && tabIndex == tabs.getWidgetCount()-1) {
                    populatePreferences();
                }
                lastTab = tabIndex;
            }

        });
        tabs.setSize("99%", "99%");
        tabs.selectTab(0);
        initWidget(tabs);

    }


    public void populatePreferences() {
        if (hasViewAll) {
            Set<String> names = Preferences.getPrefNames();
            if (names == null) return;
            int nPrefs = names.size();
            String [] namesSrt = names.toArray(new String[nPrefs]);
            Arrays.sort(namesSrt);
            grid.resize(nPrefs+1, 2);
            grid.setStyleName("grid");
            grid.setText(0, 0, "Preference Name");
            grid.setText(0, 1, "Preference Value");
            grid.getRowFormatter().setStyleName(0, "grid-row-header");

            for (int i = 0; i<nPrefs; i++) {
                grid.setText(i+1, 0, namesSrt[i]);
                grid.setText(i+1, 1, Preferences.get(namesSrt[i]));
                grid.getRowFormatter().setStyleName(i+1, "grid-row");

            }
        }

        // sync forms with preferences
        for (PrefGroupPanel pgp : groupPanels) {
            pgp.populateForm();
        }
        if (lastTab < 0) {
            tabs.selectTab(0);
        }
    }   

    @Override
    protected void onLoad() {
        super.onLoad();
        populatePreferences();
    }

}
