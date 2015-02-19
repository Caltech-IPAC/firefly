/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;


@XStreamAlias("Constraints")
public class ConstraintsTag implements Serializable {

    // xml element 'Condition'
    @XStreamAlias("Condition")
    protected ConditionTag conditionTag;

    // xml element 'And'
    @XStreamAlias("And")
    protected AndTag andTag;

    // xml element 'Or'
    @XStreamAlias("Or")
    protected OrTag orTag;


    public ConditionTag getCondition() {
        return conditionTag;
    }
    public void setCondition(ConditionTag conditionTag) {
        this.conditionTag = conditionTag;
    }


    public AndTag getAnd() {
        return andTag;
    }
    public void setAnd(AndTag andTag) {
        this.andTag = andTag;
    }


    public OrTag getOr() {
        return orTag;
    }
    public void setOr(OrTag orTag) {
        this.orTag = orTag;
    }

}

