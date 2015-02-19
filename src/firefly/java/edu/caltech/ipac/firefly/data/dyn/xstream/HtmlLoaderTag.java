/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.util.dd.UIComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("HtmlLoader")
public class HtmlLoaderTag implements UIComponent, Serializable {

    protected String queryId;
    protected LabelTag Label;

    // xml element 'Param*'
    protected List<ParamTag> paramTags;

    public HtmlLoaderTag() {
        queryId = "";
    }
    public HtmlLoaderTag(String str) {
        queryId = str;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
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

    public LabelTag getLabel() {
        return Label;
    }

    public void setLabel(LabelTag label) {
        Label = label;
    }
}

