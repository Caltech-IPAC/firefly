/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.util.ArrayList;
import java.util.List;

// custom converter used (EventWorkerConverter) - no annotations needed within class
@XStreamAlias("EventWorker")
public class EventWorkerTag extends XidBaseTag {

    // xml attribute 'type'
    protected String type;

    // xml attribute 'id?'
    protected String id;

    // xml attribute 'delayTime?'
    protected int delayTime;

    // xml element 'QueryId*'
    protected List<String> queryIds;

    // xml element 'ShortDescription?'
    protected String shortDesc;

    // xml element 'Param*'
    protected List<ParamTag> paramTags;


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

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(String delayTime) {
        try {
            this.delayTime = Integer.parseInt(delayTime);
        } catch (Exception e) {
            this.delayTime = 0;
        }
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

}

