package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author tatianag
 *         $Id: VoMin.java,v 1.1 2010/09/01 18:35:28 tatianag Exp $
 */
@XStreamAlias("VoMin")
public class VoMin {

    public VoMin() {};

    // xml attribute value
    @XStreamAsAttribute
    protected String value;

    // xml attribute inclusive
    @XStreamAsAttribute
    protected String inclusive;

    public String getValue() { return value; }
    public boolean isInclusive() { return inclusive.equalsIgnoreCase("yes"); }
}
