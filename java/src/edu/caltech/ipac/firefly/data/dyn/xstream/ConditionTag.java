package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


@XStreamAlias("Condition")
public class ConditionTag implements Serializable {

    // xml attribute 'fieldDefId'
    @XStreamAsAttribute
    protected String fieldDefId;

    // xml attribute 'fieldDefVisible'
    @XStreamAsAttribute
    protected String fieldDefVisible;

    // xml attribute 'fieldDefHidden'
    @XStreamAsAttribute
    protected String fieldDefHidden;

    // xml attribute 'value'
    @XStreamAsAttribute
    protected String value;


    public String getFieldDefId() {
        return fieldDefId;
    }
    public void setFieldDefId(String fieldDefId) {
        this.fieldDefId = fieldDefId;
    }


    public String getFieldDefVisible() {
        return fieldDefVisible;
    }
    public void setFieldDefVisible(String fieldDefVisible) {
        this.fieldDefVisible = fieldDefVisible;
    }


    public String getFieldDefHidden() {
        return fieldDefHidden;
    }
    public void setFieldDefHidden(String fieldDefHidden) {
        this.fieldDefHidden = fieldDefHidden;
    }

    
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
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
