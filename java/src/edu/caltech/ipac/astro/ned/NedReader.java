//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class NedReader
//
//--- Description -------------------------------------------------------------
//	NedReader provides the ability to search the NED astronomical database
//	and retrieve results as Java objects.
//
//	NedReader is based on the original NED client C code by Xiuqin Wu & the NED team.
//	It provides equivalent capability to the original C code, except for the following:
//	* The reference code expansion feature is not supported.
//	* The Bibligraphic search feature is not supported.
//
//--- Notes -------------------------------------------------------------------
//	PLEASE NOTE:  Positions that are returned from NED via the "server mode"
//	interface (which this client uses) are always in the J2000 equinox.
//	Similarly, position arguments for searchNearPosition must be in J2000.
//
//	See the NED home page, http://nedsrv.ipac.caltech.edu/, for more information
//	about NED.
//
//--- Development History -----------------------------------------------------
//
//	08/31/98	J. Jones / 588
//
//		Original implementation based on existing code in NedClient.java.
//		Code was separated out for decoupling from rest of SEA code.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * NedReader provides the ability to search the <A HREF="http://nedsrv.ipac.caltech.edu/">NED</A>
 * astronomical database and retrieve results as Java objects.
 *
 * <P>NedReader is based on the original NED client C code by Xiuqin Wu & the NED team.
 * It provides equivalent capability to the original C code, except for the following:
 * <BR>* The reference code expansion feature is not supported.
 * <BR>* The Bibligraphic search feature is not supported.
 *
 * <P>PLEASE NOTE:  Positions that are returned from NED via the "server mode"
 * interface (which this client uses) are always in the J2000 equinox.
 * Similarly, position arguments for searchNearPosition must be in J2000.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the NGST SEA project.
 *
 * @version	08/31/98
 * @author	J. Jones / 588
**/
public class NedReader
{
	/**
	 * Name of the NED host.
	**/
	private static final String		NED_HOST = "nedsrv.ipac.caltech.edu";
	
	/**
	 * The NED server mode port number.
	**/	
	private static final int		NED_PORT = 10011;
	
	/**
	 * Input stream for the NED server mode connection.
	**/	
	private BufferedReader			fDataStream;

	/**
	 * Output stream for the NED server mode connection.
	**/	
	private BufferedWriter			fQueryStream;

	/**
	 * Socket for the NED server mode connection.
	**/	
	private Socket					fConnection;

	/**
	 * Connection state of the client - whether or not currently connected to database.
	**/
	private boolean					fConnected;
	
	/**
	 * Creates a new NedReader instance (not yet connected).
	**/
	public NedReader()
	{
		super();

		fDataStream = null;
		fQueryStream = null;
		fConnection = null;
		fConnected = false;
	}

	/**
	 * Connects to the NED database.
	 *
	 * @exception	IOException			 		thrown when IO error occurs
	 * @exception	UnknownHostException		thrown if unable to connect to host
	**/
	public synchronized void connect() throws IOException, UnknownHostException
	{
		if (!isConnected())
		{
			InetAddress host = InetAddress.getByName(NED_HOST);
			fConnection = new Socket(host, NED_PORT);

			fDataStream = new BufferedReader(new InputStreamReader(
					fConnection.getInputStream()));

			fQueryStream = new BufferedWriter(new OutputStreamWriter(
					fConnection.getOutputStream()));

			setConnected(true);
		}
	}

	/**
	 * Disconnects from the NED database.
	 *
	 * @exception	IOException			 thrown when IO error occurs
	**/
	public synchronized void disconnect() throws IOException
	{
		if (fQueryStream != null)
		{
			fQueryStream.close();
		}

		if (fDataStream != null)
		{
			fDataStream.close();
		}

		if (fConnection != null)
		{
			fConnection.close();
		}
		
		if (isConnected())
		{
			setConnected(false);
		}
	}

	/**
	 * Returns true if client is connected to the database, false if not.
	 *
	 * @return	whether or not client is connected to the database
	**/
	public boolean isConnected()
	{
		return fConnected;
	}

	/**
	 * Sets whether or not the client is connected to the database.
	 *
	 * @param flag true if connected, false if not
	**/
	protected void setConnected(boolean flag)
	{
		fConnected = flag;
	}
	
