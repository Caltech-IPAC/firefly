package edu.caltech.ipac.target;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * <BR>
 * Copyright (C) 1999 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged.
 * <BR>
 * @version $Id: TargetAny.java,v 1.2 2005/12/08 22:31:04 tatianag Exp $
 * @author Xiuqin Wu
 */
public class TargetAny extends Target implements Cloneable, java.io.Serializable {
    // no class variable definitions //

    /**
     * Default constructor
     */
    public TargetAny() {
        super();
    }

    /**
     * Initialization constructor
     */
    public TargetAny( String name) {
        super.setName( name );
    }

    /**
     * Returns 'type' value -- target type: "Fixed Single" (max 32 chars)
     */
    public String getType() {
        return "Any";
    }

    /**
     * Returns 'coords' value -- target coordinates (for user reference) (max 32 chars)
     */
    public String getCoords() {
        return "No coordinates specified";
    }
    /**
     * Returns target equinox entered by user
     */
    public String getCoordSysDescription() {
        return "";
    }

    public Iterator locationIterator() { return new ArrayList(0).iterator(); }

    /**
     * Implementation of the cloneable interface
     */
    public Object clone() {
        TargetAny t = new TargetAny( super.getName());
        return t;
    }

    public boolean equals(Object o) {
        return o instanceof TargetAny;
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
