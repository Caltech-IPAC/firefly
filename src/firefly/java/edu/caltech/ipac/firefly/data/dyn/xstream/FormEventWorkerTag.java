/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;
import java.util.ArrayList;

import edu.caltech.ipac.firefly.data.dyn.DynUtils;

// custom converter used (FormEventWorkerConverter) - no annotations needed within class
@XStreamAlias("FormEventWorker")
public class FormEventWorkerTag extends XidBaseTag {

    // xml attribute 'type'
    protected String type;

    // xml attribute 'id?'
    protected String id;

    // xml element 'ShortDescription?'
    protected String shortDesc;

    // xml element 'Param*'
    protected List<ParamTag> paramTags;

    // xml element 'FieldDefIds?'
    protected List<ParamTag> fieldDefIds;

    public String getType() {
        if (type == null) {
            return DynUtils.DEFAULT_EVENT_WORKER_TYPE;
        } else {
            return type;
        }
    }
    public void setType(String value) {
        type = value;
    }


    public String getId() {
        return id;
    }
    public void setId(String value) {
        id = value;
    }


    public String getShortDescription() {
        return shortDesc;
    }
    public void setShortDescription(String value) {
        shortDesc = value;
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


    public List<ParamTag> getFieldDefIds() {
        if (fieldDefIds == null) {
            fieldDefIds = new ArrayList<ParamTag>();
        }
        return fieldDefIds;
    }
    public void setFieldDefIds(List<ParamTag> value) {
        fieldDefIds = value;
    }
    public void addFieldDefId(ParamTag value) {
        if (fieldDefIds == null) {
            fieldDefIds = new ArrayList<ParamTag>();
        }

        fieldDefIds.add(value);
    }

}

