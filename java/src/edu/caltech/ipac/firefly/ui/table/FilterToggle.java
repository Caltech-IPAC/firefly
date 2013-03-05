package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;

import java.util.List;

/**
* Date: 3/4/13
*
* @author loi
* @version $Id: $
*/
public class FilterToggle extends Composite {
    Image clearFilters;
    ImageResource showRes = TableImages.Creator.getInstance().getEnumList();
    ImageResource clearRes = TableImages.Creator.getInstance().getClearFilters();
    Label text;
    private TablePanel table;


    public FilterToggle(TablePanel tablePanel) {
        this.table = tablePanel;
        clearFilters = new Image(showRes);
        text = new Label();
        HorizontalPanel vp = new HorizontalPanel();
        vp.add(clearFilters);
        vp.add(text);
        initWidget(vp);
        GwtUtil.makeIntoLinkButton(this);
        setTitle("The Filter Panel can be used to remove unwanted data from the search results");

        getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

        text.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        toggleFilters();
                    }
                });

        clearFilters.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        List<String> vals = table.getTable().getFilters();
                        int selCount = vals == null ? 0 : vals.size();
                        if (selCount > 0) {
                            table.getTable().setFilters(null);
                            table.doFilters();
                        } else {
                            toggleFilters();
                        }

                    }
                });

        reinit();
    }

    private void toggleFilters() {
        if (!table.isActiveView(TableView.NAME)) {
            table.getTable().togglePopoutFilters(this, PopupPane.Align.BOTTOM_LEFT);
//                showNotAllowWarning(FEATURE_ONLY_TABLE);
        } else {
            if (table.getTable().isShowFilters()) {
                table.getTable().showFilters(false);
            } else {
                if (!table.isTableLoaded()) {
                    table.showNotLoadedWarning();
                } else {
                    table.getTable().setFilters(table.getLoader().getUserFilters());
                    table.getTable().showFilters(true);
                }
            }
            reinit();
        }
    }

    public void reinit() {
        List<String> vals = table.getTable().getFilters();
        int selCount = vals == null ? 0 : vals.size();
        if (selCount > 0) {
            clearFilters.setResource(clearRes);
            String f = selCount > 1 ? " filters " : " filter ";
            text.setText(selCount + f + "applied");
        } else {
            clearFilters.setResource(showRes);
            text.setText("Filters");
        }
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
