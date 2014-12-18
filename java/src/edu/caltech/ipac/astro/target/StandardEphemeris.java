package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;
/**
 * *******************************************************
 * Moving target with standard ephemeris.
 */
public class StandardEphemeris implements Ephemeris, java.io.Serializable {

    /**
     * For moving target with standard ephemeris. @serial
     */
    private final int naifID;

    /**
     * Symbolic Naif name. @serial
     */
    private final String naifName;

    /**
     * Initialization constructor
     */
    public StandardEphemeris( int naifID ) {
        this(naifID, null);
    }

    /**
     * Initialization constructor
     */
    public StandardEphemeris( int naifID, String naifName ) {
        this.naifID = naifID;
        this.naifName = naifName;
    }

    /**
     * Return the NAIF Id for the ephemeris
     */
    public int getNaifID() {
        return naifID;
    }

    /**
     * Return the NAIF Name for the ephemeris
     */
    public String getNaifName() {
        return naifName;
    }

    /**
     * Implementation of the Cloneable interface
     */
    public Object clone() {
        return new StandardEphemeris(naifID, naifName);
    }

    
    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof StandardEphemeris) {
          StandardEphemeris se= (StandardEphemeris)o;
          retval=  (naifID ==se.naifID &&
                    ComparisonUtil.equals(naifName, se.naifName));
       }
       return retval;
    }
}

