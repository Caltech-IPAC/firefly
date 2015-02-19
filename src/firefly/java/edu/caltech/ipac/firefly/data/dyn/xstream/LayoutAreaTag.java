/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

