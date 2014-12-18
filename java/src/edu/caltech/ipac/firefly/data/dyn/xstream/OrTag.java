package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("Or")
public class OrTag implements Serializable {

    // xml element 'Condition*'
    @XStreamImplicit
    protected List<ConditionTag> conditionTags;

    // xml element 'And'
    @XStreamAlias("And")
    protected AndTag andTag;


    public List<ConditionTag> getConditions() {
        if (conditionTags == null) {
            conditionTags = new ArrayList<ConditionTag>();
        }
        return this.conditionTags;
    }


    public AndTag getAnd() {
        return andTag;
    }

    public void setAnd(AndTag andTag) {
        this.andTag = andTag;
    }

}