	/**
	 * Searches for objects that match a given object name.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	objectName	search for objects that match this name
	 * @return				NED objects that match the search criteria
	**/
	public NedResultSet searchByName(String objectName)
			throws IOException, NedException
	{
		String query = "obj_byname \"" + objectName + "\"\n";

		sendCommand(query);

		int status = readStatus();
		int objectCount = readObjectCount();

		if (status < 0)
		{
			if (objectCount <= 0)
			{
				throw new NedException(NedException.NAME);
			}
			else
			{
				throw new NedException(NedException.NOBJ);
			}
		}
		else
		{
			return new NedResultSet(readObjects(objectCount));
		}
	}

	/**
	 * Searches for objects that are near a given object
	 * out to a given radius in arcminutes.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	objectName	search for objects that match this name
	 * @param	radius		search out to this radius (arcmin)
	 * @return				NED objects that match the search criteria
	**/
	public NedResultSet searchNearName(String objectName, double radius)
			throws IOException, NedException
	{
		if (radius < 0.0 || radius > 300.0)
		{
			throw new NedException(NedException.RADIUS);
		}

		String query = "obj_nearname \"" + objectName + "\", " + radius + "\n";

		sendCommand(query);

		int status = readStatus();
		int objectCount = readObjectCount();

		if (status < 0)
		{
			if (objectCount <= 0)
			{
				throw new NedException(NedException.NAME);
			}
			else
			{
				throw new NedException(NedException.NOBJ);
			}
		}
		else
		{
			return new NedResultSet(readObjects(objectCount));
		}
	}

	/**
	 * Searches for objects that are near a given position.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	ra_j2000	right-ascension of center position (J2000 equinox)
	 * @param	dec_j2000	declination of center position (J2000 equinox)	 
	 * @param	radius		search out to this radius (arcmin)
	 * @return				NED objects that match the search criteria
	**/
	public NedResultSet searchNearPosition(double ra_j2000, double dec_j2000, double radius)
			throws IOException, NedException
	{
		if (ra_j2000 < 0.0 || ra_j2000 > 360.0)
		{
			throw new NedException(NedException.RA);
		}

		if (dec_j2000 < -90.0 || dec_j2000 > 90.0)
		{
			throw new NedException(NedException.DEC);
		}

		if (radius < 0.0 || radius > 300.0)
		{
			throw new NedException(NedException.RADIUS);
		}

		String query = "obj_nearposn " + ra_j2000 + ", " + dec_j2000 + ", " + radius + "\n";

		sendCommand(query);

		int status = readStatus();
		int objectCount = readObjectCount();

		if (status < 0)
		{
			if (objectCount <= 0)
			{
				throw new NedException(NedException.NAME);
			}
			else
			{
				throw new NedException(NedException.NOBJ);
			}
		}
		else
		{
			return new NedResultSet(readObjects(objectCount));
		}
	}

	/**
	 * Searches for objects that match a given object name where the
	 * object name is in IAU format.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	iauFormatName	search for objects that match this name
	 * @param	style			LIBERAL or STRICT name interpretation
	 * @param	equinox			return objects with this equinox
	 * @return					NED objects that match the search criteria
	**/
	public NedResultSet searchByIauName(
			String iauFormatName,
			char style,
			String equinox) throws IOException, NedException
	{
		char firstChar = Character.toUpperCase(equinox.charAt(0));
		char calendar = ' ';
		double epoch = 0.0;

		if (firstChar == 'J' || firstChar == 'B')
		{
			try
			{
				epoch = Double.valueOf(equinox.substring(1).trim()).doubleValue();
			}
			catch (NumberFormatException e)
			{
				throw new NedException(NedException.EPOCH);
			}

			calendar = firstChar;
		}
		else if (Character.isDigit(firstChar))
		{
			try
			{
				epoch = Double.valueOf(equinox.trim()).doubleValue();
			}
			catch (NumberFormatException e)
			{
				throw new NedException(NedException.EPOCH);
			}

			if (epoch <= 1990)
			{
				calendar = 'B';
			}
			else
			{
				calendar = 'J';
			}
		}
		else
		{
			throw new NedException(NedException.JB);
		}

		if (epoch < 1500.0 || epoch > 2500.0)
		{
			throw new NedException(NedException.EPOCH);
		}

		String query = "obj_iauformat " + iauFormatName + ", " + style +
				", " + calendar + ", " + epoch + "\n";

		sendCommand(query);

		int status = readStatus();
		int objectCount = readObjectCount();

		if (status < 0)
		{
			if (objectCount <= 0)
			{
				throw new NedException(NedException.IAU);
			}
			else
			{
				throw new NedException(NedException.NOBJ);
			}
		}
		else
		{
			// TBD: Return these, too.
			String ra = readString();
			String dec = readString();
			double radius = readDouble();

			return new NedResultSet(readObjects(objectCount));
		}
	}

