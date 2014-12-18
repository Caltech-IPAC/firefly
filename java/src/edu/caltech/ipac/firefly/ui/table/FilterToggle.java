package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.List;

/**
* Date: 3/4/13
*
* @author loi
* @version $Id: $
*/
public class FilterToggle extends Composite {
    BadgeButton clearButton = new BadgeButton(new Image(TableImages.Creator.getInstance().getClearFilters()));
    BadgeButton showButton = new BadgeButton(new Image(TableImages.Creator.getInstance().getFilterImage()));
    private FilterToggleSupport support;


    public FilterToggle(final FilterToggleSupport support) {
        this.support = support;
        FlowPanel vp = new FlowPanel();
        vp.add(clearButton.getWidget());
        vp.add(showButton.getWidget());
        showButton.setBadgeYOffset(-1);
        GwtUtil.setStyle(clearButton.getWidget(), "cssFloat", "left");
        GwtUtil.setStyle(showButton.getWidget(), "cssFloat", "left");
        initWidget(vp);
        showButton.setTitle("The Filter Panel can be used to remove unwanted data from the search results");
        clearButton.setTitle("Remove all filters");

//        getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

        showButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                support.toggleFilters();
                reinit();
            }
        });

        clearButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                List<String> vals = support.getFilters();
                int selCount = vals == null ? 0 : vals.size();
                if (selCount > 0) {
                    support.clearFilters();
                    reinit();
                } else {
                    support.toggleFilters();
                    reinit();
                }
            }
        });

        reinit();
    }

    public void reinit() {
        List<String> vals = support.getFilters();
        int selCount = vals == null ? 0 : vals.size();
        clearButton.getWidget().setVisible(selCount > 0);
        showButton.setBadgeCount(selCount);
    }

    public static interface FilterToggleSupport {
        public void toggleFilters();
        public List<String> getFilters();
        public void clearFilters();
    }
}
