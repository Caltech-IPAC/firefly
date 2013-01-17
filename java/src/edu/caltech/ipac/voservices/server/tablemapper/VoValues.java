package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 * @author tatianag
 *         $Id: VoValues.java,v 1.1 2010/09/01 18:35:28 tatianag Exp $
 */
@XStreamAlias("VoValues")
public class VoValues {

    public VoValues() {}

    // xml element VoMin
    @XStreamAsAttribute
    protected VoMin voMin;

    // xml element VoMax
    @XStreamAsAttribute
    protected VoMax voMax;

    // xml element VoOption*
    @XStreamImplicit
    protected List<VoOption> voOptions;

    public VoMin getVoMin() { return voMin; }
    public VoMax getVoMax() { return voMax; }
    public List<VoOption> getVoOptions() { return voOptions; }

}