	/**
	 * Searches for all the possible cross-referenced names
	 * for the object that matches the given object name.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	objectName	search for names for this object
	 * @return				array of Strings that match the search criteria
	**/
	public String[] resolveNames(String objectName)
			throws IOException, NedException
	{
		String query = "name_resolver \"" + objectName + "\"\n";

		sendCommand(query);

		int status = readStatus();
		int objectCount = readObjectCount();

		if (status < 0)
		{
			if (objectCount <= 0)
			{
				throw new NedException(NedException.NAME);
			}
			else
			{
				throw new NedException(NedException.NOBJ);
			}
		}
		else
		{
			String[] crossIds = new String[objectCount];

			// Read the names, store in String array
			for (int i = 0; i < objectCount; ++i)
			{
				crossIds[i] = readString().trim();
			}

			return crossIds;
		}
	}
	
	/**
	 * Sends the specified command string to the NED server.
	 *
	 * @param	command		command string to send to server
	**/
	protected void sendCommand(String command) throws IOException
	{
		fQueryStream.write(command);
		fQueryStream.flush();

		outputDebug("Sent command: " + command);
	}

	/**
	 * Attempts to parse the next line of the input stream as an integer
	 * and returns the result.
	 *
	 * @return	integer extracted from data stream
	**/
	protected int readInteger() throws IOException, NumberFormatException
	{
		return Integer.parseInt(fDataStream.readLine().trim());
	}

	/**
	 * Attempts to parse the next line of the input stream as a double
	 * and returns the result.
	 *
	 * @return	double extracted from data stream
	**/
	protected double readDouble() throws IOException, NumberFormatException
	{
		return (Double.valueOf(fDataStream.readLine().trim())).doubleValue();
	}

	/**
	 * Returns the next line in the input stream as a string.
	 *
	 * @return	next string in the input stream
	**/
	protected String readString() throws IOException
	{
		return fDataStream.readLine();
	}

	/**
	 * Attempts to parse the next line of the input stream as a status
	 * indicator.  Returns -1 if failed to read the status.
	 *
	 * @return	the status indicator, or -1 if failure
	**/
	private int readStatus() throws IOException
	{
		int status = -1; // the default indicating failure
		try
		{
			status = readInteger();
		}
		catch (NumberFormatException e)
		{
			outputError(e.toString());
		}

		return status;
	}

	/**
	 * Attempts to parse the next line of the input stream as the
	 * object count.
	 *
	 * @return	the object count as an integer
	**/
	private int readObjectCount() throws IOException
	{
		int objectCount = 0; // default to zero objects returned
		try
		{
			objectCount = readInteger();
		}
		catch (NumberFormatException e)
		{
			outputError(e.toString());
		}

		return objectCount;
	}

