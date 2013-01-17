package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.util.ArrayList;
import java.util.List;


// custom converter used (PreviewConverter) - no annotations needed within class
@XStreamAlias("Preview")
public class PreviewTag extends LayoutContentTypeTag {

    // xml attribute 'id'
    protected String id;

    // xml attribute 'align'
    protected String align;

    // xml attribute 'type'
    protected String type;

    // xml attribute 'frameType'
    protected String frameType;

    // xml element 'QueryId*'
    protected List<String> queryIds;

    // xml element 'EventWorkerId*'
    protected List<String> eventWorkerIds;

    // xml element 'Title?'
    protected String title;

    // xml element 'ShortDescription?'
    protected String shortDesc;

    // xml element 'Width?'
    protected String width;

    // xml element 'Height?'
    protected String height;

    // xml element 'TopIndent?'
    protected String topIndent;

    // xml element 'BottomIndent?'
    protected String bottomIndent;

    // xml element 'LeftIndent?'
    protected String leftIndent;

    // xml element 'RightIndent?'
    protected String rightIndent;

    // xml element 'Param*'
    protected List<ParamTag> paramTags;

    // xml element 'Constraints*'
    protected ConstraintsTag constraintsTag;


    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }


    public String getAlign() {
        if (align == null) {
            align = DynUtils.DEFAULT_PREVIEW_ALIGN;
        }

        return align;
    }

    public void setAlign(String value) {
        align = value;
    }


    public String getType() {
        if (type == null) {
            type = DynUtils.DEFAULT_PREVIEW_TYPE;
        }

        return type;
    }

    public void setType(String value) {
        type = value;
    }


    public String getFrameType() {
        if (frameType == null) {
            frameType = DynUtils.DEFAULT_PREVIEW_FRAME_TYPE;
        }

        return frameType;
    }

    public void setFrameType(String value) {
        frameType = value;
    }


    public List<String> getQueryIds() {
        return queryIds;
    }

    public void setQueryIds(List<String> value) {
        queryIds = value;
    }

    public void addQueryId(String value) {
        if (queryIds == null) {
            queryIds = new ArrayList<String>();
        }

        queryIds.add(value);
    }


    public List<String> getEventWorkerIds() {
        return eventWorkerIds;
    }

    public void setEventWorkerIds(List<String> value) {
        eventWorkerIds = value;
    }

    public void addEventWorkerId(String value) {
        if (eventWorkerIds == null) {
            eventWorkerIds = new ArrayList<String>();
        }

        eventWorkerIds.add(value);
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        title = value;
    }


    public String getShortDescription() {
        return shortDesc;
    }

    public void setShortDescription(String value) {
        shortDesc = value;
    }


    public String getWidth() {
        return width;
    }

    public void setWidth(String value) {
        width = value;
    }


    public String getHeight() {
        return height;
    }

    public void setHeight(String value) {
        height = value;
    }


    public String getTopIndent() {
        return topIndent;
    }

    public void setTopIndent(String value) {
        topIndent = value;
    }


    public String getBottomIndent() {
        return bottomIndent;
    }

    public void setBottomIndent(String value) {
        bottomIndent = value;
    }


    public String getLeftIndent() {
        return leftIndent;
    }

    public void setLeftIndent(String value) {
        leftIndent = value;
    }


    public String getRightIndent() {
        return rightIndent;
    }

    public void setRightIndent(String value) {
        rightIndent = value;
    }


    public List<ParamTag> getParams() {
        if (paramTags == null) {
            paramTags = new ArrayList<ParamTag>();
        }
        return paramTags;
    }
    public void setParams(List<ParamTag> values) {
        paramTags = values;
    }
    public void addParam(ParamTag value) {
        if (paramTags == null) {
            paramTags = new ArrayList<ParamTag>();
        }

        paramTags.add(value);
    }

    public ConstraintsTag getConstraints() {
        return this.constraintsTag;
    }

    public void setConstraints(ConstraintsTag value) {
        constraintsTag = value;
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
