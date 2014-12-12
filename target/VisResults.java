package edu.caltech.ipac.target;


import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

/**
 * Visibility windows results from VIS server
 * <BR>
 * Copyright (C) 2001 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged.
 * <BR>
 * @version $Id: VisResults.java,v 1.3 2011/03/03 22:55:49 roby Exp $
 * @author Booth Hartley
 */
public class VisResults extends    TargetAttribute
                        implements Serializable, Cloneable {
	// class variable definitions //

        public static final String VIS_ESTIMATES = "VisEstimates";
	/**
	 * array of visibility windows
	 * @serial
	 */
	private final TimeWin timewin[];

	/**
	 * Bright object avoidance message from VIS server
	 * @serial
	 */
	private final String avoidString;

	/**
	 * Initialization constructor
	 */
	public VisResults(TimeWin[] timewin, String avoidString) {
            super(VIS_ESTIMATES);
	    this.timewin = timewin;
	    this.avoidString = avoidString;
	}

	/**
	 * Returns 'timewin' value -- array of visibility windows
	 */
	public TimeWin[] getTimeWin() {
		return timewin;
	}

	/**
	 * Returns 'avoid_string' value -- 
         * Bright object avoidance message from VIS server
	 */
	public String getAvoidString() {
		return avoidString;
	}

	/**
	 * Perform a deep copy of this object.
	 * @see Cloneable
	 * @return Object reference to the cloned object
	 */
	public Object clone() {
             return new VisResults(timewin, avoidString);
	}

        public boolean equals(Object o) {
           boolean retval= false;
           if (o==this) {
              retval= true;
           }
           else if (o!=null && o instanceof VisResults) {
              VisResults vr= (VisResults)o;
              retval= ComparisonUtil.equals(timewin, vr.timewin) &&
                      ComparisonUtil.equals(avoidString, vr.avoidString);
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
