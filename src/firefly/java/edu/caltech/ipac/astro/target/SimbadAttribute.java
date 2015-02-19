/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