	/**
	 * Parses the input stream and extracts a vector of NedObjects.
	 *
	 * @param	objectCount	number of objects available for reading
	 * @return				vector of NedObjects read
	**/
	private Vector readObjects(int objectCount) throws IOException
	{
		Vector objects = new Vector(objectCount);

		int totalCrossCount = readInteger(); // what is this count for?
		int crossCount = 0;
		
		outputDebug("Receiving " + objectCount + " objects...");

		for (int i = 0; i < objectCount; ++i)
		{
			// Parse data for a single object, populate a new NedObject with the data,
			// and add it to the objects Vector.
			
			NedObject object = new NedObject();

			// Read number of cross-ids
			crossCount = readInteger();

			// Read the cross-identifications, store in array
			NedCrossId[] idents = null;
			if (crossCount > 0)
			{
				idents = new NedCrossId[crossCount];
			}
			for (int j = 0; j < crossCount; ++j)
			{
				idents[j] = new NedCrossId(readString().trim(), readString().trim());
				
				// Name is the first cross-id
				if (j == 0)
				{
					object.setName(idents[j].getName());
				}
			}
			object.setCrossIds(idents);

			// Read all the other object data
			object.setDistanceToSearchCenter(readDouble());
			object.setNumberOfReferences(readInteger());
			object.setNumberOfNotes(readInteger());
			object.setNumberOfPhotos(readInteger());
			object.setType(readString().trim());
			object.setRA(readDouble());
			object.setDec(readDouble());
			object.setUncertaintyMajor(readDouble());
			object.setUncertaintyMinor(readDouble());
			object.setUncertaintyAngle(readDouble());
			object.setReferenceCode(readString().trim());

			// Read the "more" fields, store in hashtable
			int more = readInteger();
			if (more > 0)
			{
				String moreType;
				String moreData;

				Hashtable moreTable = new Hashtable();

				while (more > 0)
				{
					moreType = readString().trim();
					moreData = readString().trim();

					// Add the element to the hashtable
					moreTable.put(moreType, moreData);

					more = readInteger();
				}

				try
				{
					// Fill object's optional fields from more table
					processMoreData(object, moreTable);
				}
				catch (NumberFormatException ex)
				{
					outputError(ex.toString());
				}
			}
			
			// Add the new NedObject to the Vector
			objects.addElement(object);
		}
		
		outputDebug("" + objectCount + " objects received.");

		return objects;
	}

	/**
	 * Fills the OptionalField values for the specified NedObject
	 * using string information from the given hashtable.
	 *
	 * @param	object		values will be stored in this object
	 * @param	moreTable	hashtable that contains the original string data
	**/
	private void processMoreData(NedObject object, Hashtable moreTable) 
			throws NumberFormatException
	{
		String key;
		String data;

		// For each value in moreTable, store as optional field in object
		for (Enumeration enumeration = moreTable.keys(); enumeration.hasMoreElements();)
		{
			key = (String) enumeration.nextElement();
			
			// If the value can be represented as a double
			if (key.equals("BH_EXTIN") 
					|| key.equals("Z") 
					|| key.equals("Z_UNC")
					|| key.equals("HRV") 
					|| key.equals("HRV_UNC")
					|| key.equals("MAG") 
					|| key.equals("DIAM1")
					|| key.equals("DIAM2")
					|| key.equals("SIZE")
					|| key.equals("POP"))
			{
				// Store as Double
				object.setOptionalField(key, Double.valueOf((String) moreTable.get(key)));
			}
			else
			{
				// Store as String
				object.setOptionalField(key, moreTable.get(key));
			}
		}
	}

	/**
	 * Outputs a debug message to the standard debug log.
	 *
	 * @param	message		message to log
	**/
	private void outputDebug(String message)
	{
		//MessageLogger.getInstance().writeDebug(this, message);
		System.out.println("NedReader: " + message);
	}

	/**
	 * Outputs an error message to the standard error log.
	 *
	 * @param	message		message to log
	**/
	private void outputError(String message)
	{
		//MessageLogger.getInstance().writeError(this, message);
		System.out.println("NedReader Error: " + message);
	}

	/**
	 * Tests NedReader by connecting to NED, performing a search, and outputting the results.
	**/
	public static void main(String[] args)
	{
		NedReader instance = new NedReader();

		System.out.println("Connecting to NED...");

		try
		{
			instance.connect();
		}
		catch (Exception e)
		{
			System.err.println(e.toString());
			System.exit(0);
		}

		System.out.println("Connected to NED.");

		// Try a search
		NedResultSet objects = null;
		try
		{
			//objects = instance.searchNearPosition(204.25324304, -29.86628773, 5.0);
			objects = instance.searchByName("M83");
		}
		catch (Exception e)
		{
			System.exit(0);
		}

		// Print object contents to stdout
		System.out.println("OBJECT COUNT: " + objects.size());
		for (Enumeration enumeration = objects.elements(); enumeration.hasMoreElements();)
		{
			System.out.println("OBJECT: " + ((NedObject) enumeration.nextElement()).getName());
		}

		try
		{
			instance.disconnect();
		}
		catch (Exception e)
		{
			System.exit(0);
		}

		System.out.println("Disconnected from NED.");
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
