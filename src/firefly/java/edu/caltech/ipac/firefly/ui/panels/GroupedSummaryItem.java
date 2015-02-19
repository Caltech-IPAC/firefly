/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.user.client.ui.FlexTable;

import java.util.Arrays;


/**
 * Date: Nov 9, 2010
*
* @author loi
* @version $Id: GroupedSummaryItem.java,v 1.3 2010/11/24 01:55:57 loi Exp $
*/
public class GroupedSummaryItem extends SearchSummaryItem {

    public GroupedSummaryItem(String name) {
        setName(name);
        setColumns(Arrays.asList("Name", "Status"));
    }

    @Override
    public void setTitleCol(String titleCol) {
        for (SearchSummaryItem i : getChildren()) {
            i.setTitleCol(titleCol);
        }
    }

    @Override
    public void renderItem(FlexTable table, int row, String... ignoreCols) {
        super.renderItem(table, row, ignoreCols);
    }

    public void setName(String name) {
        setValue("Name", name);
    }

    public String getName() {
        return getValue("Name");
    }

    @Override
    public void checkUpdate() {
        int completed = 0;
        int total = getChildren().size();
        for (SearchSummaryItem ssi : getChildren()) {
            if (ssi.isLoaded()) completed++;
        }
        String status = "<i>" + completed + " of " + total + " completed</i>";
        setLoaded(completed == total);
        setValue("Status", isLoaded() ? "" : status);
    }
}

