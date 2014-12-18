package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


@XStreamAlias("Access")
public class AccessTag implements Serializable {

    // xml attribute 'includes'
    @XStreamAsAttribute
    protected String includes;

    // xml attribute 'excludes'
    @XStreamAsAttribute
    protected String excludes;


    public String getIncludes() {
        if (includes == null) {
            includes = "";
        }

        return includes;
    }
    public void setIncludes(String value) {
        this.includes = value;
    }


    public String getExcludes() {
        if (excludes == null) {
            excludes = "";
        }

        return excludes;
    }
    public void setExcludes(String value) {
        this.excludes = value;
    }

}

