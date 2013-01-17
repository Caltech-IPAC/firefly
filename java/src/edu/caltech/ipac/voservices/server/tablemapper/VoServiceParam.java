package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.util.List;

/**
 * @author tatianag
 *         $Id: VoServiceParam.java,v 1.3 2010/09/22 22:29:42 tatianag Exp $
 */
@XStreamAlias("VoServiceParam")
public class VoServiceParam {
    public VoServiceParam() {}

    // xml attribute 'name'
    @XStreamAsAttribute
    protected String name;

    // xml attribute 'desc'
    @XStreamAsAttribute
    protected String desc;

    // xml attribute 'unit'
    @XStreamAsAttribute
    protected String unit;

    // xml attribute 'datatype'
    @XStreamAsAttribute
    protected String datatype;

    // xml attribute 'precision'
    @XStreamAsAttribute
    protected String precision;

    // xml attribute 'value'
    @XStreamAsAttribute
    protected String value;

    // xml attribute 'testvalue'
    @XStreamAsAttribute
    protected String testvalue;


    // xml attribute 'ucd'
    @XStreamAsAttribute
    protected String ucd;

    // xml attribute 'utype'
    @XStreamAsAttribute
    protected String utype;


    // xml attribute 'arraysize'
    @XStreamAsAttribute
    protected String arraysize;

    // xml element VoValues
    @XStreamAlias("VoValues")
    @XStreamAsAttribute
    protected VoValues voValues;


    public String getName() { return name; }
    public String getDesc() { return desc; }
    public String getUnit() { return unit; }
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
    public String getPrecision() { return precision; }
    public String getValue() { return value; }
    public String getTestValue() { return testvalue; }
    public String getUcd() { return ucd; }
    public String getUtype() { return utype; }
    public VoValues getVoValues() { return voValues; }

    public String getAsString() {
        String retval = "VoServiceParam name: "+name+"; unit="+unit+"; datatype="+datatype+
                "; precision="+precision+"; value="+value+
                "; ucd="+ucd+"; utype="+utype+"; arraysize="+arraysize+"\n";
        if (voValues != null) {
            VoMin min = voValues.getVoMin();
            if ( min != null) {
                retval += "    min="+min.getValue()+"; inclusive="+min.isInclusive()+"\n";
            }
            VoMax max = voValues.getVoMax();
            if (max != null) {
                retval += "    max="+max.getValue()+"; inclusive="+max.isInclusive()+"\n";
            }
            List<VoOption> voOptions = voValues.getVoOptions();
            if (voOptions != null) {
                for (VoOption opt : voOptions) {
                    retval += "    opt name="+opt.getName()+"; value="+opt.getValue()+"\n";
                }
            }
        }
        return retval;
    }

}
