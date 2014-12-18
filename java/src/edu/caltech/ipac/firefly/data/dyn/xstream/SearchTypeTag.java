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

