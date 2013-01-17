package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;
import java.io.Serializable;

@XStreamAlias("VoField")
public class VoField implements Serializable {

    public VoField() {}

    // xml attribute 'name'
    @XStreamAsAttribute
    protected String name;
    
    // xml attribute 'desc'
    @XStreamAsAttribute
    protected String desc;

    // xml attribute 'unit'
    @XStreamAsAttribute
    protected String unit;

    // xml attribute 'ucd'
    @XStreamAsAttribute
    protected String ucd;

    // xml attribute 'utype'
    @XStreamAsAttribute
    protected String utype;


    // xml attribute 'datatype'
    @XStreamAsAttribute
    protected String datatype;

    // xml attribute 'arraysize'
    @XStreamAsAttribute
    protected String arraysize;


    // xml attribute 'defaultvalue'
    @XStreamAsAttribute
    protected String defaultvalue;

    // xml attribute 'width'
    @XStreamAsAttribute
    protected String width;

    @XStreamAsAttribute
    protected boolean optional = false;

    // list of IpacField objects
    // xml element 'IpacField*'
    @XStreamImplicit
    protected List<IpacField> ipacFields;

    

    public String getName() { return name; }
    public String getDesc() { return desc; }
    public String getUnit() { return unit; }
    public String getUcd() { return ucd; }
    public String getUtype() { return utype; }
    public String getDataType() {
        if (datatype != null)
            return datatype;
        else {
            return "char";
        }
    }
    public String getArraySize() {
        if (arraysize == null && getDataType().equals("char")) {
            return "*";
        } else {
            return arraysize;
        }
    }
    public String getDefaultValue() { return defaultvalue; }
    public String getWidth() { return width; }
    public boolean isOptional() { return optional; }

    public List<IpacField> getIpacFields() { return ipacFields; }


    public String getAsString() {
        String retval = "name: "+name+"; ucd="+ucd+"; utype="+utype+"; datatype="+datatype+"; arraysize="+arraysize+"\n";
        if (ipacFields != null) {
            for (IpacField f : ipacFields) {
                retval += f.getAsString();
            }
        }
        return retval;
    }
}