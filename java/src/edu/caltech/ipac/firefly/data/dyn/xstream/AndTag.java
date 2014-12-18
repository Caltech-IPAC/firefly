package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("And")
public class AndTag implements Serializable {

    // xml element 'Condition*'
    @XStreamImplicit
    protected List<ConditionTag> conditionTags;

    // xml element 'Or'
    @XStreamAlias("Or")
    protected OrTag orTag;


    public List<ConditionTag> getConditions() {
        if (conditionTags == null) {
            conditionTags = new ArrayList<ConditionTag>();
        }
        return this.conditionTags;
    }


    public OrTag getOr() {
        return orTag;
    }

    public void setOr(OrTag orTag) {
        this.orTag = orTag;
    }

}

