//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class NedResultSet
//
//--- Description -------------------------------------------------------------
//	 An immutable collection of NedObjects.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	08/31/98	J. Jones / 588
//
//		Original implementation.
//
//--- Warning -----------------------------------------------------------------
//	This software is property of the National Aeronautics and Space
//	Administration.  Unauthorized use or duplication of this software is
//	strictly prohibited.  Authorized users are subject to the following
//	restrictions:
//	*	Neither the author, their corporation, nor NASA is responsible for
//		any consequence of the use of this software.
//	*	The origin of this software must not be misrepresented either by
//		explicit claim or by omission.
//	*	Altered versions of this software must be plainly marked as such.
//	*	This notice may not be removed or altered.
//
//=== End File Prolog =========================================================

package edu.caltech.ipac.astro.ned;

import java.util.Vector;
import java.util.Enumeration;

/**
 * An immutable collection of NedObjects.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	08/31/98
 * @author	J. Jones / 588
**/
public class NedResultSet
{
	/**
	 * The list of NedObjects in the result set.
	**/
	private Vector	fObjects;
	
	/**
	 * Constructs a new result set for the specified vector of objects.
	 *
	 * @param	objects		objects to be contained within the result set
	**/
	public NedResultSet(Vector objects)
	{
		fObjects = objects;
	}
	
	/**
	 * Returns an Enumeration that iterates through all the NedObjects
	 * within the result set.
	 *
	 * @return	Enumeration over all objects in the set
	**/
	public Enumeration elements()
	{
		return fObjects.elements();
	}
	
	/**
	 * Returns the number of objects in the result set.
	 *
	 * @return	number of objects in the set
	**/
	public int size()
	{
		return fObjects.size();
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
