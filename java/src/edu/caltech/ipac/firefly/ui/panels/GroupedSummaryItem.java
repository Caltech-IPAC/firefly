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
