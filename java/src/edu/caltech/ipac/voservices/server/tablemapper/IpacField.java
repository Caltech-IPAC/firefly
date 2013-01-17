package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

import edu.caltech.ipac.util.DataType;

@XStreamAlias("IpacField")
public class  IpacField implements Serializable {

    public IpacField() {}

    // xml attribute 'name'
    @XStreamAsAttribute
    protected String name;

    @XStreamAsAttribute
    protected String format;


    public String getName() { return name; }

    public String getFormat() { return format; } 

    public String getAsString() {
	return ("IpacField - name="+name);
    }
}