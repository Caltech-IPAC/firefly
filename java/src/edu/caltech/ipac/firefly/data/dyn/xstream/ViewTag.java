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

