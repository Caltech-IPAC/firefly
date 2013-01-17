package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author tatianag
 *         $Id: VoOption.java,v 1.1 2010/09/01 18:35:28 tatianag Exp $
 */
@XStreamAlias("VoOption")
public class VoOption {

    public VoOption() {}

    // xml attribute name
    @XStreamAsAttribute
    protected String name;

    // xml attribute value
    @XStreamAsAttribute
    protected String value;

    public String getName() { return name; }
    public String getValue() { return value; }
}
