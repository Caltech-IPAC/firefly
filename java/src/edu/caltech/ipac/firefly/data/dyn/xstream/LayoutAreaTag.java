package edu.caltech.ipac.firefly.data.dyn.xstream;

import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// custom converter used (LayoutAreaConverter) - no annotations needed within class
public class LayoutAreaTag implements Serializable {

    public enum LayoutDirection {
        NORTH,
        SOUTH,
        EAST,
        WEST,
        CENTER
    };

    protected LayoutDirection direction;

    // xml attribute 'xid' within North|South|East|West|Center
    protected String id;

    // xml attribute 'layout' within North|South|East|West|Center
    protected String layout;

    // xml attribute 'layoutName' within North|South|East|West|Center
    protected String layoutName;

    // xml attribute 'title' within North|South|East|West|Center
    protected String title;

    // xml attribute 'initialHeight' within North|South
    protected String intialHeight;

    // xml attribute 'initialWidth' within East|West
    protected String intialWidth;

    // xml attribute 'groupId' within North|South|East|West|Center
    protected String groupId;

    // xml attribute 'helpId' within North|South|East|West|Center
    protected String helpId;

    // xml attribute 'tagIt' within North|South|East|West|Center
    protected String tagIt;

    // xml element 'Table*, Preview*'
    protected List<LayoutContentTypeTag> layoutContentTypeTags;

    // xml element 'HtmlLoader'
    protected List<HtmlLoaderTag> htmlLoaders;

    // xml element 'Form*'
    protected List<FormTag> formTags;

    // xml element 'SplitPanel*'
    protected List<SplitPanelTag> splitPanelTags;


    public void setType(LayoutDirection dir) {
        direction = dir;
    }

    public LayoutDirection getType() {
        return direction;
    }

    public String getLayout() {
        return layout;
    }
    public void setLayout(String value) {
        layout = value;
    }

    public String getId() {
        return id;
    }
    public void setId(String value) {
        id = value;
    }

    public String getLayoutName() {
        return layoutName;
    }
    public void setLayoutName(String value) {
        layoutName = value;
    }


    public String getTitle() {
        return title;
    }
    public void setTitle(String value) {
        title = value;
    }


    public double getIntialHeight() {
        if (intialHeight == null)
            intialHeight = DynUtils.DEFAULT_LAYOUT_AREA_HEIGHT +"";

        return Double.parseDouble(intialHeight);
    }
    public void setIntialHeight(String intialHeight) {
        this.intialHeight = intialHeight;
    }


    public double getIntialWidth() {
        if (intialWidth == null)
            intialWidth = DynUtils.DEFAULT_LAYOUT_AREA_WIDTH + "";

        return Double.parseDouble(intialWidth);
    }
    public void setIntialWidth(String intialWidth) {
        this.intialWidth = intialWidth;
    }


    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String value) {
        groupId = value;
    }


    public String getHelpId() {
        return helpId;
    }
    public void setHelpId(String value) {
        helpId = value;
    }

    public String getTagIt() {
        return tagIt;
    }
    public void setTagIt(String value) {
        tagIt = value;
    }


    public List<LayoutContentTypeTag> getLayoutContentTypes() {
        if (layoutContentTypeTags == null) {
            layoutContentTypeTags = new ArrayList<LayoutContentTypeTag>();
        }
        return this.layoutContentTypeTags;
    }
    public void addLayoutContentType(LayoutContentTypeTag lct) {
        if (layoutContentTypeTags == null) {
            layoutContentTypeTags = new ArrayList<LayoutContentTypeTag>();
        }
        layoutContentTypeTags.add(lct);
    }


    public List<FormTag> getForms() {
        if (formTags == null) {
            formTags = new ArrayList<FormTag>();
        }
        return formTags;
    }
    public void addForm(FormTag f) {
        if (formTags == null) {
            formTags = new ArrayList<FormTag>();
        }
        formTags.add(f);
    }


    public List<SplitPanelTag> getSplitPanels() {
        if (splitPanelTags == null) {
            splitPanelTags = new ArrayList<SplitPanelTag>();
        }
        return this.splitPanelTags;
    }
    public void addSplitPanel(SplitPanelTag sp) {
        if (splitPanelTags == null) {
            splitPanelTags = new ArrayList<SplitPanelTag>();
        }
        splitPanelTags.add(sp);
    }

    public List<HtmlLoaderTag> getHtmlLoaders() {
        if (htmlLoaders == null) {
            htmlLoaders = new ArrayList<HtmlLoaderTag>();
        }
        return this.htmlLoaders;
    }

    public void addHtmlLoaders(HtmlLoaderTag htmlLoader) {
        if (htmlLoaders == null) {
            htmlLoaders = new ArrayList<HtmlLoaderTag>();
        }
        htmlLoaders.add(htmlLoader);
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
