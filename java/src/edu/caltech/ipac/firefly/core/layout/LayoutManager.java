package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Date: Nov 1, 2007
 *
 * @author loi
 * @version $Id: LayoutManager.java,v 1.18 2012/05/16 01:39:04 loi Exp $
 */
public interface LayoutManager {

    public static String BANNER_REGION = "banner";
    public static String MENU_REGION = "menu";
    public static String CONTENT_REGION = "content";
    public static String DROPDOWN_REGION = "drop_down";
    public static String RESULT_REGION = "result";
    public static String FOOTER_REGION = "footer";
    public static String DOWNLOAD_REGION = "download";
    public static String SEARCH_TITLE_REGION = "searchTitle";
    public static String SEARCH_DESC_REGION = "searchDesc";
    public static String VIS_MENU_HELP_REGION = "visMenuHelpRegion";
    public static String VIS_TOOLBAR_REGION = "visToolBar";
    public static String VIS_PREVIEW_REGION = "visPreview";
    public static String VIS_READOUT_REGION = "visReadout";
    public static String STATUS = "status";
    public static String APP_ICON_REGION = "appIcon";
    public static String ADDTL_ICON_REGION = "addtlIcon";

    public static String SMALL_ICON_REGION = "smallIcon";
    public static String SMALL_ICON_REGION2 = "smallIcon2";
    public static String USER_INFO_REGION = "user_info";
    public static String QUICK_NAV_REGION = "quick_nav";
    public static String LARGE_ICON_REGION = "largeIcon";

    public Widget getDisplay();
    public Region getRegion(String id);
    public void addRegion(Region region);
    public void setRegion(String id, Region region);
    public List<Region> getRegions();
    public void resize();
    public int getMinHeight();
    public int getMinWidth();

    public boolean isLoading();
    public void setLoading(Boolean isLoading, String msg);

    public void layout(String loadToDiv);
    public LayoutSelector getLayoutSelector();
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
