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


