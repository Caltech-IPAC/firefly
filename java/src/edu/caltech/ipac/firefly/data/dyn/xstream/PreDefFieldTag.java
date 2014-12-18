package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import edu.caltech.ipac.util.dd.UIComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("PreDefField")
public class PreDefFieldTag implements UIComponent, Serializable {

    // xml attribute 'id'
    @XStreamAsAttribute
    protected String id;

    // xml element 'Param*'
    @XStreamImplicit
    protected List<ParamTag> paramTags;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }


    public List<ParamTag> getParams() {
        if (paramTags == null) {
            paramTags = new ArrayList<ParamTag>();
        }
        return this.paramTags;
    }

}

