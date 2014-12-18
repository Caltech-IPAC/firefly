//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class NedException
//
//--- Description -------------------------------------------------------------
//	Represents all exceptional conditions related to the NED database that are
//	not covered by regular Java exceptions.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	04/29/98	J. Jones / 588
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

import edu.caltech.ipac.astro.ned.AstroDatabaseException;

/**
 * Represents all exceptional conditions related to the NED database that are
 * not covered by regular Java exceptions.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	04/29/98
 * @author	J. Jones / 588
**/
public class NedException extends AstroDatabaseException
{
	/**
	 * Type of exception.
	**/
	private int fType;

	// Exception types
	public static final int UNKNOWN   = 0;  // unknown error
	public static final int NAME      = 1;  // obj name not recognized by NED name
	public static final int AMBN      = 2;  // ambiguous input name
	public static final int RA        = 3;  // RA is  out [0.0, 360.0]
	public static final int DEC       = 4;  // DEC is out [-90.0, 90.0]
	public static final int RADIUS    = 5;  // radius is out (0.0, 300]
	public static final int JB        = 6;  // equinox starts with J or B
	public static final int EPOCH     = 7;  // epoch is out [1500.0, 2500.0]
	public static final int IAU       = 8;  // unacceptible IAU format
	public static final int NOBJ      = 9;  // no object found
	public static final int EREFC     = 10; // the refcode is not a 19-digit code
	public static final int NOREFC    = 11; // no detailed infomation for the refcode
	public static final int NOREF     = 12; // no reference for given objname

	/**
	 * Constructs a NedException of UNKNOWN type.
	**/
	public NedException()
	{
		super(getMessage(UNKNOWN));

		fType = UNKNOWN; // unknown
	}

	/**
	 * Constructs a NedException with the specified TYPE.
	 *
	 * @param	type	NED exception type
	**/
	public NedException(int type)
	{
		super(getMessage(type));

		fType = type;
	}

	/**
	 * Returns the type of exception.
	**/
	public int getType()
	{
		return fType;
	}

	/**
	 * Returns a description of the exception type.
	 *
	 * @param	type	exception type
	 * @return			description of exception type
	**/
	public static String getMessage(int type)
	{
		String message;

		switch (type)
		{
			case NAME:
				message = "Object name not recognized by NED name interpreter";
				break;

			case AMBN:
				message = "Ambiguous input name";
				break;

			case RA:
				message = "RA is out of range [0.0, 360.0]";
				break;

			case DEC:
				message = "DEC is out of range [-90.0, 90.0]";
				break;

			case RADIUS:
				message = "Radius is out of range [0.0, 300]";
				break;

			case NOBJ:
				message = "No object found";
				break;

			case UNKNOWN:
			default:
				message = "Unknown error";
				break;
	    }

	    return message;
	}
}


