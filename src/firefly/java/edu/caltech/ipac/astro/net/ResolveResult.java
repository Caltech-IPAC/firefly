/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;


import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.io.Serializable;

/**
 * Date: Dec 20, 2004
 *
 * @author Trey Roby
 * @version $id:$
 */
public class ResolveResult implements Serializable {

    public static final String V_MAG = "V";
    public static final String B_MAG = "B";

    private final ResolvedWorldPt worldPt;
    private final Resolver resolver;
    private final String objName;
    private String formalName="";
    private String type = "";
    private double magnitudeV = Double.NaN;
    private double magnitudeB = Double.NaN;
    private String spectralType = "";
    private double parallax = Double.NaN;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public ResolveResult(Resolver resolver, String objName, ResolvedWorldPt worldPt) {
        this.resolver= resolver;
        this.objName= objName;
        this.worldPt = worldPt;
    }

//============================================================================

    public ResolvedWorldPt getWorldPt() { return worldPt; }

    public Resolver getResolver() { return resolver; }

    public String getType() {
        return type;
    }

    public String getObjName() { return objName; }

    public void setFormalName(String formalName) { this.formalName= formalName; }
    public String getFormalName() { return formalName; }


    public void setType(String type) {
        this.type = type;
    }

    /**
     * return the V magnitude if exist, or return the B magnitude.
     */
    public double getMagnitude() { return Double.isNaN(getVMagnitude()) ? getBMagnitude() : getVMagnitude(); }

    /**
     * return the type of magnitude return by {@link #getMagnitude()}
     */
    public String getMagBand() { return  Double.isNaN(getVMagnitude()) ? B_MAG : V_MAG; }

    public double getVMagnitude() { return magnitudeV; }
    public void setVMagnitude(double mag) { magnitudeV = mag; }

    public double getBMagnitude() { return magnitudeB; }
    public void setBMagnitude(double mag) { magnitudeB = mag; }

    public String getSpectralType() { return spectralType; }
    public void setSpectralType(String spectralType) { this.spectralType = spectralType; }

    public double getParallax() { return parallax; }
    public void setParallax(double parallax) { this.parallax = parallax; }


}
