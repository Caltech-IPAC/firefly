package edu.caltech.ipac.astro.target;


/**
 * Date: Dec 20, 2004
 *
 * @author Trey Roby
 * @version $id:$
 */
public class SimbadAttribute extends TargetAttribute {

    public static final String SIMBAD= "SIMBAD";
    public static final String V_MAG = "V";
    public static final String B_MAG = "B";

    private PositionJ2000 pos;
    private String name;
    private String type = "";
    private double magnitudeV = Double.NaN;
    private double magnitudeB = Double.NaN;
    private String spectralType = "";
    private double parallax = Double.NaN;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public SimbadAttribute(PositionJ2000 pos) {
        super(SIMBAD);
        this.pos = pos;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public PositionJ2000 getPosition() {
        return pos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * return the V magnitude if exist, or return the B magnitude.
     */
    public double getMagnitude() {
        return Double.isNaN(getVMagnitude()) ? getBMagnitude() : getVMagnitude();
    }

    /**
     * return the type of magnitude return by {@link #getMagnitude()}
     */
    public String getMagBand() {
        return  Double.isNaN(getVMagnitude()) ? B_MAG : V_MAG;
    }

    public double getVMagnitude() {
        return magnitudeV;
    }

    public void setVMagnitude(double mag) {
        magnitudeV = mag;
    }

    public double getBMagnitude() {
        return magnitudeB;
    }

    public void setBMagnitude(double mag) {
        magnitudeB = mag;
    }

    public String getSpectralType() {
        return spectralType;
    }

    public void setSpectralType(String spectralType) {
        this.spectralType = spectralType;
    }

    public double getParallax() {
        return parallax;
    }

    public void setParallax(double parallax) {
        this.parallax = parallax;
    }

//============================================================================
//-------------- Methods from TargetAttribute Interface --------------------
//============================================================================

    public Object clone() {
        return new SimbadAttribute(pos);
    }

}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
