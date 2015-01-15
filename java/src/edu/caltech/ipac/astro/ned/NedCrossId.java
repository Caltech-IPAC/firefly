/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class NedCrossId
//
//--- Description -------------------------------------------------------------
//	Object cross-identification information.  Contains a name and type.
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

/**
 * Object cross-identification information.  Contains a name and type.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	08/31/98
 * @author	J. Jones / 588
**/
public class NedCrossId
{
	private String	fName;
	private String	fType;
	
	/**
	 * Constructs a new NedCrossId for given name and type.
	 *
	 * @param	name	cross-identification name
	 * @param	type	cross-identification type
	**/
	public NedCrossId(String name, String type)
	{
		fName = name;
		fType = type;
	}
	
	public String getName()
	{
		return fName;
	}
	
	public void setName(String name)
	{
	    fName = name;
	}
	
	public String getType()
	{
		return fType;
	}
	
	public void setType(String type)
	{
	    fType = type;
	}
}



