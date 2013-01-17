package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("SearchType")
public class SearchTypeTag implements Serializable {

    // xml attribute 'helpId'
    @XStreamAsAttribute
    protected String helpId;

    // xml attribute 'layoutSelector'
    @XStreamAsAttribute
    protected String layoutSelector;

    // xml element 'Name'
    @XStreamAlias("Name")
    protected String name;

    // xml element 'Title'
    @XStreamAlias("Title")
    protected String title;

    // xml element 'Tooltip?'
    @XStreamAlias("Tooltip")
    protected String tooltip;

    // xml element 'Access?'
    @XStreamAlias("Access")
    protected AccessTag access;

    // xml element 'CommandId?'
    @XStreamAlias("CommandId")
    protected String commandId;

    // xml element 'Form'
    @XStreamAlias("Form")
    protected FormTag formTag;

    // xml element 'Query*'
    @XStreamImplicit
    protected List<QueryTag> queryTags;

    // xml element 'Result'
    @XStreamAlias("Result")
    protected ResultTag resultTag;


    public String getHelpId() {
        return helpId;
    }
    public void setHelpId(String value) {
        this.helpId = value;
    }

    public String getLayoutSelector() {
        return layoutSelector;
    }

    public void setLayoutSelector(String layoutSelector) {
        this.layoutSelector = layoutSelector;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }


    public String getTooltip() {
        return tooltip;
    }
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }


    public AccessTag getAccess() {
        return access;
    }
    public void setAccess(AccessTag value) {
        this.access = value;
    }


    public String getCommandId() {
        return commandId;
    }
    public void setCommandId(String value) {
        this.commandId = value;
    }


    public FormTag getForm() {
        return formTag;
    }
    public void setForm(FormTag value) {
        this.formTag = value;
    }


    public List<QueryTag> getQueries() {
        if (queryTags == null) {
            queryTags = new ArrayList<QueryTag>();
        }
        return this.queryTags;
    }


    public ResultTag getResult() {
        return resultTag;
    }
    public void setResult(ResultTag value) {
        this.resultTag = value;
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
