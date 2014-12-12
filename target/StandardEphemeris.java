package edu.caltech.ipac.target;

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
