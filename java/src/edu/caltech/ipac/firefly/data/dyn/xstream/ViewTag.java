package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.util.ArrayList;
import java.util.List;


// custom converter used (TableConverter) - no annotations needed within class
@XStreamAlias("View")
public class ViewTag extends LayoutContentTypeTag {

    // xml attribute 'id'
    protected String id;

    // xml attribute 'type'
    protected String type;

    // xml element 'QueryId'
    protected String queryId;

    // xml element 'Name'
    protected String name;

    // xml element 'ShortDescription?'
    protected String shortDesc;

    // xml element 'ShortDescription?'
    protected String index;

    // xml element 'Param*'
    protected List<ParamTag> paramTags;


    public String getId() {
        return id;
    }
    public void setId(String value) {
        id = value;
    }


    public String getType() {
        return type;
    }
    public void setType(String value) {
        this.type = value;
    }


    public String getQueryId() {
        return queryId;
    }
    public void setQueryId(String value) {
        this.queryId = value;
    }


    public String getName() {
        return name;
    }
    public void setName(String value) {
        this.name = value;
    }

    public String getShortDescription() {
        return shortDesc;
    }
    public void setShortDescription(String value) {
        this.shortDesc = value;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
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

}
