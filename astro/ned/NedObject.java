//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class NedObject
//
//--- Description -------------------------------------------------------------
//	Contains all possible data returned by NED for a single astronomical
//	object.  All required fields are stored as properties of the object.
//	Optional fields are stored in a hashtable and may be retrieved using
//	the "NED data type code" as the argument to getOptionalField().
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
import java.util.Hashtable;

/**
 * Contains all possible data returned by NED for a single astronomical
 * object.  All required fields are stored as properties of the object.
 * Optional fields are stored in a hashtable and may be retrieved using
 * the "NED data type code" as the argument to getOptionalField().
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	08/31/98
 * @author	J. Jones / 588
**/
public class NedObject
{
	private String			fName;
	private NedCrossId[]	fCrossIds;
	private int				fNumNotes;
	private int				fNumPhotos;
	private int				fNumRefs;
	private double			fDistance;
	private String			fType;
	private double			fRA;
	private double			fDec;
	private double			fUncMajor;
	private double			fUncMinor;
	private double			fUncAngle;
	private String			fRefCode;
	private Hashtable		fOptionalFields;
	
	/**
	 * Constructs a new NedObject initialized with empty values.
	**/
	public NedObject()
	{
		fName = "";
		fCrossIds = null;
		fNumNotes = 0;
		fNumPhotos = 0;
		fNumRefs = 0;
		fDistance = 0.0;
		fType = "";
		fRA = 0.0;
		fDec = 0.0;
		fUncMajor = 0.0;
		fUncMinor = 0.0;
		fUncAngle = 0.0;
		fRefCode = "";
		fOptionalFields = new Hashtable();
	}
	
	public String getName()
	{
		return fName;
	}
	
	public void setName(String name)
	{
		fName = name;
	}
	
	public NedCrossId[] getCrossIds()
	{
		return fCrossIds;
	}
	
	public void setCrossIds(NedCrossId[] ids)
	{
		fCrossIds = ids;
	}
	
	public int getNumberOfNotes()
	{
		return fNumNotes;
	}
	
	public void setNumberOfNotes(int notes)
	{
		fNumNotes = notes;
	}
	
	public int getNumberOfPhotos()
	{
		return fNumPhotos;
	}
	
	public void setNumberOfPhotos(int photos)
	{
		fNumPhotos = photos;
	}

	public int getNumberOfReferences()
	{
		return fNumRefs;
	}
	
	public void setNumberOfReferences(int refs)
	{
		fNumRefs = refs;
	}
	
	public double getDistanceToSearchCenter()
	{
		return fDistance;
	}
	
	public void setDistanceToSearchCenter(double distance)
	{
		fDistance = distance;
	}
	
	public String getType()
	{
		return fType;
	}
	
	public void setType(String type)
	{
		fType = type;
	}

	public double getRA()
	{
		return fRA;
	}
	
	public void setRA(double ra)
	{
		fRA = ra;
	}
	
	public double getDec()
	{
		return fDec;
	}
	
	public void setDec(double dec)
	{
		fDec = dec;
	}

	public double getUncertaintyMajor()
	{
		return fUncMajor;
	}
	
	public void setUncertaintyMajor(double unc)
	{
		fUncMajor = unc;
	}
	
	public double getUncertaintyMinor()
	{
		return fUncMinor;
	}
	
	public void setUncertaintyMinor(double unc)
	{
		fUncMinor = unc;
	}
	
	public double getUncertaintyAngle()
	{
		return fUncAngle;
	}
	
	public void setUncertaintyAngle(double unc)
	{
		fUncAngle = unc;
	}
	
	public String getReferenceCode()
	{
		return fRefCode;
	}
	
	public void setReferenceCode(String refcode)
	{
		fRefCode = refcode;
	}
	
	public Object getOptionalField(String fieldName)
	{
		return fOptionalFields.get(fieldName);
	}
	
	public void setOptionalField(String fieldName, Object value)
	{
		fOptionalFields.put(fieldName, value);
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
